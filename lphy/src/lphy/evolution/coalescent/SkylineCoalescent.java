package lphy.evolution.coalescent;

import lphy.core.distributions.Utils;
import lphy.evolution.tree.TimeTree;
import lphy.evolution.tree.TimeTreeNode;
import lphy.graphicalModel.*;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

/**
 * A skyline coalescent tree generative distribution.
 * Time-stamped leaves and piecewise constant population function with change points on coalescent events that
 * optionally groups coalescent intervals is implemented.
 * The prior on population sizes and group sizes is handled up-stream in the graphical model.
 */
@Citation(value="Drummond, A. J., Rambaut, A., Shapiro, B, & Pybus, O. G. (2005).\n" +
        "Bayesian coalescent inference of past population dynamics from molecular sequences.\n" +
        "Molecular biology and evolution, 22(5), 1185-1192.",
        year = 2005, firstAuthorSurname = "Drummond", DOI="10.1093/molbev/msi103")
public class SkylineCoalescent implements GenerativeDistribution<TimeTree> {

    private final String thetaParamName;
    private final String groupSizesParamName;
    private final String agesParamName;
    private final String nParamName;
    private final String taxaAgesParamName;
    private Value<Double[]> theta;
    private Value<Integer[]> groupSizes;
    private Value<Integer> n;
    private Value<Double[]> ages;
    private Value<Map<String, Double>> taxaAges;

    RandomGenerator random;

    public SkylineCoalescent(@ParameterInfo(name = "theta", description = "effective population size, one value for" +
            " each group of coalescent intervals, ordered from present to past. Possibly scaled to mutations or" +
            " calendar units. If no groupSizes are specified, then the number of coalescent intervals will be equal" +
            " to the number of population size parameters.") Value<Double[]> theta,
                             @ParameterInfo(name = "groupSizes", description = "A tuple of group sizes. The sum of" +
                                     " this tuple determines the number of coalescent events in the tree and thus the" +
                                     " number of taxa. By default all group sizes are 1 which is equivalent to the" +
                                     " classic skyline coalescent.", optional=true) Value<Integer[]> groupSizes,
                             @ParameterInfo(name = "n", description = "number of taxa.", optional = true) Value<Integer> n,
                             @ParameterInfo(name = "ages", description = "an array of leaf node ages.", optional = true) Value<Double[]> ages,
                             @ParameterInfo(name = "taxaAges", description = "an array of leaf node ages.", optional = true) Value<Map<String, Double>> taxaAges) {
        this.theta = theta;
        this.groupSizes = groupSizes;
        this.n = n;
        this.ages = ages;
        this.taxaAges = taxaAges;
        this.random = Utils.getRandom();

        thetaParamName = getParamName(0);
        groupSizesParamName = getParamName(1);
        nParamName = getParamName(2);
        agesParamName = getParamName(3);
        taxaAgesParamName = getParamName(4);

        int c = (ages == null ? 0 : 1) + (taxaAges == null ? 0 : 1) + (n == null ? 0 : 1);

        if (c > 1) {
            throw new IllegalArgumentException("One one of " + nParamName + ", " + agesParamName + " and " + taxaAgesParamName + " may be specified in " + getName());
        }
        checkDimensions();
    }

    private void checkDimensions() {
        boolean success = true;
        if (n != null && n.value() != n()) {
            success = false;
        }
        if (ages != null && ages.value().length != n()) {
            success = false;
        }
        if (taxaAges != null && taxaAges.value().keySet().size() != n()) {
            success = false;
        }
        if (!success) {
            throw new IllegalArgumentException("The number of theta values must be exactly one less than the number of taxa!");
        }
    }

    private int n() {
        if (groupSizes != null) {
            int sum = 0;
            for (int groupSize : groupSizes.value()) {
                sum += groupSize;
            }
            return sum + 1;
        } else return theta.value().length + 1;
    }

