package kelvinspatola.mode.smartcode;

import static kelvinspatola.mode.smartcode.Constants.BLOCK_CLOSING;
import static kelvinspatola.mode.smartcode.Constants.BLOCK_OPENING;
import static kelvinspatola.mode.smartcode.Constants.INDENT;
import static kelvinspatola.mode.smartcode.Constants.LF;
import static kelvinspatola.mode.smartcode.Constants.STRING_TEXT;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.RenderingHints;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EventListener;
import java.util.stream.Stream;

import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import kelvinspatola.mode.smartcode.completion.BracketCloser;
import kelvinspatola.mode.smartcode.completion.SnippetManager;
import kelvinspatola.mode.smartcode.ui.MultiCursorManager;
import processing.app.Messages;
import processing.app.Platform;
import processing.app.Preferences;
import processing.app.SketchCode;
import processing.app.syntax.Brackets;
import processing.app.syntax.TextAreaDefaults;
import processing.app.ui.Editor;
import processing.mode.java.JavaTextArea;

public class SmartCodeTextArea extends JavaTextArea {
    public static Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    public static Cursor TEXT_CURSOR = new Cursor(Cursor.TEXT_CURSOR);
    public static Cursor MOVE_DROP_CURSOR;
    public static Cursor COPY_DROP_CURSOR;
    public static Cursor INVALID_CURSOR;
    static {
        try {
            MOVE_DROP_CURSOR = Cursor.getSystemCustomCursor("MoveDrop.32x32");
            COPY_DROP_CURSOR = Cursor.getSystemCustomCursor("CopyDrop.32x32");
            INVALID_CURSOR = Cursor.getSystemCustomCursor("Invalid.32x32");
        } catch (HeadlessException | AWTException e) {
            e.printStackTrace();
        }
    }

    protected TextAreaMouseHandler textAreaMouseHandler;
    protected GutterAreaMouseHandler gutterAreaMouseHandler;
    protected JPopupMenu gutterRightClickPopup;
    protected SnippetManager snippetManager;
    protected MultiCursorManager multiCursorManager;

    protected static int tabSize;
    protected static String tabSpaces;

    private boolean isDraggingText;
    private boolean mouseExited;
    private int dropCaretX, dropCaretY;

    private final Brackets bracketHelper = new Brackets();
    private final Timer multicursorTimer;

    // CONSTRUCTOR
    public SmartCodeTextArea(TextAreaDefaults defaults, SmartCodeEditor editor) {
        super(defaults, editor);

        SmartCodeInputHandler inputHandler = new SmartCodeInputHandler(editor);

        inputHandler.addKeyListener(new BracketCloser(editor));

        if (SmartCodePreferences.TEMPLATES_ENABLED) {
            snippetManager = new SnippetManager(editor);
            inputHandler.addKeyListener(snippetManager);
            addCaretListener(snippetManager);
            getSmartCodePainter().addLinePainter(snippetManager);
        }
        // default behaviour for the textarea in regards to TAB and ENTER key
        inputHandler.addKeyListener(editor);

        multiCursorManager = new MultiCursorManager(this);
        inputHandler.addKeyListener(multiCursorManager);
        getSmartCodePainter().addLinePainter(multiCursorManager);
        
        setInputHandler(inputHandler);

        // Remove PdeTextArea's default gutterCursorMouseAdapter listener so we
        // can add our own listener
        painter.removeMouseMotionListener(gutterCursorMouseAdapter);

        // Handles mouse clicks to toggle line bookmarks
        gutterAreaMouseHandler = new GutterAreaMouseHandler();
        painter.addMouseListener(gutterAreaMouseHandler);
        painter.addMouseMotionListener(gutterAreaMouseHandler);

        // Remove JEditTextArea's default MouseHandler and DragHandler listeners so we
        // can add our own MouseListener and MouseMotionListener
        painter.removeMouseListener(painter.getMouseListeners()[2]);
        painter.removeMouseMotionListener(painter.getMouseMotionListeners()[1]);

        textAreaMouseHandler = new TextAreaMouseHandler();

        // Remove JEditTextArea's default MouseWheelListener listener so we
        // can add our own listener
        removeMouseWheelListener(getMouseWheelListeners()[0]);
        
        addMouseWheelListener(e -> {
            if (e.isControlDown()) { // zoom in/out text
                int clicks = e.getWheelRotation();
                getSmartCodePainter().setFontSize(getSmartCodePainter().getFontSize() - clicks);

            } else if (scrollBarsInitialized) {
                if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                    int scrollAmount = e.getUnitsToScroll();
                    
                    if (e.isShiftDown()) {
                        int charWidth = painter.getFontMetrics().charWidth('w'); 
                        int val = Math.max(-charWidth, getHorizontalScrollPosition() + scrollAmount * charWidth);
                        setHorizontalScrollPosition(val);
                    } else {
                        int val = getVerticalScrollPosition() + scrollAmount;
                        setVerticalScrollPosition(val);
                    }
                }
            }
        });

