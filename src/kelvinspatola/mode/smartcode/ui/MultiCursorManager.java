package kelvinspatola.mode.smartcode.ui;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.swing.text.BadLocationException;

import kelvinspatola.mode.smartcode.KeyListener;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;

public class MultiCursorManager implements LinePainter, KeyListener {
    private Map<Integer, List<Cursor>> cursors = new TreeMap<>();
    private SmartCodeTextArea textArea;
    private int lastCaretPosition;
    private boolean isActive;
    private boolean blink;
    
    private int cursorCount; // TODO: validar se isto faz o mesmo que o método getCount com stream
    private int topLine, bottomLine;

    // CONSTRUCTOR
    public MultiCursorManager(SmartCodeTextArea textarea) {
        this.textArea = textarea;
    }

    public void addCursor(int dot) {
        if (cursors.isEmpty()) {
            Cursor source = new Cursor(lastCaretPosition);
            int sourceLine = source.getLine();
            
            List<Cursor> list = new ArrayList<>();
            list.add(source);
            cursors.put(sourceLine, list);   
            
            topLine = sourceLine;
            bottomLine = sourceLine;
            cursorCount = 1;
            
            textArea.getSmartCodeEditor().stopTrackingCodeOccurences();
            textArea.setCaretVisible(false);
            isActive = true;
            
            textBuffer = new StringBuilder(textArea.getText());
        }
        
        Cursor newCursor = new Cursor(dot);
        int line = newCursor.getLine(); 
        merge(cursors, line, newCursor);
        
        cursorCount++;
        
        if (line < topLine) topLine = line;
        if (line > bottomLine) bottomLine = line;
        
        textArea.setCaretPosition(lastCaretPosition);
        textArea.repaint();
        
        if (getCount() == 1) { 
            // This can only happen if we add the first cursor right on top of the caret
            // position. Multicursor won't be activated in this situation.
            clear();
        }
    }
    
    private void addCursorWithKeyboard(boolean up) {
        int currentCaret = textArea.getCaretPosition();
        int newLine = up ? -1 : 1;
        if (cursors.isEmpty()) {
            newLine += textArea.getCaretLine();
        } else {
            newLine += getTopOrBottomLine(up);   
        }
        
        Cursor c = new Cursor(currentCaret);
        int currentMagicCaret = c.getMagicCaretPosition();
        int newDot = textArea.getLineStartOffset(newLine) + textArea.xToOffset(newLine, currentMagicCaret);
        
        addCursor(newDot);
    } 

    @Override
    public boolean handlePressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        if (e.isAltDown() && e.isShiftDown() && keyCode == KeyEvent.VK_UP) {
            addCursorWithKeyboard(true);
        } else if (e.isAltDown() && e.isShiftDown() && keyCode == KeyEvent.VK_DOWN) {
            addCursorWithKeyboard(false);
        }
        
        if (e.isAltDown()) {
            lastCaretPosition = textArea.getCaretPosition();

            if (isActive) {
                // Mark this event as having an undefined keystroke 
                // so that it won't trigger any menu mnemonics.
                e.setKeyCode(KeyEvent.VK_UNDEFINED);
                e.setKeyChar(KeyEvent.CHAR_UNDEFINED);
            }
            return false; 
        }

        if (!isActive)
            return false;
        
        blink = true; // cursors should not blink when we are typing

        if (keyCode == KeyEvent.VK_UP) {
            moveVertically(cursors, true);
        } else if (keyCode == KeyEvent.VK_DOWN) {
            moveVertically(cursors, false);
        } else if (keyCode == KeyEvent.VK_LEFT) {
            moveHorizontally(cursors, true);
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            moveHorizontally(cursors, false);
        }
        
//        cursors.keySet().stream().forEach(line -> System.out.println((line + 1) + " - " + cursors.get(line).stream().map(c -> c.dot).collect(Collectors.toList())));
        
//        cursors.keySet().stream().forEach(line -> cursors.get(line).stream().forEach(c -> {
//            System.out.println("line: " + c.getLine() + " - dot: " + c.dot + " - offset: " + c.getOffset());            
//        }));
//        System.out.println("count: " +  getCount());
//        System.out.println();
        
        if (getCount() < 2) {
            clear();
        }
        