    @GeneratorInfo(name = "SkylineCoalescent", description = "The skyline coalescent distribution over tip-labelled time trees. If no group sizes are specified, then there is one population parameter per coalescent event (as per classic skyline coalescent of Pybus, Rambaut and Harvey 2000)")
    public RandomVariable<TimeTree> sample() {

        TimeTree tree = new TimeTree();

        List<TimeTreeNode> leafNodes = createLeafTaxa(tree);
        List<TimeTreeNode> activeNodes = new ArrayList<>();
        List<TimeTreeNode> leavesToBeAdded = new ArrayList<>();

        double time = 0.0;

        for (TimeTreeNode leaf : leafNodes) {
            if (leaf.getAge() <= time) {
                activeNodes.add(leaf);
            } else {
                leavesToBeAdded.add(leaf);
            }
        }

        leavesToBeAdded.sort(
                (o1, o2) -> Double.compare(o2.getAge(), o1.getAge())); // REVERSE ORDER - youngest age at end of list

        Double[] theta = this.theta.value();
        int thetaIndex = 0;
        int groupIndex = 0;
        int countWithinGroup = 0;
        while ((activeNodes.size() + leavesToBeAdded.size()) > 1) {
            int k = activeNodes.size();

            if (k == 1) {
                time = leavesToBeAdded.get(leavesToBeAdded.size() - 1).getAge();
            } else {

                // draw next time;
                double rate = (k * (k - 1.0)) / (theta[thetaIndex] * 2.0);
                double x = -Math.log(random.nextDouble()) / rate;
                time += x;

                if (leavesToBeAdded.size() > 0 && time > leavesToBeAdded.get(leavesToBeAdded.size() - 1).getAge()) {
                    time = leavesToBeAdded.get(leavesToBeAdded.size() - 1).getAge();
                } else {

                    // do coalescence
                    TimeTreeNode a = activeNodes.remove(random.nextInt(activeNodes.size()));
                    TimeTreeNode b = activeNodes.remove(random.nextInt(activeNodes.size()));

                    TimeTreeNode parent = new TimeTreeNode(time, new TimeTreeNode[]{a, b});
                    activeNodes.add(parent);
                    if (groupSizes != null) {
                        int groupSize = groupSizes.value()[groupIndex];
                        if (countWithinGroup == (groupSize - 1)) {
                            groupIndex += 1;
                            countWithinGroup = 0;
                            thetaIndex += 1;
                        } else {
                            countWithinGroup += 1;
                        }

                    } else {
                        thetaIndex += 1;
                    }
                }
            }

            while (leavesToBeAdded.size() > 0 && leavesToBeAdded.get(leavesToBeAdded.size() - 1).getAge() == time) {
                TimeTreeNode youngest = leavesToBeAdded.remove(leavesToBeAdded.size() - 1);
                activeNodes.add(youngest);
            }
        }

        tree.setRoot(activeNodes.get(0));
        if (thetaIndex != theta.length) {
            throw new AssertionError("Programmer error in indexing the theta array during simulation!");
        }
        if (groupSizes != null && (countWithinGroup != 0 || groupIndex != groupSizes.value().length)) {
            throw new AssertionError("Programmer error in indexing the groupSizes array during simulation." + countWithinGroup + " " + groupIndex + Arrays.toString(groupSizes.value()));
        }

        return new RandomVariable<>("\u03C8", tree, this);
    }

    private List<TimeTreeNode> createLeafTaxa(TimeTree tree) {
        List<TimeTreeNode> leafNodes = new ArrayList<>();

        if (ages != null) {

            Double[] leafAges = ages.value();

            for (int i = 0; i < leafAges.length; i++) {
                TimeTreeNode node = new TimeTreeNode(i + "", tree);
                node.setAge(leafAges[i]);
                leafNodes.add(node);
            }
            return leafNodes;

        } else if (taxaAges != null) {

            Map<String, Double> leafTaxaAges = taxaAges.value();

            for (Map.Entry<String, Double> entry : leafTaxaAges.entrySet()) {
                TimeTreeNode node = new TimeTreeNode(entry.getKey(), tree);
                node.setAge(entry.getValue());
                leafNodes.add(node);
            }
            return leafNodes;

        } else {
            for (int i = 0; i < n(); i++) {
                TimeTreeNode node = new TimeTreeNode(i + "", tree);
                node.setAge(0.0);
                leafNodes.add(node);
            }
            return leafNodes;
        }
    }

    @Override
    public double logDensity(TimeTree timeTree) {

        // TODO!

        return 0.0;
    }

    @Override
    public SortedMap<String, Value> getParams() {
        SortedMap<String, Value> map = new TreeMap<>();
        map.put(thetaParamName, theta);
        if (groupSizes != null) map.put(groupSizesParamName, groupSizes);
        if (n != null) map.put(nParamName, n);
        if (ages != null) map.put(agesParamName, ages);
        if (taxaAges != null) map.put(taxaAgesParamName, taxaAges);
        return map;
    }

    @Override
    public void setParam(String paramName, Value value) {
        if (paramName.equals(thetaParamName)) theta = value;
        else if (paramName.equals(groupSizesParamName)) groupSizes = value;
        else if (paramName.equals(nParamName)) n = value;
        else if (paramName.equals(agesParamName)) ages = value;
        else if (paramName.equals(taxaAgesParamName)) taxaAges = value;
        else throw new RuntimeException("Unrecognised parameter name: " + paramName);
    }

    public String toString() {
        return getName();
    }

    public Value<Double[]> getTheta() {
        return theta;
    }

    public Value<Integer[]> getGroupSizes() {
        return groupSizes;
    }

}
