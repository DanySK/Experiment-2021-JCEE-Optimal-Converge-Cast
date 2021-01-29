@file:Suppress("UNCHECKED_CAST")

package it.unibo.alchemist.loader.export

import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Reaction
import it.unibo.alchemist.model.interfaces.Time
import org.apache.commons.math3.util.MathArrays

class Oracle : Extractor {

    override fun <T : Any> extractData(environment: Environment<T, *>, reaction: Reaction<T>?, time: Time, step: Long): DoubleArray {
        environment as Environment<Any, *>
        val leaders = environment.nodes.filter { it.contains(source) }
        for (leader in leaders) {
            val subjects = environment.reachableNodesOf(leader)
                .filter { it == leader || it.getConcentration(Companion.leader) == leader.id }
            val maxDistance = subjects.asSequence().map { environment.getDistanceBetweenNodes(leader, it) }.maxOrNull() ?: 0
            leader.setConcentration(maxDistanceOracle, maxDistance)
            leader.setConcentration(countOracle, subjects.size)
            val positions = subjects.map { environment.getPosition(it).coordinates }
            val barycenter = positions.reduce(MathArrays::ebeAdd).map { it / positions.size }
            leader.setConcentration(barycenterOracle, barycenter)
        }
        return doubleArrayOf()
    }

    fun <X> Environment<X, *>.reachableNodesOf(center: Node<X>): Iterable<Node<X>> {
        val visited = mutableSetOf<Node<X>>()
        val toVisit = mutableSetOf(center)
        val result = mutableSetOf(center)
        while (toVisit.isNotEmpty()) {
            val iterator = toVisit.iterator()
            val current = iterator.next()
            iterator.remove()
            visited.add(current)
            val neighbors = getNeighborhood(current)
            result.addAll(neighbors)
            toVisit.addAll(neighbors.filter { it !in visited })
        }
        return result
    }

    override fun getNames() = emptyList<String>()

    companion object {
        private val maxDistanceOracle = SimpleMolecule("maxDistanceOracle")
        private val countOracle = SimpleMolecule("countOracle")
        private val barycenterOracle = SimpleMolecule("barycenterOracle")
        private val source = SimpleMolecule("source")
        private val leader = SimpleMolecule("leader")
    }
}