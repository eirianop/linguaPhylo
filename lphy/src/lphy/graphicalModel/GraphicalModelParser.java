package lphy.graphicalModel;

import lphy.evolution.birthdeath.BirthDeathTree;
import lphy.evolution.birthdeath.BirthDeathTreeDT;
import lphy.evolution.coalescent.Coalescent;
import lphy.evolution.birthdeath.Yule;
import lphy.evolution.alignment.ErrorModel;
import lphy.core.LPhyParser;
import lphy.core.PhyloBrownian;
import lphy.evolution.likelihood.PhyloCTMC;
import lphy.core.commands.Remove;
import lphy.core.distributions.*;
import lphy.core.distributions.Exp;
import lphy.core.functions.*;
import lphy.evolution.substitutionmodel.*;
import lphy.graphicalModel.types.*;
import lphy.app.GraphicalModelChangeListener;
import lphy.app.GraphicalModelListener;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphicalModelParser implements LPhyParser {

    // CURRENT MODEL STATE
    private SortedMap<String, Value<?>> dictionary = new TreeMap<>();

    // HISTORY OF LINES PARSED
    List<String> lines = new ArrayList<>();

    // PARSER STATE
    public Map<String, Set<Class<?>>> genDistDictionary = new TreeMap<>();
    Map<String, Set<Class<?>>> functionDictionary = new TreeMap<>();
    Map<String, Command> commandDictionary = new TreeMap<>();


    List<GraphicalModelChangeListener> listeners = new ArrayList<>();
    List<GraphicalModelListener> gmListeners = new ArrayList<>();

    public GraphicalModelParser() {

        Class<?>[] genClasses = {Normal.class, LogNormal.class, LogNormalMulti.class, Exp.class, Coalescent.class,
                PhyloCTMC.class, PhyloBrownian.class, Dirichlet.class, Gamma.class, DiscretizedGamma.class,
                ErrorModel.class, BirthDeathTree.class, BirthDeathTreeDT.class, Yule.class, Beta.class, Geometric.class, Bernoulli.class};

        for (Class<?> genClass : genClasses) {
            addGenerator(genClass, genDistDictionary);
        }

        Class<?>[] functionClasses = {
                lphy.core.functions.Exp.class, JukesCantor.class, K80.class, F81.class, HKY.class, TN93.class,
                GTR.class, BinaryRateMatrix.class, Newick.class, Rep.class, NTaxa.class, NodeCount.class};

        for (Class<?> functionClass : functionClasses) {
            addGenerator(functionClass, functionDictionary);
        }

        addCommand(new Remove(this));
    }

    public static boolean isCommentLine(String line) {
        return line.trim().startsWith("//");
    }

    private void addGenerator(Class<?> generatorClass, Map<String, Set<Class<?>>> setDictionary) {
        String name = Generator.getGeneratorName(generatorClass);

        Set<Class<?>> generatorSet = setDictionary.computeIfAbsent(name, k -> new HashSet<>());
        generatorSet.add(generatorClass);
    }

    public void addCommand(Command command) {
        commandDictionary.put(command.getName(), command);
    }

    @Override
    public Collection<Command> getCommands() {
        return commandDictionary.values();
    }

    public List<Value> getAllValuesFromRoots() {
        List<Value> values = new ArrayList<>();
        for (Value v : getSinks()) {
            getAllValues(v, values);
        }
        return values;
    }

    public List<RandomVariable> getAllVariablesFromSinks() {
        return getAllValuesFromRoots().stream()
                .filter(RandomVariable.class::isInstance)
                .map(RandomVariable.class::cast)
                .collect(Collectors.toList());
    }

    private void getAllValues(GraphicalModelNode node, List<Value> values) {
        if (node instanceof Value && !values.contains(node)) {
            values.add((Value) node);
        }
        for (GraphicalModelNode childNode : (List<GraphicalModelNode>) node.getInputs()) {
            getAllValues(childNode, values);
        }
    }

    /**
     * @return a list of keywords including names of distributions, functions, commands, and param names.
     */
    public List<String> getKeywords() {
        List<String> keywords = new ArrayList<>();
        keywords.addAll(genDistDictionary.keySet());
        keywords.addAll(functionDictionary.keySet());
        keywords.addAll(commandDictionary.keySet());

        Set<Class<?>> classes = new HashSet<>();
        for (Set<Class<?>> genDistClasses : genDistDictionary.values()) {
            classes.addAll(genDistClasses);
        }
        for (Set<Class<?>> functionClasses : functionDictionary.values()) {
            classes.addAll(functionClasses);
        }

        for (Class<?> c : classes) {
            Generator.getAllParameterInfo(c).forEach((pinfo) -> {
                String name = pinfo.name();
                if (name.length() > 2 && !keywords.contains(name)) keywords.add(name);
            });
        }

        return keywords;
    }

    public List<RandomVariable> getRandomVariables() {
        ArrayList<RandomVariable> randomVariables = new ArrayList<>();
        dictionary.values().forEach((val) -> {
            if (val instanceof RandomVariable) randomVariables.add((RandomVariable) val);
        });
        return randomVariables;
    }

    public List<RandomVariable> collectRandomVariablesFromRoots() {
        ArrayList<RandomVariable> randomVariables = new ArrayList<>();
        dictionary.values().forEach((val) -> {
            if (val instanceof RandomVariable) randomVariables.add((RandomVariable) val);
        });
        return randomVariables;
    }

    public void clear() {
        // clear current model state
        dictionary.clear();

        // clear history of lines
        lines.clear();
        notifyListeners();
    }

    public void addGraphicalModelChangeListener(GraphicalModelChangeListener listener) {
        listeners.add(listener);
    }

    public void addGraphicalModelListener(GraphicalModelListener listener) {
        gmListeners.add(listener);
    }

    public SortedMap<String, Value<?>> getDictionary() {
        return dictionary;
    }

    public void parse(String code) {
        parseLine(code);
    }

    @Override
    public Map<String, Set<Class<?>>> getGeneratorClasses() {
        SortedMap<String, Set<Class<?>>> generatorClasses = new TreeMap<>();

        generatorClasses.putAll(genDistDictionary);
        generatorClasses.putAll(functionDictionary);

        return generatorClasses;
    }

    public void parseLines(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            parseLine(lines[i]);
        }
    }

    public void parseLine(String line) {
        lines.add(line);
        int lineNumber = nextLineNumber();
        if (isRandomVariableLine(line)) {
            parseRandomVariable(line, lineNumber);
        } else if (isFunctionLine(line)) {
            parseFunctionLine(line, lineNumber);
        } else if (isFixedParameterLine(line)) {
            parseFixedParameterLine(line, lineNumber);
        } else if (isCommandLine(line)) {
            parseCommandLine(line, lineNumber);
        } else if (isValueId(trim(line))) {
            selectValue(dictionary.get(trim(line)));
        } else {
            throw new RuntimeException("Parse error on line " + lineNumber + ": " + line);
        }
        notifyListeners();
    }

    /**
     * @param valueString a piece of a string that can represent a value. Could be literal or an id of a value or a function call.
     * @param lineNumber  the line number this value expression was extracted from
     * @return A Value constructed or produced from this expression
     */
    private Value parseValueExpression(String valueString, int lineNumber) {

        if (isValueId(valueString)) {
            return dictionary.get(valueString);
        } else if (isLiteral(valueString)) {
            return parseLiteralValue(null, valueString, lineNumber);
        } else if (isFunction(valueString)) {
            return parseDeterministicFunction(null, valueString, lineNumber);
        } else throw new RuntimeException("Failed to parse value expression " + valueString + " on line " + lineNumber);
    }

    private boolean isFunction(String functionString) {
        return functionString.indexOf('(') > 0 && functionDictionary.keySet().contains(functionString.substring(0, functionString.indexOf('(')));
    }

    private static boolean isLiteral(String expression) {

        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            // is string
            return true;
        }

        if (expression.startsWith("[") && expression.endsWith("]")) {
            // is list
            return true;
        }

        if (isDouble(expression)) return true;

        if (isInteger(expression)) return true;

        return (isBoolean(expression));
    }

    private void selectValue(Value value) {
        for (GraphicalModelListener listener : gmListeners) {
            listener.valueSelected(value);
        }
    }

    private boolean isValueId(String valueString) {
        return dictionary.keySet().contains(valueString);
    }

    private String trim(String str) {
        str = str.trim();
        if (str.endsWith(";")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    private void parseCommandLine(String line, int lineNumber) {
        line = line.trim();
        Command command = null;
        for (Command c : commandDictionary.values()) {
            if (line.startsWith(c.getName())) {
                command = c;
                break;
            }
        }
        if (command == null) throw new RuntimeException("Parsing error: trying to find command at line " + lineNumber);

        String remainder = line.substring(command.getName().length());
        remainder = trim(remainder);
        Map<String, Value<?>> arguments = parseArguments(remainder.substring(1, remainder.length() - 1), lineNumber);
        command.execute(arguments);
    }

    private boolean isCommandLine(String line) {
        line = line.trim();
        for (Command c : commandDictionary.values()) {
            if (line.startsWith(c.getName())) {
                return true;
            }
        }
        return false;
    }

    private void notifyListeners() {
        for (GraphicalModelChangeListener listener : listeners) {
            listener.modelChanged();
        }
    }

    private int nextLineNumber() {
        return lines.size();
    }

    private void parseFixedParameterLine(String line, int lineNumber) {
        int firstEquals = line.indexOf('=');
        if (firstEquals > 0) {
            String id = line.substring(0, firstEquals).trim();
            String remainder = line.substring(firstEquals + 1).trim();
            if (remainder.endsWith(";")) {
                remainder = remainder.substring(0, remainder.length() - 1);
            }

            Value literalValue = parseLiteralValue(id, remainder, lineNumber);
            dictionary.put(literalValue.getId(), literalValue);
        }
    }

    private Value parseLiteralValue(String id, String valueString, int lineNumber) {

        if (valueString.startsWith("\"") && valueString.endsWith("\"")) {
            // parse string
            return new Value<>(id, valueString.substring(1, valueString.length() - 1));
        }


        if (valueString.startsWith("[") && valueString.endsWith("]")) {
            return parseList(id, valueString, lineNumber);
        }

        try {
            Integer intVal = Integer.parseInt(valueString);
            return new IntegerValue(id, intVal);
        } catch (NumberFormatException ignored) {
        }

        try {
            Double val = Double.parseDouble(valueString);
            return new DoubleValue(id, val);
        } catch (NumberFormatException ignored) {
        }

        try {
            Boolean booleanValue = Boolean.parseBoolean(valueString);
            return new Value<>(id, booleanValue);
        } catch (NumberFormatException ignored) {
        }


        throw new RuntimeException("Parsing fixed parameter " + id + " with value " + valueString + " failed on line " + lineNumber);
    }

    private Value parseList(String id, String valueString, int lineNumber) {

        // remove whitespace
        valueString = valueString.replace(" ", "");

        // remove outer brackets
        valueString = valueString.substring(1, valueString.length() - 1);

        if (valueString.startsWith("[")) {
            // nested list -> parse as [][]

            String[] parts = valueString.split("\\]\\,");

            // remove opening bracket from each element and closing bracket from last element
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].replace("[", "");
                parts[i] = parts[i].replace("]", "");
            }

            Number[][] array;

            Number[] num = parseNumberArray(parts[0], lineNumber);
            if (num instanceof Double[]) {
                array = new Double[parts.length][];
            } else {
                array = new Integer[parts.length][];
            }

            for (int i = 0; i < parts.length; i++) {
                array[i] = parseNumberArray(parts[i], lineNumber);
            }

            if (array instanceof Double[][]) {
                return new DoubleArray2DValue(id, (Double[][]) array);
            } else {
                return new IntegerArray2DValue(id, (Integer[][]) array);
            }

        } else {

            Number[] val = parseNumberArray(valueString, lineNumber);

            if (val instanceof Integer[]) {

                return new IntegerArrayValue(id, (Integer[]) val);
            } else {
                return new DoubleArrayValue(id, (Double[]) val);
            }
        }
    }

    private Number[] parseNumberArray(String arrayString, int lineNumber) {

        String[] elements = arrayString.split(",");
        for (int i = 0; i < elements.length; i++) {
            elements[i] = elements[i].trim();
        }

        if (isInteger(elements[0])) {
            Integer[] val = new Integer[elements.length];
            for (int i = 0; i < elements.length; i++) {
                try {
                    val[i] = Integer.parseInt(elements[i]);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Parser error: parsing integer list at line number " + lineNumber + " but found non-integer:" + elements[i]);
                }
            }
            return val;

        } else if (isDouble(elements[0])) {
            Double[] val = new Double[elements.length];

            for (int i = 0; i < elements.length; i++) {
                try {
                    val[i] = Double.parseDouble(elements[i]);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Parser error: parsing real list at line number " + lineNumber + " but found non-real:" + elements[i]);
                }
            }
            return val;
        }
        throw new RuntimeException("Parser error: parsing number array at line number " + lineNumber + " but found non-number:" + elements[0]);
    }

    private static boolean isInteger(String s) {
        try {
            Integer intVal = Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDouble(String s) {
        try {
            Double doubleVal = Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isBoolean(String s) {
        return s.equals("true") || s.equals("false");
    }

    private void parseFunctionLine(String line, int lineNumber) {
        int firstEquals = line.indexOf('=');
        String id = line.substring(0, firstEquals).trim();
        String remainder = line.substring(firstEquals + 1);

        if (remainder.endsWith(";")) remainder = remainder.substring(0, remainder.length() - 1);
        String functionString = remainder;
        Value val = parseDeterministicFunction(id, functionString, lineNumber);
        dictionary.put(val.getId(), val);
    }

    /**
     * @param functionString includes the function name and open and closed brackets
     */
    private Value parseDeterministicFunction(String id, String functionString, int lineNumber) {
        int pos = functionString.indexOf('(');
        String[] parts = {functionString.substring(0, pos), functionString.substring(pos + 1)};
        String name = parts[0].trim();
        String remainder = parts[1].trim();

        if (remainder.endsWith(")")) remainder = remainder.substring(0, remainder.length() - 1);
        String argumentString = remainder;

        Map<String, Value<?>> arguments = parseArguments(argumentString, lineNumber);

        Set<Class<?>> functionClasses = functionDictionary.get(name);

        if (functionClasses == null)
            throw new RuntimeException("Found no implementation for generative distribution " + name);

        for (Class functionClass : functionClasses) {
            try {
                List<Object> initargs = new ArrayList<>();
                Constructor constructor = getConstructorByArguments(arguments, functionClass, initargs);
                if (constructor == null) {
                    System.err.println("DeterministicFunction class: " + functionClass);
                    System.err.println("     Arguments: " + arguments);
                    throw new RuntimeException("Parser error: no constructor found for deterministic function " + name + " with arguments " + arguments);
                }

                DeterministicFunction func = (DeterministicFunction) constructor.newInstance(initargs.toArray());
                for (String parameterName : arguments.keySet()) {
                    Value value = arguments.get(parameterName);
                    func.setInput(parameterName, value);
                }
                Value val = func.apply();
                val.setId(id);
                return val;
            } catch (InstantiationException e) {
                e.printStackTrace();
                throw new RuntimeException("Parsing generative distribution " + name + " failed on line " + lineNumber);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException("Parsing generative distribution " + name + " failed on line " + lineNumber);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException("Parsing generative distribution " + name + " failed on line " + lineNumber);
            }
        }
        throw new RuntimeException("Parser exception: no constructor found for " + functionString);
    }

    private void parseRandomVariable(String line, int lineNumber) {
        String[] parts = line.split("~");
        if (parts.length != 2)
            throw new RuntimeException("Parsing random variable " + parts[0] + "failed on line " + lineNumber);
        String id = parts[0].trim();
        String genString = parts[1].substring(0, parts[1].indexOf(';'));
        GenerativeDistribution genDist = parseGenDist(genString, lineNumber);
        RandomVariable var = genDist.sample(id);
        addValueToDictionary(var);
    }

    private void addValueToDictionary(Value value) {

        if (!value.isAnonymous()) {
            String id = value.getId();
            Value oldValue = dictionary.get(id);
            if (oldValue != null) {
                oldValue.setId(id + ".old");
            }

            dictionary.put(id, value);
        }
    }

    private GenerativeDistribution parseGenDist(String genString, int lineNumber) {

        int pos = genString.indexOf('(');
        String[] parts = {genString.substring(0, pos), genString.substring(pos + 1)};
        String name = parts[0].trim();
        String remainder = parts[1].trim();
        String argumentString = remainder.substring(0, remainder.length() - 1);
        Map<String, Value<?>> arguments = parseArguments(argumentString, lineNumber);

        Set<Class<?>> genDistClasses = genDistDictionary.get(name);

        if (genDistClasses == null)
            throw new RuntimeException("Found no implementation for generative distribution " + name);

        for (Class genDistClass : genDistClasses) {
            try {
                List<Object> initargs = new ArrayList<>();
                Constructor constructor = getConstructorByArguments(arguments, genDistClass, initargs);

                if (constructor != null) {
                    GenerativeDistribution dist = (GenerativeDistribution) constructor.newInstance(initargs.toArray());
                    for (String parameterName : arguments.keySet()) {
                        Value value = arguments.get(parameterName);
                        dist.setInput(parameterName, value);
                    }
                    return dist;
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
                throw new RuntimeException("Parsing generative distribution " + name + " failed on line " + lineNumber);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException("Parsing generative distribution " + name + " failed on line " + lineNumber);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException("Parsing generative distribution " + name + " failed on line " + lineNumber);
            }
        }
        throw new RuntimeException("Parser exception: no constructor found for " + genString);
    }

    private Constructor getConstructorByArguments(Map<String, Value<?>> arguments, Class genDistClass, List<Object> initargs) {
        System.out.println(genDistClass.getSimpleName() + " " + arguments);
        for (Constructor constructor : genDistClass.getConstructors()) {
            List<ParameterInfo> pInfo = Generator.getParameterInfo(constructor);
            if (match(arguments, pInfo)) {
                for (int i = 0; i < pInfo.size(); i++) {
                    Value arg = arguments.get(pInfo.get(i).name());
                    if (arg != null) {
                        initargs.add(arg);
                    } else if (!pInfo.get(i).optional()) {
                        throw new RuntimeException("Required argument " + pInfo.get(i).name() + " not found!");
                    } else {
                        initargs.add(null);
                    }
                }
                return constructor;
            }
        }
        return null;
    }

    /**
     * A match occurs if the required parameters are in the argument map and the remaining arguments in the map match names of optional arguments.
     *
     * @param arguments
     * @param pInfo
     * @return
     */
    private boolean match(Map<String, Value<?>> arguments, List<ParameterInfo> pInfo) {

        Set<String> requiredArguments = new TreeSet<>();
        Set<String> optionalArguments = new TreeSet<>();
        for (ParameterInfo pinfo : pInfo) {
            if (pinfo.optional()) {
                optionalArguments.add(pinfo.name());
            } else {
                requiredArguments.add(pinfo.name());
            }
        }

        if (!arguments.keySet().containsAll(requiredArguments)) {
            return false;
        }
        Set<String> allArguments = optionalArguments;
        allArguments.addAll(requiredArguments);
        return allArguments.containsAll(arguments.keySet());
    }

    private String[] parseArguments(String argumentString) {

        if (argumentString.length() == 0) return new String[]{};

        String argumentSplitterPattern =
                ",(?=(([^']*'){2})*[^']*$)(?=(([^\"]*\"){2})*[^\"]*$)(?![^()]*\\))(?![^\\[]*\\])";

        return argumentString.split(argumentSplitterPattern);
    }

    private Map<String, Value<?>> parseArguments(String argumentString, int lineNumber) {

        String[] argumentStrings = parseArguments(argumentString);

        TreeMap<String, Value<?>> arguments = new TreeMap<>();
        int argumentCount = 0;
        for (int i = 0; i < argumentStrings.length; i++) {

            String argumentPair = argumentStrings[i];
            System.out.println("arg" + i + ": " + argumentPair);

            if (argumentPair.indexOf('=') < 0) {
                argumentPair = argumentCount + "=" + argumentPair;
            }
            int pos = argumentPair.indexOf('=');

            String key = argumentPair.substring(0, pos).trim();
            String valueString = argumentPair.substring(pos + 1).trim();

            Value val = parseValueExpression(valueString, lineNumber);
            arguments.put(key, val);
            argumentCount += 1;
        }
        return arguments;
    }

    private String consumeArgument(String argumentString, int lineNumber) {

        int firstQuote = argumentString.indexOf('"');
        int firstComma = argumentString.indexOf(',');

        // if next argument is a string or contains a string return immediately
        if (firstQuote >= 0 && firstQuote < firstComma) {
            System.out.println("next argument contains quotes: " + argumentString);
            return argumentString.substring(0, argumentString.indexOf("\"", firstQuote + 1) + 1);
        }

        int pos = firstComma;
        if (pos > 0) {
            String nextArgument = argumentString.substring(0, pos);
            while (pos > 0 && !matchingBrackets(nextArgument)) {
                pos = argumentString.indexOf(',', pos + 1);
                if (pos > 0) nextArgument = argumentString.substring(0, pos);
            }
            if (matchingBrackets(nextArgument)) {
                return nextArgument;
            } else
                throw new RuntimeException("Failed to parse argument string on line " + lineNumber + ". nextArgument=" + nextArgument);
        } else {
            return argumentString;
        }
    }

    private boolean matchingBrackets(String string) {
        return string.replace(")", "").length() == string.replace("(", "").length();
    }

    public boolean isFunctionLine(String line) {
        int firstEquals = line.indexOf('=');
        if (firstEquals > 0) {
            String id = line.substring(0, firstEquals).trim();
            String remainder = line.substring(firstEquals + 1).trim();
            return isFunction(remainder);
        } else return false;
    }

    public static boolean isRandomVariableLine(String line) {
        return (line.indexOf('~') > 0);
    }

    public static boolean isFixedParameterLine(String line) {
        int firstEquals = line.indexOf('=');
        if (firstEquals > 0) {
            String id = line.substring(0, firstEquals).trim();

            String remainder = line.substring(firstEquals + 1).trim();
            String valueString = remainder.substring(0, remainder.indexOf(';'));
            valueString = valueString.trim();

            return isLiteral(valueString);

        } else return false;
    }

    public List<String> getLines() {
        return lines;
    }

    /**
     * @param sourceFile the lphy source file
     */
    public void source(File sourceFile) throws IOException {

        FileReader reader = new FileReader(sourceFile);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line = bufferedReader.readLine();
        while (line != null) {
            parseLine(line);
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        reader.close();
    }

    public static void main(String[] args) {

        GraphicalModelParser parser = new GraphicalModelParser();

        String argumentString = "conc=[1,1,1,1],Q=jukesCantor(),label=\"foo,bar\",newick=newick(\"((A,B),C)\")";

        String[] arguments = parser.parseArguments(argumentString);

        for (int i = 0; i < arguments.length; i++) {
            System.out.println("arg" + i + ": " + arguments[i]);
        }
    }
}