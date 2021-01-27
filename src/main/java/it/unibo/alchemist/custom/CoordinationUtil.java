package it.unibo.alchemist.custom;

import org.protelis.lang.datatype.Tuple;

import it.unibo.alchemist.model.implementations.positions.LatLongPosition;
import it.unibo.alchemist.model.interfaces.GeoPosition;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.protelis.AlchemistExecutionContext;
import java.util.Objects;

public class CoordinationUtil {
    
    public static <P extends Position<P>> double computeDistance(AlchemistExecutionContext<P> context, Tuple target) {
        if (Objects.requireNonNull(target).size() == 2) {
            final Object lat = target.get(0);
            final Object lon = target.get(1);
            if (lat instanceof Number && lon instanceof Number) {
                final var  destination = context.getEnvironmentAccess().makePosition(((Number) lat).doubleValue(), ((Number) lon).doubleValue());
                return context.getDevicePosition().distanceTo(destination);
            }
        }
        throw new IllegalArgumentException("Not a position: " + target);
    }

}
