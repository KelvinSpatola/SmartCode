package kelvinspatola.mode.smartcode.completion;

import static kelvinspatola.mode.smartcode.SmartCodePreferences.*;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import kelvinspatola.mode.smartcode.KeyListener;
import kelvinspatola.mode.smartcode.SmartCodeEditor;
import processing.app.ui.Editor;

public class BracketCloser implements KeyListener {
    static public final Map<Character, Character> tokens = new HashMap<>();
    static private boolean isSkipped;
    private char nextToken;
    private Editor editor;

    static {
        tokens.put('(', ')');
        tokens.put('[', ']');
        tokens.put('{', '}');
        tokens.put('<', '>');
        tokens.put('"', '"');
        tokens.put('\'', '\'');
    }

    // CONSTRUCTOR
    public BracketCloser(SmartCodeEditor editor) {
        this.editor = editor;
    }

    @Override
    public boolean handlePressed(KeyEvent e) {
        char keyChar = e.getKeyChar();
        isSkipped = false;

        if (!tokens.containsKey(keyChar) && !tokens.containsValue(keyChar)) {
            // this keyChar is not our business, so let's get the hell outta here
            return false;
        }

        // closing token -> skipping
        if (keyChar == nextToken && !editor.isSelectionActive()) {
            skipNextToken(keyChar);
            return false;
        }

        // closing token -> inserting
        if (isClosingBracket(keyChar)) {
            editor.setSelectedText(String.valueOf(keyChar), true);
            return false;
        }

        // opening token -> wrapping, inserting (if disabled) or auto-close
        if (tokens.containsKey(keyChar)) {
            // if selection is active and text selection wrapping is enabled,
            // we must wrap a pair of tokens around the selection
            if (AUTOCLOSE_WRAP_TEXT && editor.isSelectionActive()) {
                wrapSelection(keyChar);

            } else if (isTokenDisabled(keyChar)) {
                // if the user has disabled this token in the preferences menu, insert it in the
                // text then without auto-closing it.
                editor.setSelectedText(String.valueOf(keyChar), true);

            } else { // otherwise, add a closing token
                addClosingToken(keyChar);
            }
        }

        return false;
    }

    @Override
    public boolean handleTyped(KeyEvent e) {
        char keyChar = e.getKeyChar();
        return tokens.containsKey(keyChar) || isClosingBracket(keyChar);
    }

    static public boolean isSkipped() {
        return isSkipped;
    }

    private void addClosingToken(char token) {
        int line = editor.getTextArea().getCaretLine();
        int caret = editor.getCaretOffset() - editor.getLineStartOffset(line);
        String lineText = editor.getLineText(line);

        if (isTokenInside("'", "'", caret, lineText) || (token == '<' && (isTokenInside("(", ")", caret, lineText)))) {
            editor.setSelectedText(String.valueOf(token), true);
            return;
        }

        nextToken = tokens.get(token);
        editor.setSelectedText("" + token + nextToken, true);
        // step back one char so that it is in the middle of the tokens
        int newCaret = editor.getCaretOffset() - 1;
        editor.setSelection(newCaret, newCaret);
    }

    private void skipNextToken(char token) {
        int caret = editor.getCaretOffset();
        char[] code = editor.getText().toCharArray();

        if (caret < code.length && code[caret] == nextToken) {
            editor.setSelection(caret + 1, caret + 1);
            nextToken = Character.UNASSIGNED;
            isSkipped = true;

        } else if (isClosingBracket(token)) { // if it's one of these: )]}>
            editor.setSelectedText(String.valueOf(token), true);

        } else { // if it's either \' or \"
            addClosingToken(token);
        }
    }

    private void wrapSelection(char token) {
        StringBuilder selectedText = new StringBuilder(editor.getSelectedText());

        if (AUTOCLOSE_WRAP_REPLACE) {
            char firstChar = selectedText.charAt(0);
            char lastChar = selectedText.charAt(selectedText.length() - 1);

            boolean isAlreadyWrapped = false;

            if (tokens.containsKey(firstChar) && tokens.containsValue(lastChar) && lastChar == tokens.get(firstChar))
                isAlreadyWrapped = true;

            if (isAlreadyWrapped) {
                // if the selected text is already wrapped with this token, then toggle it off
                if (token == firstChar) {
                    selectedText.delete(0, 1);
                    selectedText.delete(selectedText.length() - 1, selectedText.length());
                } else {
                    // otherwise, wrap it with a new pair of tokens
                    selectedText.setCharAt(0, token);
                    selectedText.setCharAt(selectedText.length() - 1, tokens.get(token));
                }

            } else {
                selectedText.insert(0, token).append(tokens.get(token)).toString();
            }

        } else { // otherwise, stack
            selectedText.insert(0, token).append(tokens.get(token)).toString();
        }

        int start = editor.getSelectionStart();
        int end = start + selectedText.length();

        editor.startCompoundEdit();
        editor.setSelectedText(selectedText.toString());
        editor.setSelection(start, end);
        editor.stopCompoundEdit();
    }

    private static boolean isTokenDisabled(char token) {
        if (!AUTOCLOSE_QUOTES) {
            if ("\"\'".contains(String.valueOf(token)))
                return true;
        }

        if (!AUTOCLOSE_BRACKETS) {
            if ("([{<".contains(String.valueOf(token)))
                return true;
        }
        return false;
    }

    private static boolean isTokenInside(String openToken, String closeToken, int caretPos, String lineText) {
        if (!lineText.matches("^.*\\" + openToken + ".*?\\" + closeToken + ".*$"))
            return false;

        int openIndex = lineText.lastIndexOf(openToken, caretPos) - 1;
        int closeIndex = lineText.indexOf(closeToken, caretPos);

        if (openIndex == -1 || closeIndex == -1)
            return false;

        return (caretPos > openIndex && caretPos <= closeIndex);
    }

    private static boolean isClosingBracket(char ch) {
        String tokens = ")]}>";
        return tokens.contains(String.valueOf(ch));
    }
}
