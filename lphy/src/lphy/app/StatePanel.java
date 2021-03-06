package lphy.app;

import lphy.core.LPhyParser;
import lphy.graphicalModel.RandomVariable;
import lphy.graphicalModel.Value;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StatePanel extends JPanel {

    GraphicalLPhyParser parser;

    List<JLabel> labels = new ArrayList<>();
    List<JComponent> editors = new ArrayList<>();
    GroupLayout layout = new GroupLayout(this);

    boolean includeRandomValues = true;
    boolean includeFixedValues = true;

    public StatePanel(GraphicalLPhyParser parser, boolean includeFixedValues, boolean includeRandomValues) {
        this.parser = parser;

        this.includeFixedValues = includeFixedValues;
        this.includeRandomValues = includeRandomValues;

        setLayout(layout);

        generateComponents();

        parser.addGraphicalModelChangeListener(this::generateComponents);
    }

    void generateComponents() {

        labels.clear();
        editors.clear();
        removeAll();

        ;

        for (Value value : LPhyParser.Utils.getAllValuesFromSinks(parser)) {
            if ((value.isRandom() && includeRandomValues) || (!value.isRandom() && includeFixedValues)) {
                JLabel label = new JLabel(value.getLabel()+":");
                label.setForeground(Color.gray);
                labels.add(label);
                editors.add(value.getViewer());
            }
        }
        GroupLayout.ParallelGroup horizParallelGroup = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
        GroupLayout.ParallelGroup horizParallelGroup2 = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup vertSequentialGroup = layout.createSequentialGroup();
        for (int i = 0; i < labels.size(); i++) {
            horizParallelGroup.addComponent(labels.get(i));
            horizParallelGroup2.addComponent(editors.get(i));
            GroupLayout.ParallelGroup vertParallelGroup = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
            vertParallelGroup.addComponent(labels.get(i));
            vertParallelGroup.addComponent(editors.get(i));
            vertSequentialGroup.addGroup(vertParallelGroup);
            vertSequentialGroup.addGap(2);
        }

        GroupLayout.SequentialGroup horizSequentialGroup = layout.createSequentialGroup();
        horizSequentialGroup.addGroup(horizParallelGroup).addGap(5).addGroup(horizParallelGroup2);

        layout.setHorizontalGroup(horizSequentialGroup);

        layout.setVerticalGroup(vertSequentialGroup);
    }

    private boolean isFixedValue(Value value) {
        return !(value instanceof RandomVariable) && value.getGenerator() == null;
    }
}
