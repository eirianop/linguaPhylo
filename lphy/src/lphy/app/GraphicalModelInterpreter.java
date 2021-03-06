package lphy.app;

import lphy.core.LPhyParser;
import lphy.graphicalModel.Command;
import lphy.graphicalModel.GenerativeDistribution;
import lphy.graphicalModel.Generator;
import lphy.parser.CodeColorizer;
import lphy.utils.LoggerUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class GraphicalModelInterpreter extends JPanel {

    LPhyParser parser;
    GraphicalModelTextPane textPane;
    JPanel activeLine = new JPanel();
    JTextField interpreterField;
    JLabel infoLine = new JLabel("  ", SwingConstants.LEFT);

    private static final String COMMIT_ACTION = "commit";

    Font interpreterFont = new Font("monospaced", Font.PLAIN, 12);

    int BORDER_SIZE = 10;

    Border textBorder = BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE);

    Map<String, String> canonicalWords = new TreeMap<>();

    public GraphicalModelInterpreter(LPhyParser parser) {
        this.parser = parser;

        textPane = new GraphicalModelTextPane(parser);
        textPane.setBorder(textBorder);
        textPane.setFont(interpreterFont);
        JScrollPane scrollPane = new JScrollPane(textPane);
        TextLineNumber tln = new TextLineNumber(textPane);
        scrollPane.setRowHeaderView(tln);

        interpreterField = new JTextField(80);
        interpreterField.setFont(interpreterFont);
        interpreterField.setBorder(textBorder);
        interpreterField.setFocusTraversalKeysEnabled(false);

        List<String> keywords = parser.getKeywords();
        keywords.addAll(Arrays.asList(Symbols.greekLetterCodes));

        List<String> commandStrings =
                parser.getCommands().stream().map(Command::getName).collect(Collectors.toList());
        keywords.addAll(commandStrings);

        Autocomplete autoComplete = new Autocomplete(interpreterField, keywords);

        for (Map.Entry<String, Set<Class<?>>> entry : parser.getGeneratorClasses().entrySet()) {

            Set<Class<?>> classes = entry.getValue();
            Iterator iterator = classes.iterator();

            StringBuilder builder = new StringBuilder();
            for (Class c : classes) {
                builder.append(Generator.getSignature((Class) iterator.next()));
                builder.append("; ");
            }
            final String message = builder.toString();

            autoComplete.getActionMap().put(entry.getKey(), new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setMessage(message);
                }
            });
        }

        for (Command command : parser.getCommands()) {
            autoComplete.getActionMap().put(command.getName(), new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setMessage(command.getSignature());
                }
            });
        }

        interpreterField.getDocument().addDocumentListener(autoComplete);
        interpreterField.getInputMap().put(KeyStroke.getKeyStroke('\t'), COMMIT_ACTION);
        interpreterField.getActionMap().put(COMMIT_ACTION, autoComplete.new CommitAction());

        interpreterField.addActionListener(e -> {
            interpretInput(interpreterField.getText());
            interpreterField.setText("");
        });


        for (int i = 0; i < Symbols.greekLetterCodes.length; i++) {
            canonicalWords.put(Symbols.greekLetterCodes[i], Symbols.greekLetters[i]);
        }

        interpreterField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ' || e.getKeyChar() == '=' || e.getKeyChar() == ',' || e.getKeyChar() == '~') {
                    String lastWord = lastWord(" |\\=|\\,|~|\\(");
                    String canonicalWord = getCanonicalWord(lastWord);
                    if (!lastWord.equals(canonicalWord)) {
                        String newText = interpreterField.getText().replace(lastWord, canonicalWord);
                        interpreterField.setText(newText);
                        interpreterField.setCaretPosition(newText.length());
                    }
                }
            }
        });

        //BoxLayout boxLayout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
        //setLayout(boxLayout);

        BoxLayout boxLayout2 = new BoxLayout(activeLine, BoxLayout.LINE_AXIS);
        activeLine.setLayout(boxLayout2);

        JLabel label = new JLabel("  >");
        label.setFont(interpreterFont);
        label.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 2, Color.gray), new EmptyBorder(BORDER_SIZE, tln.getBorderGap(), BORDER_SIZE, tln.getBorderGap() + 2)));

        activeLine.add(label);
        activeLine.add(interpreterField);
        
        activeLine.setPreferredSize(new Dimension(2000,interpreterField.getPreferredSize().height));
        activeLine.setMaximumSize(new Dimension(2000,interpreterField.getPreferredSize().height));

        //add(scrollPane);
        //add(activeLine);

        infoLine.setBorder(new EmptyBorder(2,43,2,2));
        infoLine.setHorizontalTextPosition(SwingConstants.LEFT);
        infoLine.setFont(infoLine.getFont().deriveFont(10.0f));
        infoLine.setForeground(Color.gray);
        //add(infoLine);

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        GroupLayout.ParallelGroup horizParallelGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        horizParallelGroup.addComponent(scrollPane);
        horizParallelGroup.addComponent(activeLine);
        horizParallelGroup.addComponent(infoLine);

        GroupLayout.SequentialGroup vertSequentialGroup = layout.createSequentialGroup();
        vertSequentialGroup.addComponent(scrollPane);
        vertSequentialGroup.addComponent(activeLine);
        vertSequentialGroup.addComponent(infoLine);


        layout.setHorizontalGroup(horizParallelGroup);

        layout.setVerticalGroup(vertSequentialGroup);

        LoggerUtils.log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {

                String message = record.getMessage();

                if (record.getLevel() == Level.SEVERE) {
                    message = "<html><font color=\"red\">SEVERE: " + message + "</font></html>";
                } else if (record.getLevel() == Level.WARNING) {
                    message = "<html><font color=\"#FFA500\">WARNING: " + message + "</font></html>";
                } else if (record.getLevel() != Level.INFO) {
                    return;
                }
                infoLine.setText(message);
            }

            @Override
            public void flush() {
                infoLine.setText("");
            }

            @Override
            public void close() throws SecurityException {
                infoLine.setText("");

            }
        });
    }

    private void setMessage(String message) {
        infoLine.setText(message);
        repaint();
    }

    private String getCanonicalWord(String word) {
        String canonicalWord = canonicalWords.get(word);
        if (canonicalWord != null) return canonicalWord;
        return word;
    }

    private String lastWord(String delimiters) {
        String[] words = interpreterField.getText().split(delimiters);
        return words[words.length - 1];
    }

    public void interpretInput(String input) {

//        // split on ; that are not in string
//        String[] lines = input.split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
//
//        for (String line : lines) {
//            line = line.trim();
//
//            if (!line.equals("")) {
//
//                if (line.charAt(line.length() - 1) != ';') {
//                    line = line + ";";
//                }
//                textPane.addLine(line);
//                parser.parse(line);
//            }
//        }

        parser.parse(input);
        CodeColorizer codeColorizer = new CodeColorizer(parser.getDictionary(), textPane);
        codeColorizer.parse(input);
    }

    public void clear() {
        textPane.setText("");
    }
}
