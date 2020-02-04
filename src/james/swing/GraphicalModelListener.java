package james.swing;

import james.graphicalModel.Function;
import james.graphicalModel.GenerativeDistribution;
import james.graphicalModel.Value;

public interface GraphicalModelListener {

    void valueSelected(Value value);

    void generativeDistributionSelected(GenerativeDistribution g);

    void functionSelected(Function f);
}