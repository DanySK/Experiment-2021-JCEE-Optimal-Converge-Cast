package it.unibo.alchemist.model.implementations.actions

import it.unibo.alchemist.loader.displacements.Polygon
import it.unibo.alchemist.model.implementations.movestrategies.ChangeTargetOnCollision
import it.unibo.alchemist.model.implementations.movestrategies.speed.ConstantSpeed
import it.unibo.alchemist.model.implementations.routes.PolygonalChain
import it.unibo.alchemist.model.interfaces.*
import org.apache.commons.math3.random.RandomGenerator
import java.lang.IllegalStateException

class RandomTargetInPolygon<T>(
    environment: MapEnvironment<T>,
    node: Node<T>,
    reaction: Reaction<T>,
    speed: Double,
    val positionGenerator: Polygon<GeoPosition>
) : MoveOnMap<T>(
    environment,
    node,
    { current, final -> PolygonalChain(current, final) },
    ConstantSpeed(reaction, speed),
    object : ChangeTargetOnCollision<GeoPosition>({ environment.getPosition(node) }) {
        override tailrec fun chooseTarget() = positionGenerator.stream()
            .findFirst().orElseThrow { IllegalStateException("Bug in Alchemist.") }
    },
) {
    constructor(
        randomGenerator: RandomGenerator,
        environment: MapEnvironment<T>,
        node: Node<T>,
        reaction: Reaction<T>,
        speed: Double,
        polygonCoordinates: List<List<Number>>
    ) : this (environment, node, reaction, speed, Polygon(environment, randomGenerator, 1, polygonCoordinates))

}
