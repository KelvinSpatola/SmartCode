package kelvinspatola.mode.smartcode;

import static kelvinspatola.mode.smartcode.Constants.*;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import kelvinspatola.mode.smartcode.completion.BracketCloser;
import kelvinspatola.mode.smartcode.completion.SnippetManager;
import processing.app.SketchCode;
import processing.app.syntax.TextAreaDefaults;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaTextArea;

public class SmartCodeTextArea extends JavaTextArea {

    // CONSTRUCTOR
    public SmartCodeTextArea(TextAreaDefaults defaults, JavaEditor editor) {
        super(defaults, editor);

        SmartCodeInputHandler inputHandler = new SmartCodeInputHandler(editor);

        if (SmartCodePreferences.BRACKETS_AUTO_CLOSE) {
            inputHandler.addKeyListener(new BracketCloser(editor));
        }
        if (SmartCodePreferences.TEMPLATES_ENABLED) {
            SnippetManager sm = new SnippetManager((SmartCodeEditor) editor);
            inputHandler.addKeyListener(sm);
            addCaretListener(sm);
        }
        // default behaviour for the textarea in regards to TAB and ENTER key
        inputHandler.addKeyListener((SmartCodeEditor) editor);
        setInputHandler(inputHandler);
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
        System.out.println("start: " + start + " len: " + len);

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

            if (lineText.matches(STRING_TEXT)) {
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
                    selection = selection.replaceAll("\t", TAB);
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
                            indent = getLineIndentation(getCaretLine()) + TAB_SIZE;
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

    public static String indentText(String text, int indent) {
        String[] lines = text.split(LF);
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            sb.append(addSpaces(indent)).append(line).append(LF);
        }
        return sb.toString();
    }

    public static String outdentText(String text) {
        String[] lines = text.split(LF);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String str = lines[i].substring(TAB_SIZE);
            if (i < lines.length - 1)
                sb.append(str).append(LF);
            else
                sb.append(str);
        }
        return sb.toString();
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

            if (lineText.matches(BLOCK_OPENING)) {
                depthUp++;
            }

            else if (lineText.matches(BLOCK_CLOSING)) {
                depthUp--;
                isTheFirstBlock = false;
            }

            lineIndex--;
        }

        lineIndex = line;
        boolean isTheLastBlock = true;

        if (getLineText(lineIndex).matches(BLOCK_OPENING)) {
            depthDown = 1;
        }

        while (lineIndex < getLineCount()) {
            String lineText = getLineText(lineIndex);

            if (lineText.matches(BLOCK_CLOSING))
                depthDown++;

            else if (lineText.matches(BLOCK_OPENING)) {
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

            if (getLineText(lineIndex).matches(BLOCK_CLOSING)) {
                lineIndex--;
            }

            while (lineIndex >= 0) {
                String lineText = getLineText(lineIndex);

                if (lineText.matches(BLOCK_CLOSING)) {
                    blockDepth++;

                } else if (lineText.matches(BLOCK_OPENING)) {
                    blockDepth--;

                    if (blockDepth == 0)
                        return lineIndex;

                }
                lineIndex--;
            }
        } else { // go down

            if (getLineText(lineIndex).matches(BLOCK_OPENING)) {
                lineIndex++;
            }

            while (lineIndex < getLineCount()) {
                String lineText = getLineText(lineIndex);

                if (lineText.matches(BLOCK_OPENING)) {
                    blockDepth++;

                } else if (lineText.matches(BLOCK_CLOSING)) {
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

            if (lineText.matches(BLOCK_CLOSING)) {
                blockDepth++;

            } else if (lineText.matches(BLOCK_OPENING) && !first) {
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
        if (length <= 0)
            return "";
        return String.format("%1$" + length + "s", "");
    }

}
