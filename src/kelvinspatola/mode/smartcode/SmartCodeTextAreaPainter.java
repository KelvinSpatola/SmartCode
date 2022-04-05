package kelvinspatola.mode.smartcode;

import static kelvinspatola.mode.smartcode.Constants.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Segment;
import javax.swing.text.Utilities;

import kelvinspatola.mode.smartcode.ui.SmartCodeTheme;
import processing.app.Preferences;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.app.ui.Editor;
import processing.mode.java.JavaTextAreaPainter;

public class SmartCodeTextAreaPainter extends JavaTextAreaPainter {
    protected List<LinePainter> painters = new ArrayList<>();
    static private int presetFontSize = Preferences.getInteger("editor.font.size");
    protected Color bookmarkIconColor;

    public SmartCodeTextAreaPainter(SmartCodeTextArea textarea, TextAreaDefaults defaults) {
        super(textarea, defaults);

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
                for (LinePainter painter : painters) {
                    if (painter.canPaint(gfx, line, y + getLineDisplacement(), fontMetrics.getHeight(), textarea)) {
                        //
                    } else {
                        repaint();
                    }
                }
            }
        };
    }

    @Override
    protected void updateTheme() {
        super.updateTheme();
        bookmarkIconColor = SmartCodeTheme.getColor("bookmarks.icon.color");

        if (painters != null) {
            for (LinePainter painter : painters) {
                painter.updateTheme();
            }
        }
    }

    public SmartCodeEditor getSmartCodeEditor() {
        return (SmartCodeEditor) getEditor();
    }

    public void addLinePainter(LinePainter painter) {
        painters.add(painter);
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

    /**
     * Gets the current value of the font size being rendered by the editor. Note
     * that this may not match the value defined in the preferences file.
     * 
     * @return the editor's current font size.
     */
    public int getFontSize() {
        return getFontMetrics().getFont().getSize();
    }

    /**
     * Changes the editor font size without saving the changes to the preferences
     * file.
     * <p>
     * Internally, this method actually writes the value in the preferences file,
     * calls <code>updateTheme()</code> in order to update the editor's rendering,
     * and finally rewrites the original value back to the preferences file without
     * updating the editor.
     * 
     * @param size the font size. The minimum value that can be assigned is 8.
     */
    public void setFontSize(int size) {
        size = Math.max(8, size);
        Preferences.set("editor.font.size", String.valueOf(size));
        updateTheme();
        getSmartCodeEditor().getMarkerColumn().updateTheme();
        // Since the font size has already been changed, let's write the default value
        // back to the preferences file. This way we prevent font size changes
        // from passing to other editors when opening them. We also prevent these
        // changes from being "saved" in the preferences file. The actual
        // changes are supposed to be made in the preferences' window.
        Preferences.set("editor.font.size", String.valueOf(presetFontSize));

        getSmartCodeEditor().timedStatusNotice("Font size: " + size, 1500);
    }

    /**
     * Used whenever the user defines a new font size in the preferences window. It
     * should be called inside <code>Editor.applyPreferences()</code> so it can be
     * updated on every new change.
     */
    static public void updateDefaultFontSize() {
        presetFontSize = Preferences.getInteger("editor.font.size");
    }
}
