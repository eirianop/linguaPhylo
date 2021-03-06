package lphy.app;

import lphy.core.LPhyParser;
import lphy.graphicalModel.Command;
import lphy.graphicalModel.Value;

import java.util.*;

public class GraphicalLPhyParser implements LPhyParser {

    LPhyParser wrappedParser;
    List<GraphicalModelChangeListener> listeners = new ArrayList<>();

    public GraphicalLPhyParser(LPhyParser parser) {
        wrappedParser = parser;
    }

    @Override
    public Map<String, Value<?>> getDictionary() {
        return wrappedParser.getDictionary();
    }

    @Override
    public void addCommand(Command command) {
        wrappedParser.addCommand(command);
    }

    public Collection<Command> getCommands() {
        return wrappedParser.getCommands();
    }

    @Override
    public void parse(String code) {
        wrappedParser.parse(code);
        notifyListeners();
    }

    @Override
    public Map<String, Set<Class<?>>> getGeneratorClasses() {
        return wrappedParser.getGeneratorClasses();
    }

    @Override
    public List<String> getLines() {
        return wrappedParser.getLines();
    }

    @Override
    public void clear() {
        wrappedParser.clear();
        notifyListeners();
    }

    public void addGraphicalModelChangeListener(GraphicalModelChangeListener listener) {
        listeners.add(listener);
    }

    public void notifyListeners() {
        for (GraphicalModelChangeListener listener : listeners) {
            listener.modelChanged();
        }
    }
}
