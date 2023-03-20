package kelvinspatola.mode.smartcode;

import static kelvinspatola.mode.smartcode.Constants.BLOCK_CLOSING;
import static kelvinspatola.mode.smartcode.Constants.BLOCK_OPENING;
import static kelvinspatola.mode.smartcode.Constants.INDENT;
import static kelvinspatola.mode.smartcode.Constants.LF;
import static kelvinspatola.mode.smartcode.Constants.STRING_TEXT;

import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EventListener;
import java.util.stream.Stream;

import javax.swing.JPopupMenu;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import kelvinspatola.mode.smartcode.completion.BracketCloser;
import kelvinspatola.mode.smartcode.completion.SnippetManager;
import processing.app.Platform;
import processing.app.Preferences;
import processing.app.SketchCode;
import processing.app.syntax.Brackets;
import processing.app.syntax.TextAreaDefaults;
import processing.app.ui.Editor;
import processing.mode.java.JavaTextArea;

public class SmartCodeTextArea extends JavaTextArea {
    private MouseListener textAreaMouseListener;
    private MouseMotionListener pdeDragHandlerListener;
    protected JPopupMenu gutterRightClickPopup;
    protected SnippetManager snippetManager;

    protected static int tabSize;
    protected static String tabSpaces;
    
    private final Brackets bracketHelper = new Brackets();

