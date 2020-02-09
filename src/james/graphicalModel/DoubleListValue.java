package james.graphicalModel;

import java.util.List;

public class DoubleListValue extends Value<List<Double>> {

    public DoubleListValue(String id, List<Double> value) {
        super(id, value);
    }

    public DoubleListValue(String id, List<Double> value, DeterministicFunction function) {
        super(id, value, function);
    }
}
