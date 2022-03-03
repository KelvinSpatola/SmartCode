package kelvinspatola.mode.smartcode.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import kelvinspatola.mode.smartcode.SmartCodePreferences;
import processing.app.Problem;
import processing.app.syntax.PdeTextArea;
import processing.app.ui.Editor;
import processing.app.ui.MarkerColumn;
import processing.app.ui.Theme;
import processing.core.PApplet;

public class SmartCodeMarkerColumn extends MarkerColumn {
    private List<LineMarker> errors = new ArrayList<>();
    private List<LineMarker> bookmarks = new ArrayList<>();
    private Color errorColor;
    private Color warningColor;
    private Color bookmarkColor;
    private Color occurrenceColor;

    private final int lineHeight;

    public SmartCodeMarkerColumn(Editor editor, int height, List<LineMarker> bookmarks) {
        super(editor, height);
        this.bookmarks = bookmarks;

        updateTheme();
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
    protected void updateTheme() {
//        errorColor = Theme.getColor("editor.column.error.color");
//        warningColor = Theme.getColor("editor.column.warning.color");
//        bookmarkColor = new Color(255, 255, 0);
        bookmarkColor = SmartCodePreferences.COLUMN_BOOKMARK_COLOR;
        errorColor = SmartCodePreferences.COLUMN_ERROR_COLOR;
        occurrenceColor = SmartCodePreferences.COLUMN_OCCURRENCE_COLOR;
        warningColor = SmartCodePreferences.COLUMN_WARNING_COLOR;
    }
    
    @Override
    public void updateErrorPoints(final List<Problem> problems) {
        errors = problems.stream().map(ErrorsAndWarnings::new).collect(Collectors.toList());
        repaint();
    }
    
    @Override
    public void paintComponent(Graphics g) {
        PdeTextArea pta = editor.getPdeTextArea();
        if (pta != null) {
            g.drawImage(pta.getGutterGradient(), 0, 0, getWidth(), getHeight(), this);
        }
        
        if (editor.isDebuggerEnabled())
            return;

        int currentTabIndex = editor.getSketch().getCurrentCodeIndex();

        for (LineMarker lm : errors) {
            if (currentTabIndex == lm.getTabIndex()) {
                if (((ErrorsAndWarnings) lm).problem.isError()) {
                    g.setColor(errorColor);
                } else {
                    g.setColor(warningColor);
                }
                int y = lineToY(lm.getLine() + 1);
                g.fillRect(1, y, getWidth() - 2, 2);
//                g.drawRect(2, y, getWidth() - 4, 2);
            }
        }

        for (LineMarker lm : bookmarks) {
            if (currentTabIndex == lm.getTabIndex()) {
                int y = lineToY(lm.getLine() + 1);
                g.setColor(bookmarkColor);
                g.drawRect(1, y, getWidth() - 2, 2);
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

    private LineMarker findClosestMarker(final int mouseY) {
        List<LineMarker> candidates = new ArrayList<>();
        int currentTabIndex = editor.getSketch().getCurrentCodeIndex();

        for (LineMarker lm : errors) {
            if (lm.getTabIndex() == currentTabIndex) {
                candidates.add(lm);
            }
        }

        for (LineMarker lm : bookmarks) {
            if (lm.getTabIndex() == currentTabIndex) {
                candidates.add(lm);
            }
        }

        LineMarker closest = null;
        int closestDist = Integer.MAX_VALUE;

        for (LineMarker lm : candidates) {
            int y = lineToY(lm.getLine() + 1);

            int dist = Math.abs(mouseY - y);
            if (dist < 3 && dist < closestDist) {
                closest = lm;
                closestDist = dist;
            }
        }
        return closest;
    }

    /** Find out which line marker the user has clicked and scroll to it */
    private void scrollToMarkerAt(final int mouseY) {
        try {
            LineMarker lm = findClosestMarker(mouseY);
            if (lm != null) {
                int tab = lm.getTabIndex();
                int start = lm.getStartOffset();
                int stop = lm.getStopOffset();
                editor.highlight(tab, start, stop);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Show tooltip on hover. */
    private void showMarkerHover(final int mouseY) {
        try {
            LineMarker lm = findClosestMarker(mouseY);
            if (lm != null) {
                editor.statusToolTip(this, lm.getText(), false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class ErrorsAndWarnings implements LineMarker {
        final Problem problem;

        public ErrorsAndWarnings(Problem problem) {
            this.problem = problem;
        }

        public int getTabIndex() {
            return problem.getTabIndex();
        }

        public int getLine() {
            return problem.getLineNumber();
        }

        public int getStartOffset() {
            return problem.getStartOffset();
        }

        public int getStopOffset() {
            return problem.getStopOffset();
        }

        public String getText() {
            return problem.getMessage();
        }
    }
    
}