package kelvinspatola.mode.smartcode.ui;

import static kelvinspatola.mode.smartcode.SmartCodePreferences.COLUMN_BOOKMARK_COLOR;
import static kelvinspatola.mode.smartcode.SmartCodePreferences.COLUMN_ERROR_COLOR;
import static kelvinspatola.mode.smartcode.SmartCodePreferences.COLUMN_OCCURRENCE_COLOR;
import static kelvinspatola.mode.smartcode.SmartCodePreferences.COLUMN_WARNING_COLOR;

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
import kelvinspatola.mode.smartcode.SmartCodePreferences;
import kelvinspatola.mode.smartcode.ui.CodeOccurrences.Occurrence;
import processing.app.Preferences;
import processing.app.Problem;
import processing.app.syntax.PdeTextArea;
import processing.app.ui.MarkerColumn;
import processing.core.PApplet;

public class SmartCodeMarkerColumn extends MarkerColumn {
    private List<LineMarker> allMarkers = new ArrayList<>();
    private int lineHeight;

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
        
        COLUMN_BOOKMARK_COLOR = Preferences.getColor(SmartCodePreferences.attributes[12]);
        COLUMN_ERROR_COLOR = Preferences.getColor(SmartCodePreferences.attributes[13]);
        COLUMN_OCCURRENCE_COLOR = Preferences.getColor(SmartCodePreferences.attributes[14]);
        COLUMN_WARNING_COLOR = Preferences.getColor(SmartCodePreferences.attributes[15]);
    }

    @Override
    public void updateErrorPoints(final List<Problem> problems) {
        List<LineMarker> errors = problems.stream().map(ErrorsAndWarnings::new).collect(Collectors.toList());
        updatePoints(errors, ErrorsAndWarnings.class);
    }

    public void updatePoints(List<LineMarker> points, Class<?> parent) {
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

                if (lm.getParent() == ErrorsAndWarnings.class) {
                    boolean isError = ((ErrorsAndWarnings) lm).problem.isError();
                    g.setColor(isError ? COLUMN_ERROR_COLOR : COLUMN_WARNING_COLOR);
                    g.fillRect(1, y, getWidth() - 2, 2);

                } else if (lm.getParent() == Occurrence.class) {
                    g.setColor(COLUMN_OCCURRENCE_COLOR);
                    g.fillRect(1, y, getWidth() - 2, 2);

                } else if (lm.getParent() == LineBookmark.class) {
                    g.setColor(COLUMN_BOOKMARK_COLOR);
                    g.drawRect(1, y, getWidth() - 2, 2);

                }

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
            return problem.getMessage();
        }
        
        public Class<?> getParent() {
            return this.getClass();
        }
    }
}