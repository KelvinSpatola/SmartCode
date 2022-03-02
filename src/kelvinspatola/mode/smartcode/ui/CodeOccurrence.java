package kelvinspatola.mode.smartcode.ui;

import processing.app.ui.Editor;

public class CodeOccurrence  implements LineMarker {
    protected Editor editor;
    
    public CodeOccurrence(Editor editor) {
        this.editor = editor;
    }

    @Override
    public int getTabIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getLine() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getStartOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getStopOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getText() {
        // TODO Auto-generated method stub
        return null;
    }

}
