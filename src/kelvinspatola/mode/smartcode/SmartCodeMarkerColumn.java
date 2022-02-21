package kelvinspatola.mode.smartcode;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import kelvinspatola.mode.smartcode.ui.LineBookmark;
import processing.app.ui.Editor;
import processing.app.ui.MarkerColumn;
import processing.core.PApplet;

public class SmartCodeMarkerColumn extends MarkerColumn {
    private List<LineBookmark> bookmarkedLines = new ArrayList<>();
    private Color bookmarkColor = new Color(255, 255, 0);
    private final int lineHeight; 

    public SmartCodeMarkerColumn(Editor editor, int height, List<LineBookmark> bookmarkedLines) {
        super(editor, height);
        this.bookmarkedLines = bookmarkedLines;
        
        lineHeight = editor.getTextArea().getPainter().getFontMetrics().getHeight();
        
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                scrollToMarkerAt(e.getY());
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(final MouseEvent e) {
                showMarkerHover(e.getY());
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (LineBookmark bm : bookmarkedLines) {
            if (editor.getSketch().getCurrentCodeIndex() == bm.getTab()) {
                int y = lineToY(bm.getLine() + 1);
                g.setColor(bookmarkColor);
//                g.drawLine(2, y, getWidth() - 2, y);
                g.drawRect(2, y, getWidth() - 4, 2);
            }
        }
    }
    
    private int lineToY(int line) {  
        int top = lineHeight / 2, bottom = 0;
        int lineCount = editor.getLineCount();
        int visibleLines = editor.getTextArea().getVisibleLines();
        
        if (lineCount > visibleLines) {
            bottom = getHeight() - lineHeight - top;
        } else {
            bottom = (lineCount * lineHeight) - top;
        }
        return (int) PApplet.map(line, 1, lineCount, top, bottom);
    }

    private LineBookmark findClosestMarker(final int mouseY) {
        LineBookmark closest = null;
        int closestDist = Integer.MAX_VALUE;
        
        for (LineBookmark bm : bookmarkedLines) {
            if (editor.getSketch().getCurrentCodeIndex() != bm.getTab())
                continue;

            int y = lineToY(bm.getLine() + 1);

            int dist = Math.abs(mouseY - y);
            if (dist < 3 && dist < closestDist) {
                closest = bm;
                closestDist = dist;
            }
        }
        return closest;
    }

    /** Find out which error/warning the user has clicked and scroll to it */
    private void scrollToMarkerAt(final int mouseY) {
        try {
            LineBookmark bm = findClosestMarker(mouseY);
            if (bm != null) {
                int caretLine = editor.getTextArea().getCaretLine();
                int tab = bm.getTab();
                int line = bm.getLine();
                int start = editor.getLineStartOffset(line);
                int end = editor.getLineStopOffset(line) - 1;
                editor.highlight(tab, start, end);
                
                int lineCount = editor.getLineCount();
                int visibleLines = editor.getTextArea().getVisibleLines();
                int destination;
                
                if (line < caretLine) {
                    destination = line - (visibleLines / 2);
                    if (destination <= 0)
                        destination = 0;
                } else {
                    destination = line + (visibleLines / 2);                    
                    if (destination >= lineCount)
                        destination = lineCount - 1;
                }
                editor.getTextArea().scrollTo(destination, 0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Show tooltip on hover. */
    private void showMarkerHover(final int mouseY) {
        try {
            LineBookmark bm = findClosestMarker(mouseY);
            if (bm != null) {
                editor.statusToolTip(this, bm.getText(), false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