        multicursorTimer = new Timer(500, e -> {
            if (hasFocus() && multiCursorManager.isActive()) {
                multiCursorManager.blinkCursors();
            }
        });
        multicursorTimer.setInitialDelay(500);
        multicursorTimer.start();
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
                    painter.removeMouseListener(textAreaMouseHandler);
                    painter.removeMouseMotionListener(textAreaMouseHandler);
                    painter.setCursor(DEFAULT_CURSOR);
                }

            } else { // check if the cursor is OUTSIDE the left gutter area (inside the text area)
                if (lastX < Editor.LEFT_GUTTER) {
                    painter.addMouseListener(textAreaMouseHandler);
                    painter.addMouseMotionListener(textAreaMouseHandler);
                    painter.setCursor(TEXT_CURSOR);
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

    protected class TextAreaMouseHandler extends MouseAdapter {
        int lastX; // previous horizontal position of the mouse cursor

        @Override
        public void mousePressed(MouseEvent e) {
            if (!hasFocus()) {
                if (!requestFocusInWindow()) {
                    return;
                }
            }

            boolean windowsRightClick = Platform.isWindows() && (e.getButton() == MouseEvent.BUTTON3);
            if ((e.isPopupTrigger() || windowsRightClick) && (popup != null)) {
                int offset = xyToOffset(e.getX(), e.getY());
                int selectionStart = getSelectionStart();
                int selectionStop = getSelectionStop();
                if (offset < selectionStart || offset >= selectionStop) {
                    select(offset, offset);
                }

                popup.show(painter, e.getX(), e.getY());
                return;
            }

            int line = yToLine(e.getY());
            int offset = xToOffset(line, e.getX());
            int dot = getLineStartOffset(line) + offset;

            selectLine = false;
            selectWord = false;

            switch (e.getClickCount()) {
            case 1:
                if (isCaretInsideSelection(dot)) {
                    isDraggingText = true;
                    updateDropCaretOffset(e.getX(), e.getY());
                    updateDragAndDropIcon(e);
                    break;
                }
                doSingleClick(e, line, offset, dot);
                break;
            case 2:
                doDoubleClick(e, line, offset, dot);
                break;
            case 3:
                doTripleClick(e, line, offset, dot);
                break;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isDraggingText) {
                if (mouseExited) {
                    painter.setCursor((e.getX() < Editor.LEFT_GUTTER) ? DEFAULT_CURSOR : TEXT_CURSOR);
                    editor.setCursor(DEFAULT_CURSOR);
                    isDraggingText = false;
                    return;
                }

                int caret = xyToOffset(e.getX(), e.getY());
                int start = caret;
                int end = caret;

                if (!isCaretInsideSelection(caret)) {
                    document.beginCompoundEdit();
                    String selectedText = getSelectedText();
                    int oldSelectionStart = selectionStart;
                    int selectionLength = selectedText.length();

                    if (e.isControlDown()) { // copy-paste operation
                        setCaretPosition(caret);
                        start = caret;
                        end = start + selectionLength;

                    } else { // cut-paste operation
                        setSelectedText("");
                        if (caret > oldSelectionStart) {
                            setCaretPosition(caret - selectionLength);
                            start = caret - selectionLength;
                            end = caret;
                        } else {
                            setCaretPosition(caret);
                            start = caret;
                            end = start + selectionLength;
                        }
                    }

                    setSelectedText(selectedText);
                    document.endCompoundEdit();
                }
                select(start, end);
                painter.setCursor(TEXT_CURSOR);
                isDraggingText = false;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (popup != null && popup.isVisible())
                return;

            int x = e.getX();
            int y = e.getY();

            if (isDraggingText) {
                if (x < Editor.LEFT_GUTTER && lastX >= Editor.LEFT_GUTTER) {
                    painter.setCursor(INVALID_CURSOR);
                    mouseExited = true;
                    painter.repaint();

                } else if (x >= Editor.LEFT_GUTTER) {
                    if (lastX < Editor.LEFT_GUTTER) {
                        updateDragAndDropIcon(e);
                        mouseExited = false;

                    } else if (!mouseExited) {
                        updateDropCaretOffset(x, y);
                    }
                }
                lastX = x;

            } else if (!multiCursorManager.isActive()) { // Do not enable text selection while we're on multicursor mode
                if (!selectWord && !selectLine) {
                    try {
                        select(getMarkPosition(), xyToOffset(x, y));
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        Messages.err("xToOffset problem", ex);
                    }
                } else {
                    int line = yToLine(y);
                    if (selectWord) {
                        setNewSelectionWord(line, xToOffset(line, x));
                    } else {
                        newSelectionStart = getLineStartOffset(line);
                        newSelectionEnd = getLineSelectionStopOffset(line);
                    }
                    if (newSelectionStart < selectionAncorStart) {
                        select(newSelectionStart, selectionAncorEnd);
                    } else if (newSelectionEnd > selectionAncorEnd) {
                        select(selectionAncorStart, newSelectionEnd);
                    } else {
                        select(newSelectionStart, newSelectionEnd);
                    }
                }
            }
            
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouseExited = true;
            if (isDraggingText) {
                editor.setCursor(INVALID_CURSOR);
                painter.repaint();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            mouseExited = false;
            if (isDraggingText) {
                updateDragAndDropIcon(e);
            }
            editor.setCursor(DEFAULT_CURSOR);
        }

        private void doSingleClick(MouseEvent e, int line, int offset, int dot) {
            if (e.isShiftDown()) {
                select(getMarkPosition(), dot);
            } else {
                setCaretPosition(dot);
            }

            if (e.getButton() == MouseEvent.BUTTON1) {
                if (e.isAltDown()) {
                    multiCursorManager.addCursor(dot);
                } else if (multiCursorManager.isActive()) {
                    multiCursorManager.clear();
                }
            }
        }

        private void doDoubleClick(MouseEvent e, int line, int offset, int dot) {
            // Ignore empty lines
            if (getLineLength(line) != 0) {
                String text = "";
                try {
                    text = document.getText(0, document.getLength());
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }

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

        private void updateDropCaretOffset(int x, int y) {
            int line = yToLine(y);
            int xOffset = xyToOffset(x, y) - getLineStartOffset(line);
            dropCaretX = offsetToX(line, xOffset);
            dropCaretY = lineToY(line) + getSmartCodePainter().getLineDisplacement();
            painter.repaint();
        }
    }

    private void updateDragAndDropIcon(InputEvent e) {
        if (e.isControlDown()) {
            painter.setCursor(COPY_DROP_CURSOR);
        } else {
            painter.setCursor(MOVE_DROP_CURSOR);
        }
    }

    @Override
    public void processKeyEvent(KeyEvent evt) {
        if (isDraggingText && !mouseExited) {
            updateDragAndDropIcon(evt);
            return;
        }
        super.processKeyEvent(evt);
    }

    protected void paintDropCaret(Graphics gfx) {
        if (isDraggingText && !mouseExited) {
            Graphics2D g2 = (Graphics2D) gfx;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getDefaults().bracketHighlightColor);
            g2.fillRect(dropCaretX, dropCaretY, 2, painter.getFontMetrics().getHeight());
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
    
    @Override
    public void select(int start, int end) {
        super.select(start, end);
        
        if (multicursorTimer != null && multiCursorManager.isActive()) {
            multicursorTimer.restart();            
        }
    }
    
    @Override
    public void removeNotify() {
        super.removeNotify();
        multicursorTimer.stop();
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
    
    public int caretPositionInsideLineOfOffset(int offset) {
        int line = getLineOfOffset(offset);
        return offset - getLineStartOffset(line);
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
                if (i < cursor)
                    oddLeft = !oddLeft;
                else
                    oddRight = !oddRight;

                if (oddLeft && oddRight) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isCaretInsideSelection() {
        return isCaretInsideSelection(getCaretPosition());
    }

    public boolean isCaretInsideSelection(int caretPos) {
        if (!isSelectionActive())
            return false;
        return caretPos >= selectionStart && caretPos <= selectionEnd;
    }

    static private String addSpaces(int length) {
        return new String(new char[length]).replace('\0', ' ');
    }
}
