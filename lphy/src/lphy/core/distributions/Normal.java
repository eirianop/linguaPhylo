package lphy.core.distributions;

import lphy.graphicalModel.*;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Normal distribution
 */
public class Normal implements GenerativeDistribution1D<Double> {

    private final String meanParamName;
    private final String sdParamName;

    private Value<Double> mean;
    private Value<Double> sd;

    private RandomGenerator random;

    NormalDistribution normalDistribution;

    public Normal(@ParameterInfo(name = "mean", description = "the mean of the distribution.") Value<Double> mean,
                  @ParameterInfo(name = "sd", description = "the standard deviation of the distribution.") Value<Double> sd) {

        this.mean = mean;
        if (mean == null) throw new IllegalArgumentException("The mean value can't be null!");
        this.sd = sd;
        if (sd == null) throw new IllegalArgumentException("The sd value can't be null!");
        random = Utils.getRandom();

        meanParamName = getParamName(0);
        sdParamName = getParamName(1);
    }

    @GeneratorInfo(name="Normal", description="The normal probability distribution.")
    public RandomVariable<Double> sample() {

        // in case the mean is type integer
        double d =((Number)mean.value()).doubleValue();

        normalDistribution = new NormalDistribution(random, d, sd.value());
        double x = normalDistribution.sample();
        return new RandomVariable<>("x", x, this);
    }

    @Override
    public double density(Double x) {
        return normalDistribution.density(x);
    }

    public Map<String, Value> getParams() {
        SortedMap<String, Value> map = new TreeMap<>();
        map.put(meanParamName, mean);
        map.put(sdParamName, sd);
        return map;
    }

    @Override
    public void setParam(String paramName, Value value) {
        if (paramName.equals(meanParamName)) mean = value;
        else if (paramName.equals(sdParamName)) sd = value;
        else throw new RuntimeException("Unrecognised parameter name: " + paramName);
    }

    public String toString() {
        return getName();
    }

    public Value<Double> getMean() {
        return mean;
    }

    public Value<Double> getSd() {
        return sd;
    }

    private static final Double[] domainBounds = {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
    public Double[] getDomainBounds() {
        return domainBounds;
    }
}