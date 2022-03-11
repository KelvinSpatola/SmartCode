package kelvinspatola.mode.smartcode;

import static kelvinspatola.mode.smartcode.Constants.*;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Segment;
import javax.swing.text.Utilities;

import kelvinspatola.mode.smartcode.completion.Snippet;
import kelvinspatola.mode.smartcode.ui.LineMarker;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.app.ui.Editor;
import processing.app.ui.Theme;
import processing.mode.java.JavaTextArea;
import processing.mode.java.JavaTextAreaPainter;

public class SmartCodeTextAreaPainter extends JavaTextAreaPainter {
    protected List<Painter> painters = new ArrayList<>();
    protected Color occurrencesColor = SmartCodePreferences.OCCURRENCES_HIGHLIGHT_COLOR;
    protected Color bookmarkLineColor = SmartCodePreferences.BOOKMARKS_HIGHLIGHT_COLOR;
    protected Color bookmarkIconColor;

    public SmartCodeTextAreaPainter(JavaTextArea textarea, TextAreaDefaults defaults) {
        super(textarea, defaults);

        painters.add(new Bookmarks());
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

        // Handle mouse clicks to toggle line bookmarks
        addMouseListener(new MouseAdapter() {
            long lastTime; // OS X seems to be firing multiple mouse events

            public void mousePressed(MouseEvent e) {
                long thisTime = e.getWhen();

                if (thisTime - lastTime > 100) {
                    int lineIndex = yToLine(e.getY());
                    int lastLine = textArea.getLineCount();

                    if (e.getX() < Editor.LEFT_GUTTER && lineIndex < lastLine) {
                        getSmartCodeEditor().toggleLineBookmark(lineIndex);
                    }
                    lastTime = thisTime;
                }
            }
        });
    }

    @Override
    protected void updateTheme() {
        super.updateTheme();
        bookmarkIconColor = Theme.getColor("footer.icon.selected.color");
    }

    public SmartCodeEditor getSmartCodeEditor() {
        return (SmartCodeEditor) getEditor();
    }

    /**
     * Converts a y co-ordinate to a line index. Rewriting this because i need it to
     * not clamp the returned value as the original JEditTextArea.yToLine() method
     * does.
     * 
     * @param y The y co-ordinate
     */
    public int yToLine(int y) {
        FontMetrics fm = getFontMetrics();
        return Math.max(0, (y / fm.getHeight() + textArea.getFirstLine()));
    }

    int x1, x2, line;
    boolean isReading;

    public void hightlight(Snippet snippet) {
        if (isReading = snippet != null) {
            x1 = ((SmartCodeTextArea) textArea).getPositionInsideLineWithOffset(snippet.leftBoundary);
            x2 = ((SmartCodeTextArea) textArea).getPositionInsideLineWithOffset(snippet.rightBoundary - 1);
        }
    }

    interface Painter {
        boolean canPaint(Graphics gfx, int line, int y);
    }

    public static float delta;

    class Occurrences implements Painter {
        @Override
        public boolean canPaint(Graphics gfx, int line, int y) {
            if (SmartCodePreferences.OCCURRENCES_HIGHLIGHT && getSmartCodeEditor().hasOccurrences()) {
                delta++;
                if (delta < 10000)
                    if (delta < 10000)
                    return false;

                int currentTabIndex = getSmartCodeEditor().getSketch().getCurrentCodeIndex();
                for (LineMarker occurrence : getSmartCodeEditor().getOccurrences()) {
                    if (occurrence.getTabIndex() == currentTabIndex && occurrence.getLine() == line) {
                        int lineStartOffset = textArea.getLineStartOffset(line);
                        int wordStart = occurrence.getStartOffset() - lineStartOffset;
                        int wordEnd = occurrence.getStopOffset() - lineStartOffset;
                        int x = textArea._offsetToX(line, wordStart);
                        int w = textArea._offsetToX(line, wordEnd) - x;
                        int h = fontMetrics.getHeight();

                        gfx.setColor(new Color(190, 220, 255));
                        gfx.fillRect(x, y + getLineDisplacement(), w, h);
                    }
                }
                return true;
            }
            delta = 0;
            return false;
        }
    }

    class SnippetMarker implements Painter {
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

    class Bookmarks implements Painter {
        @Override
        public boolean canPaint(Graphics gfx, int line, int y) {
            if (SmartCodePreferences.BOOKMARKS_HIGHLIGHT && !getEditor().isDebuggerEnabled()
                    && getSmartCodeEditor().isLineBookmark(line)) {
                y += getLineDisplacement();
                gfx.setColor(bookmarkLineColor);
                gfx.fillRect(0, y, getWidth(), fontMetrics.getHeight());

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

            if (text != null && text.equals(PIN_MARKER)) {
                text = null;
            }

        } else if (getSmartCodeEditor().isLineBookmark(line)) {
            text = PIN_MARKER;
        }

        gfx.setColor(line < textArea.getLineCount() ? gutterTextColor : gutterPastColor);

        int textRight = Editor.LEFT_GUTTER - Editor.GUTTER_MARGIN;
        int textBaseline = textArea.lineToY(line) + fontMetrics.getHeight();

        if (text != null) {
            if (text.equals(PdeTextArea.BREAK_MARKER)) {
                drawDiamond(gfx, textRight - 8, textBaseline - 8, 8, 8);

            } else if (text.equals(PdeTextArea.STEP_MARKER)) {
                drawRightArrow(gfx, textRight - 7, textBaseline - 7.5f, 7, 7);

            } else if (text.equals(PIN_MARKER)) {
                gfx.setColor(bookmarkIconColor);
                float w = 8f;
                float xx = Editor.LEFT_GUTTER - Editor.GUTTER_MARGIN - w;
                float h = w * 1.4f;
                float yy = y + (fontMetrics.getHeight() - h) / 2;
                drawBookmark(gfx, xx, yy, w, h);
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

    static private void drawBookmark(Graphics g, float x, float y, float w, float h) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GeneralPath path = new GeneralPath();
        path.moveTo(x, y);
        path.lineTo(x + w, y);
        path.lineTo(x + w, y + h);
        path.lineTo(x + w / 2, y + h * 0.65f);
        path.lineTo(x, y + h);
        path.closePath();
        g2.fill(path);
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
