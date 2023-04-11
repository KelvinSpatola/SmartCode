package kelvinspatola.mode.smartcode.ui;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import kelvinspatola.mode.smartcode.KeyListener;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;

public class MultiCursorManager implements LinePainter, KeyListener {
    private Map<Integer, List<Cursor>> cursors = new TreeMap<>();
    private SmartCodeTextArea textArea;
    private int lastCaretPosition;
    private boolean isActive;
    private boolean blink;

    // CONSTRUCTOR
    public MultiCursorManager(SmartCodeTextArea textarea) {
        this.textArea = textarea;
    }

    public void addCursor(int dot) {
        if (cursors.isEmpty()) {
            List<Cursor> list = new ArrayList<>();
            list.add(new Cursor(lastCaretPosition));
            cursors.put(textArea.getLineOfOffset(lastCaretPosition), list);    
            
            textArea.getSmartCodeEditor().stopTrackingCodeOccurences();
            textArea.setCaretVisible(false);
            isActive = true;
        }
        
        Cursor newCursor = new Cursor(dot);
        int line = newCursor.getLine(); 
        merge(cursors, line, newCursor);
        textArea.setCaretPosition(lastCaretPosition);
        textArea.repaint();
        
        if (getCount() == 1) { 
            // This can only happen if we add the first cursor right on top of the caret
            // position. Multicursor won't be activated in this situation.
            clear();
        }
    }
    
    public void addCursorWithKeyboard(boolean up) {
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
        if (e.isAltDown() && e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_UP) {
            addCursorWithKeyboard(true);
        } else if (e.isAltDown() && e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_DOWN) {
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

        switch (e.getKeyCode()) {
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_BACK_SPACE:
            moveHorizontally(cursors, true);
            break;   
        case KeyEvent.VK_RIGHT:
            moveHorizontally(cursors, false);
            break;   
        case KeyEvent.VK_UP:
            moveVertically(cursors, true);
            break;   
        case KeyEvent.VK_DOWN:
            moveVertically(cursors, false);
            break;   
        default:
            moveHorizontally(cursors, false);
        }
        
        cursors.keySet().stream().forEach(line -> System.out.println((line + 1) + " - " + cursors.get(line).stream().map(c -> c.dot).collect(Collectors.toList())));
        
//        cursors.keySet().stream().forEach(line -> cursors.get(line).stream().forEach(c -> {
//            System.out.println("line: " + c.getLine() + " - dot: " + c.dot + " - offset: " + c.getOffset());            
//        }));
        System.out.println("count: " +  getCount());
        System.out.println();
        
        if (getCount() < 2) {
            clear();
        }
        
        textArea.repaint();
        return false;
    }
    
    private void moveVertically(Map<Integer, List<Cursor>> cursors, boolean goUp) {
        Map<Integer, List<Cursor>> temp = new TreeMap<>();
        var  entrySetItr = cursors.entrySet().iterator();
        int inc = goUp ? -1 : 1;
        int lastLine = textArea.getLineCount() - 1;
        
        
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
            textArea.scrollTo(getTopOrBottomLine(goUp), 0);
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
        if (up)
            return cursors.keySet().stream().mapToInt(v -> v).min().orElseThrow();
        return cursors.keySet().stream().mapToInt(v -> v).max().orElseThrow();
    }
    
    @Override
    public boolean handleTyped(KeyEvent e) {
        return false;
    }
    
    public boolean isLineSelected(int line) {
        return cursors.containsKey(line);
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void clear() {
        cursors.clear();
        isActive = false;
        textArea.getSmartCodeEditor().startTrackingCodeOccurences();
        textArea.setCaretVisible(true);
    }
    
    public long getCount() {
        return cursors.values().stream().flatMap(v -> v.stream()).count();
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
        int dot;
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
    
    
//  public void readInput(KeyEvent e) {
//      int key = e.getKeyChar();
//
//      // won't do anything if this is a not printable character (except backspace and
//      // delete)
//      if (key == 8 || key >= 32 && key <= 127) { // 8 -> VK_BACK_SPACE, 127 -> VK_DELETE
//          int step = 1;
//
//          if (key == KeyEvent.VK_BACK_SPACE || key == KeyEvent.VK_DELETE) {
//              tabstops.get(stopIndex).currentOffset -= step;
//              rightBoundary -= step;
//              return; // return here to avoid all next if statements
//          }
//
//          if (SmartCodePreferences.AUTOCLOSE_BRACKETS) {
//              if ("([{".contains(String.valueOf(e.getKeyChar()))) {
//                  step = 2;
//              }
//          }
//
//          if (SmartCodePreferences.AUTOCLOSE_QUOTES) {
//              if ("\"\'".contains(String.valueOf(e.getKeyChar()))) {
//                  step = 2;
//              }
//          }
//          
//          // ver BracketCloser.class linha 90
//
//          tabstops.get(stopIndex).currentOffset += step;
//          rightBoundary += step;
//      }
//  }
}