package kelvinspatola.mode.smartcode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Segment;
import javax.swing.text.Utilities;

import kelvinspatola.mode.smartcode.completion.Snippet;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.app.ui.Editor;
import processing.mode.java.JavaTextArea;
import processing.mode.java.JavaTextAreaPainter;

public class SmartCodeTextAreaPainter extends JavaTextAreaPainter {
    protected List<Painter> painters = new ArrayList<>();

    public SmartCodeTextAreaPainter(JavaTextArea textarea, TextAreaDefaults defaults) {
        super(textarea, defaults);

        painters.add(new PinnedLines());
        painters.add(new Occurrences());
        painters.add(new SnippetMarker());

        highlights = new Highlight() {
            @Override
            public String getToolTipText(MouseEvent evt) {
                return null;
            }

            @Override
            public void init(JEditTextArea textarea, Highlight next) {
            }

            @Override
            public void paintHighlight(Graphics gfx, int line, int y) {
                for (Painter painter : painters) {
                    if (painter.canPaint(gfx, line, y)) {
                        //
                    } else {
                        repaint();
                    }
                }
            }
        };

        // Handle mouse clicks to toggle line pins
        addMouseListener(new MouseAdapter() {
            long lastTime; // OS X seems to be firing multiple mouse events

            public void mousePressed(MouseEvent event) {
                long thisTime = event.getWhen();

                if (thisTime - lastTime > 100) {
                    if (event.getX() < Editor.LEFT_GUTTER) {
                        int offset = textArea.xyToOffset(event.getX(), event.getY());

                        if (offset >= 0) {
                            int lineIndex = textArea.getLineOfOffset(offset);
                            getSmartCodeEditor().toggleLinePin(lineIndex);
                        }
                    }
                    lastTime = thisTime;
                }
            }
        });
    }

    public SmartCodeEditor getSmartCodeEditor() {
        return (SmartCodeEditor) getEditor();
    }

    int x1, x2, line;
    boolean isReading;

    public void hightlight(Snippet snippet) {
        if (isReading = snippet != null) {
            x1 = ((SmartCodeTextArea) textArea).getPositionInsideLineWithOffset(snippet.leftBoundary);
            x2 = ((SmartCodeTextArea) textArea).getPositionInsideLineWithOffset(snippet.rightBoundary - 1);
        }
    }

    abstract static class Painter {
        abstract boolean canPaint(Graphics gfx, int line, int y);
    }

    class Occurrences extends Painter {
        @Override
        public boolean canPaint(Graphics gfx, int line, int y) {
            if (textArea.isSelectionActive()) {
                String selection = textArea.getSelectedText();

                String lineText = textArea.getLineText(line);
                boolean hasText = lineText.contains(selection);

                if (hasText) {
                    int x1 = lineText.indexOf(selection);
                    int x2 = x1 + selection.length();

                    int x = textArea._offsetToX(line, x1);
                    y += getLineDisplacement();
                    int w = textArea._offsetToX(line, x2) - x;
                    int h = fontMetrics.getHeight();

                    gfx.setColor(defaults.lineHighlightColor);
                    gfx.fillRect(x, y, w, h);
                }
                return true;
            }
            return false;
        }
    }

    class SnippetMarker extends Painter {
        @Override
        public boolean canPaint(Graphics gfx, int line, int y) {
            if (isReading && line == textArea.getCaretLine()) {
                int x = textArea._offsetToX(line, x1);
                y += getLineDisplacement();
                int w = textArea._offsetToX(line, x2) - x;
                int h = fontMetrics.getHeight();

                gfx.setColor(defaults.eolMarkerColor);
                gfx.drawRect(x, y, w, h);

                return true;
            }
            return false;
        }
    }

