package kelvinspatola.mode.smartcode.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Set;

import kelvinspatola.mode.smartcode.SmartCodePreferences;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;

public class ColorDecorator implements LinePainter {
    
    // TODO: usar LineID para identificar as linhas target
    Set<String> targetFunctions = Set.of("fill", "stroke", "tint", "background");
    
    

    @Override
    public void paintLine(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta) {
        if (!SmartCodePreferences.COLOR_DECORATOR_ENABLED)
            return;
        
        String lineText = ta.getLineText(line);
        boolean isOk = false;
        
        for (String target : targetFunctions) {
            if (lineText.contains(target) && lineText.contains(";")) {
                isOk = true;
                break;
            }
        }
        
        if (!isOk) return;

        int lineEnd = lineText.indexOf(";") + 1;
        int x = ta._offsetToX(line, lineEnd);
        
        Graphics2D g2 = (Graphics2D) gfx;
        
        int w = h - 1;

        g2.setColor(Color.LIGHT_GRAY);//(ta.getDefaults().bracketHighlightColor);
        g2.fillRoundRect(x, y, w, w, 5, 5);

        // draw black outline
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(x, y, w, w, 5, 5);

//        if (ilegalColor) {
        g2.setColor(Color.RED);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(x + 1, y + 1, x + w - 1, y + w - 1);
//        }
    }

    @Override
    public void updateTheme() {
        // TODO Auto-generated method stub

    }

}
