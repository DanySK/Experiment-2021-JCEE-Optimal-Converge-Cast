package utils;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;


public class PacketLossDistribution extends AbstractRealDistribution {
    private final double r50, r99, k;
    private static final double K_BASE = Math.log(6792093d/29701);
    private static final double RADIX = 1.0/3;

    public PacketLossDistribution(RandomGenerator randomGenerator, final double r50, final double r99) {
        super(randomGenerator);
        if (r50 >= r99) {
            throw new IllegalStateException(
                "The radius at which packet loss is 50% (" + r50
                    + ") must be smaller than the radius at which packet loss is 99% (" + r99 + ')'
            );
        }
        this.r50 = r50;
        this.r99 = r99;
        k = K_BASE / (this.r99 - this.r50);
    }

    @Override
    public double density(double x) {
        /*
         * derivative in x of (1/(7 * e^(k*(r-x)) + 1))^(1/3)
         * (7 e^(k (r - x)) k)/(3 (1 + 7 e^(k (r - x)))^(4/3))
         */
        final double exp = Math.exp(k * (r50 - x));
        return (7 * exp * k)/(3 * Math.pow(1 + 7 * exp, 4.0/3.0));
    }

    @Override
    public double cumulativeProbability(double x) {
        return Math.pow(1/(7 * Math.exp(k * (r50 - x)) + 1), RADIX);
    }

    @Override
    public double getNumericalMean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getNumericalVariance() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getSupportLowerBound() {
        return 0;
    }

    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public boolean isSupportLowerBoundInclusive() {
        return false;
    }

    @Override
    public boolean isSupportUpperBoundInclusive() {
        return false;
    }

    @Override
    public boolean isSupportConnected() {
        return true;
    }

    public static void main(String[] args) {
        final var r99 = 10e6;
        System.out.println(new PacketLossDistribution(new MersenneTwister(), 0.8*r99, r99).cumulativeProbability(100));
    }
}
