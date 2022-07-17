package kelvinspatola.mode.smartcode;

import static kelvinspatola.mode.smartcode.Constants.*;
import static kelvinspatola.mode.smartcode.ui.SmartCodeTheme.OCCURRENCES_HIGHLIGHT;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.CaretListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import kelvinspatola.mode.smartcode.ui.*;
import kelvinspatola.mode.smartcode.ui.LineBookmarks.Bookmark;
import kelvinspatola.mode.smartcode.ui.LineBookmarks.BookmarkListListener;
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
import processing.app.ui.MarkerColumn;
import processing.app.ui.Toolkit;
import processing.core.PApplet;
import processing.mode.java.JavaEditor;
import processing.mode.java.debug.LineID;

public class SmartCodeEditor extends JavaEditor implements KeyListener {
	static private SmartCodePreferencesFrame preferencesFrame;
	private ColorTag currentBookmarkColor = ColorTag.COLOR_1;
    private CodeOccurrences occurrences;
    private LineBookmarks lineBookmarks;
    private ShowBookmarks showBookmarks;
    private Timer generalTimer;

    protected int tabSize;
    protected String tabSpaces;

//    CodeContext context;

    // CONSTRUCTOR
    public SmartCodeEditor(Base base, String path, EditorState state, Mode mode) throws EditorException {
        super(base, path, state, mode);

        lineBookmarks = new LineBookmarks(this);
        getSmartCodePainter().addLinePainter(lineBookmarks);

        showBookmarks = new ShowBookmarks(this);

        buildMenu();
        buildPopupMenu();
        buildGutterPopupMenu();
        buildMarkerColumn(new SmartCodeMarkerColumn(this));

        // get bookmarks from marker comments
        Map<LineID, ColorTag> loadedBookmarks = stripBookmarkComments();
        for (LineID lineID : loadedBookmarks.keySet()) {
            lineBookmarks.addBookmark(lineID, loadedBookmarks.get(lineID));
        }
        // setting bookmarks will flag sketch as modified, so override this here
        sketch.setModified(false);
        // report to the preprocService that we made changes to the sketch's documents
        preprocService.notifySketchChanged();

        occurrences = new CodeOccurrences(this, preprocService);
        getSmartCodePainter().addLinePainter(occurrences);
        if (OCCURRENCES_HIGHLIGHT) {
            textarea.addCaretListener(occurrences);
        }

        getSmartCodePainter().addLinePainter(getTextArea().snippetManager);

//        context = new CodeContext(this, preprocService);
//        textarea.addCaretListener(context);

        timedAction(this::printHelloMessage, 1000);
    }

    private void printHelloMessage() {
        statusNotice("SmartCode is active");
        System.out.println("SmartCode 0.0.1\n" + "created by Kelvin Spatola\n");
    }

    @Override
    protected JEditTextArea createTextArea() {
        return new SmartCodeTextArea(new PdeTextAreaDefaults(), this);
    }

    @Override
    public SmartCodeTextArea getTextArea() {
        return (SmartCodeTextArea) textarea;
    }

