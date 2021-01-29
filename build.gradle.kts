import java.io.ByteArrayOutputStream
import java.awt.GraphicsEnvironment

plugins {
    application
    kotlin("jvm")
}

repositories {
    mavenCentral()
    /* 
     * The following repositories contain beta features and should be added for experimental mode only
     * 
     * maven("https://dl.bintray.com/alchemist-simulator/Alchemist/")
     * maven("https://dl.bintray.com/protelis/Protelis/")
     */
}
/*
 * Only required if you plan to use Protelis, remove otherwise
 */
sourceSets {
    main {
        resources {
            srcDir("src/main/protelis")
        }
    }
}
dependencies {
    fun alchemist(module: String? = null) = "it.unibo.alchemist:alchemist${if (module == null) "" else "-$module"}:_"
    fun protelis(module: String? = null) = "org.protelis:protelis${if (module == null) "" else "-$module"}:_"
    implementation(alchemist())
    implementation(alchemist("incarnation-protelis"))
    implementation(alchemist("maps"))
    implementation(alchemist("euclidean-geometry"))
    if (!GraphicsEnvironment.isHeadless()) {
        implementation(alchemist("swingui"))
        // implementation(alchemist("fxui"))
    }
    implementation(kotlin("stdlib-jdk8"))
    // For extensions
    implementation(protelis("interpreter"))
    implementation("org.danilopianini:jirf:_")
    implementation("org.jgrapht:jgrapht-core:_")
    implementation("org.locationtech.jts:jts-core:_")
    implementation("org.apache.commons:commons-lang3:_")
}

// Heap size estimation for batches
val maxHeap: Long? by project
val heap: Long = maxHeap ?:
if (System.getProperty("os.name").toLowerCase().contains("linux")) {
    ByteArrayOutputStream().use { output ->
        exec {
            executable = "bash"
            args = listOf("-c", "cat /proc/meminfo | grep MemAvailable | grep -o '[0-9]*'")
            standardOutput = output
        }
        output.toString().trim().toLong() / 1024
    }
        .also { println("Detected ${it}MB RAM available.") }  * 9 / 10
} else {
    // Guess 16GB RAM of which 2 used by the OS
    14 * 1024L
}
val taskSizeFromProject: Int? by project
val taskSize = taskSizeFromProject ?: 1024
val threadCount = maxOf(1, minOf(Runtime.getRuntime().availableProcessors(), heap.toInt() / taskSize ))

val alchemistGroup = "Run Alchemist"
/*
 * This task is used to run all experiments in sequence
 */
val runAllGraphic by tasks.register<DefaultTask>("runAllGraphic") {
    group = alchemistGroup
    description = "Launches all simulations with the graphic subsystem enabled"
}
val runAllBatch by tasks.register<DefaultTask>("runAllBatch") {
    group = alchemistGroup
    description = "Launches all experiments"
}
/*
 * Scan the folder with the simulation files, and create a task for each one of them.
 */
File(rootProject.rootDir.path + "/src/main/yaml").listFiles()
    ?.filter { it.extension == "yml" }
    ?.sortedBy { it.nameWithoutExtension }
    ?.forEach {
        fun basetask(name: String, additionalConfiguration: JavaExec.() -> Unit = {}) = tasks.register<JavaExec>(name) {
            group = alchemistGroup
            description = "Launches graphic simulation ${it.nameWithoutExtension}"
            main = "it.unibo.alchemist.Alchemist"
            classpath = sourceSets["main"].runtimeClasspath
            args("-y", it.absolutePath)
            if (System.getenv("CI") == "true") {
                args("-hl", "-t", "2")
            } else {
                args("-g", "effects/${it.nameWithoutExtension}.aes")
            }
            this.additionalConfiguration()
        }
        val capitalizedName = it.nameWithoutExtension.capitalize()
        val graphic by basetask("run${capitalizedName}Graphic") {
            args("-e", "test.txt" )
        }
        runAllGraphic.dependsOn(graphic)
        val batch by basetask("run${capitalizedName}Batch") {
            description = "Launches batch experiments for $capitalizedName"
            jvmArgs("-XX:+AggressiveHeap")
            maxHeapSize = "${minOf(heap.toInt(), Runtime.getRuntime().availableProcessors() * taskSize * 2)}m"
            File("data").mkdirs()
            args(
                "-e", "data/${it.nameWithoutExtension}",
                "-b",
                "-var", "seed", "packetLoss50_99Ratio", "people",
                "-p", threadCount,
                "-i", 1
            )
        }
        runAllBatch.dependsOn(batch)
    }

