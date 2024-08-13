package kelvinspatola.mode.smartcode.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import kelvinspatola.mode.smartcode.SmartCodeEditor;
import kelvinspatola.mode.smartcode.SmartCodePreferences;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;
import processing.mode.java.debug.LineHighlight;
import processing.mode.java.debug.LineID;

public class ColorDecorator implements LinePainter {
    private final Set<TargetLine> targetLines = new HashSet<>();
    private SmartCodeEditor editor;
    
//    Set<String> targetFunctions = Set.of("fill", "stroke", "tint", "background");
    
    private final Pattern COLORING_CONTEXT_FUNCTION = Pattern.compile("^.*(?:fill|color|stroke|tint|background)\\s*\\(\\s*\\d+\\s*(?:,\\s*\\d+\\s*){0,3}\\s*\\)\\s*;?$");
    
    
    // CONSTRUCTOR
    public ColorDecorator(SmartCodeEditor editor) {
        this.editor = editor;
    }
    
    public void validateLine(int line) {
        if (!SmartCodePreferences.COLOR_DECORATOR_ENABLED) 
            return;
        
        if (COLORING_CONTEXT_FUNCTION.matcher(editor.getLineText(line)).matches()) {
            editor.timedAction(() -> {
                targetLines.add(new TargetLine(editor.getLineIDInCurrentTab(line)));
                editor.repaint();
                System.out.println("Line " + line + " added");                
            }, 1500);
        } 
        
        System.out.println("targetLines.size(): " + targetLines.size());
    }
    
    public boolean containsLine(int line) {
        for (TargetLine target : targetLines)  {
            if (target.getLine() == line) {
                return true;
            }
        }
        return false;
    }
    

    @Override
    public void paintLine(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta) {
        if (targetLines.isEmpty() || !containsLine(line) || editor.isDebuggerEnabled() || !SmartCodePreferences.COLOR_DECORATOR_ENABLED)
            return;
        
//        String lineText = ta.getLineText(line);
//        boolean isOk = false;
//        
//        for (String target : targetFunctions) {
//            if (lineText.contains(target) && lineText.contains(";")) {
//                isOk = true;
//                break;
//            }
//        }
//        
//        if (!isOk) return;
        
        String lineText = ta.getLineText(line);
        if (!lineText.contains(";")) return;
        
        int lineEnd = lineText.indexOf(";") + 1;
        int x = ta._offsetToX(line, lineEnd);
        
        Graphics2D g2 = (Graphics2D) gfx;
        
        int w = h - 1;

        g2.setColor(Color.LIGHT_GRAY);//(ta.getDefaults().bracketHighlightColor);
        g2.fillRoundRect(x, y, w, w, 5, 5);
        
//        if (ilegalColor) {
        g2.setColor(Color.RED);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(x + 1, y + 1, x + w - 1, y + w - 1);
//        }

        // draw black outline
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(x, y, w, w, 5, 5);
    }

    @Override
    public void updateTheme() {
        // TODO Auto-generated method stub
    }

    class TargetLine {
        LineHighlight highlight;

//        int i = 0;
        TargetLine(LineID lineID) {
            highlight = new LineHighlight(lineID, editor); /*{
                public void lineChanged(LineID line, int oldLineIdx, int newLineIdx) {
                    super.lineChanged(lineID, oldLineIdx, newLineIdx);
                    System.out.println(i++ + " LINE CHANGED " + lineID.toString());
                }
            };*/
            highlight.setPriority(1);
        }

        boolean isOnLine(LineID testLine) {
            return highlight.isOnLine(testLine);
        }

        void stopTracking() {
            highlight.getLineID().stopTracking();
        }

        void startTracking() {
            highlight.getLineID().startTracking(editor.currentDocument());
        }

        void dispose() {
            highlight.clear();
            highlight.dispose();
        }

        LineID getLineID() {
            return highlight.getLineID();
        }
        
        int getLine() {
            return highlight.getLineID().lineIdx();
        }

        @Override
        public int hashCode() {
            return highlight.getLineID().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TargetLine other = (TargetLine) obj;
            return Objects.equals(this.getLineID(), other.getLineID());
        }


    }

}
