package lphy.evolution.birthdeath;

import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.*;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A Birth-death-sampling tree generative distribution
 */
public class BirthDeathSamplingTreeDT implements GenerativeDistribution<TimeTree> {

    private final String diversificationParamName;
    private final String turnoverParamName;

    private Value<Double> diversificationRate;
    private Value<Double> turnover;
    private Value<Double> rho;
    private Value<Double> rootAge;

    BirthDeathSamplingTree wrapped;

    public BirthDeathSamplingTreeDT(@ParameterInfo(name = "diversification", description = "diversification rate.") Value<Double> diversification,
                                    @ParameterInfo(name = "turnover", description = "turnover.") Value<Double> turnover,
                                    @ParameterInfo(name = "rho", description = "the sampling proportion.") Value<Double> rho,
                                    @ParameterInfo(name = "rootAge", description = "the number of taxa.") Value<Double> rootAge
                          ) {

        this.turnover = turnover;
        this.diversificationRate = diversification;

        diversificationParamName = getParamName(0);
        turnoverParamName = getParamName(1);
        setup();
    }


    @GeneratorInfo(name="BirthDeathSampling", description="The Birth-death-sampling tree distribution over tip-labelled time trees.<br>" +
            "Conditioned on root age.")
    public RandomVariable<TimeTree> sample() {

        setup();
        RandomVariable<TimeTree> tree = wrapped.sample();
        return new RandomVariable<>("\u03C8", tree.value(), this);
    }

    private void setup() {
        double denom = Math.abs(1.0 - turnover.value());
        double birth_rate = diversificationRate.value() / denom;
        double death_rate = (turnover.value() * diversificationRate.value()) / denom;

        wrapped =
                new BirthDeathSamplingTree(
                        new Value<>("birthRate", birth_rate),
                        new Value<>("deathRate", death_rate),
                        rho, rootAge);
    }

    @Override
    public double logDensity(TimeTree timeTree) {

        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public SortedMap<String, Value> getParams() {
        SortedMap<String, Value> map = new TreeMap<>();
        map.put(diversificationParamName, diversificationRate);
        map.put(turnoverParamName, turnover);
        map.put(wrapped.rhoParamName, rho);
        map.put(wrapped.rootAgeParamName, rootAge);
        return map;
    }

    @Override
    public void setParam(String paramName, Value value) {
        if (paramName.equals(diversificationParamName)) diversificationRate = value;
        else if (paramName.equals(turnoverParamName)) turnover = value;
        else if (paramName.equals(wrapped.rhoParamName)) rho = value;
        else if (paramName.equals(wrapped.rootAgeParamName)) rootAge = value;
        else throw new RuntimeException("Unrecognised parameter name: " + paramName);
    }

    public Value<Double> getDiversificationRate() {
        return getParams().get(diversificationParamName);
    }

    public Value<Double> getTurnover() {
        return getParams().get(turnoverParamName);
    }

    public Value<Double> getRho() {
        return rho;
    }

    public Value<Double> getRootAge() {
        return rootAge;
    }

    public String toString() {
        return getName();
    }


}
