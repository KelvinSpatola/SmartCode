package kelvinspatola.mode.smartcode.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import kelvinspatola.mode.smartcode.LinePainter;
import kelvinspatola.mode.smartcode.SmartCodeEditor;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;
import processing.app.Messages;
import processing.mode.java.ASTUtils;
import processing.mode.java.PreprocService;
import processing.mode.java.PreprocSketch;

public class CodeOccurrences implements CaretListener, LinePainter {
    private List<LineMarker> occurrences = new ArrayList<>();
    private SmartCodeEditor editor;
    private PreprocService pps;

    private String code;
    private String prevCandidate;
    private int prevLine = -1;

    private Color occurenceColor;
    private Color columnColor;

    private boolean firstReading = true;
    private Method javaOffsetToPdeOffsetMethod;

    // CONSTRUCTOR
    public CodeOccurrences(SmartCodeEditor editor, PreprocService pps) {
        this.editor = editor;
        this.pps = pps;
        updateTheme();
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        searchCandidatesAt(e.getDot());
    }

    private void searchCandidatesAt(int caret) {
        final int line = editor.getTextArea().getCaretLine();

        // update 'code' only if we move the caret from one line to another
        if (line != prevLine) {
            code = editor.getText();
            prevLine = line;
        }
        // In case we 'touch' a word on the right side
        if (caret > 0 && caret < code.length() && !isValidChar(caret) && isValidChar(caret - 1)) {
            caret--;
        }

        if ((caret < code.length()) && isValidChar(caret)) {
            int startOffset = findWordStart(code, caret);
            int stopOffset = findWordEnd(code, caret);

            if (startOffset == -1 || stopOffset == -1)
                return;

            String candidate = code.substring(startOffset, stopOffset);
//            if (!candidate.equals(prevCandidate)) {
                pps.whenDoneBlocking(ps -> handleCollectOccurrences(ps, startOffset, stopOffset));
                prevCandidate = candidate;
//            }
        } else {
            occurrences.clear();
            prevCandidate = null;
        }
        editor.updateColumnPoints(occurrences, Occurrence.class);
    }

    private void handleCollectOccurrences(PreprocSketch ps, int startTabOffset, int stopTabOffset) {
        occurrences.clear();

        // Map offsets
        int tab = editor.getSketch().getCurrentCodeIndex();
        int startJavaOffset = ps.tabOffsetToJavaOffset(tab, startTabOffset);
        int stopJavaOffset = ps.tabOffsetToJavaOffset(tab, stopTabOffset);

        CompilationUnit root = ps.compilationUnit;
        SimpleName name = ASTUtils.getSimpleNameAt(root, startJavaOffset, stopJavaOffset);
        if (name == null) return;

        occurrences = findAllOccurrences(root, name).stream()
                .map(node -> mapToOccurrence(ps, node, name))
                .collect(Collectors.toList());
    }

