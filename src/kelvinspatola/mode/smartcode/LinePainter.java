package kelvinspatola.mode.smartcode;

import java.awt.Graphics;

public interface LinePainter {
    void paintLine(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta);

    void updateTheme();
}