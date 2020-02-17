package james.graphicalModel;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

/**
 * Created by adru001 on 18/12/19.
 */
public class RandomVariable<T> extends Value<T> {

    GenerativeDistribution<T> g;

    public RandomVariable(String name, T value, GenerativeDistribution<T> g) {
        super(name, value);
        this.g = g;
    }

    public GenerativeDistribution<T> getGenerativeDistribution() {
        return g;
    }

    public void print(PrintWriter p) {
        for (Value val : g.getParams().values()) {
            val.print(p);
            p.print("\n");
        }

        p.print(codeString());
    }

    public String codeString() {
        StringBuilder builder = new StringBuilder();
        builder.append(id);
        builder.append(" ~ ");
        builder.append(g.codeString());
        return builder.toString();
    }

    @Override
    public List<GraphicalModelNode> getInputs() {
        return Collections.singletonList(g);
    }
}