    static private List<SimpleName> findAllOccurrences(ASTNode root, SimpleName cantidate) {
        List<SimpleName> result = new ArrayList<>();  

        // Find binding
        final IBinding cadidateBinding = ASTUtils.resolveBinding(cantidate);
        if (cadidateBinding == null) return result;
        
        final String cadidateKey = cadidateBinding.getKey();
        final String cadidatePkg = cadidateKey.substring(1, cadidateKey.indexOf(';'));
        final String candidateName = cadidateBinding.getName();

        
        root.getRoot().accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName match) {
                IBinding matchBinding = ASTUtils.resolveBinding(match);
                final String matchName = matchBinding.getName();

                if (matchBinding != null && matchName.equals(candidateName)) {
                    final String key = matchBinding.getKey();
                    final String matchPkg = key.substring(1, key.indexOf(';'));

                    if (matchPkg.equals(cadidatePkg)) {
                        result.add(match);
                    }
                }
                return super.visit(match);
            }
        });
        return result;
    }

    private Occurrence mapToOccurrence(PreprocSketch ps, ASTNode node, SimpleName name) {
        int length = node.getLength();
        int startPdeOffset = 0, stopPdeOffset = 0;

        try {
            if (firstReading) {
                javaOffsetToPdeOffsetMethod = ps.getClass().getDeclaredMethod("javaOffsetToPdeOffset", int.class);
                javaOffsetToPdeOffsetMethod.setAccessible(true);
                firstReading = false;
            }

            startPdeOffset = (int) javaOffsetToPdeOffsetMethod.invoke(ps, node.getStartPosition());

            if (length == 0) {
                stopPdeOffset = startPdeOffset;
            } else {
                stopPdeOffset = (int) javaOffsetToPdeOffsetMethod.invoke(ps, node.getStartPosition() + length - 1);
                if (stopPdeOffset >= 0 && (stopPdeOffset > startPdeOffset || length == 1)) {
                    stopPdeOffset += 1;
                }
            }
        } catch (final ReflectiveOperationException e) {
            System.err.println(e);
        }

        if (startPdeOffset < 0 || stopPdeOffset < 0) {
            return new Occurrence(-1, -1, -1, name.toString());
        }

        int tabIndex = ps.pdeOffsetToTabIndex(startPdeOffset);

        if (startPdeOffset >= ps.pdeCode.length()) {
            startPdeOffset = ps.pdeCode.length() - 1;
            stopPdeOffset = startPdeOffset + 1;
        }

        return new Occurrence(tabIndex, ps.pdeOffsetToTabOffset(tabIndex, startPdeOffset),
                ps.pdeOffsetToTabOffset(tabIndex, stopPdeOffset), name.toString());
    }

    public void stopTracking() {
        editor.getTextArea().removeCaretListener(this);
        occurrences.clear();
        editor.updateColumnPoints(occurrences, Occurrence.class);
        prevCandidate = null;
        prevLine = -1;
        Messages.log("CodeOccurrences: CaretListener removed");
    }

    public void startTracking() {
        editor.getTextArea().addCaretListener(this);
        searchCandidatesAt(editor.getCaretOffset());
        editor.updateColumnPoints(occurrences, Occurrence.class);
        Messages.log("CodeOccurrences: CaretListener added");
    }

    private boolean isValidChar(int offset) {
        return Character.isLetterOrDigit(code.charAt(offset));
    }

    static private int findWordStart(String text, int pos) {
        for (int i = pos; i >= 0; i--) {
            if (!Character.isLetterOrDigit(text.charAt(i)))
                return i + 1;
        }
        return -1;
    }

    static private int findWordEnd(String text, int pos) {
        for (int i = pos; i < text.length(); i++) {
            if (!Character.isLetterOrDigit(text.charAt(i)))
                return i;
        }
        return -1;
    }

    @Override
    public boolean canPaint(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta) {
        if (occurrences.isEmpty() || !SmartCodeTheme.OCCURRENCES_HIGHLIGHT)
            return false;

        int currentTab = editor.getSketch().getCurrentCodeIndex();
        for (LineMarker occurrence : occurrences) {
            if (occurrence.getTabIndex() == currentTab && occurrence.getLine() == line) {

                if (editor.isSelectionActive() && ta.getCaretLine() == line)
                    return false;

                int lineStart = ta.getLineStartOffset(line);
                int wordStart = occurrence.getStartOffset() - lineStart;
                int wordEnd = occurrence.getStopOffset() - lineStart;
                int x = ta._offsetToX(line, wordStart);
                int w = ta._offsetToX(line, wordEnd) - x;

                gfx.setColor(occurenceColor);
                gfx.fillRect(x, y, w, h);
            }
        }
        return true;
    }

    @Override
    public void updateTheme() {
        occurenceColor = SmartCodeTheme.getColor("occurrences.highlight.color");
        columnColor = SmartCodeTheme.getColor("column.occurrence.color");
    }

    class Occurrence implements LineMarker {
        int tab, line, startOffset, stopOffset;
        String text;

        Occurrence(int tab, int startOffset, int stopOffset, String text) {
            this.tab = tab;
            this.startOffset = startOffset;
            this.stopOffset = stopOffset;
            this.line = editor.getTextArea().getLineOfOffset(startOffset);
            this.text = text;
        }

        public int getTabIndex() {
            return tab;
        }

        public int getLine() {
            return line;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getStopOffset() {
            return stopOffset;
        }

        public String getText() {
            String lineNumberIndicator = "<font color=#bbbbbb>" + (line + 1) + ": </font>";
            String lineTextIndicator = "<font color=#000000>" + text + "</font>";
            return "<html>" + lineNumberIndicator + lineTextIndicator + "</html>";
        }

        public Class<?> getParent() {
            return this.getClass();
        }

        @Override
        public void paintMarker(Graphics gfx, int x, int y, int w, int h) {
            gfx.setColor(columnColor);
            gfx.fillRect(x, y, w, h);
        }
    }
}
