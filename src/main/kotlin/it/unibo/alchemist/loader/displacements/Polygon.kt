package it.unibo.alchemist.loader.displacements

import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Position
import org.apache.commons.math3.random.RandomGenerator
import org.locationtech.jts.geom.CoordinateXY
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.impl.CoordinateArraySequence
import kotlin.math.max
import kotlin.math.min
import org.locationtech.jts.geom.Polygon as JTSPolygon

class Polygon<P: Position<P>>(
    environment: Environment<*, P>,
    randomGenerator: RandomGenerator,
    nodeCount: Int,
    coordinates: List<List<Number>>,
) : AbstractRandomDisplacement<P>(environment, randomGenerator, nodeCount) {

    val geometry = GeometryFactory()

    val polygon: JTSPolygon = JTSPolygon(
        LinearRing(
            CoordinateArraySequence(
                coordinates.map {
                    when (it.size) {
                        2 -> CoordinateXY(it[0].toDouble(), it[1].toDouble())
                        else -> throw IllegalArgumentException("Cannot convert $it to a coordinate")
                    }
                }.toTypedArray()
            ),
            geometry
        ),
        emptyArray(),
        geometry
    )

    val boundaries: List<Pair<Double, Double>>

    init {
        val firstPoint: List<Double> = coordinates.first().map { it.toDouble() }
        var accumulator = firstPoint to firstPoint
        for (point in coordinates) {
            val min = point.mapIndexed { index, number -> min(number.toDouble(), accumulator.first[index]) }
            val max = point.mapIndexed { index, number -> max(number.toDouble(), accumulator.second[index]) }
            accumulator = min to max
        }
        boundaries = accumulator.first.zip(accumulator.second)
    }

    override fun indexToPosition(i: Int): P =
        generateSequence {
            val coordinates = boundaries.map { randomDouble(it.first, it.second) }
            val jstEnvelope = CoordinateArraySequence(arrayOf(CoordinateXY(coordinates.first(), coordinates.last())))
            Point(jstEnvelope, geometry)
        }
        .filter { polygon.contains(it) }
        .map { makePosition(it.x, it.y) }
        .first()
}
