package james.graphicalModel.types;

import james.graphicalModel.DeterministicFunction;
import james.graphicalModel.Value;
import james.graphicalModel.swing.DoubleValueEditor;

import javax.swing.*;

public class DoubleValue extends NumberValue<Double> {

    public DoubleValue(String id, Double value) {

        super(id, value);
    }

    /**
     * Constructs an anonymous Double value.
     * @param value
     */
    public DoubleValue(Double value) {

        super(null, value);
    }

    public DoubleValue(String id, Value<Double[]> value) {
        super(id, value.value()[0]);

        if (value.value().length > 1) {
            System.err.println("WARNING: ignoring all but the first element in " + (value.getId()) + " of length " + value.value().length + "!");
        }
    }

    public DoubleValue(String id, Double value, DeterministicFunction<Double> function) {

        super(id, value, function);
    }

    /**
     * Constructs an anonymous Double value and records the function that it was generated by
     * @param value
     * @param function
     */
    public DoubleValue(Double value, DeterministicFunction<Double> function) {

        super(null, value, function);
    }

    @Override
    public JComponent getViewer() {
        if (getGenerator() == null) {
            return new DoubleValueEditor(this);
        }
        return super.getViewer();
    }
}
