package kelvinspatola.mode.smartcode;

import java.awt.Graphics;

public interface LinePainter {
    boolean canPaint(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta);
}