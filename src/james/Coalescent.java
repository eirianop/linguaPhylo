package james;

import james.core.Exp;
import james.graphicalModel.GenerativeDistribution;
import james.graphicalModel.RandomVariable;
import james.graphicalModel.Value;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A Kingman coalescent tree generative distribution
 */
public class Coalescent implements GenerativeDistribution<TimeTree> {

    private Value<Double> theta;
    private Value<Integer> n;

    Random random;

    private Exp exp;

    public Coalescent(Value<Double> theta, Value<Integer> n, Random random) {
        this.theta = theta;
        this.n = n;
        this.random = random;

        exp = new Exp(new Value<>("rate", 1.0), random);
    }

    public RandomVariable<TimeTree> sample() {

        TimeTree tree = new TimeTree();

        List<TimeTreeNode> activeNodes = new ArrayList<>();

        for (int i = 0; i < n.value(); i++) {
            activeNodes.add(new TimeTreeNode(i+"", tree));
        }

        double time = 0.0;
        double theta = this.theta.value();

        while (activeNodes.size() > 1) {
            int k = activeNodes.size();

            TimeTreeNode a = activeNodes.remove(random.nextInt(activeNodes.size()));
            TimeTreeNode b = activeNodes.remove(random.nextInt(activeNodes.size()));

            exp.setRate((theta * 2.0 / (k * (k - 1.0))));
            time += exp.sample().value();

            TimeTreeNode parent = new TimeTreeNode(time, new TimeTreeNode[] {a,b});
            activeNodes.add(parent);
        }

        tree.setRoot(activeNodes.get(0));

        return new RandomVariable<>("\u03C8", tree, this);
    }

    @Override
    public double logDensity(TimeTree timeTree) {

        double[] ages = getInternalNodeAges(timeTree, null);
        Arrays.sort(ages);
        double age = 0;
        int k = timeTree.n();
        double logDensity = 0;
        double theta = this.theta.value();

        for (double age1 : ages) {
            double interval = age1 - age;

            logDensity -= k * (k - 1) * interval / (2 * theta);

            age = age1;
            k -= 1;
        }

        logDensity -= (timeTree.n()-1)*Math.log(theta);

        return logDensity;
    }

    @Override
    public List<Value> getParams() {
        return Arrays.asList(theta, n);
    }

    private double[] getInternalNodeAges(TimeTree timeTree, double[] ages) {
        if (ages == null) ages = new double[timeTree.n()-1];
        if (ages.length != timeTree.n()-1) throw new IllegalArgumentException("Ages array size must be equal to the number of internal nodes in the tree.");
        int i = 0;
        for (TimeTreeNode node : timeTree.nodes) {
            if (!node.isLeaf()) {
                ages[i] = node.getAge();
                i += 1;
            }
        }
        return ages;
    }

    public static void main(String[] args) {

        Random random = new Random();

        Value<Double> thetaExpPriorRate = new Value<>("r", 20.0);
        Exp exp = new Exp(thetaExpPriorRate, random);

        RandomVariable<Double> theta = exp.sample("\u0398");
        Value<Integer> n = new Value<>("n", 20);

        Coalescent coalescent = new Coalescent(theta, n, random);

        RandomVariable<TimeTree> g = coalescent.sample();

        PrintWriter p = new PrintWriter(System.out);
        g.print(p);

    }
}