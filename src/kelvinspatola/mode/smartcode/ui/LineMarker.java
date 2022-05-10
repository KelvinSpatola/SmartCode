package kelvinspatola.mode.smartcode.ui;

import java.awt.Graphics;

public interface LineMarker {
    Class<?> getParent();
    
    int getTabIndex();

    int getLine();

    int getStartOffset();

    int getStopOffset();

    String getText();
    
    void paintMarker(Graphics gfx, int x, int y, int w, int h);
}