    // CONSTRUCTOR
    public SmartCodeTextArea(TextAreaDefaults defaults, SmartCodeEditor editor) {
        super(defaults, editor);

        SmartCodeInputHandler inputHandler = new SmartCodeInputHandler(editor);

        inputHandler.addKeyListener(new BracketCloser(editor));

        if (SmartCodePreferences.TEMPLATES_ENABLED) {
            snippetManager = new SnippetManager(editor);
            inputHandler.addKeyListener(snippetManager);
            addCaretListener(snippetManager);
//            getSmartCodePainter().addLinePainter(snippetManager);
        }
        // default behaviour for the textarea in regards to TAB and ENTER key
        inputHandler.addKeyListener(editor);

        setInputHandler(inputHandler);

        // Remove PdeTextArea's gutterCursorMouseAdapter listener so we
        // can add our own listener
        painter.removeMouseMotionListener(gutterCursorMouseAdapter);

        // Remove JEditTextArea's MouseHandler listener so we
        // can add our own listener
        painter.removeMouseListener(painter.getMouseListeners()[2]);
        
        textAreaMouseListener = new TextAreaMouseListener();

        // let's capture the default DragHandler listener
        pdeDragHandlerListener = painter.getMouseMotionListeners()[1];
        painter.removeMouseMotionListener(pdeDragHandlerListener);

        // Handle mouse clicks to toggle line bookmarks
        MouseAdapter gutterBookmarkToggling = new GutterAreaMouseHandler();
        painter.addMouseListener(gutterBookmarkToggling);
        painter.addMouseMotionListener(gutterBookmarkToggling);

        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                int clicks = e.getWheelRotation();
                getSmartCodePainter().setFontSize(getSmartCodePainter().getFontSize() - clicks);
            }
        });
    }

    class GutterAreaMouseHandler extends MouseAdapter {
        int lastX; // previous horizontal position of the mouse cursor
        long lastTime; // OS X seems to be firing multiple mouse events
        boolean isGutterPressed;

        @Override
        public void mouseMoved(MouseEvent e) {
            // check if the cursor is INSIDE the left gutter area
            if (e.getX() < Editor.LEFT_GUTTER) {
                if (lastX >= Editor.LEFT_GUTTER) {
                    painter.removeMouseListener(textAreaMouseListener);
                    painter.removeMouseMotionListener(pdeDragHandlerListener);
                    painter.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }

            } else { // check if the cursor is OUTSIDE the left gutter area (inside the text area)
                if (lastX < Editor.LEFT_GUTTER) {
                    painter.addMouseListener(textAreaMouseListener);
                    painter.addMouseMotionListener(pdeDragHandlerListener);
                    painter.setCursor(new Cursor(Cursor.TEXT_CURSOR));
                    isGutterPressed = false;
                }
            }
            lastX = e.getX();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (editor.isDebuggerEnabled())
                return;

            long thisTime = e.getWhen();

            if (thisTime - lastTime > 100) {
                int line = _yToLine(e.getY());
                int lastTextLine = getLineCount(); // constrain to the last line of text

                if ((e.getX() < Editor.LEFT_GUTTER) && (line < lastTextLine)) {
                    switch (e.getButton()) {
                    case MouseEvent.BUTTON1: // left mouse button
                        if (e.getClickCount() == 2)
                            ((SmartCodeEditor) editor).toggleLineBookmark(line);
                        break;
                    case MouseEvent.BUTTON3: // right mouse button
                        isGutterPressed = true;
                        break;
                    }
				}
                lastTime = thisTime;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (editor.isDebuggerEnabled())
                return;

            if (e.getButton() == MouseEvent.BUTTON3 && isGutterPressed) {
                int line = _yToLine(e.getY());
                // constrain to the last line of text and not the last visible line
                int lastTextLine = getLineCount();

                if ((e.getX() < Editor.LEFT_GUTTER) && (line < lastTextLine) && gutterRightClickPopup != null) {
                    gutterRightClickPopup.show(painter, e.getX(), e.getY());
                }
                isGutterPressed = false;
            }
        }
        
    }

    class TextAreaMouseListener extends MouseAdapter {

        public void mousePressed(MouseEvent event) {
            if (!hasFocus()) {
                if (!requestFocusInWindow()) {
                    return;
                }
            }

            boolean windowsRightClick = Platform.isWindows() && (event.getButton() == MouseEvent.BUTTON3);
            if ((event.isPopupTrigger() || windowsRightClick) && (popup != null)) {
                int offset = xyToOffset(event.getX(), event.getY());
                int selectionStart = getSelectionStart();
                int selectionStop = getSelectionStop();
                if (offset < selectionStart || offset >= selectionStop) {
                    select(offset, offset);
                }

                popup.show(painter, event.getX(), event.getY());
                return;
            }

            int line = yToLine(event.getY());
            int offset = xToOffset(line, event.getX());
            int dot = getLineStartOffset(line) + offset;

            selectLine = false;
            selectWord = false;

            switch (event.getClickCount()) {

            case 1:
                doSingleClick(event, line, offset, dot);
                break;

            case 2:
                // It uses the bracket matching stuff, so it can throw a BLE
                try {
                    doDoubleClick(event, line, offset, dot);
                } catch (BadLocationException bl) {
                    bl.printStackTrace();
                }
                break;

            case 3:
                doTripleClick(event, line, offset, dot);
                break;
            }
        }

        private void doSingleClick(MouseEvent e, int line, int offset, int dot) {
            if (e.isShiftDown()) {
                select(getMarkPosition(), dot);
            } else {
                setCaretPosition(dot);
            }
        }

        private void doDoubleClick(MouseEvent e, int line, int offset, int dot) throws BadLocationException {
            // Ignore empty lines
            if (getLineLength(line) != 0) {
                String text = document.getText(0, document.getLength());

                int bracket = bracketHelper.findMatchingBracket(text, Math.max(0, dot - 1));
                if (bracket != -1) {
                    int mark = getMarkPosition();
                    // Hack
                    if (bracket > mark) {
                        bracket++;
                        mark--;
                    }
                    select(mark, bracket);
                    return;
                }

                String lineText = getLineText(line);
                
                if (STRING_TEXT.matcher(lineText).matches()) {
                    char[] chars = lineText.toCharArray();

                    if ((chars[offset] == '"' || chars[offset - 1] == '"') && isCursorInsideQuotes(chars, offset)) {
                        String leftSide = lineText.substring(0, offset);
                        String rightSide = lineText.substring(offset);
                        int q1 = leftSide.length() - leftSide.lastIndexOf('"') - 1;
                        int q2 = rightSide.indexOf('"');
                        select(dot - q1, dot + q2);
                        return;
                    }
                }

                setNewSelectionWord(line, offset);

                select(newSelectionStart, newSelectionEnd);
                selectWord = true;
                selectionAncorStart = selectionStart;
                selectionAncorEnd = selectionEnd;
            }
        }

        private void doTripleClick(MouseEvent e, int line, int offset, int dot) {
            selectLine = true;
            final int lineStart = getLineStartOffset(line);
            final int lineEnd = getLineSelectionStopOffset(line);
            select(lineStart, lineEnd);
            selectionAncorStart = selectionStart;
            selectionAncorEnd = selectionEnd;
        }
    }

    public void setGutterRightClickPopup(JPopupMenu popupMenu) {
        gutterRightClickPopup = popupMenu;
    }

    public boolean containsListener(final EventListener listener, final Class<? extends EventListener> type) {
        return Stream.of(eventListenerList.getListeners(type)).anyMatch(ls -> ls == listener);
    }

    public void addCaretListenerIfAbsent(CaretListener listener) {
        if (!containsListener(listener, CaretListener.class)) {
            addCaretListener(listener);
        }
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        tabSize = Preferences.getInteger("editor.tabs.size");
        tabSpaces = addSpaces(tabSize);
    }

    /**
     * Converts a y co-ordinate to a line index. Rewriting this because i need it to
     * not clamp the returned value between 0 and getLineCount() as the original
     * JEditTextArea.yToLine() method does.
     * 
     * @param y The y co-ordinate
     */
    public int _yToLine(int y) {
        FontMetrics fm = painter.getFontMetrics();
        return Math.max(0, (y / fm.getHeight() + firstLine));
    }

    @Override
    protected SmartCodeTextAreaPainter createPainter(final TextAreaDefaults defaults) {
        return new SmartCodeTextAreaPainter(this, defaults);
    }

    public SmartCodeTextAreaPainter getSmartCodePainter() {
        return (SmartCodeTextAreaPainter) painter;
    }

    public int getLineStartOffset(int tabIndex, int line) {
        SketchCode code = editor.getSketch().getCode(tabIndex);
        Element lineElement = code.getDocument().getDefaultRootElement().getElement(line);
        return (lineElement == null) ? -1 : lineElement.getStartOffset();
    }

    public int getLineStopOffset(int tabIndex, int line) {
        SketchCode code = editor.getSketch().getCode(tabIndex);
        Element lineElement = code.getDocument().getDefaultRootElement().getElement(line);
        return (lineElement == null) ? -1 : lineElement.getEndOffset();
    }

    public String getLineText(int tabIndex, int line) {
        int start = Math.max(0, getLineStartOffset(tabIndex, line));
        int len = Math.max(0, getLineStopOffset(tabIndex, line) - start - 1);

        try {
            SketchCode code = editor.getSketch().getCode(tabIndex);
            return code.getDocument().getText(start, len);

        } catch (BadLocationException bl) {
            bl.printStackTrace();
            return null;
        }
    }

    @Override
    public void paste() {
        if (editable) {
            
            String lineText = getLineText(getCaretLine());
            boolean isInsideQuotes = false;
            if (STRING_TEXT.matcher(lineText).matches()) {
                isInsideQuotes = isCursorInsideQuotes(lineText.toCharArray(), caretPositionInsideLine());
            }

            Clipboard clipboard = getToolkit().getSystemClipboard();

            try {
                String selection = ((String) clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor));

                if (selection.contains("\r\n")) {
                    selection = selection.replaceAll("\r\n", "\n");

                } else if (selection.contains("\r")) {
                    // The Mac OS MRJ doesn't convert \r to \n, so do it here
                    selection = selection.replace('\r', '\n');
                }
                
                // Remove tabs and replace with spaces
                if (selection.contains("\t")) {
                    int tabSize = Preferences.getInteger("editor.tabs.size");
                    selection = selection.replaceAll("\t", addSpaces(tabSize));
                }

                // Replace unicode x00A0 (non-breaking space) with just a plain space.
                // Seen often on Mac OS X when pasting from Safari. [fry 030929]
                selection = selection.replace('\u00A0', ' ');

                // Remove ASCII NUL characters.
                if (selection.indexOf('\0') != -1) {
                    selection = selection.replaceAll("\0", "");
                }

                selection = selection.repeat(Math.max(0, inputHandler.getRepeatCount()));

                if (isInsideQuotes) {
                    selection = selection.replace("\\", "\\\\").replace("\\\"", "\\\\\"").stripTrailing();
                    StringBuilder sb = new StringBuilder(selection);

                    if (selection.contains(LF)) {
                        int indent = 0;
                        if (INDENT) {
                            int tabSize = Preferences.getInteger("editor.tabs.size");
                            indent = getLineIndentation(getCaretLine()) + tabSize;
                        }
                        String spaces = addSpaces(indent);

                        String[] lines = selection.split(LF);
                        sb = new StringBuilder(lines[0]).append("\\n\"").append(LF);

                        for (int i = 1; i < lines.length - 1; i++) {
                            sb.append(spaces).append("+ \"").append(lines[i]).append("\\n\"").append(LF);
                        }
                        sb.append(spaces).append("+ \"").append(lines[lines.length - 1]);
                    }
                    setSelectedText(sb.toString());

                } else {
                    setSelectedText(selection);
                }

            } catch (Exception e) {
                getToolkit().beep();
                System.err.println("Clipboard does not contain a string");
                DataFlavor[] flavors = clipboard.getAvailableDataFlavors();
                for (DataFlavor f : flavors) {
                    try {
                        Object o = clipboard.getContents(this).getTransferData(f);
                        System.out.println(f + " = " + o);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public int getLineIndentation(int line) {
        int start = getLineStartOffset(line);
        int end = getLineStartNonWhiteSpaceOffset(line);
        return end - start;
    }
    
    public static String indentOutdentText(String text, int length, boolean indent) {
        return indent ? indentText(text, length) : outdentText(text, length);
    }

    public static String indentText(String text, int length) {
        return Stream.of(text.split(LF))
                .map(s -> addSpaces(length) + s + LF)
                .reduce("", String::concat).stripTrailing();
    }

    public static String outdentText(String text, int length) {
        return Stream.of(text.split(LF)).map(s -> {
            int firstChar;
            for (firstChar = 0; firstChar < s.length(); firstChar++) {
                if (s.charAt(firstChar) != ' ') {
                    break;
                }
            }
            return s.substring(Math.min(firstChar, length)) + LF;
        }).reduce("", String::concat).stripTrailing();
    }

    public int getBlockDepth(int line) {
        if (line < 0 || line > getLineCount() - 1)
            return 0;

        int depthUp = 0;
        int depthDown = 0;
        int lineIndex = line;

        boolean isTheFirstBlock = true;

        while (lineIndex >= 0) {
            String lineText = getLineText(lineIndex);

            if (BLOCK_OPENING.matcher(lineText).matches()) {
                depthUp++;
            }

            else if (BLOCK_CLOSING.matcher(lineText).matches()) {
                depthUp--;
                isTheFirstBlock = false;
            }

            lineIndex--;
        }

        lineIndex = line;
        boolean isTheLastBlock = true;

        if (BLOCK_OPENING.matcher(getLineText(lineIndex)).matches()) {
            depthDown = 1;
        }

        while (lineIndex < getLineCount()) {
            String lineText = getLineText(lineIndex);

            if (BLOCK_CLOSING.matcher(lineText).matches())
                depthDown++;

            else if (BLOCK_OPENING.matcher(lineText).matches()) {
                depthDown--;
                isTheLastBlock = false;
            }

            lineIndex++;
        }

        isTheFirstBlock &= (depthUp == 1 && depthDown == 0);
        isTheLastBlock &= (depthDown == 1 && depthUp == 0);

        if (isTheFirstBlock && isTheLastBlock)
            return 0;
        if (isTheFirstBlock || isTheLastBlock)
            return 1;

        return Math.max(0, Math.min(depthUp, depthDown));
    }

    public int getMatchingBraceLine(int lineIndex, boolean goUp) {
        if (lineIndex < 0 || lineIndex >= getLineCount()) {
            return -1;
        }

        int blockDepth = 1;

        if (goUp) {

            if (BLOCK_CLOSING.matcher(getLineText(lineIndex)).matches()) {
                lineIndex--;
            }

            while (lineIndex >= 0) {
                String lineText = getLineText(lineIndex);

                if (BLOCK_CLOSING.matcher(lineText).matches()) {
                    blockDepth++;

                } else if (BLOCK_OPENING.matcher(lineText).matches()) {
                    blockDepth--;

                    if (blockDepth == 0)
                        return lineIndex;

                }
                lineIndex--;
            }
        } else { // go down

            if (BLOCK_OPENING.matcher(getLineText(lineIndex)).matches()) {
                lineIndex++;
            }

            while (lineIndex < getLineCount()) {
                String lineText = getLineText(lineIndex);

                if (BLOCK_OPENING.matcher(lineText).matches()) {
                    blockDepth++;

                } else if (BLOCK_CLOSING.matcher(lineText).matches()) {
                    blockDepth--;

                    if (blockDepth == 0)
                        return lineIndex;

                }
                lineIndex++;
            }
        }
        return -1;
    }

    public static boolean checkBracketsBalance(String text, String leftBrackets, String rightBrackets) {
        // Using ArrayDeque is faster than using Stack class
        Deque<Character> stack = new ArrayDeque<>();

        for (char ch : text.toCharArray()) {

            if (leftBrackets.contains(String.valueOf(ch))) {
                stack.push(ch);
                continue;
            }
            if (rightBrackets.contains(String.valueOf(ch))) {
                if (stack.isEmpty())
                    return false;

                char top = stack.pop();
                if (leftBrackets.indexOf(top) != rightBrackets.indexOf(ch))
                    return false;
            }
        }
        return stack.isEmpty();
    }

    public int caretPositionInsideLine() {
        return getCaretPosition() - getLineStartOffset(getCaretLine());
    }

    public int getOffsetOfPrevious(char ch, int offset) {
        char[] code = getText(0, offset + 1).toCharArray();

        while (offset >= 0) {
            if (code[offset] == ch) {
                return offset;
            }
            offset--;
        }
        return -1;
    }
    
    public boolean isCursorInsideQuotes(final char[] chars, final int cursor) {
        final int len = chars.length;
        boolean oddLeft = false, oddRight = false;
        
        for (int i = 0; i < len; i++) {
            if (chars[i] == '"') {
                if (i < cursor) oddLeft = !oddLeft;
                else oddRight = !oddRight;

                if (oddLeft && oddRight) {
                    return true;
                }
            }
        }
        return false;
    }

    static private String addSpaces(int length) {
        return new String(new char[length]).replace('\0', ' ');
    }
}
