package kelvinspatola.mode.smartcode;

import static kelvinspatola.mode.smartcode.Constants.*;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import processing.app.Base;
import processing.app.Language;
import processing.app.Messages;
import processing.app.Mode;
import processing.app.Platform;
import processing.app.Preferences;
import processing.app.SketchCode;
import processing.app.syntax.DefaultInputHandler;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.app.ui.EditorException;
import processing.app.ui.EditorState;
import processing.app.ui.Toolkit;
import processing.mode.java.JavaEditor;
import processing.mode.java.debug.LineHighlight;
import processing.mode.java.debug.LineID;

public class SmartCodeEditor extends JavaEditor implements KeyListener {
    protected final Set<LineHighlight> pinnedLines = new HashSet<>();
    static private boolean helloMessageViewed = false;

    public SmartCodeEditor(Base base, String path, EditorState state, Mode mode) throws EditorException {
        super(base, path, state, mode);

        buildMenu();
        buildPopupMenu();
        printHelloMessage();

    }

    private void printHelloMessage() {
        statusNotice("SmartCode is active");
        if (!helloMessageViewed) {
            console.message("SmartCode 0.0.1\n" + "created by Kelvin Spatola\n", false);
            helloMessageViewed = true;
        }
    }

    @Override
    protected JEditTextArea createTextArea() {
        return new SmartCodeTextArea(new PdeTextAreaDefaults(), this);
    }

    public SmartCodeTextArea getSmartCodeTextArea() {
        return (SmartCodeTextArea) textarea;
    }

    @Override
    public JMenu buildFileMenu() {
        // JAVA MODE ITEMS
        String appTitle = Language.text("menu.file.export_application");
        JMenuItem exportApplication = Toolkit.newJMenuItemShift(appTitle, 'E');
        exportApplication.addActionListener(e -> {
            if (sketch.isUntitled() || sketch.isReadOnly()) {
                Messages.showMessage("Save First", "Please first save the sketch.");
            } else {
                handleExportApplication();
            }
        });

        // SMARTCODE ITEMS
        JMenuItem selectTemplate = new JMenuItem("Select template");
        selectTemplate.addActionListener(a -> setText("\nvoid setup() {\n    size(300, 300);\n}"));

        return buildFileMenu(new JMenuItem[] { selectTemplate, exportApplication });
    }

