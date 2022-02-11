package kelvinspatola.mode.smartcode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.mode.java.JavaTextArea;
import processing.mode.java.JavaTextAreaPainter;

public class SmartCodeTextAreaPainter extends JavaTextAreaPainter {

    public SmartCodeTextAreaPainter(JavaTextArea textarea, TextAreaDefaults defaults) {
        super(textarea, defaults);

        highlights = new Highlight() {
            @Override
            public String getToolTipText(MouseEvent e) {
                return null;
            }

            @Override
            public void init(JEditTextArea textarea, Highlight highlight) {
            }

            @Override
            public void paintHighlight(Graphics gfx, int line, int y) {

                if (!textarea.isSelectionActive() && line == textarea.getCaretLine()) {
                    int x = textArea._offsetToX(0, 0);
                    //y += getLineDisplacement();
                    int w = textArea._offsetToX(line, ((SmartCodeTextArea) textarea).caretPositionInsideLine()) - x;
                    int h = fontMetrics.getHeight();

                    // gfx.setColor(defaults.selectionColor);
                    gfx.setColor(new Color(100, 255, 50));
                    gfx.drawRect(x, y, w, h);

                } else {
                    repaint();
                }
            }
        };

    }

}