    public SmartCodeTextAreaPainter getSmartCodePainter() {
        return getTextArea().getSmartCodePainter();
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

    protected SmartCodeMarkerColumn getMarkerColumn() {
        return (SmartCodeMarkerColumn) errorColumn;
    }

    @Override
    public void applyPreferences() {
        super.applyPreferences();
        SmartCodePreferences.load();
        SmartCodeTextAreaPainter.updateDefaultFontSize();
        tabSize = Preferences.getInteger("editor.tabs.size");
        tabSpaces = addSpaces(tabSize);

        if (occurrences != null) {
            boolean containsListener = getTextArea().containsListener(occurrences, CaretListener.class);

            if (OCCURRENCES_HIGHLIGHT && !containsListener) {
                occurrences.startTracking();
            } else if (!OCCURRENCES_HIGHLIGHT && containsListener) {
                occurrences.stopTracking();
            }
        }
    }

    @Override
    public void updateTheme() {
        SmartCodeTheme.load();
        super.updateTheme();
    }

    // TODO: lembrete de que � preciso trabalhar aqui
//    @Override
//    public JMenu buildFileMenu() {
//        // JAVA MODE ITEMS
//        String appTitle = Language.text("menu.file.export_application");
//        JMenuItem exportApplication = Toolkit.newJMenuItemShift(appTitle, 'E');
//        exportApplication.addActionListener(e -> {
//            if (sketch.isUntitled() || sketch.isReadOnly()) {
//                Messages.showMessage("Save First", "Please first save the sketch.");
//            } else {
//                handleExportApplication();
//            }
//        });
//
//        // SMARTCODE ITEMS
//        JMenuItem selectTemplate = new JMenuItem("Select template");
//        selectTemplate.addActionListener(a -> setText("\nvoid setup() {\n    size(300, 300);\n}"));
//
//        return buildFileMenu(new JMenuItem[] { selectTemplate, exportApplication });
//    }

    private void buildMenu() {
        JMenu menu = new JMenu("SmartCode");

        createItem(menu, "Duplicate lines up", "CA+UP", () -> duplicateLines(true));
        createItem(menu, "Duplicate lines down", "CA+DOWN", () -> duplicateLines(false));
        createItem(menu, "Move lines up", "A+UP", () -> moveLines(true));
        createItem(menu, "Move lines down", "A+DOWN", () -> moveLines(false));

        menu.addSeparator(); // ---------------------------------------------
        createItem(menu, "Delete line", "C+E", () -> deleteLine(textarea.getCaretLine()));
        createItem(menu, "Delete line content", "CS+E", () -> deleteLineContent(textarea.getCaretLine()));
        createItem(menu, "Insert line bellow", "S+ENTER", () -> insertNewLineBelow(textarea.getCaretLine()));
        createItem(menu, "Insert line above", "CS+ENTER", () -> insertNewLineAbove(textarea.getCaretLine()));
        createItem(menu, "Insert line break", "A+ENTER", () -> insertLineBreak(getCaretOffset()));

        menu.addSeparator(); // ---------------------------------------------
        JMenuItem[] updatableItems = { 
                createItem(menu, "Toggle block comment", "C+7", this::toggleBlockComment),
                createItem(menu, "Format selected text", "C+T", this::handleAutoFormat),
                createItem(menu, "To upper case", "CS+U", () -> changeCase(true)),
                createItem(menu, "To lower case", "CS+L", () -> changeCase(false)) 
        };
        createItem(menu, "Expand Selection", "CA+RIGHT", this::expandSelection);

        menu.addSeparator(); // ---------------------------------------------
        JMenuItem showBookmarksItem = createItem(menu, "Show bookmarks", null, showBookmarks::handleShowBookmarks);

        JMenuItem clearBookmarksItem = createItem(menu, "Clear bookmarks in current tab", null, () -> {
            Object[] options = { Language.text("prompt.ok"), Language.text("prompt.cancel") };
            int choice = JOptionPane.showOptionDialog(this,
                    "Are you sure you want to clear all bookmarks from this tab?", "Bookmarks",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == JOptionPane.YES_OPTION) {
                clearBookmarksFromTab(sketch.getCurrentCodeIndex());
                statusNotice("Deleted all bookmarks in this tab");
            }
        });

        menu.addSeparator(); // ---------------------------------------------
        createItem(menu, Language.text("menu.file.preferences"), "CS+COMMA", () -> handlePrefs());

        menu.addSeparator(); // ---------------------------------------------
        createItem(menu, "Visit GitHub page", null,
                () -> Platform.openURL("https://github.com/KelvinSpatola/SmartCode"));

        menu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuSelected(MenuEvent e) {
                // show list of boomarks only if there's at least one on the list
                showBookmarksItem.setEnabled(lineBookmarks.hasBookmarks());

                // Enable this item only if there are any bookmarks in the current tab
                clearBookmarksItem.setEnabled(hasBookmarksInCurrentTab());

                // Update state on selection/de-selection
                Stream.of(updatableItems).forEach(item -> item.setEnabled(isSelectionActive()));
            }
        });