    class PinnedLines extends Painter {
        @Override
        public boolean canPaint(Graphics gfx, int line, int y) {

            if (!getEditor().isDebuggerEnabled() && getSmartCodeEditor().isLinePinned(line)) {
                y += getLineDisplacement();
                int h = fontMetrics.getHeight();

                gfx.setColor(new Color(255, 130, 210));
                gfx.fillRect(0, y, getWidth(), h);

                return true;
            }
            return false;
        }
    }

    @Override
    protected void paintLeftGutter(Graphics gfx, int line, int x) {
        int y = textArea.lineToY(line) + getLineDisplacement();
        
        if (line == textArea.getSelectionStopLine()) {
            gfx.setColor(gutterLineHighlightColor);
            gfx.fillRect(0, y, Editor.LEFT_GUTTER, fontMetrics.getHeight());
        } else {
            Rectangle clip = gfx.getClipBounds();
            gfx.setClip(0, y, Editor.LEFT_GUTTER, fontMetrics.getHeight());
            gfx.drawImage(((PdeTextArea) textArea).getGutterGradient(), 0, 0, getWidth(), getHeight(), this);
            gfx.setClip(clip); // reset
        }

        String text = null;
        if (getEditor().isDebuggerEnabled()) {
            text = getPdeTextArea().getGutterText(line);
            
            if (text != null && text.equals(SmartCodeTextArea.PIN_MARKER)) {
                text = null;
            }
            
        } else if(getSmartCodeEditor().isLinePinned(line)) {
            text = SmartCodeTextArea.PIN_MARKER;
        }

        gfx.setColor(line < textArea.getLineCount() ? gutterTextColor : gutterPastColor);

        int textRight = Editor.LEFT_GUTTER - Editor.GUTTER_MARGIN;
        int textBaseline = textArea.lineToY(line) + fontMetrics.getHeight();
        

        if (text != null) {
            if (text.equals(PdeTextArea.BREAK_MARKER)) {
                drawDiamond(gfx, textRight - 8, textBaseline - 8, 8, 8);

            } else if (text.equals(PdeTextArea.STEP_MARKER)) {
                drawRightArrow(gfx, textRight - 7, textBaseline - 7.5f, 7, 7);

            } else if (text.equals(SmartCodeTextArea.PIN_MARKER)) {
//                gfx.fillOval(textRight - 10, textBaseline - 10, 8, 8);
                
                char[] txt = text.toCharArray();
                int tx = textRight - gfx.getFontMetrics().charsWidth(txt, 0, txt.length);
                
                gfx.setFont(gutterTextFont);
                Utilities.drawTabbedText(new Segment(txt, 0, text.length()), (float) tx, (float) textBaseline,
                        (Graphics2D) gfx, this, 0);
            }
        } else {
            // if no special text for a breakpoint, just show the line number
            text = String.valueOf(line + 1);

            gfx.setFont(gutterTextFont);
            // Right-align the text
            char[] txt = text.toCharArray();
            int tx = textRight - gfx.getFontMetrics().charsWidth(txt, 0, txt.length);
            // Using 'fm' here because it's relative to the editor text size,
            // not the numbers in the gutter
            Utilities.drawTabbedText(new Segment(txt, 0, text.length()), (float) tx, (float) textBaseline,
                    (Graphics2D) gfx, this, 0);
        }
    }

    static private void drawDiamond(Graphics g, float x, float y, float w, float h) {
        Graphics2D g2 = (Graphics2D) g;
        GeneralPath path = new GeneralPath();
        path.moveTo(x + w / 2, y);
        path.lineTo(x + w, y + h / 2);
        path.lineTo(x + w / 2, y + h);
        path.lineTo(x, y + h / 2);
        path.closePath();
        g2.fill(path);
    }

    static private void drawRightArrow(Graphics g, float x, float y, float w, float h) {
        Graphics2D g2 = (Graphics2D) g;
        GeneralPath path = new GeneralPath();
        path.moveTo(x, y);
        path.lineTo(x + w, y + h / 2);
        path.lineTo(x, y + h);
        path.closePath();
        g2.fill(path);
    }
}
