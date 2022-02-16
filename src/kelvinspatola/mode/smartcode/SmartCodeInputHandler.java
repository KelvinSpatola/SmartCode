package kelvinspatola.mode.smartcode;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import processing.app.Platform;
import processing.app.Preferences;
import processing.app.syntax.PdeInputHandler;
import processing.app.ui.Editor;

public class SmartCodeInputHandler extends PdeInputHandler {
    protected List<KeyListener> listeners;

    public SmartCodeInputHandler(Editor _editor) {
        super(_editor);

        SmartCodeEditor editor = (SmartCodeEditor) _editor;
        addKeyBinding("CA+RIGHT", e -> editor.expandSelection());
        addKeyBinding("C+7", e -> editor.toggleBlockComment());
        addKeyBinding("CS+U", e -> editor.changeCase(true));
        addKeyBinding("CS+L", e -> editor.changeCase(false));
        addKeyBinding("A+UP", e -> editor.moveLines(true));
        addKeyBinding("A+DOWN", e -> editor.moveLines(false));
        addKeyBinding("CA+UP", e -> editor.duplicateLines(true));
        addKeyBinding("CA+DOWN", e -> editor.duplicateLines(false));
        addKeyBinding("C+E", e -> editor.deleteLine(getTextArea(e).getCaretLine()));
        addKeyBinding("CS+E", e -> editor.deleteLineContent(getTextArea(e).getCaretLine()));
        addKeyBinding("A+ENTER", e -> editor.insertLineBreak(getTextArea(e).getCaretPosition()));
        addKeyBinding("CS+ENTER", e -> editor.insertNewLineAbove(getTextArea(e).getCaretLine()));
        addKeyBinding("S+ENTER", e -> editor.insertNewLineBellow(getTextArea(e).getCaretLine()));

        // for testing purposes
        addKeyBinding("CA+P", e -> {
//                System.out.println(((SmartCodeMode) editor.getMode()).checkTemplateFolder());
//                System.out.println(((SmartCodeMode) editor.getMode()).getTemplateFolder());
            System.out.println(this.hashCode());
        });

        listeners = new ArrayList<>();
    }

    public void addKeyListener(KeyListener listener) {
        listeners.add(listener);
    }

    @Override
    protected boolean isMnemonic(KeyEvent e) {
        if (!Platform.isMacOS()) {
            if (e.isAltDown() && Character.isLetter(e.getKeyChar())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handlePressed(KeyEvent e) {
        if (e.isMetaDown())
            return false;

        if (e.getKeyChar() == '}') {
            insertCloseBrace();
        }

        for (KeyListener kl : listeners) {
            if (kl.handlePressed(e)) {
                e.consume();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleTyped(KeyEvent e) {
        char keyChar = e.getKeyChar();

        if (e.isControlDown()) {
            // on linux, ctrl-comma (prefs) being passed through to the editor
            if ((keyChar == KeyEvent.VK_COMMA) || (keyChar == KeyEvent.VK_SPACE)) {
                e.consume();
                return true;
            }
        }

        if (keyChar == '}')
            return true;

        for (KeyListener kl : listeners) {
            if (kl.handleTyped(e)) {
                e.consume();
                return true;
            }
        }
        return false;
    }

    private void insertCloseBrace() {
        editor.startCompoundEdit();

        // erase any selection content
        if (editor.isSelectionActive()) {
            editor.setSelectedText("");
        }

        int indent = 0;

        if (Preferences.getBoolean("editor.indent")) {
            SmartCodeTextArea textarea = (SmartCodeTextArea) editor.getTextArea();
            
            int line = textarea.getCaretLine();

            if (editor.getLineText(line).isBlank()) {
                int startBrace = textarea.getMatchingBraceLine(line, true);

                if (startBrace != -1)
                    indent = textarea.getLineIndentation(startBrace);

                editor.setSelection(editor.getLineStartOffset(line), editor.getCaretOffset());
            }
        }

        String result = SmartCodeEditor.addSpaces(indent);

        // if the user chooses to disable the bracket closing feature in the
        // Preferences.txt file, we should then insert a closing brace here.
        // Otherwise this is handled by the BracketCloser class.
        if (!SmartCodePreferences.BRACKETS_AUTO_CLOSE)
            result += '}';

        editor.setSelectedText(result);
        editor.stopCompoundEdit();
    }
}
