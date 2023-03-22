package kelvinspatola.mode.smartcode.ui;

import java.awt.Graphics;

import kelvinspatola.mode.smartcode.SmartCodeTextArea;

public interface LinePainter {
    void paintLine(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta);

    void updateTheme();
}