package james.graphicalModel.types;

import james.graphicalModel.DeterministicFunction;
import james.graphicalModel.swing.DoubleValueEditor;
import james.graphicalModel.swing.IntegerValueEditor;
import james.graphicalModel.types.NumberValue;

import javax.swing.*;

public class IntegerValue extends NumberValue<Integer> {

    public IntegerValue(String id, Integer value) {
        super(id, value);
    }

    public IntegerValue(String id, Integer value, DeterministicFunction function) {
        super(id, value, function);
    }

    /**
     * Constructs an anonymous integer value produced by the given function.
     * @param value
     * @param function
     */
    public IntegerValue(Integer value, DeterministicFunction function) {
        super(null, value, function);
    }

    @Override
    public JComponent getViewer() {
        if (getFunction() == null) {
            return new IntegerValueEditor(this);
        }
        return super.getViewer();
    }
}