        textArea.repaint();
        return false;
    }
    
    StringBuilder textBuffer = new StringBuilder();
    
    @Override
    public boolean handleTyped(KeyEvent e) {
        if (!isActive) 
            return false;
        
        char KeyChar = e.getKeyChar();
        
        int key = KeyChar;
        
        boolean isBackspace = key == KeyEvent.VK_BACK_SPACE;

        if (Character.isISOControl(KeyChar) && !isBackspace) {
            System.out.println("ISOControl key: '" + Character.getName(KeyChar) + "'");
            return false;
        }
//
//        System.out.println("char: '" + KeyChar + "'");
                
        
        var linesWithCursors = cursors.keySet();
        int offset = 0;
        
        if (isBackspace) {
            offset++;
            for (var line : linesWithCursors) {
                var cursorsPerLine = cursors.get(line);
                
                for (var cursor : cursorsPerLine) {
                    int caret = cursor.dot - offset;
                    
                    textBuffer.delete(caret, caret + 1);
                    
                    cursor.dot = caret;
                    offset++;
                }
            }
        } else {
            
            for (var line : linesWithCursors) {
                var cursorsPerLine = cursors.get(line);
                
                for (var cursor : cursorsPerLine) {
                    int caret = cursor.dot + offset;
                    
                    textBuffer.insert(caret, KeyChar);
                    
                    cursor.dot = caret + 1;
                    offset++;
                }
            }
        }
        
        int firstLine = textArea.getFirstLine();  
        
        textArea.getDocument().beginCompoundEdit();
//        textArea.selectAll();
//        textArea.replaceSelectedText(textBuffer.toString());
        try {
            textArea.getDocument().replace(0, textArea.getDocument().getLength(), textBuffer.toString(), null);
        } catch (BadLocationException e1) {
            e1.printStackTrace();
        }
        textArea.getDocument().endCompoundEdit();
        
        Cursor rightmostCursor = cursors.values().stream()
                .flatMap(List::stream)
                .sorted((c1, c2) -> c2.getOffset() - c1.getOffset())
                .findFirst()
                .orElseThrow();

        textArea.setCaretPosition(rightmostCursor.dot);
        textArea.scrollTo(firstLine, rightmostCursor.getOffset());
        

        // OPÇÃO 1
//        textArea.getDocument().beginCompoundEdit();
//
//        for (var line : linesWithCursors) {
//            var cursorsPerLine = cursors.get(line);
//            
//            if (isBackspace) {
//                for (var cursor : cursorsPerLine) {
//                    int caret = cursor.dot - offset;
//                    textArea.select(caret - 1, caret);
//                    textArea.setSelectedText("");
//                    cursor.dot = caret - 1;
//                    offset++;
//                }                
//            } else {
//                for (var cursor : cursorsPerLine) {
//                    int caret = cursor.dot + offset;
//                    textArea.select(caret, caret);
//                    textArea.setSelectedText(String.valueOf(KeyChar));
//                    cursor.dot = caret + 1;
//                    offset++;
//                }                                
//            }
//        }
//        textArea.getDocument().endCompoundEdit();
        
        return true;
    }
    
    private void moveVertically(Map<Integer, List<Cursor>> cursors, boolean up) {
        Map<Integer, List<Cursor>> temp = new TreeMap<>();
        var  entrySetItr = cursors.entrySet().iterator();
        int lastLine = textArea.getLineCount() - 1;
        int inc = up ? -1 : 1;
        
        topLine = Math.max(0, topLine + inc);
        bottomLine = Math.min(lastLine, bottomLine + inc);
        
        while (entrySetItr.hasNext()) {
            var entry = entrySetItr.next();
            Iterator<Cursor> cursorItr = entry.getValue().iterator();
            
            int newLine = entry.getKey() + inc;
            
            if (newLine < 0) {
                while (cursorItr.hasNext()) {
                    Cursor c = cursorItr.next();
                    c.dot = 0;
                    merge(temp, 0, c);
                }
                
            } else if (newLine > lastLine) {
                int textLength = textArea.getDocumentLength();
                while (cursorItr.hasNext()) {
                    Cursor c = cursorItr.next();
                    c.dot = textLength;
                    merge(temp, lastLine, c);
                }

            } else {
                while (cursorItr.hasNext()) {
                    Cursor c = cursorItr.next();
                    int magicCaretPosition = c.getMagicCaretPosition();
                    c.dot = textArea.getLineStartOffset(newLine) + textArea.xToOffset(newLine, magicCaretPosition);
                    merge(temp, newLine, c);
                }
            }
            entrySetItr.remove();
        }
        
        temp.keySet().stream().forEach(line -> {
            temp.get(line).stream().forEach(cursor -> merge(cursors, line, cursor));
        });
        
        // Ensure that the first/last cursor stays visible
        if (!cursors.isEmpty()) {
            textArea.scrollTo(getTopOrBottomLine(up), 0);
        }
    }

    private void moveHorizontally(Map<Integer, List<Cursor>> cursors, boolean left) {
        Map<Integer, List<Cursor>> temp = new TreeMap<>();
        var  entrySetItr = cursors.entrySet().iterator();
        int textLength = textArea.getDocumentLength();
        int inc = left ? -1 : 1;

        while (entrySetItr.hasNext()) {
            List<Cursor> cursorsInCurrentLine = entrySetItr.next().getValue();
            Iterator<Cursor> cursorItr = cursorsInCurrentLine.iterator();

            while (cursorItr.hasNext()) {
                Cursor c = cursorItr.next();
                c.setMagicCaretPosition(-1);
                int line = c.getLine();
                c.dot += inc;
                int newLine = c.getLine();  
                
                if (c.dot < 0) {
                    c.dot = 0;
                } else if (c.dot > textLength) {
                    c.dot = textLength;
                } else if (line != newLine) {
                    line = newLine;
                }
                
                merge(temp, line, c);
                cursorItr.remove();
                
                if (cursorsInCurrentLine.isEmpty()) {
                    entrySetItr.remove();
                }
            }
        }
        
        temp.keySet().stream().forEach(line -> {
            temp.get(line).stream().forEach(cursor -> merge(cursors, line, cursor));
        });
    }
    
    private void merge(Map<Integer, List<Cursor>> map, int line, Cursor cursor) {
        if (map.containsKey(line)) {
            if (!map.get(line).contains(cursor)) { // avoid duplicates (overlapping cursors)
                map.get(line).add(cursor);
            }
        } else {
            List<Cursor> list = new ArrayList<>();
            list.add(cursor);
            map.put(line, list);
        }
    }
    
    private int getTopOrBottomLine(boolean up) {
//        if (up)
//            return cursors.keySet().stream().mapToInt(v -> v).min().getAsInt();
//        return cursors.keySet().stream().mapToInt(v -> v).max().getAsInt();
        if (up) 
            return topLine;
        return bottomLine;
    }
  
    public boolean isActive() {
        return isActive;
    }
    
    public void clear() {
        cursors.clear();
        cursorCount = 0;
        isActive = false;
        textArea.getSmartCodeEditor().startTrackingCodeOccurences();
        textArea.setCaretVisible(true);
        textBuffer = null;
    }
    
    public long getCount() {
        long count = cursors.values().stream().flatMap(v -> v.stream()).count();
        System.out.println("count: " + count + ", cursorCount: " + cursorCount);
        return count;
    }
    
    public void blinkCursors() {
        blink = !blink;
        cursors.keySet().stream().forEach(textArea.getPainter()::invalidateLine);
    }
    
    @Override
    public void paintLine(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta) {
        if (cursors.isEmpty() || !cursors.containsKey(line))
            return;

        if (blink) {
            List<Cursor> cursorsInThisLine = cursors.get(line);
            if (cursorsInThisLine == null)
                return;
            
            gfx.setColor(ta.getDefaults().bracketHighlightColor);

            cursorsInThisLine.stream()
                    .forEach(c -> gfx.fillRect(ta._offsetToX(line, c.getOffset()), y, 2, h));
        }

    }
    
    @Override
    public void updateTheme() {
    }
    
    class Cursor {
        public int dot;
        int magic = -1;
        
        Cursor(int dot) {
            this.dot = dot;
        }

        int getOffset() {
            return dot - textArea.getLineStartOffset(getLine());
        }
        
        int getLine() {
            return textArea.getLineOfOffset(dot);
        }
        
        void setMagicCaretPosition(int magic) {
            this.magic = magic;
        }
        
        int getMagicCaretPosition() {
            if (magic == -1) {
                magic = textArea._offsetToX(getLine(), getOffset()); // textArea.offsetToX()     
            }
            return magic;
        }
        
        public int hashCode() {
            return Objects.hash(dot);
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            return dot == ((Cursor) obj).dot;
        }

        public String toString() {
            return "Cursor [dot=" + dot + ", line=" + getLine() + ", offset=" + getOffset() + ", magic=" + magic + "]";
        }

    }

}