        JMenuBar menubar = getJMenuBar();
        int toolMenuIndex = menubar.getComponentIndex(getToolMenu());
        menubar.add(menu, toolMenuIndex);
        Toolkit.setMenuMnemonics(menubar);
    }

    /**
     * Show the Preferences window.
     */
    public void handlePrefs() {
        if (preferencesFrame == null) {
            preferencesFrame = new SmartCodePreferencesFrame(getBase());
        }
        preferencesFrame.showFrame();
    }

    private void buildPopupMenu() {
        JPopupMenu popup = textarea.getRightClickPopup();

        popup.addSeparator(); // ---------------------------------------------
        JMenuItem[] selectableItems = { 
        		createItem(popup, "Format selected text", null, this::handleAutoFormat),
                createItem(popup, "Toggle block comment", null, this::toggleBlockComment),
                createItem(popup, "To upper case", null, () -> changeCase(true)),
                createItem(popup, "To lower case", null, () -> changeCase(false)) 
        };

        popup.addSeparator(); // ---------------------------------------------
        JMenuItem clearBookmarksItem = createItem(popup, "Clear bookmarks in current tab", null, () -> {
            Object[] options = { Language.text("prompt.ok"), Language.text("prompt.cancel") };
            int choice = JOptionPane.showOptionDialog(this,
                    "Are you sure you want to clear all bookmarks from this tab?", "Bookmarks",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == JOptionPane.YES_OPTION) {
                clearBookmarksFromTab(sketch.getCurrentCodeIndex());
                statusNotice("Deleted all bookmarks in this tab");
            }
        });

        // Update state on selection/de-selection
        popup.addPopupMenuListener(new MenuAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            	Stream.of(selectableItems).forEach(item -> item.setEnabled(isSelectionActive()));
            	clearBookmarksItem.setEnabled(hasBookmarksInCurrentTab());
            }
        });
    }

    private void buildGutterPopupMenu() {
        class GutterPopupMenu extends JPopupMenu {
            JMenu submenu;
            JMenuItem removeBookmarkItem, showBookmarksListItem;
            int line;

            GutterPopupMenu() {
                submenu = new JMenu();
                addColorItem(ColorTag.COLOR_1);
                addColorItem(ColorTag.COLOR_2);
                addColorItem(ColorTag.COLOR_3);
                addColorItem(ColorTag.COLOR_4);
                addColorItem(ColorTag.COLOR_5);
                add(submenu);

                removeBookmarkItem = createItem(this, "Remove bookmark", null,
                        () -> lineBookmarks.removeBookmark(getLineIDInCurrentTab(line)));
                addSeparator();
                showBookmarksListItem = createItem(this, "Show bookmarks", null, showBookmarks::handleShowBookmarks);
            }

            @Override
            public void show(Component component, int x, int y) {
                line = textarea.yToLine(y);
                boolean isLineBookmark = lineBookmarks.isBookmark(line);

                submenu.setText(isLineBookmark ? "Change color" : "Add bookmark");
                removeBookmarkItem.setVisible(isLineBookmark);
                showBookmarksListItem.setEnabled(lineBookmarks.hasBookmarks());
                super.show(component, x, y);
            }

            void addColorItem(ColorTag colorTag) {
                JMenuItem item = new JMenuItem() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setColor(colorTag.getColor());
                        g2d.fillRect(2, 2, getWidth() - 4, getHeight() - 4);
                    }
                };
                item.setMargin(new Insets(2, -20, 2, 2));
                item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                item.addActionListener(a -> setBookmarkColor(line, colorTag));
                submenu.add(item);
            }

            void setBookmarkColor(int line, ColorTag colorTag) {
                final LineID lineID = getLineIDInCurrentTab(line);
                Bookmark bm = lineBookmarks.getBookmark(lineID);
                if (bm == null) {
                    lineBookmarks.addBookmark(lineID, colorTag);
                } else {
                    bm.setColorTag(colorTag);
                    showBookmarks.updateTree();
                }
                sketch.setModified(true);
                currentBookmarkColor = colorTag;
            }
        }

        getTextArea().setGutterRightClickPopup(new GutterPopupMenu());
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
            if (!lineBookmarks.hasBookmarks())
                return false;

            int currentLine = textarea.getCaretLine();
            if (getCaretOffset() == textarea.getLineStopOffset(currentLine) - 1) {
                Bookmark bookmarkOnNextLine = lineBookmarks.getBookmark(getLineIDInCurrentTab(currentLine + 1));

                if (bookmarkOnNextLine != null) {
                    if (getLineText(currentLine + 1).isBlank()) {
                        lineBookmarks.removeBookmark(bookmarkOnNextLine);
                    } else {
                        lineBookmarks.removeBookmark(getLineIDInCurrentTab(currentLine));
                    }
                }
            }

        } else if (keyCode == KeyEvent.VK_BACK_SPACE) { // let's deal with bookmarks deletion here
            pauseOccurrencesTracking(300);

            if (!lineBookmarks.hasBookmarks())
                return false;

            int startLine = textarea.getSelectionStartLine();
            int endLine = textarea.getSelectionStopLine();

            if (startLine != endLine) {
                if (getSelectionStart() > getLineStartOffset(startLine))
                    startLine++;

                if (getLineStartOffset(endLine) == getSelectionStop())
                    endLine--;

                for (int line = startLine; line <= endLine; line++) {
                    lineBookmarks.removeBookmark(getLineIDInCurrentTab(line));
                }
            } else {
                int line = textarea.getCaretLine();
                if (lineBookmarks.isBookmark(line)) {
                    int lineIndent = 0;
                    int brace = getTextArea().getMatchingBraceLine(line, true);
                    if (brace != -1) {
                        lineIndent = getTextArea().getLineIndentation(brace) + tabSize;
                    }

                    String lineText = getLineText(line);
                    if (lineText.isBlank() && lineText.length() <= lineIndent) {
                        lineBookmarks.removeBookmark(getCurrentLineID());
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

    private void handleEnter() {   	
        // erase any selection content
        if (isSelectionActive()) {
            if (lineBookmarks.hasBookmarks()) {
                int startLine = textarea.getSelectionStartLine();
                int endLine = textarea.getSelectionStopLine();

                if (startLine != endLine) {
                    if (getSelectionStart() > getLineStartOffset(startLine))
                        startLine++;

                    if (getLineStartOffset(endLine) == getSelectionStop())
                        endLine--;

					for (int line = startLine; line <= endLine; line++) {
						lineBookmarks.removeBookmark(getLineIDInCurrentTab(line));
					}
				}
			}
			setSelectedText(LF, true);
			return;
		}

        pauseOccurrencesTracking(300);
        
        int caret = getCaretOffset();

        int caretLine = textarea.getCaretLine();
        int positionInLine = caret - getLineStartOffset(caretLine);
        String lineText = getLineText(caretLine);            
//
//            long time = System.nanoTime();
//            
//            if (context.isStringLiteral()) {
//                int start = context.offsets()[0];
//                int end = context.offsets()[1];
//
//                if (caret > start && caret < end) {
//                    splitString(caretLine);
//                    System.out.println("time: " + (System.nanoTime() - time));
//                    return;
//                }
//            }

        if (STRING_TEXT.matcher(lineText).matches()) {
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
        
        else if (COMMENT_TEXT.matcher(lineText).matches()) {
            if (SmartCodePreferences.AUTOCLOSE_BLOCK_COMMENTS) {
                if (!lineText.contains(OPEN_COMMENT)) {
                    int line = caretLine - 1;

                    while (line >= 0) {
                        if (!COMMENT_TEXT.matcher(getLineText(line)).matches())
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
        }

        else if (BLOCK_OPENING.matcher(lineText).matches()) {
            if (SmartCodePreferences.AUTOCLOSE_BRACKETS) { // TODO: make this condition a pref of its own

                boolean bracketsAreBalanced = SmartCodeTextArea.checkBracketsBalance(getText(), "{", "}");
                boolean hasClosingBrace = BLOCK_CLOSING.matcher(lineText).matches();
                int openBrace = lineText.indexOf(OPEN_BRACE);
                int closeBrace = lineText.indexOf(CLOSE_BRACE);

                if ((!bracketsAreBalanced && positionInLine > openBrace) || (bracketsAreBalanced && hasClosingBrace
                        && positionInLine > openBrace && positionInLine <= closeBrace)) {

                    createBlockScope(caret);
                    return;
                }
            }
        }
        
        // if none of the above, then insert a new line
        insertNewLine(caret);
    }

    private void splitString(int caretLine) {
        int indent = 0;
        if (INDENT) {
            indent = getTextArea().getLineIndentation(caretLine);

            if (!getLineText(caretLine).trim().startsWith("+ \""))
                indent += tabSize;
        }

        setSelectedText("\"\n" + addSpaces(indent) + "+ \"", true);
        errorColumn.repaint();
    }

    private void splitComment(int caretLine) {
        int indent = 0;
        if (INDENT) {
            indent = getTextArea().getLineIndentation(caretLine);
        }

        startCompoundEdit();
        setSelectedText(LF + addSpaces(indent - (indent % tabSize)) + " * ", false);

        int caretPos = getCaretOffset();
        String nextText = getText().substring(caretPos);

        // Checking if we need to close this comment
        int openingToken = nextText.indexOf(OPEN_COMMENT);
        int closingToken = nextText.indexOf(CLOSE_COMMENT);
        boolean commentIsOpen = (closingToken == -1) || (closingToken > openingToken && openingToken != -1);

        if (commentIsOpen) {
            textarea.setCaretPosition(getLineStopOffset(++caretLine) - 1);
            setSelectedText(LF + addSpaces(indent - (indent % tabSize)) + " */", false);
            textarea.setCaretPosition(caretPos);
        }
        stopCompoundEdit();
        errorColumn.repaint();
    }

    private void createBlockScope(int offset) {
        int line = textarea.getLineOfOffset(offset);

        int indent = 0;
        if (INDENT) {
            indent = getTextArea().getLineIndentation(line) + tabSize;
        }

        startCompoundEdit();
        setSelection(offset, getLineStopOffset(line) - 1);

        String cutText = isSelectionActive() ? getSelectedText().trim() : "";

        if (BLOCK_CLOSING.matcher(cutText).matches()) {
            cutText = cutText.replace(CLOSE_BRACE, '\0').trim();
        }

        setSelectedText(LF + addSpaces(indent) + cutText, false);

        int newOffset = getCaretOffset();
        setSelectedText(LF + addSpaces(indent - tabSize) + CLOSE_BRACE, false);
        setSelection(newOffset, newOffset);
        stopCompoundEdit();
        errorColumn.repaint();
    }

    private void insertNewLine(int offset) {        
        int indent = 0;

        if (INDENT) {
            int line = textarea.getLineOfOffset(offset);
            String lineText = getLineText(line);

            int startBrace = getTextArea().getMatchingBraceLine(line, true);

            if (startBrace != -1) {
                indent = getTextArea().getLineIndentation(startBrace);

                if (!BLOCK_CLOSING.matcher(lineText).matches())
                    indent += tabSize;

                int positionInLine = offset - getLineStartOffset(line);
                if (BLOCK_OPENING.matcher(lineText).matches() && positionInLine <= lineText.indexOf(OPEN_BRACE))
                    indent -= tabSize;
            }
            setSelection(offset, getLineStopOffset(line) - 1);
        }
        String cutText = isSelectionActive() ? getSelectedText().trim() : "";
        setSelectedText(LF + addSpaces(indent) + cutText, true);

        int newOffset = offset + indent + 1;
        setSelection(newOffset, newOffset);
        errorColumn.repaint();
    }

    @Override
    public void handleAutoFormat() {
        if (isSelectionActive()) {
            if (getSelectedText().isBlank()) {
                return;
            }

            Selection s = new Selection();
            String selectedText = s.text;

            List<Integer> taggedLines = null;
            List<ColorTag> colorTags = null;
            boolean isTagged = false;

            //
            if (hasBookmarksInCurrentTab()) {
                taggedLines = new ArrayList<>();
                colorTags = new ArrayList<>();
                final int currentTab = sketch.getCurrentCodeIndex();

                for (int i = lineBookmarks.markerCount() - 1; i >= 0; i--) {
                    LineMarker lm = lineBookmarks.getMarkers().get(i);
                    int bmLine = lm.getLine();

                    if (lm.getTabIndex() == currentTab && bmLine >= s.startLine && bmLine <= s.endLine) {
                        taggedLines.add(bmLine);
                        colorTags.add(((Bookmark) lm).getColorTag());
                    }
                }

                // Now that we have the line references we need, let's add bookmark tags to the
                // text
                if (!taggedLines.isEmpty()) {
                    String[] textLines = selectedText.split("\\r?\\n");

                    for (int line : taggedLines) {
                        textLines[line - s.startLine] += PIN_MARKER;
                    }
                    selectedText = PApplet.join(textLines, "\n");
                    isTagged = true;
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
            int brace = getTextArea().getMatchingBraceLine(s.startLine - 1, true);
            int indent = 0;

            if (brace != -1) {
                indent = getTextArea().getLineIndentation(brace) + tabSize;
            }
            
			// Gotta add a line feed after indenting the text because indentText() strips
			// any trailing whitespace, and that's unwanted here!
			formattedText = SmartCodeTextArea.indentText(formattedText, indent) + LF;

            if (formattedText.equals(selectedText) && isSourceIntact) {
                statusNotice(Language.text("editor.status.autoformat.no_changes"));

            } else {
                int start = s.start;
                int end = s.end + 1;

                startCompoundEdit();
                // we need to remove bookmarks here before actually inserting the formatted text
                // back to the code, otherwise we'll some weird bugs and errors
                if (isTagged) {
                    for (int line : taggedLines) {
                        lineBookmarks.removeBookmark(getLineIDInCurrentTab(line));
                    }
                }

                setSelection(start, end);
                setSelectedText(formattedText);

                end = start + formattedText.length() - 1;
                setSelection(start, end);

                if (isTagged) {
                    String[] textLines = formattedText.split("\\r?\\n");

                    int line = s.startLine;
                    int colorIndex = colorTags.size() - 1;
                    for (String textLine : textLines) {
                        if (textLine.endsWith(PIN_MARKER)) {
                            textarea.select(getLineStartOffset(line), getLineStopOffset(line) - 1);
                            textarea.setSelectedText(textLine.replaceAll(PIN_MARKER, ""));
                            lineBookmarks.addBookmark(getLineIDInCurrentTab(line), colorTags.get(colorIndex--));
                        }
                        line++;
                    }
                }

                stopCompoundEdit();
                sketch.setModified(true);
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

    protected String refactorStringLiterals(String text) {
        int maxLength = SmartCodePreferences.AUTOFORMAT_STRINGS_LENGTH;

        // Concatenate every multiline split-string into a single one-line string before
        // doing anything else.
        String preformattedText = text.replaceAll("\\\"\\\n\\h*\\+\\s*\\\"", "");

        List<String> lines = new ArrayList<>(Arrays.asList(preformattedText.split(LF)));
        int depth = 0;
        int indent = 0;

        for (int i = 0; i < lines.size(); i++) {
            String lineText = lines.get(i);

            if (STRING_TEXT.matcher(lineText).matches()) {
                int stringStart = lineText.indexOf("\"");
                int stringEnd = lineText.lastIndexOf("\"");

                if ((stringEnd - stringStart) > maxLength) {
                    if (depth == 0) {
                        indent = getLineIndentation(lineText);
                    }
                    String preffix = addSpaces(indent) + tabSpaces + "+ \"";
                    String currLine = lineText.substring(0, stringStart + maxLength - 1) + "\"";
                    String nextLine = preffix + lineText.substring(stringStart + maxLength - 1);

                    lines.set(i, currLine);
                    lines.add(i + 1, nextLine);
                    depth++;
                    continue;
                }
            }
            lines.set(i, lineText);
            depth = 0;
            indent = 0;
        }

        StringBuilder result = new StringBuilder();
        lines.forEach(line -> result.append(line + LF));

        return result.toString();
    }

    
    /**
     * Deletes the line provided by the index. If text is selected, delete all lines that are selected.
     * 
     * @param line the line index
     */
    public void deleteLine(int line) {
        if (getText().isEmpty()) {
            getToolkit().beep();
            return;
        }
//        pauseOccurrencesTracking(300);
        
        if (lineBookmarks.hasBookmarks()) {
            lineBookmarks.removeBookmark(getCurrentLineID());
        } 
        
        Selection s = new Selection();
        
        // in case we are in the last line of text (but not when it's also first one)
        if (line == getLineCount() - 1 && line != 0) {
            // subtracting 1 from getLineStartOffset() will delete the line break prior
            // to this line, causing the caret to move to the end of the previous line
            s.start--;
            s.end--;
        } 
        
        setSelection(s.start, s.end + 1);
        setSelectedText("", true);
        sketch.setModified(true);
    }

    
    /**
     * Deletes the text on the given line, leaving the appropriate indentation.
     * 
     * @param line the line index
     */
    public void deleteLineContent(int line) {
        int indent = 0;
        int brace = getTextArea().getMatchingBraceLine(line, true);
        if (brace != -1) {
            indent = getTextArea().getLineIndentation(brace) + tabSize;
        }

        setSelection(getLineStartOffset(line), getLineStopOffset(line) - 1);
        setSelectedText(addSpaces(indent), true);
        sketch.setModified(true);
    }
    
    
    public void toggleBlockComment() {
        if (isSelectionActive()) {
            String selectedText = getSelectedText();
            StringBuilder result = new StringBuilder();

            if (selectedText.startsWith(OPEN_COMMENT) && selectedText.endsWith(CLOSE_COMMENT)) {
                result.append(selectedText);
                result.delete(0, 2);
                result.delete(result.length() - 2, result.length());

            } else {
                result.append(OPEN_COMMENT).append(selectedText).append(CLOSE_COMMENT);
            }

            int selectionStart = getSelectionStart();
            int selectionEnd = selectionStart + result.length();

            setSelectedText(result.toString(), true);
            setSelection(selectionStart, selectionEnd);
        }
    }

    
    /**
     * Change the case for selected text to <b>upper</b> or <b>lower</b> case.
     * 
     * @param toUpperCase text will be changed to upper case if true, lower case
     *                    otherwise.
     */
    public void changeCase(boolean toUpperCase) {
        if (!isSelectionActive())
            return;

        int start = getSelectionStart();
        int end = getSelectionStop();

        String original = getSelectedText();
        String modified = toUpperCase ? original.toUpperCase() : original.toLowerCase();

        setSelectedText(modified, true);
        setSelection(start, end);

        // flag the sketch as modified only if any changes have taken place
        if (!modified.equals(original)) {
            sketch.setModified(true);
        }
    }

    
    /**
     * Duplicates selected lines of text. Can duplicate a selection block or just a
     * line if no text is selected.
     * 
     * @param up insert the duplicate lines above the cursor line, otherwise insert
     *           them below the cursor line
     */
    public void duplicateLines(boolean up) {
        Selection s = new Selection();

        if (s.endLine == getLineCount() - 1) {
            int caret = s.end;
            setSelection(caret, caret);
            setSelectedText(LF + s.text, true);
        } else {
            int caret = s.end + 1;
            setSelection(caret, caret);
            setSelectedText(s.text + LF, true);
        }

        if (up) {
            setSelection(s.end, s.start);
        } else {
            setSelection(getCaretOffset(), s.end + 1);
        }
        sketch.setModified(true);
    }

    
    /**
     * Moves text up or down. Can move a selection block or just a line if no text
     * is selected.
     * <p>
     * After changing lines, the selected text will be indented automatically. This
     * functionality (automatic indentation) can be disabled if
     * {@link SmartCodePreferences#MOVE_LINES_AUTO_INDENT}.
     * 
     * @param moveUp move text up one line, otherwise move down one line.
     * @see SmartCodePreferences
     */
    public void moveLines(boolean moveUp) {
        Selection s = new Selection();
        int targetLine = moveUp ? s.startLine - 1 : s.endLine + 1;
        if (targetLine < 0 || targetLine >= getLineCount()) {
            getToolkit().beep();
            return;
        }
        int targetStart = getLineStartOffset(targetLine);
        int targetEnd = getLineStopOffset(targetLine) - 1;
        String targetText = getText(targetStart, targetEnd);

        boolean isTargetLineBookmarked;
        Bookmark targetBookmark = lineBookmarks.getBookmark(getLineIDInCurrentTab(targetLine));
        ColorTag targetColorTag = null;
        if (isTargetLineBookmarked = (targetBookmark != null)) {
            targetColorTag = targetBookmark.getColorTag();
            lineBookmarks.removeBookmark(targetBookmark);
        }

        // SWAP LINES
        String replacedText;
        int replacedTextStart, replacedTextEnd;
        int newSelectionStart, newSelectionEnd;

        startCompoundEdit();
        pauseOccurrencesTracking(300);
        lineBookmarks.stopBookmarkTracking();
        
        if (moveUp) {
            replacedText = s.text + LF + targetText;
            replacedTextStart = targetStart;
            replacedTextEnd = targetStart + replacedText.length();
            setSelection(replacedTextStart, replacedTextEnd);
            setSelectedText(replacedText, false);
            newSelectionStart = getLineStartOffset(targetLine);
            newSelectionEnd = newSelectionStart + s.text.length();

            for (int line = s.startLine; line <= s.endLine; line++) {
                moveBookmarkTo(line, line - 1);
            }

        } else {
            replacedText = targetText + LF + s.text;
            replacedTextStart = s.start;
            replacedTextEnd = s.start + replacedText.length();
            setSelection(replacedTextStart, replacedTextEnd);
            setSelectedText(replacedText, false);
            newSelectionStart = getLineStartOffset(s.startLine + 1);
            newSelectionEnd = newSelectionStart + s.text.length();

            for (int line = s.endLine; line >= s.startLine; line--) {
                moveBookmarkTo(line, line + 1);
            }
        }
        lineBookmarks.startBookmarkTracking();
        
        if (isTargetLineBookmarked) {
            LineID lineID = getLineIDInCurrentTab(moveUp ? s.endLine : s.startLine);
            lineBookmarks.addBookmark(lineID, targetColorTag);
        }

        setSelection(newSelectionStart, newSelectionEnd);

        if (!SmartCodePreferences.MOVE_LINES_AUTO_INDENT) {
            stopCompoundEdit();
            return;
        }
        
        // RESOLVE INDENTATION
        String indentedText;
        if (BLOCK_OPENING.matcher(targetText).matches()) {
            indentedText = SmartCodeTextArea.indentOutdentText(s.text, tabSize, !moveUp);
        } else if (BLOCK_CLOSING.matcher(targetText).matches()) {
            indentedText = SmartCodeTextArea.indentOutdentText(s.text, tabSize, moveUp);
        } else {
            indentedText = s.text;
        }
        
        lineBookmarks.stopBookmarkTracking();
        setSelectedText(indentedText, false);
        lineBookmarks.startBookmarkTracking();
        stopCompoundEdit();

        // UPDATE SELECTION
        newSelectionEnd = newSelectionStart + indentedText.length();
        setSelection(newSelectionStart, newSelectionEnd);
        sketch.setModified(true);
    }

    
    /**
     * Inserts an empty line below the current caret position with the appropriate
     * indentation. This operation does not affect any text.
     * 
     * @param line the current caret line
     */
    public void insertNewLineBelow(int line) {
        String lineText = getLineText(line);
        int indent = 0;

        if (INDENT) {
            if (BLOCK_CLOSING.matcher(lineText).matches()) {
                indent = getLineIndentation(lineText);

            } else {
                int startBrace = getTextArea().getMatchingBraceLine(line, true);
                if (startBrace != -1) {
                    indent = getTextArea().getLineIndentation(startBrace) + tabSize;
                }
            }
        }
        int lineEnd = getLineStopOffset(line) - 1;
        setSelection(lineEnd, lineEnd);
        setSelectedText(LF + addSpaces(indent), true);
    }
    

    /**
     * Inserts an empty line above the current caret position with the appropriate
     * indentation. This operation does not affect any text.
     * 
     * @param line the current caret line
     */
    public void insertNewLineAbove(int line) {
        int start = getLineStartOffset(line);
        int newCaretPos = getTextArea().getLineStartNonWhiteSpaceOffset(line);
        String lineText = getLineText(line);
        
        startCompoundEdit();
        setSelection(newCaretPos, newCaretPos);
        setSelectedText(LF + addSpaces(newCaretPos - start), false);
        setSelection(newCaretPos, newCaretPos);
        
        if (BLOCK_CLOSING.matcher(lineText).matches()) {
            setSelectedText(addSpaces(tabSize), false);
        }
        stopCompoundEdit();
    }
    

    /**
     * Inserts a line feed at the specified location.
     * <p>
     * The remaining text after the line feed will be indented according to its
     * relative scope, and the caret will remain in its original location.
     * 
     * @param offset the caret position
     */
    public void insertLineBreak(int offset) {
        Selection s = new Selection();

        int caretPos = offset - s.start;
        String s1 = s.text.substring(0, caretPos);
        String s2 = s.text.substring(caretPos).trim();
        int indent = 0;
        
        if (INDENT) {
            if (BLOCK_OPENING.matcher(s.text).matches()) {
                indent = getLineIndentation(s.text);

                if (caretPos > s.text.indexOf('{'))
                    indent += tabSize;

            } else if (BLOCK_CLOSING.matcher(s.text).matches()) {
                indent = getLineIndentation(s.text);
                int closeBrace = s.text.indexOf('}');

                if (caretPos <= closeBrace) {
                    offset += closeBrace + tabSize - caretPos;
                    s1 += addSpaces(indent + tabSize - caretPos);
                }

            } else {
                int startBrace = getTextArea().getMatchingBraceLine(s.startLine, true);
                
                if (startBrace != -1) // an opening brace was found, we are in a block scope
                    indent = getTextArea().getLineIndentation(startBrace) + tabSize;
            }
        }
        setSelection(s.start, s.end);
        setSelectedText(s1 + LF + addSpaces(indent) + s2, true);
        setSelection(offset, offset);
    }
    
    
    /**
     * Handles text tabulation at current caret position.
     * 
     * @param isShiftDown indent if true, outdent otherwise.
     */
    public void handleTabulation(boolean isShiftDown) {
        if (isShiftDown) {
            handleOutdent();

        } else if (isSelectionActive()) {
            handleIndent();

        } else if (Preferences.getBoolean("editor.tabs.expand")) {
            // "editor.tabs.expand" means that each tab is made up of a
            // stipulated number of spaces, and not just a single solid \t
            setSelectedText(tabSpaces);

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

        int startLine = s.startLine;
        int endLine = s.endLine;

        if (isSelectionActive()) {
            String code = getText();

            int lastLineOfSelection = textarea.getSelectionStopLine();
            boolean isLastBlock = (getSelectionStop() == getLineStartOffset(lastLineOfSelection));

            if (isLastBlock) {
                end = getLineStopOffset(lastLineOfSelection) - 1;
                setSelection(s.start, end);
                return;

            }
            if (code.charAt(start - 1) == OPEN_BRACE && code.charAt(end) == CLOSE_BRACE) {
                setSelection(s.start, s.end);
                return;

            }
            if (start == s.start && end == s.end) {
                startLine--;
                endLine++;
            }
        }

        final SmartCodeTextArea ta = getTextArea();

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
    
    

    /*
     * 
     * TEXT UTILS
     * 
     */

    protected class Selection {
        int start, end, startLine, endLine;
        String text;

        Selection() {
            startLine = textarea.getSelectionStartLine();
            endLine = textarea.getSelectionStopLine();

            // in case this selection ends with the caret at the beginning of the last line,
            // not selecting any text
            if (isSelectionActive() && getLineStartOffset(endLine) == getSelectionStop()) {
                endLine--;
            }

            start = getLineStartOffset(startLine);
            end = Math.max(start, getLineStopOffset(endLine) - 1);
            text = getText(start, end);
        }
    }
    

    static protected String addSpaces(int length) {
        return new String(new char[length]).replace('\0', ' ');
    }
    
    
    static protected int getLineIndentation(String lineText) {
        char[] chars = lineText.toCharArray();
        int index = 0;

        while (index < chars.length && Character.isWhitespace(chars[index])) {
            index++;
        }
        return index;
    }
    

    /****************
     * 
     * LINE BOOKMARKS
     * 
     */
    
    public final void addBookmark(LineID lineID, ColorTag colorTag) {
        lineBookmarks.addBookmark(lineID, colorTag);
    }
    
    public final void removeBookmark(LineMarker bookmark) {
        lineBookmarks.removeBookmark(bookmark);
    }
    
    public final boolean isBookmark(int line) {
        return lineBookmarks.isBookmark(line);
    }
    
    public final boolean hasBookmarks() {
        return lineBookmarks.hasBookmarks();
    }
    
    public final boolean hasBookmarksInCurrentTab() {
        int currentTab = sketch.getCurrentCodeIndex();
        return lineBookmarks.getMarkers().stream().anyMatch(lm -> lm.getTabIndex() == currentTab);
    }
    
    public final List<LineMarker> getBookmarks() {
        return lineBookmarks.getMarkers();
    }
    
    public final void addBookmarkListListener(BookmarkListListener ls) {
        lineBookmarks.addBookmarkListListener(ls);
    }

    public void toggleLineBookmark(int line) {
        final LineID lineID = getLineIDInCurrentTab(line);
        Bookmark bm = lineBookmarks.getBookmark(lineID);

        if (bm == null) {
            lineBookmarks.addBookmark(lineID, currentBookmarkColor);
        } else {
            lineBookmarks.removeBookmark(bm);
        }
        sketch.setModified(true);
    }

    public boolean moveBookmarkTo(int oldLinePos, int newLinePos) {
        Bookmark bm = lineBookmarks.getBookmark(getLineIDInCurrentTab(oldLinePos));

        if (bm != null) { // it's faster than checking with isLineBookmark()
            ColorTag colorTag = bm.getColorTag();
            lineBookmarks.removeBookmark(bm);
            lineBookmarks.addBookmark(getLineIDInCurrentTab(newLinePos), colorTag);
            return true;
        }
        return false;
    }

    public void clearBookmarksFromTab(int tabIndex) {
        lineBookmarks.getMarkers().stream().filter(lm -> lm.getTabIndex() == tabIndex)
                .sorted(Collections.reverseOrder()).forEach(this::removeBookmark);

        sketch.setModified(true);
    }

    protected void addBookmarkComments(String tabFilename) {
        final Map<Integer, ColorTag> bms = new HashMap<>();

        for (LineMarker lm : lineBookmarks.getMarkers()) {
            Bookmark bm = (Bookmark) lm;
            if (bm.getLineID().fileName().equals(tabFilename)) {
                bms.put(bm.getLine(), bm.getColorTag());
            }
        }

        SketchCode tab = getTab(tabFilename);
        try {
            tab.load();
            String code = tab.getProgram();
            String[] codeLines = code.split("\\r?\\n");

            for (int line : bms.keySet()) {
                String commentTag = bms.get(line).getTag();
                // to avoid duplication, do it only if this line is not already marked
                if (!codeLines[line].endsWith(commentTag)) {
                    codeLines[line] += commentTag;
                }
            }
            code = PApplet.join(codeLines, "\n");
            tab.setProgram(code);
            tab.save();
        } catch (IOException ex) {
            Messages.err(null, ex);
        }
    }

    protected Map<LineID, ColorTag> stripBookmarkComments() {
        final String bookmarkCommentRegex = "(\\/{2}<bm_color[1-5]>\\/{2})";
        final Pattern commentedLinePattern = Pattern.compile("^.*" + bookmarkCommentRegex + "$");
        final Map<LineID, ColorTag> bms = new HashMap<>();

        // iterate over all tabs
        for (SketchCode tab : sketch.getCode()) {
            String code = tab.getProgram();
            String[] codeLines = code.split("\\r?\\n");

            // scan code for bookmark comments
            int line = 0;
            for (String textLine : codeLines) {
                Matcher m = commentedLinePattern.matcher(textLine);

                if (m.matches()) {
                    bms.put(new LineID(tab.getFileName(), line), ColorTag.valueOf(m.group(1)));
                    codeLines[line] = textLine.replaceAll(bookmarkCommentRegex, "");
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

        if (occurrences != null) {
            occurrences.updateAST();
        }
        statusEmpty();
    }

    @Override
    public void sketchChanged() {
        super.sketchChanged();

        if (showBookmarks != null) {
            showBookmarks.updateTree();
        }

        if (occurrences != null) {
            occurrences.updateAST();
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

    public void updateColumnPoints(List<? extends LineMarker> points, Class<? extends LineMarker> parent) {
        ((SmartCodeMarkerColumn) errorColumn).updatePoints(points, parent);
        repaint();
    }

    protected final void pauseOccurrencesTracking(int millis) {
        if (!OCCURRENCES_HIGHLIGHT || occurrences == null) {
            return;
        }

        if (getTextArea().containsListener(occurrences, CaretListener.class)) {
            occurrences.stopTracking();
        }

        timedAction(() -> {
            occurrences.startTracking();
            occurrences.updateAST();
        }, millis);
    }

    public void timedStatusNotice(String msg, int millis) {
        statusNotice(msg);
        timedAction(this::statusEmpty, millis);
    }

    public void timedAction(Runnable task, int millis) {
        if (generalTimer != null) {
            generalTimer.cancel();
            generalTimer.purge();
        }
        generalTimer = new Timer();
        generalTimer.schedule(new TimerTask() {
            public void run() {
                task.run();
                generalTimer.cancel(); // Terminate the timer thread
            }
        }, millis);
    }
}
