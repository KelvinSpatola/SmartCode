package kelvinspatola.mode.smartcode;

import static kelvinspatola.mode.smartcode.Constants.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.*;

import kelvinspatola.mode.smartcode.ui.*;
import processing.app.Base;
import processing.app.Language;
import processing.app.Messages;
import processing.app.Mode;
import processing.app.Platform;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.syntax.DefaultInputHandler;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.app.ui.EditorException;
import processing.app.ui.EditorState;
import processing.app.ui.MarkerColumn;
import processing.app.ui.Toolkit;
import processing.core.PApplet;
import processing.mode.java.JavaEditor;
import processing.mode.java.debug.LineID;

public class SmartCodeEditor extends JavaEditor implements KeyListener {
    protected final List<LineMarker> bookmarkedLines = new ArrayList<>();
    protected CodeOccurrences occurrences;
    protected ShowBookmarks showBookmarks;
    private Color currentBookmarkColor = SmartCodePreferences.BOOKMARKS_HIGHLIGHT_COLOR;


    // CONSTRUCTOR
    public SmartCodeEditor(Base base, String path, EditorState state, Mode mode) throws EditorException {
        super(base, path, state, mode);

        showBookmarks = new ShowBookmarks(this, bookmarkedLines);
        if (SmartCodePreferences.BOOKMARKS_HIGHLIGHT) {
            getSmartCodePainter().addLinePainter(getBookmarkLinePainter());
        }

        buildMenu();
        buildPopupMenu();
        buildGutterPopupMenu();
        buildMarkerColumn(new SmartCodeMarkerColumn(this, textarea.getMinimumSize().height));

        occurrences = new CodeOccurrences(this, preprocService);
        textarea.addCaretListener(occurrences);
        getSmartCodePainter().addLinePainter(occurrences);

        // get bookmarkers from marker comments
        for (LineID lineID : stripBookmarkComments()) {
            addLineBookmark(lineID);
        }
        getSketch().setModified(false);

        printHelloMessage();
    }

    private void printHelloMessage() {
        statusNotice("SmartCode is active");
        console.message("SmartCode 0.0.1\n" + "created by Kelvin Spatola\n", false);
    }

    @Override
    protected JEditTextArea createTextArea() {
        return new SmartCodeTextArea(new PdeTextAreaDefaults(), this);
    }

    public SmartCodeTextArea getSmartCodeTextArea() {
        return (SmartCodeTextArea) textarea;
    }

    public SmartCodeTextAreaPainter getSmartCodePainter() {
        return getSmartCodeTextArea().getSmartCodePainter();
    }

    protected void buildMarkerColumn(MarkerColumn errorColumn) {
        // hack to add a JPanel to the right-hand side of the text area
        JPanel editorPanel = (JPanel) textarea.getParent();
        // remove the original errorColumn and textarea objects
        editorPanel.removeAll();
        // creating a new BorderLayout
        editorPanel.setLayout(new BorderLayout());
        // replacing the original MarkerColumn object with our custom
        // SmartCodeMarkerColumn object
        this.errorColumn = errorColumn;
        editorPanel.add(errorColumn, BorderLayout.EAST);
        textarea.setBounds(0, 0, errorColumn.getX() - 1, textarea.getHeight());
        // let's put textarea back in the game
        editorPanel.add(textarea);
    }

    // TODO: lembrete de que é preciso trabalhar aqui
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

        JMenuItem showBookmarksItem = createItem(menu, "Show bookmarks", null, showBookmarks::handleShowBookmarks);
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
        JMenuItem[] updatableItems = { createItem(menu, "Toggle block comment", "C+7", this::toggleBlockComment),
                createItem(menu, "Format selected text", "C+T", this::handleAutoFormat),
                createItem(menu, "To upper case", "CS+U", () -> changeCase(true)),
                createItem(menu, "To lower case", "CS+L", () -> changeCase(false)) };
        createItem(menu, "Expand Selection", "CA+RIGHT", this::expandSelection);

        menu.addSeparator(); // ---------------------------------------------
        createItem(menu, "Visit GitHub page", null,
                () -> Platform.openURL("https://github.com/KelvinSpatola/SmartCode"));

