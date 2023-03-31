package kelvinspatola.mode.smartcode.ui;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import kelvinspatola.mode.smartcode.KeyListener;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;

public class MultiCursorManager implements LinePainter, CaretListener, KeyListener {
    private Map<Integer, List<Cursor>> cursors = new HashMap<>();
    private SmartCodeTextArea textarea;
    private boolean isActive;
    private int lastCaretPosition;
    private boolean blink;

    // CONSTRUCTOR
    public MultiCursorManager(SmartCodeTextArea textarea) {
        this.textarea = textarea;
    }

    public void addCursor(int line, int offset, int dot) {
        if (cursors.isEmpty()) {
            isActive = true;
        }

        if (cursors.containsKey(line)) {
            cursors.get(line).add(new Cursor(line, offset, dot));
        } else {
            List<Cursor> list = new ArrayList<>();
            list.add(new Cursor(line, offset, dot));
            cursors.put(line, list);
        }
        textarea.setCaretPosition(lastCaretPosition);
        textarea.repaint();
    }

    @Override
    public boolean handlePressed(KeyEvent e) {
        if (e.isAltDown()) {
            lastCaretPosition = textarea.getCaretPosition();
            return true; // TODO: is this really a good choice?
        }
        blink = true; // cursors should not blink when we are typing
        return false;
    }

    @Override
    public boolean handleTyped(KeyEvent e) {
        return false;
    }

    @Override
    public void caretUpdate(CaretEvent e) {
//        if (!isMultiCursorActive || isAddingCursor) {
//            isAddingCursor = false;
//            return;
//        }
//        
//        int currentLine = textarea.getCaretLine();
//        
//        if (currentLine != lastCaretLine) {
//            cursors.clear();
//            isMultiCursorActive = false;
//        }
//        isAddingCursor = false;
//        System.out.println("isMultiCursorActive: " + isMultiCursorActive);
//        int dot = e.getDot();
//
//        System.out.println("CURRENT: " + dot);
//        System.out.println("LAST: " + lastCaretPosition);
//
//        if (!isMultiCursorActive) {
//            lastCaretPosition = dot;
//            System.out.println("here: " + lastCaretPosition);
//        }
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
//          // TODO: verificar situacao quando o utilizador insere um bracket dentro de ''
//          // ver BracketCloser.class linha 90
//
//          tabstops.get(stopIndex).currentOffset += step;
//          rightBoundary += step;
//      }
//  }
    
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
        cursors.keySet().stream().forEach(textarea.getPainter()::invalidateLine);
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
            
            int caretOffset = ta.getCaretPosition();
            
            cursorsInThisLine.stream()
            .filter(c -> c.dot != caretOffset)
            .forEach(c -> {
                int x = ta._offsetToX(line, c.offset);
                gfx.fillRect(x, y, 2, h);
            });            
        }

    }

    @Override
    public void updateTheme() {
    }

    static class Cursor implements Comparable<Cursor> {
        int line, offset, dot;

        public Cursor(int line, int offset, int dot) {
            this.line = line;
            this.offset = offset;
            this.dot = dot;
        }

        @Override
        public int compareTo(Cursor other) {
            return this.dot - other.dot;
        }

        @Override
        public String toString() {
            return "Cursor [line=" + line + ", offset=" + offset + ", dot=" + dot + "]";
        }

    }


}
