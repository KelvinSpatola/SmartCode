package kelvinspatola.mode.smartcode.completion;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import kelvinspatola.mode.smartcode.SmartCodePreferences;

public class Snippet {
    protected StringBuilder buffer;
    protected String sourceText;
    protected String[] sourceLines;

    protected List<Tabstop> tabstops;
    protected boolean isLastPosition;
    protected int stopIndex;
    protected int startingPosition;
    public int leftBoundary, rightBoundary;
    protected int indent;

    // CONSTRUCTOR
    public Snippet(String source) {
        if (source == null)
            return;

        buffer = new StringBuilder();
        tabstops = new ArrayList<>();
        processSourceText(source);
    }

    protected void processSourceText(String source) {
        char[] sourceChars = source.toCharArray();
        this.sourceText = source;
        int index = 0, indexOffset = 0;

        while (index < sourceChars.length) {
            if (sourceChars[index] == Tabstop.CURSOR) {
                tabstops.add(new Tabstop(index - indexOffset));
                indexOffset++;
            }
            index++;
        }

        String sanitizedSource = source.replace(String.valueOf(Tabstop.CURSOR), "");
        sourceLines = sanitizedSource.split("\n");
        setIndentation(0);
    }

    public String getCode() {
        return getCode(indent);
    }

    public String getCode(int indent) {
        if (indent != this.indent && indent >= 0) {
            setIndentation(indent);
        }
        return buffer.toString();
    }

    public void setIndentation(int indent) {
        this.indent = indent;

        String spaces = new String(new char[indent]).replace('\0', ' ');
        buffer = new StringBuilder();

        for (String line : sourceLines) {
            buffer.append(spaces).append(line).append('\n');
        }
        // get rid of a LF at the end of the last line
        buffer.deleteCharAt(buffer.length() - 1);
    }

    public int getStartPosition(int currentOffset) {
        if (tabstops.isEmpty())
            return currentOffset;

        stopIndex = 0;
        isLastPosition = false;

        for (Tabstop c : tabstops) {
            c.reset();
        }

        int caret = tabstops.get(stopIndex).currentOffset;
        startingPosition = currentOffset - buffer.length();

        leftBoundary = startingPosition + caret + (indent * calcLine(caret));
        rightBoundary = leftBoundary + 1;

        if (tabstops.size() == 1)
            isLastPosition = true;

        return leftBoundary;
    }

    public int nextPosition() {
        int caret = 0, delta = 0;
        stopIndex++;

        for (int i = 0; i < stopIndex && i < tabstops.size(); i++) {
            delta += tabstops.get(i).delta();
        }

        caret = tabstops.get(stopIndex).startOffset;
        leftBoundary = startingPosition + caret + (indent * calcLine(caret)) + delta;
        rightBoundary = leftBoundary + 1;

        if (stopIndex == tabstops.size() - 1) {
            isLastPosition = true;
        }

        return leftBoundary;
    }

    public int getNextStopPosition() {
        if (stopIndex == tabstops.size() - 1)
            return -1;

        int caret = 0, delta = 0;

        for (int i = 0; i < stopIndex && i < tabstops.size(); i++) {
            delta += tabstops.get(i).delta();
        }

        caret = tabstops.get(stopIndex + 1).startOffset;
        return startingPosition + caret + (indent * calcLine(caret)) + delta + (rightBoundary - leftBoundary - 1);
    }

    public void readInput(KeyEvent e) {
        int key = e.getKeyChar();

        // won't do anything if this is a not printable character (except backspace and
        // delete)
        if (key == 8 || key >= 32 && key <= 127) { // 8 -> VK_BACK_SPACE, 127 -> VK_DELETE
            int step = 1;

            if (key == KeyEvent.VK_BACK_SPACE || key == KeyEvent.VK_DELETE) {
                tabstops.get(stopIndex).currentOffset -= step;
                rightBoundary -= step;
                return; // return here to avoid all next if statements
            }

            if (SmartCodePreferences.AUTOCLOSE_BRACKETS) {
                if ("([{".contains(String.valueOf(e.getKeyChar()))) {
                    step = 2;
                }
            }

            if (SmartCodePreferences.AUTOCLOSE_QUOTES) {
                if ("\"\'".contains(String.valueOf(e.getKeyChar()))) {
                    step = 2;
                }
            }
            
            // TODO: verificar situacao quando o utilizador insere um bracket dentro de ''
            // ver BracketCloser.class linha 90

            tabstops.get(stopIndex).currentOffset += step;
            rightBoundary += step;
        }
    }

    public boolean isLastPosition() {
        return isLastPosition;
    }

    public boolean contains(int caret) {
        return (caret >= leftBoundary && caret <= rightBoundary);
    }

    public String getSource() {
        return sourceText;
    }

    protected int calcLine(int offset) {
        int line = 1, index = 0;

        while (index < offset && offset < sourceText.length()) {
            if (sourceText.charAt(index) == '\n') {
                line++;
            }
            index++;
        }
        return line;
    }

    static class Tabstop {
        static final char CURSOR = '$';
        int currentOffset, startOffset;

        Tabstop(int currentOffset) {
            this.currentOffset = currentOffset;
            startOffset = currentOffset;
        }

        int delta() {
            return currentOffset - startOffset;
        }

        void reset() {
            currentOffset = startOffset;
        }
    }
}