        // Update state on selection/de-selection
        menu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuSelected(MenuEvent e) {
                showBookmarksItem.setEnabled(!bookmarkedLines.isEmpty());

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

        popup.addSeparator(); // ---------------------------------------------
        JMenuItem[] selectableItems = { createItem(popup, "Format selected text", null, this::handleAutoFormat),
                createItem(popup, "Toggle block comment", null, this::toggleBlockComment),
                createItem(popup, "To upper case", null, () -> changeCase(true)),
                createItem(popup, "To lower case", null, () -> changeCase(false)) };

        // Update state on selection/de-selection
        popup.addPopupMenuListener(new MenuAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                for (JMenuItem item : selectableItems) {
                    item.setEnabled(isSelectionActive());
                }
            }
        });
    }

    private void buildGutterPopupMenu() {
        
        class GutterPopupMenu extends JPopupMenu {
            JMenu submenu = new JMenu();
            JMenuItem removeBookmarkItem, showBookmarksListItem;
            int line;
            
            GutterPopupMenu() {
                createItem(submenu, "Violet", null, () -> setBookmarkColor(line, Color.decode("#9b5de5")));
                createItem(submenu, "Magenta", null, () -> setBookmarkColor(line, Color.decode("#f15bb5")));
                createItem(submenu, "Yellow", null, () -> setBookmarkColor(line, Color.decode("#fee440")));
                createItem(submenu, "Blue", null, () -> setBookmarkColor(line, Color.decode("#00bbf9")));
                createItem(submenu, "Green", null, () -> setBookmarkColor(line, Color.decode("#00f5d4")));
                
                add(submenu);    
                removeBookmarkItem = createItem(this, "Remove bookmark", null, () -> removeLineBookmark(getLineIDInCurrentTab(line)));
                addSeparator();       
                showBookmarksListItem = createItem(this, "Show bookmarks", null, showBookmarks::handleShowBookmarks);
            }
               
            void setBookmarkColor(int line, Color color) {
                currentBookmarkColor = color;
                final LineID lineID = getLineIDInCurrentTab(line);
                
                if (isLineBookmark(lineID)) {
                    getLineBookmark(lineID).setColor(color);
                } else {
                    addLineBookmark(lineID);
                }
            }
            
            @Override
            public void show(Component component, int x, int y) {
                line = textarea.yToLine(y);
                boolean isLineBookmark = isLineBookmark(line);
                
                submenu.setText(isLineBookmark ? "Change color" : "Add bookmark");
                removeBookmarkItem.setVisible(isLineBookmark);
                showBookmarksListItem.setEnabled(!bookmarkedLines.isEmpty());
                super.show(component, x, y);
            }
        };
        
        getSmartCodeTextArea().setGutterRightClickPopup(new GutterPopupMenu());
    }

    protected static JMenuItem createItem(JComponent menu, String title, String keyBinding, Runnable action) {
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

    abstract static class MenuAdapter implements MenuListener, PopupMenuListener {
        @Override
        public void menuCanceled(MenuEvent e) {
        }

        @Override
        public void menuDeselected(MenuEvent e) {
        }

        @Override
        public void menuSelected(MenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        }
    }

    /*
     * ******** METHODS ********
     */

    @Override
    public boolean handlePressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        boolean noModifiers = (e.getModifiersEx()
                & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)) == 0;

        if (keyCode == KeyEvent.VK_ENTER && noModifiers) {
            handleEnter();

        } else if (keyCode == KeyEvent.VK_TAB) {
            handleTabulation(e.isShiftDown());

        } else if (keyCode == KeyEvent.VK_DELETE) {
            if (bookmarkedLines.isEmpty())
                return false;

            int line = textarea.getCaretLine();
            if (getCaretOffset() == textarea.getLineStopOffset(line) - 1) {
                if (isLineBookmark(line + 1)) {
                    if (getLineText(line + 1).isBlank()) {
                        removeLineBookmark(getLineIDInCurrentTab(line + 1));
                    } else {
                        removeLineBookmark(getLineIDInCurrentTab(line));
                    }
                }
            }

        } else if (keyCode == KeyEvent.VK_BACK_SPACE) { // let's deal with bookmarks deletion here
            if (bookmarkedLines.isEmpty())
                return false;

            int startLine = textarea.getSelectionStartLine();
            int endLine = textarea.getSelectionStopLine();

            if (startLine != endLine) {
                if (getSelectionStart() > getLineStartOffset(startLine))
                    startLine++;

                if (getLineStartOffset(endLine) == getSelectionStop())
                    endLine--;

                for (int line = startLine; line <= endLine; line++) {
                    removeLineBookmark(getLineIDInCurrentTab(line));
                }
            } else {
                int line = textarea.getCaretLine();
                if (isLineBookmark(line)) {
                    int lineIndent = 0;
                    int brace = getSmartCodeTextArea().getMatchingBraceLine(line, true);
                    if (brace != -1) {
                        lineIndent = getSmartCodeTextArea().getLineIndentation(brace) + TAB_SIZE;
                    }

                    String lineText = getLineText(line);
                    if (lineText.isBlank() && lineText.length() <= lineIndent) {
                        removeLineBookmark(getCurrentLineID());
                    }
                }
            }
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
        errorColumn.repaint();
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
        if (isSelectionActive()) {
            if (!bookmarkedLines.isEmpty()) {
                int startLine = textarea.getSelectionStartLine();
                int endLine = textarea.getSelectionStopLine();

                if (startLine != endLine) {
                    if (getSelectionStart() > getLineStartOffset(startLine))
                        startLine++;

                    if (getLineStartOffset(endLine) == getSelectionStop())
                        endLine--;

                    for (int line = startLine; line <= endLine; line++) {
                        removeLineBookmark(getLineIDInCurrentTab(line));
                    }
                }
            }
            setSelectedText("");
            setSelectedText(LF);
            stopCompoundEdit();
            return;
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
            String selectedText = s.getText();

            final List<Integer> removedLines = new ArrayList<>();
            boolean isBookmarksRemoved = false;

            //
            if (!bookmarkedLines.isEmpty()) {
                final int currTabIndex = getSketch().getCurrentCodeIndex();

                for (int i = bookmarkedLines.size() - 1; i >= 0; i--) {
                    LineBookmark bm = (LineBookmark) bookmarkedLines.get(i);
                    int bmLine = bm.getLineID().lineIdx();

                    if (bm.getTabIndex() == currTabIndex && bmLine >= s.getStartLine() && bmLine <= s.getEndLine()) {
                        removedLines.add(bmLine);
                    }
                }

                if (!removedLines.isEmpty()) {
                    String[] textLines = selectedText.split("\\r?\\n");

                    for (int line : removedLines) {
                        textLines[line - s.getStartLine()] += PIN_MARKER;
                    }
                    selectedText = PApplet.join(textLines, "\n");
                    isBookmarksRemoved = true;
                }
            }

            boolean isSourceIntact = true;

            // long string literals are formatted here
            if (SmartCodePreferences.AUTOFORMAT_STRINGS) {
                String sourceText = selectedText;
                selectedText = refactorStringLiterals(sourceText);
                isSourceIntact = selectedText.stripTrailing().equals(sourceText);
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

                for (int line : removedLines) {
                    removeLineBookmark(getLineIDInCurrentTab(line));
                }

                setSelection(start, end);
                setSelectedText(formattedText);

                end = start + formattedText.length() - 1;
                setSelection(start, end);

                if (isBookmarksRemoved) {
                    String[] textLines = formattedText.split("\\r?\\n");

                    int line = s.getStartLine();
                    for (String textLine : textLines) {
                        if (textLine.endsWith(PIN_MARKER)) {
                            textarea.select(getLineStartOffset(line), getLineStopOffset(line) - 1);
                            textarea.setSelectedText(textLine.replaceAll(PIN_MARKER, ""));
                            addLineBookmark(getLineIDInCurrentTab(line));
                        }
                        line++;
                    }
                }

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
        int maxLength = SmartCodePreferences.AUTOFORMAT_STRINGS_LENGTH;

        // Concatenate every multiline split-string into a single one-line string before
        // doing anything else.
        String preformattedText = text.replaceAll("\\\"\\\n\\h*\\+\\s*\\\"", "");

        List<String> lines = new ArrayList<>(Arrays.asList(preformattedText.split(LF)));
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
        lines.forEach(line -> result.append(line + LF));

        return result.toString();
    }

    public void deleteLine(int line) {
        if (!bookmarkedLines.isEmpty()) {
            removeLineBookmark(getCurrentLineID());
        }

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
        int selectionStartLine = s.getStartLine();
        int selectionEndLine = s.getEndLine();

        // SWAP LINES

        stopBookmarkTracking();

        LineID lineID = getLineIDInCurrentTab(targetLine);
        boolean isTargetLineBookmarked;
        if (isTargetLineBookmarked = isLineBookmark(lineID))
            removeLineBookmark(lineID);

        if (moveUp) {
            setSelection(target_start, s.getEnd());
            setSelectedText(selectedText + LF + replacedText);

            newSelectionStart = getLineStartOffset(targetLine);
            newSelectionEnd = getLineStopOffset(s.getEndLine() - 1) - 1;

            for (int line = selectionStartLine; line <= selectionEndLine; line++)
                moveBookmarkTo(line, line - 1);

        } else {
            setSelection(s.getStart(), target_end);
            setSelectedText(replacedText + LF + selectedText);

            newSelectionStart = getLineStartOffset(s.getStartLine() + 1);
            newSelectionEnd = getLineStopOffset(targetLine) - 1;

            for (int line = selectionEndLine; line >= selectionStartLine; line--)
                moveBookmarkTo(line, line + 1);
        }

        if (isTargetLineBookmarked)
            addLineBookmark(getLineIDInCurrentTab(moveUp ? selectionEndLine : selectionStartLine));

        startBookmarkTracking();

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
        int brace = ta.getMatchingBraceLineAlt(line);

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
     * LINE BOOKMARKS
     * 
     */

    public void toggleLineBookmark(int line) {
        if (!isDebuggerEnabled()) {
            final LineID lineID = getLineIDInCurrentTab(line);

            if (isLineBookmark(lineID)) {
                removeLineBookmark(lineID);
            } else {
                addLineBookmark(lineID);
            }
            getSketch().setModified(true);
        }
    }

    protected void addLineBookmark(LineID lineID) {
        bookmarkedLines.add(new LineBookmark(this, lineID, currentBookmarkColor));
        bookmarkedLines.sort(null);
        updateColumnPoints(bookmarkedLines, LineBookmark.class);
        showBookmarks.updateTree();
    }

    protected void removeLineBookmark(LineID lineID) {
        LineBookmark bm = getLineBookmark(lineID);

        if (bm != null) {
            bm.dispose();
            bookmarkedLines.remove(bm);
            bm = null;
            // repaint current line if it's on this line
            if (currentLine != null && currentLine.getLineID().equals(lineID)) {
                currentLine.paint();
            }
            updateColumnPoints(bookmarkedLines, LineBookmark.class);
            showBookmarks.updateTree();
        }
    }

    public boolean isLineBookmark(int line) {
        return isLineBookmark(getLineIDInCurrentTab(line));
    }

    public boolean isLineBookmark(LineID lineID) {
        for (LineMarker lm : bookmarkedLines) {
            LineBookmark bm = (LineBookmark) lm;
            if (bm.isOnLine(lineID)) {
                return true;
            }
        }
        return false;
    }

    protected LineBookmark getLineBookmark(LineID lineID) {
        for (LineMarker lm : bookmarkedLines) {
            LineBookmark bm = (LineBookmark) lm;
            if (bm.isOnLine(lineID)) {
                return bm;
            }
        }
        return null;
    }

    protected boolean moveBookmarkTo(int oldLinePos, int newLinePos) {
        LineID lineID = getLineIDInCurrentTab(oldLinePos);
        if (isLineBookmark(lineID)) {
            removeLineBookmark(lineID);
            addLineBookmark(getLineIDInCurrentTab(newLinePos));
            return true;
        }
        return false;
    }

    public List<LineBookmark> getBookmarkedLines() {
        return bookmarkedLines.stream().map(bm -> (LineBookmark) bm).collect(Collectors.toList());
    }

    public void stopBookmarkTracking() {
        bookmarkedLines.forEach(bm -> ((LineBookmark) bm).stopTracking());
    }

    public void startBookmarkTracking() {
        bookmarkedLines.forEach(bm -> ((LineBookmark) bm).startTracking());
    }

    protected void clearBookmarksFromTab(int tabIndex) {
        for (int i = bookmarkedLines.size() - 1; i >= 0; i--) {
            LineBookmark bm = (LineBookmark) bookmarkedLines.get(i);
            if (bm.getTabIndex() == tabIndex) {
                removeLineBookmark(bm.getLineID());
            }
        }
    }

    protected LinePainter getBookmarkLinePainter() {
        return (gfx, line, y, h, textarea) -> {
            if (!isDebuggerEnabled() && isLineBookmark(line)) {
                gfx.setColor(getLineBookmark(getLineIDInCurrentTab(line)).getColor());
                gfx.fillRect(0, y, getWidth(), h);
                return true;
            }
            return false;
        };
    }

    protected void addBookmarkComments(String tabFilename) {
        final List<Integer> bms = new ArrayList<>();
        for (LineBookmark bm : getBookmarkedLines()) {
            LineID lineID = bm.getLineID();
            if (lineID.fileName().equals(tabFilename)) {
                bms.add(lineID.lineIdx());
            }
        }

        SketchCode tab = getTab(tabFilename);
        try {
            tab.load();
            String code = tab.getProgram();
            String[] codeLines = code.split("\\r?\\n");
            for (int line : bms) {
                // to avoid duplication, do it only if this line is not already marked
                if (!codeLines[line].endsWith(PIN_MARKER)) {
                    codeLines[line] += PIN_MARKER;
                }
            }
            code = PApplet.join(codeLines, "\n");
            tab.setProgram(code);
            tab.save();
        } catch (IOException ex) {
            Messages.err(null, ex);
        }
    }

    protected List<LineID> stripBookmarkComments() {
        List<LineID> bms = new ArrayList<>();
        // iterate over all tabs
        Sketch sketch = getSketch();
        for (SketchCode tab : sketch.getCode()) {
            String code = tab.getProgram();
            String[] codeLines = code.split("\\r?\\n");

            // scan code for bookmark comments
            int line = 0;
            for (String textLine : codeLines) {
                if (textLine.endsWith(PIN_MARKER)) {
                    bms.add(new LineID(tab.getFileName(), line));
                    codeLines[line] = textLine.replaceAll(PIN_MARKER, "");
                }
                line++;
            }
            code = PApplet.join(codeLines, "\n");
            tab.setProgram(code);
            setTabContents(tab.getFileName(), code);
        }
        return bms;
    }

    /****************
     * 
     * MISC
     * 
     */

    @Override
    public void setCode(SketchCode code) {
        super.setCode(code);
        // send information to SmartCodeTextAreaPainter.paintLeftGutter()
        // to paint these lines
        if (bookmarkedLines != null) {
            for (LineMarker lm : bookmarkedLines) {
                LineBookmark bm = (LineBookmark) lm;
                if (isInCurrentTab(bm.getLineID())) {
                    bm.paint();
                }
            }
        }
//        errorColumn.repaint();
    }

    @Override
    public void sketchChanged() {
        super.sketchChanged();

        if (showBookmarks != null) {
            showBookmarks.updateTree();
        }
    }

    @Override
    protected void handleOpenInternal(String path) throws EditorException {
        try {
            sketch = new SmartCodeSketch(path, this);
        } catch (IOException e) {
            throw new EditorException("Could not create the sketch.", e);
        }
        header.rebuild();
        updateTitle();
    }

    public void highlight(LineMarker lm) {
        highlight(lm.getTabIndex(), lm.getStartOffset(), lm.getStopOffset());
    }

    @Override
    public void highlight(int tabIndex, int startOffset, int stopOffset) {
        // Switch to tab
        toFront();
        sketch.setCurrentCode(tabIndex);

        // Make sure offsets are in bounds
        int length = textarea.getDocumentLength();
        startOffset = PApplet.constrain(startOffset, 0, length);
        stopOffset = PApplet.constrain(stopOffset, 0, length);

        int firstLine = textarea.getFirstLine();

        // Highlight the code
        textarea.select(startOffset, stopOffset);

        // Scroll to error line
        int targetLine = textarea.getLineOfOffset(startOffset);
        int visibleLines = textarea.getVisibleLines();

        if (targetLine < firstLine) {
            targetLine -= (visibleLines / 2);
            if (targetLine <= 0)
                targetLine = 0;
        } else {
            targetLine += (visibleLines / 2);
            if (targetLine >= getLineCount())
                targetLine = getLineCount() - 1;
        }
        textarea.scrollTo(targetLine, 0);
        repaint();
    }

    @Override
    public void dispose() {
        showBookmarks.dispose();
        super.dispose();
    }

    public void updateColumnPoints(List<LineMarker> points, Class<?> parent) {
        ((SmartCodeMarkerColumn) errorColumn).updatePoints(points, parent);
    }
}
