package kelvinspatola.mode.smartcode;

import static kelvinspatola.mode.smartcode.Constants.*;

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
import processing.app.Preferences;
import processing.app.SketchCode;
import processing.app.syntax.TextAreaDefaults;
import processing.app.ui.Editor;
import processing.mode.java.JavaTextArea;

public class SmartCodeTextArea extends JavaTextArea {
    private MouseListener pdeMouseHandlerListener;
    private MouseMotionListener pdeDragHandlerListener;
    protected JPopupMenu gutterRightClickPopup;
    protected SnippetManager snippetManager;

    protected static int tabSize;
    protected static String tabSpaces;

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

        // let's capture the default MouseHandler listener
        pdeMouseHandlerListener = painter.getMouseListeners()[2];
        painter.removeMouseListener(pdeMouseHandlerListener);

        // let's capture the default DragHandler listener
        pdeDragHandlerListener = painter.getMouseMotionListeners()[1];
        painter.removeMouseMotionListener(pdeDragHandlerListener);

        // Handle mouse clicks to toggle line bookmarks
        MouseAdapter gutterBookmarkToggling = new GutterMouseHandler();
        painter.addMouseListener(gutterBookmarkToggling);
        painter.addMouseMotionListener(gutterBookmarkToggling);

        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                int clicks = e.getWheelRotation();
                getSmartCodePainter().setFontSize(getSmartCodePainter().getFontSize() - clicks);
            }
        });
    }

    class GutterMouseHandler extends MouseAdapter {
        int lastX; // previous horizontal position of the mouse cursor
        long lastTime; // OS X seems to be firing multiple mouse events
        boolean isGutterPressed;

        @Override
        public void mouseMoved(MouseEvent e) {
            // check if the cursor is INSIDE the left gutter area
            if (e.getX() < Editor.LEFT_GUTTER) {
                if (lastX >= Editor.LEFT_GUTTER) {
                    painter.removeMouseListener(pdeMouseHandlerListener);
                    painter.removeMouseMotionListener(pdeDragHandlerListener);
                    painter.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }

            } else { // check if the cursor is OUTSIDE the left gutter area (inside the text area)
                if (lastX < Editor.LEFT_GUTTER) {
                    painter.addMouseListener(pdeMouseHandlerListener);
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
    };

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
            final String lineText = getLineText(getCaretLine());
            boolean isInsideQuotes = false;

            if (STRING_TEXT.matcher(lineText).matches()) {
                final int caret = caretPositionInsideLine();
                int leftQuotes = 0, rightQuotes = 0;

                for (int i = caret - 1; i >= 0; i--) {
                    if (lineText.charAt(i) == '"') {
                        leftQuotes++;
                    }
                }
                for (int i = caret; i < lineText.length(); i++) {
                    if (lineText.charAt(i) == '"') {
                        rightQuotes++;
                    }
                }
                isInsideQuotes = (leftQuotes % 2 != 0) && (rightQuotes % 2 != 0);
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

                        String[] lines = selection.split(LF);
                        sb = new StringBuilder(lines[0] + "\\n\"" + LF);

                        for (int i = 1; i < lines.length - 1; i++) {
                            sb.append(addSpaces(indent) + "+ \"" + lines[i] + "\\n\"" + LF);
                        }
                        sb.append(addSpaces(indent) + "+ \"" + lines[lines.length - 1]);
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

    static public int getLineIndentation(String lineText) {
        char[] chars = lineText.toCharArray();
        int index = 0;

        while (index < chars.length && Character.isWhitespace(chars[index])) {
            index++;
        }
        return index;
    }

    public int getLineIndentation(int line) {
        int start = getLineStartOffset(line);
        int end = getLineStartNonWhiteSpaceOffset(line);
        return end - start;
    }

    public int getLineIndentationOfOffset(int offset) {
        int line = getLineOfOffset(offset);
        return getLineIndentation(line);
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

    public int getMatchingBraceLine(boolean goUp) {
        return getMatchingBraceLine(getCaretLine(), goUp);
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

    // HACK
    public int getMatchingBraceLineAlt(int lineIndex) {
        if (lineIndex < 0) {
            return -1;
        }

        int blockDepth = 1;
        boolean first = true;

        while (lineIndex >= 0) {
            String lineText = getLineText(lineIndex);

            if (BLOCK_CLOSING.matcher(lineText).matches()) {
                blockDepth++;

            } else if (BLOCK_OPENING.matcher(lineText).matches() && !first) {
                blockDepth--;

                if (blockDepth == 0)
                    return lineIndex;

            }
            lineIndex--;
            first = false;
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
        return getPositionInsideLineWithOffset(getCaretPosition());
    }

    public int getPositionInsideLineWithOffset(int offset) {
        int line = getLineOfOffset(offset);
        int lineStartOffset = getLineStartOffset(line);
        return offset - lineStartOffset;
    }

    public int getOffsetOfPrevious(char ch) {
        return getOffsetOfPrevious(ch, getCaretPosition());
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

    public char prevChar() {
        return prevChar(getText(), getCaretPosition() - 1);
    }

    public char prevChar(int index) {
        return prevChar(getText(), index);
    }

    public static char prevChar(String text, int index) {
        char[] code = text.toCharArray();

        while (index >= 0) {
            if (!Character.isWhitespace(code[index])) {
                return code[index];
            }
            index--;
        }
        return Character.UNASSIGNED;
    }

    private static String addSpaces(int length) {
        return new String(new char[length]).replace('\0', ' ');
    }

}
