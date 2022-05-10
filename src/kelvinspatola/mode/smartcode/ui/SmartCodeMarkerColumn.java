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

import kelvinspatola.mode.smartcode.SmartCodeEditor;
import processing.app.Problem;
import processing.app.syntax.PdeTextArea;
import processing.app.ui.MarkerColumn;
import processing.core.PApplet;

public class SmartCodeMarkerColumn extends MarkerColumn {
    private List<LineMarker> allMarkers = new ArrayList<>();
    private int lineHeight;
    private Color errorColor, warningColor;

    public SmartCodeMarkerColumn(SmartCodeEditor editor) {
        super(editor, editor.getTextArea().getMinimumSize().height);

        updateTheme();

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
    public void updateTheme() {
        lineHeight = editor.getTextArea().getPainter().getFontMetrics().getHeight();
        errorColor = SmartCodeTheme.getColor("column.error.color");
        warningColor = SmartCodeTheme.getColor("column.warning.color");
    }

    @Override
    public void updateErrorPoints(final List<Problem> problems) {
        List<LineMarker> errors = problems.stream().map(ErrorsAndWarnings::new).collect(Collectors.toList());
        updatePoints(errors, ErrorsAndWarnings.class);
    }

    public void updatePoints(List<? extends LineMarker> points, Class<? extends LineMarker> parent) {
        if (points == null)
            return;

        for (int i = allMarkers.size() - 1; i >= 0; i--) {
            if (allMarkers.get(i).getParent() == parent) {
                allMarkers.remove(i);
            }
        }

        allMarkers.addAll(points);
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

        for (LineMarker lm : allMarkers) {
            if (currentTabIndex == lm.getTabIndex()) {
                int y = lineToY(lm.getLine() + 1);
                lm.paintMarker(g, 1, y, getWidth() - 2, 2);
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

        return (lineCount == 1) ? top : (int) PApplet.map(line, 1, lineCount, top, bottom);
    }

    private LineMarker findClosestMarker(final int mouseY) {
        LineMarker closest = null;
        int closestDist = Integer.MAX_VALUE;
        int currentTabIndex = editor.getSketch().getCurrentCodeIndex();

        for (LineMarker lm : allMarkers) {
            if (lm.getTabIndex() == currentTabIndex) {
                int y = lineToY(lm.getLine() + 1);

                int dist = Math.abs(mouseY - y);
                if (dist < 3 && dist < closestDist) {
                    closest = lm;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }

    /** Find out which line marker the user has clicked and scroll to it */
    private void scrollToMarkerAt(final int mouseY) {
        try {
            LineMarker lm = findClosestMarker(mouseY);
            if (lm != null) {
                ((SmartCodeEditor) editor).highlight(lm);
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
            String lineNumberIndicator = "<font color=#bbbbbb>" + (getLine() + 1) + ": </font>";
            String lineTextIndicator = "<font color=#000000>" + problem.getMessage() + "</font>";
            return "<html>" + lineNumberIndicator + lineTextIndicator + "</html>"; 
        }

        public Class<?> getParent() {
            return this.getClass();
        }

        @Override
        public void paintMarker(Graphics gfx, int x, int y, int w, int h) {
            gfx.setColor(problem.isError() ? errorColor : warningColor);
            gfx.fillRect(x, y, w, h);
        }
    }
}