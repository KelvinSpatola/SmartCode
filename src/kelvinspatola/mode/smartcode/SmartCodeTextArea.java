package kelvinspatola.mode.smartcode;

import java.util.ArrayDeque;
import java.util.Deque;

import processing.app.Preferences;
import processing.app.syntax.TextAreaDefaults;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaTextArea;

public class SmartCodeTextArea extends JavaTextArea {
    public static final int TAB_SIZE = Preferences.getInteger("editor.tabs.size");
    public static final String TAB = addSpaces(TAB_SIZE);;
    public static final String LF = "\n";
    
    public static final String BLOCK_OPENING = "^(?!.*?\\/+.*?\\{.*|\\h*\\*.*|.*?\\\".*?\\{.*?\\\".*).*?\\{.*$";
    public static final String BLOCK_CLOSING = "^(?!.*?\\/+.*?\\}.*|.*\\/\\*.*|\\h*\\*.*).*?\\}.*";


    public SmartCodeTextArea(TextAreaDefaults defaults, JavaEditor editor) {
        super(defaults, editor);
        
        SmartCodeInputHandler inputHandler = new SmartCodeInputHandler(editor);
        
        if (SmartCodePreferences.BRACKETS_AUTO_CLOSE) {
            inputHandler.addKeyListener(new BracketCloser(editor));
        }
//        if (SmartCodePreferences.TEMPLATES_ENABLED) {
//            inputHandler.addKeyListener(new TemplatesManager(editor));
//        }
        setInputHandler(inputHandler);
        
    }
    
    @Override
    protected SmartCodeTextAreaPainter createPainter(final TextAreaDefaults defaults) {
        return new SmartCodeTextAreaPainter(this, defaults);
    }
    
    protected SmartCodeTextAreaPainter getSmartCodePainter() {
        return (SmartCodeTextAreaPainter) painter;
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

    private static String addSpaces(int length) {
        if (length <= 0)
            return "";
        return String.format("%1$" + length + "s", "");
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
}
