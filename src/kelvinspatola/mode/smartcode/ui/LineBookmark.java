package kelvinspatola.mode.smartcode.ui;

import static kelvinspatola.mode.smartcode.Constants.PIN_MARKER;

import java.awt.Color;

import kelvinspatola.mode.smartcode.SmartCodeEditor;
import processing.app.SketchCode;
import processing.mode.java.debug.LineHighlight;
import processing.mode.java.debug.LineID;

public class LineBookmark implements LineMarker, Comparable<LineBookmark> {
    private SmartCodeEditor editor;
    private LineHighlight highlight;
    private Color color;

    
    public LineBookmark(SmartCodeEditor editor, LineID lineID, Color color) {
        this.editor = editor;
        highlight = new LineHighlight(lineID, editor);
        highlight.setMarker(PIN_MARKER);
        this.color = color;
    }
        
    public void paint() {
        highlight.paint();
    }

    @Override
    public int compareTo(LineBookmark other) {
        if (this.getTabIndex() - other.getTabIndex() < 0)
            return -1;
        else if (this.getTabIndex() - other.getTabIndex() > 0)
            return 1;

        return this.getLine() - other.getLine();
    }

    public boolean isOnLine(LineID testLine) {
        return highlight.getLineID().equals(testLine);
    }
    
    public void stopTracking() {
        highlight.getLineID().stopTracking();
    }
    
    public void startTracking() {
        highlight.getLineID().startTracking(editor.currentDocument());
    }
    
    public void dispose() {
        highlight.clear();
        highlight.dispose();
    }
    
    public LineID getLineID() {
        return highlight.getLineID();
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }

    @Override
    public int getTabIndex() {
        SketchCode[] code = editor.getSketch().getCode();
        for (int i = 0; i < code.length; i++) {
            String tabName = code[i].getFileName();
            if (getLineID().fileName().equals(tabName))
                return i;
        }
        return -1;
    }
    
    @Override
    public int getLine() {
        return highlight.getLineID().lineIdx();
    }

    @Override
    public int getStartOffset() {
        return editor.getSmartCodeTextArea().getLineStartOffset(getTabIndex(), getLine());
    }
    
    @Override
    public int getStopOffset() {
        return editor.getSmartCodeTextArea().getLineStopOffset(getTabIndex(), getLine()) - 1;
    }
    
    @Override
    public String getText() {
        return editor.getSmartCodeTextArea().getLineText(getTabIndex(), getLine());
    }
    
    public Class<?> getParent() {
        return this.getClass();
    }
}
