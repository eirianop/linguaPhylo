package lphy.core;

import lphy.evolution.tree.TimeTree;
import lphy.evolution.tree.TimeTreeNode;
import lphy.core.distributions.Utils;
import lphy.graphicalModel.*;
import lphy.graphicalModel.types.DoubleValue;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by adru001 on 2/02/20.
 */
public class PhyloMatrixBrownian implements GenerativeDistribution<Map<String, Double>> {

    Value<TimeTree> tree;
    Value<Double> diffusionRate;
    Value<Double> y0;
    RandomGenerator random;

    String treeParamName;
    String diffusionRateParamName;
    String y0RateParam;

    int numStates;

    public PhyloMatrixBrownian(@ParameterInfo(name = "treeMatrix", description = "the variance-covariance structure of a tree.") Value<Double[][]> treeMatrix,
                               @ParameterInfo(name = "diffusionRate", description = "the diffusion rate.") Value<Double> diffusionRate,
                               @ParameterInfo(name = "y0", description = "the value of continuous trait at the root.") Value<Double> y0) {
        this.tree = tree;
        this.diffusionRate = diffusionRate;
        this.y0 = y0;
        this.random = Utils.getRandom();

        treeParamName = getParamName(0);
        diffusionRateParamName = getParamName(1);
        y0RateParam = getParamName(2);
    }

    @Override
    public SortedMap<String, Value> getParams() {
        SortedMap<String, Value> map = new TreeMap<>();
        map.put(treeParamName, tree);
        map.put(diffusionRateParamName, diffusionRate);
        map.put(y0RateParam, y0);
        return map;
    }

    @Override
    public void setParam(String paramName, Value value) {
        if (paramName.equals(treeParamName)) tree = value;
        else if (paramName.equals(diffusionRateParamName)) diffusionRate = value;
        else if (paramName.equals(y0RateParam)) y0 = value;
        else throw new RuntimeException("Unrecognised parameter name: " + paramName);
    }

    public RandomVariable<Map<String, Double>> sample() {

        SortedMap<String, Integer> idMap = new TreeMap<>();
        fillIdMap(tree.value().getRoot(), idMap);

        Map<String, Double> tipValues = new TreeMap<>();

        traverseTree(tree.value().getRoot(), y0, tipValues, diffusionRate.value(), idMap);

        return new RandomVariable<>("x", tipValues, this);
    }

    private void fillIdMap(TimeTreeNode node, SortedMap<String, Integer> idMap) {
        if (node.isLeaf()) {
            Integer i = idMap.get(node.getId());
            if (i == null) {
                int nextValue = 0;
                for (Integer j : idMap.values()) {
                    if (j >= nextValue) nextValue = j + 1;
                }
                idMap.put(node.getId(), nextValue);
            }
        } else {
            for (TimeTreeNode child : node.getChildren()) {
                fillIdMap(child, idMap);
            }
        }
    }

    private void traverseTree(TimeTreeNode node, Value<Double> nodeState, Map<String, Double> tipValues, double diffusionRate, Map<String, Integer> idMap) {
        if (node.isLeaf()) {
            tipValues.put(node.getId(), nodeState.value());
        } else {
            for (TimeTreeNode child : node.getChildren()) {

                double variance = diffusionRate * (node.getAge() - child.getAge());

                //TODO I don't want to do a new on every branch! Should be made efficient :)
                NormalDistribution distribution = new NormalDistribution(nodeState.value(), Math.sqrt(variance));

                double newState = distribution.sample();

                traverseTree(child, new DoubleValue("x", newState), tipValues, diffusionRate, idMap);
            }
        }
    }
}
