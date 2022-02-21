package kelvinspatola.mode.smartcode.ui;

import static kelvinspatola.mode.smartcode.Constants.PIN_MARKER;

import kelvinspatola.mode.smartcode.SmartCodeEditor;
import processing.app.SketchCode;
import processing.mode.java.debug.LineHighlight;
import processing.mode.java.debug.LineID;

public class LineBookmark implements Comparable<LineBookmark> {
    private SmartCodeEditor editor;
    private LineHighlight highlight;

    
    public LineBookmark(SmartCodeEditor editor, LineID lineID) {
        this.editor = editor;
        highlight = new LineHighlight(lineID, editor);
        highlight.setMarker(PIN_MARKER);
    }
        
    public void paint() {
        highlight.paint();
    }

    @Override
    public int compareTo(LineBookmark other) {
        if (this.getTab() - other.getTab() < 0)
            return -1;
        else if (this.getTab() - other.getTab() > 0)
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

    public int getTab() {
        SketchCode[] code = editor.getSketch().getCode();
        for (int i = 0; i < code.length; i++) {
            String tabName = code[i].getFileName();
            if (getLineID().fileName().equals(tabName))
                return i;
        }
        return -1;
    }
    
    public int getLine() {
        return highlight.getLineID().lineIdx();
    }
    
    public LineID getLineID() {
        return highlight.getLineID();
    }

    public String getText() {
        return editor.getLineText(getLine());
    }
}
