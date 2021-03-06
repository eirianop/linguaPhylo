package lphy.parser;


import java.io.*;
import java.util.*;

import lphy.core.LPhyParser;
import lphy.graphicalModel.Command;
import lphy.graphicalModel.Value;

/** A simple Read-Eval-Print-Loop for the graphicalModelSimulator language **/ 
public class REPL implements LPhyParser {

	SortedMap<String, Value<?>> dictionary = new TreeMap<>();

	SortedMap<String, Command> commands = new TreeMap<>();

	private List<String> lines = new ArrayList<>();

	public REPL() {
	}

	public void doREPL() {
		while (true) {
			System.out.print(">");
			try {
				String cmd = (new BufferedReader(new InputStreamReader(System.in))).readLine();
				parse(cmd);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	final public Map<String, Value<?>> getDictionary() {
		return dictionary;
	}

	@Override
	public void addCommand(Command command) {
		commands.put(command.getName(), command);
	}

	@Override
	public Collection<Command> getCommands() {
		return commands.values();
	}

	public void parse(String cmd) {
		if (cmd == null) {
			return;
		}

		final String commandString = cmd;

		final boolean[] found = new boolean[1];
		commands.forEach((key, command) -> {
			if (commandString.startsWith(key)) {
				command.execute(commandString, dictionary);
				found[0] = true;
			}
		});

		if (!found[0]) {
			if (cmd.trim().length() == 0) {
				// ignore empty lines
				return;
			} else if (!cmd.startsWith("?")) {
				try {
					SimulatorListenerImpl parser = new SimulatorListenerImpl(dictionary);
					if (!cmd.endsWith(";")) {
						cmd = cmd + ";";
					}
					Object o = parser.parse(cmd);
					//parser.parse(cmd);
				} catch (SimulatorParsingException e) {
					System.out.println(cmd);
					System.out.println(e.getMessage());
				} catch (Exception e) {
					e.printStackTrace(System.err);
					System.err.println("Error: " + e.getMessage());
				}
				lines.add(cmd);

				// wrap the ExpressionNodes before returning from parse
				LPhyParser.Utils.wrapExpressionNodes(this);
			} else throw new RuntimeException();
		}
	}

	@Override
	public Map<String, Set<Class<?>>> getGeneratorClasses() {
		SortedMap<String, Set<Class<?>>> generatorClasses = new TreeMap<>();

		generatorClasses.putAll(SimulatorListenerImpl.genDistDictionary);
		generatorClasses.putAll(SimulatorListenerImpl.functionDictionary);

		return generatorClasses;
	}

	@Override
	public List<String> getLines() {
		return lines;
	}

	@Override
	public void clear() {
		dictionary.clear();
		lines.clear();
	}


	public static void main(String[] args) {
		System.out.println("A  simple Read-Eval-Print-Loop for the lphy language ");
		REPL repl = new REPL();
		repl.doREPL();
	}


}
