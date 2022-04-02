package kelvinspatola.mode.smartcode.completion;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import kelvinspatola.mode.smartcode.KeyListener;
import kelvinspatola.mode.smartcode.LinePainter;
import kelvinspatola.mode.smartcode.SmartCodeEditor;
import kelvinspatola.mode.smartcode.SmartCodeMode;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;
import processing.app.ui.Theme;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

public class SnippetManager implements KeyListener, CaretListener, LinePainter {
    protected final Map<String, Snippet> snippets = new HashMap<>();
    protected boolean isReadingKeyboardInput;
    protected Color boundingBoxColor;
    protected Snippet currentSnippet;
    protected SmartCodeEditor editor;

    // CONSTRUCTOR
    public SnippetManager(SmartCodeEditor editor) {
        this.editor = editor;
        addDefaultSnippets();
        addSnippetsFromFile(((SmartCodeMode) editor.getMode()).loadSnippetsFile(), snippets);
    }

    protected void addDefaultSnippets() {
        snippets.put("if", new Snippet("if ($) {\n    $\n}"));
        snippets.put("ifelse", new Snippet("if ($) {\n    $\n} else {\n    \n}"));
        snippets.put("switch", new Snippet("switch ($) {\ncase $:\n    $\n    break;\n}"));
        snippets.put("for", new Snippet("for (var i = $; i < $; i++) {\n    $\n}"));
        snippets.put("while", new Snippet("while ($) {\n    $\n}"));
        snippets.put("do", new Snippet("do {\n    $\n} while ($);"));
        snippets.put("try", new Snippet("try {\n    $\n}\ncatch (Exception e) {\n    e.printStackTrace();\n}"));
        snippets.put("sout", new Snippet("System.out.println($);$"));
        snippets.put("for:", new Snippet("for (var # = $; # < $; #++) {\n    $\n}"));
        snippets.put("foreach:", new Snippet("for ($ : #) {\n    $\n}"));
    }

    @Override
    public boolean handlePressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (e.isControlDown() && keyCode == KeyEvent.VK_SPACE) {

            String trigger = checkTrigger();
            String placeholder = null;

            if (trigger.contains(":")) {
                String tempStr = trigger;
                trigger = trigger.substring(0, trigger.indexOf(":") + 1);
                placeholder = tempStr.substring(tempStr.indexOf(":") + 1);

                if (placeholder.isBlank()) {
                    try {
                        tempStr = (String) editor.getToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);

                        if (tempStr.matches("\\w+")) {
                            placeholder = tempStr;
                        } else {
                            System.err.println("Clipboard does not contain a valid string");
                            editor.getToolkit().beep();
                        }

                    } catch (UnsupportedFlavorException | IOException e1) {
                        System.err.println("Clipboard does not contain a string");
                        editor.getToolkit().beep();
                    }

                    if (placeholder.isBlank())
                        return false;
                }
            }

            if (snippets.containsKey(trigger)) {
                int line = editor.getTextArea().getCaretLine();
                int indent = editor.getSmartCodeTextArea().getLineIndentation(line);

                currentSnippet = snippets.get(trigger);

                if (placeholder != null) {
                    String source = currentSnippet.getSource().replaceAll("#", placeholder);
//                    source = source.replaceAll("@", "kelvin");
                    currentSnippet = new Snippet(source);
                }
                editor.startCompoundEdit();
                editor.setSelection(editor.getLineStartOffset(line), editor.getCaretOffset());
                editor.setSelectedText(currentSnippet.getCode(indent));

                int caret = currentSnippet.getStartPosition(editor.getCaretOffset());
                editor.setSelection(caret, caret);
                editor.stopCompoundEdit();
            }

        } else if (isReadingKeyboardInput) {
            if (BracketCloser.isSkipped())
                return false;

            if (currentSnippet.isLastPosition()) {
                currentSnippet = null;
                return isReadingKeyboardInput = false;
            }

            if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_TAB) {
                int caret = currentSnippet.nextPosition();
                editor.setSelection(caret, caret);
                return true;
            }
            currentSnippet.readInput(e);

        } else {
            currentSnippet = null;
        }

//        if (isReadingKeyboardInput()) {
//            editor.statusMessage("EDITING PARAMETERS", EditorStatus.WARNING);
//        }
        return false;
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        if (currentSnippet == null) {
            isReadingKeyboardInput = false;
            return;
        }
        isReadingKeyboardInput = currentSnippet.contains(e.getDot());
    }

    @Override
    public boolean handleTyped(KeyEvent e) {
        return false;
    }

    @Override
    public boolean canPaint(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta) {
        if (isReadingKeyboardInput) {
            int lineStart = ta.getLineStartOffset(line);
            if (line == ta.getCaretLine()) {
                int start = currentSnippet.leftBoundary - lineStart;
                int end = currentSnippet.rightBoundary - lineStart - 1;
                int x = ta._offsetToX(line, start);
                int w = ta._offsetToX(line, end) - x;

                gfx.setColor(boundingBoxColor);
                gfx.drawRect(x, y, w, h);
            }

            int nextStopOffset = currentSnippet.getNextStopPosition();
            if (nextStopOffset != -1) {
                int stopLine = ta.getLineOfOffset(nextStopOffset);
                if (line == stopLine) {
                    int start = currentSnippet.getNextStopPosition() - lineStart;
                    int x = ta._offsetToX(line, start);

                    gfx.setColor(boundingBoxColor);
                    gfx.fillRect(x, y, 1, h);
                }
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void updateTheme() {
        boundingBoxColor = Theme.getColor("editor.eolmarkers.color");
    }

    private String checkTrigger() {
        int line = editor.getTextArea().getCaretLine();
        String lineText = editor.getLineText(line);

        StringBuilder sb = new StringBuilder();
        int index = editor.getSmartCodeTextArea().caretPositionInsideLine() - 1;

        while (index >= 0) {
            char ch = lineText.charAt(index);

            if (Character.isWhitespace(ch)) {
                break;
            }
            sb.append(ch);
            index--;
        }
        return sb.reverse().toString();
    }

    private static void addSnippetsFromFile(File file, Map<String, Snippet> snippets) {
        JSONObject jsonFile = PApplet.loadJSONObject(file);
        JSONArray user_snippets = jsonFile.getJSONArray("User-Snippets");

        for (int i = 0; i < user_snippets.size(); i++) {
            JSONObject template = user_snippets.getJSONObject(i);
            JSONArray lines = template.getJSONArray("code");

            StringBuilder source = new StringBuilder();
            for (int j = 0; j < lines.size(); j++) {
                source.append(lines.getString(j)).append("\n");
            }
            source.deleteCharAt(source.length() - 1);

            String key = template.getString("key");
            snippets.put(key, new Snippet(source.toString()));
        }
    }
}
