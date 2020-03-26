package lphy.core.lightweight;

import lphy.graphicalModel.ParameterInfo;

public class Argument implements Comparable<Argument> {

    public final int index;
    public final String name;
    public final String description;
    public final Class type;

    public Argument(int index, ParameterInfo parameterInfo, Class type) {
        this.index = index;
        this.name = parameterInfo.name();
        this.description = parameterInfo.description();
        this.type = type;
    }

    public int compareTo(Argument a) {
        if (a.index != index) return Integer.compare(index, a.index);
        return name.compareTo(a.name);
    }

    public final String setMethodName() {
        return setMethodName(name);
    }

    static String setMethodName(String name) {
        return "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public final String getMethodName() {
        return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public String toString() {
        return "argument " + name + " index=" + index + " type=" + type + " description=" + description;
    }
}
