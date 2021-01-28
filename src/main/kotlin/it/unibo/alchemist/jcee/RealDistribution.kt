package it.unibo.alchemist.jcee

import it.unibo.alchemist.model.math.RealDistributionUtil
import it.unibo.alchemist.protelis.AlchemistExecutionContext
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.random.RandomGenerator
import org.protelis.lang.datatype.Tuple
import java.lang.IllegalArgumentException

private typealias Key = Triple<RandomGenerator, String, Tuple>

object RealDistributionMaker {

    private val cache: MutableMap<Key, RealDistribution> = mutableMapOf()

    @JvmStatic
    fun realDistribution(context: AlchemistExecutionContext<*>, name: String, parameters: Tuple): RealDistribution =
        cache.getOrPut(Key(context.randomGenerator, name, parameters)) {
            RealDistributionUtil.makeRealDistribution(
                context.randomGenerator,
                name,
                *parameters.toList()
                    .map { (it as? Number)?.toDouble() ?: throw IllegalArgumentException("Wrong parameters: $parameters") }
                    .toDoubleArray()
            )
        }
}