    private void buildMenu() {
        JMenu menu = new JMenu("SmartCode");
        List<JMenuItem> updatableItems = new ArrayList<>();

        createItem(menu, "Pin line", null, () -> toggleLinePin(textarea.getCaretLine()));

        menu.addSeparator(); // ---------------------------------------------
        createItem(menu, "Duplicate lines up", "CA+UP", () -> duplicateLines(true));
        createItem(menu, "Duplicate lines down", "CA+DOWN", () -> duplicateLines(false));
        createItem(menu, "Move lines up", "A+UP", () -> moveLines(true));
        createItem(menu, "Move lines down", "A+DOWN", () -> moveLines(false));

        menu.addSeparator(); // ---------------------------------------------
        createItem(menu, "Delete line", "C+E", () -> deleteLine(textarea.getCaretLine()));
        createItem(menu, "Delete line content", "CS+E", () -> deleteLineContent(textarea.getCaretLine()));
        createItem(menu, "Insert line bellow", "S+ENTER", () -> insertNewLineBellow(textarea.getCaretLine()));
        createItem(menu, "Insert line above", "CS+ENTER", () -> insertNewLineAbove(textarea.getCaretLine()));
        createItem(menu, "Insert line break", "A+ENTER", () -> insertLineBreak(getCaretOffset()));

        menu.addSeparator(); // ---------------------------------------------
        updatableItems.add(createItem(menu, "Toggle block comment", "C+7", this::toggleBlockComment));
        updatableItems.add(createItem(menu, "Format selected text", "C+T", this::handleAutoFormat));
        updatableItems.add(createItem(menu, "To upper case", "CS+U", () -> changeCase(true)));
        updatableItems.add(createItem(menu, "To lower case", "CS+L", () -> changeCase(false)));
        createItem(menu, "Expand Selection", "CA+RIGHT", this::expandSelection);

        menu.addSeparator(); // ---------------------------------------------
        createItem(menu, "Visit GitHub page", null, () -> Platform.openURL("https://github.com/KelvinSpatola"));

        // Update state on selection/de-selection
        menu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuSelected(MenuEvent e) {
                for (JMenuItem item : updatableItems) {
                    item.setEnabled(isSelectionActive());
                }
            }
        });

        JMenuBar menubar = getJMenuBar();
        int toolMenuIndex = menubar.getComponentIndex(getToolMenu());
        menubar.add(menu, toolMenuIndex);
        Toolkit.setMenuMnemonics(menubar);
    }

    private void buildPopupMenu() {
        JPopupMenu popup = textarea.getRightClickPopup();
        JMenu submenu = new JMenu("SmartCode");
        List<JMenuItem> updatableItems = new ArrayList<>();
        createItem(submenu, "Pin line", null, () -> toggleLinePin(textarea.getCaretLine()));

        popup.addSeparator(); // ---------------------------------------------
        updatableItems.add(createItem(submenu, "Format selected text", null, this::handleAutoFormat));
        updatableItems.add(createItem(submenu, "Toggle block comment", null, this::toggleBlockComment));
        updatableItems.add(createItem(submenu, "To upper case", null, () -> changeCase(true)));
        updatableItems.add(createItem(submenu, "To lower case", null, () -> changeCase(false)));

        // Update state on selection/de-selection
        submenu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuSelected(MenuEvent e) {
                for (JMenuItem item : updatableItems) {
                    item.setEnabled(isSelectionActive());
                }
            }
        });

        submenu.setFont(popup.getFont().deriveFont(Font.BOLD));
        popup.add(submenu);
    }

    protected static JMenuItem createItem(JMenu menu, String title, String keyBinding, Runnable action) {
        JMenuItem item = new MenuItem(title);
        item.addActionListener(a -> action.run());
        if (keyBinding != null)
            item.setAccelerator(DefaultInputHandler.parseKeyStroke(keyBinding));
        menu.add(item);
        return item;
    }

    static class MenuItem extends JMenuItem {
        MenuItem(String title) {
            super(title);
        }

        @Override
        public void setAccelerator(KeyStroke keyStroke) {
            super.setAccelerator(keyStroke);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "none");
        }
    }

    abstract static class MenuAdapter implements MenuListener {
        @Override
        public void menuCanceled(MenuEvent e) {
        }

        @Override
        public void menuDeselected(MenuEvent e) {
        }

        @Override
        public void menuSelected(MenuEvent e) {
        }
    }

    /*
     * ******** METHODS ********
     */

    @Override
    public boolean handlePressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_ENTER) {
            handleEnter();

        } else if (keyCode == KeyEvent.VK_TAB) {
            handleTabulation(e.isShiftDown());
        }
        return false;
    }

    @Override
    public boolean handleTyped(KeyEvent e) {
        return false;
    }

    public void handleEnter() {
        int caret = getCaretOffset();

        if (!isSelectionActive()) {

            int positionInLine = getSmartCodeTextArea().getPositionInsideLineWithOffset(caret);
            int caretLine = textarea.getCaretLine();
            String lineText = getLineText(caretLine);

            if (lineText.matches(STRING_TEXT)) {
                int leftQuotes = 0, rightQuotes = 0;

                for (int i = positionInLine - 1; i >= 0; i--) {
                    if (lineText.charAt(i) == '"') {
                        leftQuotes++;
                    }
                }
                for (int i = positionInLine; i < lineText.length(); i++) {
                    if (lineText.charAt(i) == '"') {
                        rightQuotes++;
                    }
                }

                boolean isInsideQuotes = (leftQuotes % 2 != 0) && (rightQuotes % 2 != 0);

                if (isInsideQuotes) {
                    splitString(caretLine);
                    return;
                }
            }

            if (lineText.matches(COMMENT_TEXT)) {
                if (!lineText.contains(OPEN_COMMENT)) {
                    int line = caretLine - 1;

                    while (line >= 0) {
                        if (!getLineText(line).matches(COMMENT_TEXT))
                            break;
                        line--;
                    }
                    if (!getLineText(line + 1).contains(OPEN_COMMENT)) {
                        insertNewLine(caret);
                        return;
                    }
                }
                int commentStart = lineText.indexOf(OPEN_COMMENT);
                int commentStop = (lineText.contains(CLOSE_COMMENT) ? lineText.indexOf(CLOSE_COMMENT)
                        : lineText.length()) + 2;

                if (positionInLine > commentStart && positionInLine < commentStop) {
                    splitComment(caretLine);
                    return;
                }
            }

            if (lineText.matches(BLOCK_OPENING)) {
                if (SmartCodePreferences.BRACKETS_AUTO_CLOSE) {

                    boolean bracketsAreBalanced = SmartCodeTextArea.checkBracketsBalance(getText(), "{", "}");
                    boolean hasClosingBrace = lineText.matches(BLOCK_CLOSING);
                    int openBrace = lineText.indexOf(OPEN_BRACE);
                    int closeBrace = lineText.indexOf(CLOSE_BRACE);

                    if ((!bracketsAreBalanced && positionInLine > openBrace) || (bracketsAreBalanced && hasClosingBrace
                            && positionInLine > openBrace && positionInLine <= closeBrace)) {

                        createBlockScope(caret);
                        return;
                    }
                }
            }
        }
        // if none of the above, then insert a new line
        insertNewLine(caret);
    }

    private void splitString(int caretLine) {
        int indent = 0;
        if (INDENT) {
            indent = getSmartCodeTextArea().getLineIndentation(caretLine);

            if (!getLineText(caretLine).matches(SPLIT_STRING_TEXT))
                indent += TAB_SIZE;
        }

        stopCompoundEdit();
        insertText("\"\n" + addSpaces(indent) + "+ \"");
        stopCompoundEdit();
    }

    private void splitComment(int caretLine) {
        int indent = 0;
        if (INDENT) {
            indent = getSmartCodeTextArea().getLineIndentation(caretLine);
        }

        startCompoundEdit();
        insertText(LF + addSpaces(indent - (indent % TAB_SIZE)) + " * ");

        int caretPos = getCaretOffset();
        String nextText = getText().substring(caretPos);

        // Checking if we need to close this comment
        int openingToken = nextText.indexOf(OPEN_COMMENT);
        int closingToken = nextText.indexOf(CLOSE_COMMENT);
        boolean commentIsOpen = (closingToken == -1) || (closingToken > openingToken && openingToken != -1);

        if (commentIsOpen) {
            textarea.setCaretPosition(getLineStopOffset(++caretLine) - 1);
            insertText(LF + addSpaces(indent - (indent % TAB_SIZE)) + " */");
            textarea.setCaretPosition(caretPos);
        }
        stopCompoundEdit();
    }

    private void createBlockScope(int offset) {
        int line = textarea.getLineOfOffset(offset);

        int indent = 0;
        if (INDENT) {
            indent = getSmartCodeTextArea().getLineIndentation(line) + TAB_SIZE;
        }

        startCompoundEdit();
        setSelection(offset, getLineStopOffset(line) - 1);

        String cutText = isSelectionActive() ? getSelectedText().trim() : "";

        if (cutText.matches(BLOCK_CLOSING)) {
            cutText = cutText.replace(CLOSE_BRACE, '\0').trim();
        }

        setSelectedText(LF + addSpaces(indent) + cutText);

        int newOffset = getCaretOffset();
        insertText(LF + addSpaces(indent - TAB_SIZE) + CLOSE_BRACE);
        setSelection(newOffset, newOffset);
        stopCompoundEdit();
    }

    private void insertNewLine(int offset) {
        int indent = 0;
        startCompoundEdit();

        // erase any selection content
        if (isSelectionActive()) { // TOOD: ha um bug aqui quando temos muito texto selecionado
            offset = getSelectionStart() + 1;
            setSelectedText("");
            setSelectedText(LF);

            setSelection(offset, offset);
            stopCompoundEdit();

        }
        if (INDENT) {
            int line = textarea.getLineOfOffset(offset);
            String lineText = getLineText(line);

            int startBrace = getSmartCodeTextArea().getMatchingBraceLine(line, true);

            if (startBrace != -1) {
                indent = getSmartCodeTextArea().getLineIndentation(startBrace);

                if (!lineText.matches(BLOCK_CLOSING))
                    indent += TAB_SIZE;

                int positionInLine = getSmartCodeTextArea().getPositionInsideLineWithOffset(offset);

                if (lineText.matches(BLOCK_OPENING) && positionInLine <= lineText.indexOf(OPEN_BRACE))
                    indent -= TAB_SIZE;
            }
            setSelection(offset, getLineStopOffset(line) - 1);
        }
        String cutText = isSelectionActive() ? getSelectedText().trim() : "";
        setSelectedText(LF + addSpaces(indent) + cutText);

        int newOffset = offset + indent + 1;
        setSelection(newOffset, newOffset);
        stopCompoundEdit();
    }

    @Override
    public void handleAutoFormat() {
        if (isSelectionActive()) {

            if (getSelectedText().isBlank()) {
                return;
            }

            Selection s = new Selection();

            String selectedText;
            boolean isSourceIntact = true;

            // long string literals are formatted here
            if (SmartCodePreferences.AUTOFORMAT_STRINGS) {
                selectedText = refactorStringLiterals(s.getText());
                isSourceIntact = selectedText.stripTrailing().equals(s.getText());
            } else {
                selectedText = s.getText();
            }

            // and everything else is formatted here
            String formattedText = createFormatter().format(selectedText);

            // but they need to be indented, anyway...
            int brace = getSmartCodeTextArea().getMatchingBraceLine(s.getStartLine() - 1, true);
            int indent = 0;

            if (brace != -1) {
                indent = getSmartCodeTextArea().getLineIndentation(brace) + TAB_SIZE;
            }

            formattedText = SmartCodeTextArea.indentText(formattedText, indent);

            if (formattedText.equals(selectedText) && isSourceIntact) {
                statusNotice(Language.text("editor.status.autoformat.no_changes"));

            } else {
                int start = s.getStart();
                int end = s.getEnd() + 1;

                startCompoundEdit();

                setSelection(start, end);
                setSelectedText(formattedText);

                end = start + formattedText.length() - 1;
                setSelection(start, end);

                stopCompoundEdit();

                getSketch().setModified(true);
                statusNotice(Language.text("editor.status.autoformat.finished"));
            }

        } else {
            int caretPos = getCaretOffset();
            int scrollPos = getScrollPosition();

            handleSelectAll();
            handleAutoFormat();
            setSelection(caretPos, caretPos);

            if (scrollPos != getScrollPosition()) {
                textarea.setVerticalScrollPosition(scrollPos);
            }
        }
    }

    protected static String refactorStringLiterals(String text) {
        int maxLength = SmartCodePreferences.AUTOFORMAT_LINE_LENGTH;

        List<String> lines = new ArrayList<>(Arrays.asList(text.split(LF)));
        int depth = 0;
        int indent = 0;

        for (int i = 0; i < lines.size(); i++) {
            String lineText = lines.get(i);

            if (lineText.matches(STRING_TEXT) && lineText.length() > maxLength) {

                if (depth == 0) {
                    indent = SmartCodeTextArea.getLineIndentation(lineText);
                }

                String preffix = addSpaces(indent) + TAB + "+ \"";

                String currLine = lineText.substring(0, maxLength - 1) + "\"";
                String nextLine = preffix + lineText.substring(maxLength - 1);

                lines.set(i, currLine);
                lines.add(i + 1, nextLine);
                depth++;

            } else {
                lines.set(i, lineText);
                depth = 0;
                indent = 0;
            }
        }

        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            result.append(line + LF);
        }

        return result.toString();
    }

    public void deleteLine(int line) {
        // in case we are in the last line of text (but not when it's also first one)
        if (line == getLineCount() - 1 && line != 0) {
            // subtracting 1 from getLineStartOffset() will delete the line break prior
            // to this line, causing the caret to move to the end of the previous line
            int start = getLineStartOffset(line) - 1;
            int end = getLineStopOffset(line) - 1;

            setSelection(start, end);
            setSelectedText("");

        } else if (getLineCount() > 1) {
            setLineText(line, "");

        } else { // in case we are deleting the only line that remains
            if (getLineText(line).isEmpty()) {
                getToolkit().beep();
                return;
            }
            setText("");
        }
    }

    public void deleteLineContent(int line) {
        int start = getTextArea().getLineStartNonWhiteSpaceOffset(line);
        int end = getLineStopOffset(line) - 1;

        setSelection(start, end);
        setSelectedText("");
    }

    public void toggleBlockComment() {
        if (isSelectionActive()) {
            String selectedText = getSelectedText();
            StringBuilder result = new StringBuilder();

            if (selectedText.startsWith(OPEN_COMMENT) && selectedText.endsWith(CLOSE_COMMENT)) {
                result.append(selectedText);
                result.delete(0, 3);
                result.delete(result.length() - 3, result.length());

            } else {
                result.append(OPEN_COMMENT).append(" " + selectedText + " ").append(CLOSE_COMMENT);
            }

            int selectionStart = getSelectionStart();
            int selectionEnd = selectionStart + result.length();

            startCompoundEdit();
            setSelectedText(result.toString());
            setSelection(selectionStart, selectionEnd);
            stopCompoundEdit();
        }
    }

    public void changeCase(boolean toUpperCase) {
        if (isSelectionActive()) {
            int start = getSelectionStart();
            int end = getSelectionStop();

            if (toUpperCase) {
                setSelectedText(getSelectedText().toUpperCase());
            } else {
                setSelectedText(getSelectedText().toLowerCase());
            }
            setSelection(start, end);
        }
    }

    public void duplicateLines(boolean up) {
        Selection s = new Selection();

        if (s.getEndLine() == getLineCount() - 1) {
            int caret = getLineStopOffset(s.getEndLine());
            setSelection(caret, caret);
            insertText(LF + s.getText());

        } else {
            int caret = s.getEnd() + 1;
            setSelection(caret, caret);
            insertText(s.getText() + LF);
        }

        if (up)
            setSelection(s.getEnd(), s.getStart());
        else
            setSelection(getCaretOffset() - 1, s.getEnd() + 1);
    }

    public void moveLines(boolean moveUp) {
        Selection s = new Selection();

        int targetLine = moveUp ? s.getStartLine() - 1 : s.getEndLine() + 1;

        if (targetLine < 0 || targetLine >= getLineCount()) {
            getToolkit().beep();
            return;
        }

        int target_start = getLineStartOffset(targetLine);
        int target_end = getLineStopOffset(targetLine) - 1;

        String selectedText = s.getText();
        String replacedText = getText(target_start, target_end);

        startCompoundEdit();
        setSelection(s.getStart(), s.getEnd());

        int newSelectionStart, newSelectionEnd;

        // SWAP LINES
        if (moveUp) {
            setSelection(target_start, s.getEnd());
            setSelectedText(selectedText + LF + replacedText);

            newSelectionStart = getLineStartOffset(targetLine);
            newSelectionEnd = getLineStopOffset(s.getEndLine() - 1) - 1;

        } else {
            setSelection(s.getStart(), target_end);
            setSelectedText(replacedText + LF + selectedText);

            newSelectionStart = getLineStartOffset(s.getStartLine() + 1);
            newSelectionEnd = getLineStopOffset(targetLine) - 1;
        }

        // UPDATE SELECTION
        setSelection(newSelectionStart, newSelectionEnd);

        // RESOLVE INDENTATION
        if (!SmartCodePreferences.MOVE_LINES_AUTO_INDENT) {
            stopCompoundEdit();
            return;
        }

        final SmartCodeTextArea ta = getSmartCodeTextArea();

        s = new Selection();

        int line = s.getStartLine();
        String lineText = getLineText(line);

        int blockIndent = 0;
        int brace = getSmartCodeTextArea().getMatchingBraceLineAlt(line);

        if (brace != -1) { // we are inside a block here
            if (lineText.matches(BLOCK_OPENING)) {
                brace = ta.getMatchingBraceLineAlt(line);
                blockIndent = ta.getLineIndentation(brace) + TAB_SIZE;

            } else if (lineText.matches(BLOCK_CLOSING)) {
                brace = ta.getMatchingBraceLine(line, true);
                blockIndent = ta.getLineIndentation(brace);

            } else {
                blockIndent = ta.getLineIndentation(brace) + TAB_SIZE;
            }
        }

        int selectionIndent = SmartCodeTextArea.getLineIndentation(lineText);

        if (selectionIndent < blockIndent)
            handleIndent();

        else if (selectionIndent > blockIndent)
            handleOutdent();

        stopCompoundEdit();
    }

    public void insertNewLineBellow(int line) {
        int indent = 0;

        if (INDENT) {
            if (getLineText(line).matches(BLOCK_CLOSING)) {
                indent = getSmartCodeTextArea().getLineIndentation(line);

            } else {
                int startBrace = getSmartCodeTextArea().getMatchingBraceLine(line, true);
                if (startBrace != -1) {
                    indent = getSmartCodeTextArea().getLineIndentation(startBrace) + TAB_SIZE;
                }
            }
        }

        int lineEnd = getLineStopOffset(line) - 1;

        startCompoundEdit();
        setSelection(lineEnd, lineEnd);
        insertText(LF + addSpaces(indent));
        stopCompoundEdit();
    }

    public void insertNewLineAbove(int line) {
        String indent = addSpaces(getSmartCodeTextArea().getLineIndentation(line));
        int lineStart = getTextArea().getLineStartNonWhiteSpaceOffset(line);

        startCompoundEdit();
        setSelection(lineStart, lineStart);
        insertText(LF + indent);
        setSelection(lineStart, lineStart);
        stopCompoundEdit();
    }

    public void insertLineBreak(int offset) {
        final SmartCodeTextArea ta = getSmartCodeTextArea();
        int line = ta.getLineOfOffset(offset);
        String lineText = getLineText(line);

        int indent = 0;
        int caretPos = ta.caretPositionInsideLine();

        if (INDENT) {

            if (lineText.matches(BLOCK_OPENING)) {
                indent = ta.getLineIndentation(line);

                if (caretPos > lineText.indexOf('{'))
                    indent += TAB_SIZE;

            } else if (lineText.matches(BLOCK_CLOSING)) {
                indent = ta.getLineIndentation(line);
                int closeBrace = lineText.indexOf('}');

                if (caretPos <= closeBrace) {
                    offset += (closeBrace - caretPos);
                    setSelection(offset, offset);
                    insertText(TAB);
                    offset += TAB_SIZE;
                }

            } else {
                int startBrace = ta.getMatchingBraceLine(line, true);

                if (startBrace != -1) // an opening brace was found, we are in a block scope
                    indent = ta.getLineIndentation(startBrace) + TAB_SIZE;
            }
        }

        startCompoundEdit();
        insertText(LF + addSpaces(indent));
        setSelection(offset, offset);
        stopCompoundEdit();
    }

    public void handleTabulation(boolean isShiftDown) {
        if (isShiftDown) {
            handleOutdent();

        } else if (isSelectionActive()) {
            handleIndent();

        } else if (Preferences.getBoolean("editor.tabs.expand")) {
            // "editor.tabs.expand" means that each tab is made up of a
            // stipulated number of spaces, and not just a single solid \t
            setSelectedText(TAB);

        } else {
            setSelectedText("\t");
        }
    }

    // TODO: corrigir bug quando ha dois braces na mesma linha
    // ex: if() {
    // } else { <-- esta a ser ignorado!!
    // }
    public void expandSelection() {
        int start = getSelectionStart();
        int end = getSelectionStop();

        Selection s = new Selection();

        int startLine = s.getStartLine();
        int endLine = s.getEndLine();

        if (isSelectionActive()) {
            String code = getText();

            int lastLineOfSelection = textarea.getSelectionStopLine();
            boolean isLastBlock = (getSelectionStop() == getLineStartOffset(lastLineOfSelection));

            if (isLastBlock) {
                end = getLineStopOffset(lastLineOfSelection) - 1;
                setSelection(s.getStart(), end);
                return;

            }
            if (code.charAt(start - 1) == OPEN_BRACE && code.charAt(end) == CLOSE_BRACE) {
                setSelection(s.getStart(), s.getEnd());
                return;

            }
            if (start == s.getStart() && end == s.getEnd()) {
                startLine--;
                endLine++;
            }
        }

        final SmartCodeTextArea ta = getSmartCodeTextArea();

        // go up and search for the corresponding open brace
        int brace = ta.getMatchingBraceLine(startLine, true);

        // open brace not found
        if (brace == -1) {
            return;
        }

        int lineEnd = getLineStopOffset(brace) - 1;
        start = ta.getOffsetOfPrevious(OPEN_BRACE, lineEnd) + 1;

        // now go down and search for the corresponding close brace
        brace = ta.getMatchingBraceLine(endLine, false);

        // close brace not found
        if (brace == -1) {
            return;
        }

        lineEnd = getLineStopOffset(brace) - 1;
        end = ta.getOffsetOfPrevious(CLOSE_BRACE, lineEnd);

        setSelection(start, end);
    }

    class Selection {
        private String text = "";
        private int start, end, startLine, endLine;

        public Selection() {
            startLine = textarea.getSelectionStartLine();
            endLine = textarea.getSelectionStopLine();

            // in case this selection ends with the caret at the beginning of the last line,
            // not selecting any text
            if (isSelectionActive() && getLineStartOffset(endLine) == getSelectionStop()) {
                endLine--;
            }

            start = getLineStartOffset(startLine);
            end = Math.max(start, getLineStopOffset(endLine) - 1);

            text = SmartCodeEditor.this.getText(start, end);
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getStartLine() {
            return startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public String getText() {
            return text;
        }

        public boolean isEmpty() {
            return text.isEmpty();
        }
    }

    static String addSpaces(int length) {
        if (length <= 0)
            return "";
        return String.format("%1$" + length + "s", "");
    }

    /****************
     * 
     * LINE PINS
     * 
     */

    public void toggleLinePin(int line) {
        if (!isDebuggerEnabled()) {
            final LineID lineID = getLineIDInCurrentTab(line);
            
            if (isLinePinned(lineID)) {
                removePinnededLine(lineID);
            } else {
                addPinnededLine(lineID);
            }
        }
        System.out.println("pins: " + pinnedLines.size());
    }

    /** Add highlight for a pinned line. */
    protected void addPinnededLine(LineID lineID) {
        LineHighlight hl = new LineHighlight(lineID, this);
        hl.setMarker(PIN_MARKER);
        pinnedLines.add(hl);
    }

    /** Clear the highlight for the pinned line. */
    protected void removePinnededLine(LineID lineID) {
        LineHighlight pin = getPinnedLine(lineID);

        if (pin != null) {
            pin.clear();
            pinnedLines.remove(pin);
            pin.dispose();
            // repaint current line if it's on this line
            if (currentLine != null && currentLine.getLineID().equals(lineID)) {
                currentLine.paint();
            }
        }
    }
    
    public boolean isLinePinned(int line) {
        return isLinePinned(getLineIDInCurrentTab(line));
    }

    public boolean isLinePinned(LineID lineID) {
        for (LineHighlight pin : pinnedLines) {
            if (pin.getLineID().equals(lineID)) {
                return true;
            }
        }
        return false;
    }

    protected LineHighlight getPinnedLine(LineID lineID) {
        for (LineHighlight pin : pinnedLines) {
            if (pin.getLineID().equals(lineID)) {
                return pin;
            }
        }
        return null;
    }

    public Set<LineHighlight> getPinnedLines() {
        return pinnedLines;
    }

    @Override
    public void setCode(SketchCode code) {
        super.setCode(code);
        // send information to SmartCodeTextAreaPainter.paintLeftGutter()
        // to paint these lines
        if (pinnedLines != null) {
            for (LineHighlight hl : pinnedLines) {
                if (isInCurrentTab(hl.getLineID())) {
                    hl.paint();
                }
            }
        }
    }
}
