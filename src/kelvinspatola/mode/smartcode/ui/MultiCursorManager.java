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
    private boolean isActive;
    private int lastCaretPosition;
    private boolean blink;

    // CONSTRUCTOR
    public MultiCursorManager(SmartCodeTextArea textarea) {
        this.textArea = textarea;
    }

    public void addCursor(int dot) {
        if (cursors.isEmpty()) {
            isActive = true;
        }
        
        Cursor newCursor = new Cursor(dot);
        int line = newCursor.getLine();
        
        if (cursors.containsKey(line)) {
            if (!cursors.get(line).contains(newCursor)) { // avoid duplicates (overlapping cursors)
                cursors.get(line).add(newCursor);
            } 
        } else {
            List<Cursor> list = new ArrayList<>();
            list.add(newCursor);
            cursors.put(line, list);
        }       
        textArea.setCaretPosition(lastCaretPosition);
        textArea.repaint();
    }

    @Override
    public boolean handlePressed(KeyEvent e) {
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
        
        cursors.keySet().stream().forEach(line -> System.out.println((line + 1) + " - " + cursors.get(line).stream().map(c -> c.getOffset()).collect(Collectors.toList())));
        System.out.println();
        
        if (cursors.isEmpty()) {
            isActive = false;
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
            
            int newLine = entry.getKey() + inc;
            
            if (newLine < 0 || newLine > lastLine) {
                entrySetItr.remove();
                continue;
            }
            
            List<Cursor> cursorsInCurrentLine = entry.getValue();
            Iterator<Cursor> cursorItr = cursorsInCurrentLine.iterator();
            
            while (cursorItr.hasNext()) {
                Cursor c = cursorItr.next();
                int magicCaretPosition = c.getMagicCaretPosition();
                
                c.dot = textArea.getLineStartOffset(newLine) + textArea.xToOffset(newLine, magicCaretPosition);

                if (temp.containsKey(newLine)) {
                    temp.get(newLine).add(c);
                } else {
                    List<Cursor> list = new ArrayList<>();
                    list.add(c);
                    temp.put(newLine, list);
                }
            }
            entrySetItr.remove();
        }
        
        temp.keySet().stream().forEach(key -> {
            temp.get(key).stream().forEach(c -> {
                if (cursors.containsKey(key)) {
                    if (!cursors.get(key).contains(c)) { // avoid duplicates (overlapping cursors)
                        cursors.get(key).add(c);
                    }
                } else {
                    List<Cursor> list = new ArrayList<>();
                    list.add(c);
                    cursors.put(key, list);
                }
            });
        });
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
                int oldLine = c.getLine();
                
                c.dot += inc;
                
                if (c.dot < 0 || c.dot > textLength) {
                    cursorItr.remove();
                    
                    if (cursorsInCurrentLine.isEmpty()) {
                        entrySetItr.remove();
                    }
                    continue;
                }
                
                int newLine = c.getLine();                
                
                if (oldLine != newLine) {
                    if (temp.containsKey(newLine)) {
                        temp.get(newLine).add(c);
                    } else {
                        List<Cursor> list = new ArrayList<>();
                        list.add(c);
                        temp.put(newLine, list);
                    }
                    cursorItr.remove();
                }
                
                if (cursorsInCurrentLine.isEmpty()) {
                    entrySetItr.remove();
                }
            }
        }
        
        temp.keySet().stream().forEach(key -> {
            temp.get(key).stream().forEach(c -> {
                if (cursors.containsKey(key)) {
                    cursors.get(key).add(c);
                } else {
                    List<Cursor> list = new ArrayList<>();
                    list.add(c);
                    cursors.put(key, list);
                }
            });
        });
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
