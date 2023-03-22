package kelvinspatola.mode.smartcode.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import kelvinspatola.mode.smartcode.SmartCodeEditor;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;
import processing.app.Messages;
import processing.mode.java.ASTUtils;
import processing.mode.java.PreprocService;
import processing.mode.java.PreprocSketch;

public class CodeOccurrences implements CaretListener, LinePainter {
    private Map<Integer, List<LineMarker>> occurrences = new HashMap<>();
    private SmartCodeEditor editor;
    private PreprocService pps;

    private String code;
    private boolean isStopped;

    private Color occurenceColor;
    private Color columnColor;

    private boolean firstReading = true;
    private Method javaOffsetToPdeOffsetMethod;

    
    // CONSTRUCTOR
    public CodeOccurrences(SmartCodeEditor editor, PreprocService pps) {
        this.editor = editor;
        this.pps = pps;
        updateTheme();
        updateAST();
    }

    
    @Override
    public void caretUpdate(CaretEvent e) {
        searchCandidatesAt(e.getDot());
    }
    
    
    public void updateAST() {
        if (isStopped || !SmartCodeTheme.OCCURRENCES_HIGHLIGHT) 
            return;
        
        code = editor.getText();
        searchCandidatesAt(editor.getCaretOffset());
    }

    
    private void searchCandidatesAt(int caret) {
        // In case we 'touch' a word on the right side
        if (caret > 0 && caret < code.length() && !isValidChar(caret) && isValidChar(caret - 1)) {
            caret--;
        }

        if ((caret < code.length()) && isValidChar(caret)) {
            int startOffset = findWordStart(code, caret);
            int stopOffset = findWordEnd(code, caret);

            if (startOffset == -1 || stopOffset == -1)
                return;

            pps.whenDoneBlocking(ps -> handleCollectOccurrences(ps, startOffset, stopOffset));
            
        } else {
            occurrences.clear();
        }
        
        editor.updateColumnPoints(toList(occurrences), Occurrence.class);
    }

    
    private void handleCollectOccurrences(PreprocSketch ps, int startTabOffset, int stopTabOffset) {
        occurrences.clear();

        // Map offsets
        int tab = editor.getSketch().getCurrentCodeIndex();
        int startJavaOffset = ps.tabOffsetToJavaOffset(tab, startTabOffset);
        int stopJavaOffset = ps.tabOffsetToJavaOffset(tab, stopTabOffset);

        CompilationUnit root = ps.compilationUnit;
        
        SimpleName candidate = ASTUtils.getSimpleNameAt(root, startJavaOffset, stopJavaOffset);
        if (candidate == null) 
            return;

        occurrences = findAllOccurrences(root, candidate).stream()
                .map(node -> mapToOccurrence(ps, node))
                .filter(o -> o.getTabIndex() == tab) // get only the ones in the current tab
                .collect(Collectors.groupingBy(o -> o.getLine()));        
    }

    
    static private List<SimpleName> findAllOccurrences(ASTNode root, SimpleName candidate) {
        List<SimpleName> result = new ArrayList<>();  

        // Find binding
        final IBinding candidateBinding = candidate.resolveBinding();
        if (candidateBinding == null) return result;

        root.getRoot().accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName match) {
                IBinding matchBinding = match.resolveBinding();
                if (matchBinding == null)
                    return false;
                
                if (candidateBinding.getName().equals(matchBinding.getName())) {

                    if (matchBinding.isEqualTo(candidateBinding)) {
                        result.add(match);
                    
                    } else if (candidateBinding.getKind() == IBinding.METHOD) {
                        IMethodBinding meth = (IMethodBinding) candidateBinding;

                        if (meth.isConstructor()) {
                            ITypeBinding declaringClass = meth.getDeclaringClass();
                                                        
                            if (matchBinding.isEqualTo(declaringClass)) {
                                result.add(match);
                            } 
                        }
                    } else if (candidateBinding.getKind() == IBinding.TYPE && matchBinding.getKind() == IBinding.METHOD) {
                        IMethodBinding meth = (IMethodBinding) matchBinding;

                        if (meth.isConstructor()) {
                            if (candidateBinding.isEqualTo(meth.getDeclaringClass())) {
                                result.add(match);
                            }
                        }
                    }
                }
                return super.visit(match);
            }
        });
        return result;
    }

   
    private Occurrence mapToOccurrence(PreprocSketch ps, ASTNode node) {
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
            return new Occurrence(-1, -1, -1, node.toString());
        }

        int tabIndex = ps.pdeOffsetToTabIndex(startPdeOffset);

        if (startPdeOffset >= ps.pdeCode.length()) {
            startPdeOffset = ps.pdeCode.length() - 1;
            stopPdeOffset = startPdeOffset + 1;
        }

        return new Occurrence(tabIndex, ps.pdeOffsetToTabOffset(tabIndex, startPdeOffset),
                ps.pdeOffsetToTabOffset(tabIndex, stopPdeOffset), node.toString());
    }

    
    public void stopTracking() {
        editor.getTextArea().removeCaretListener(this);
        occurrences.clear();
        editor.updateColumnPoints(new ArrayList<LineMarker>(), Occurrence.class);
        isStopped = true;
        Messages.log("CodeOccurrences: CaretListener removed");
    }
    

    public void startTracking() {
        editor.getTextArea().addCaretListenerIfAbsent(this);
        searchCandidatesAt(editor.getCaretOffset());
        editor.updateColumnPoints(toList(occurrences), Occurrence.class);
        isStopped = false;
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

    
    static private List<LineMarker> toList(Map<Integer, List<LineMarker>> map) {
        return map.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }
 

    @Override
    public void paintLine(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta) {
        if (occurrences.isEmpty() || !occurrences.containsKey(line) || !SmartCodeTheme.OCCURRENCES_HIGHLIGHT)
            return;

        List<LineMarker> occurrencesInThisLine = occurrences.get(line);
        if (occurrencesInThisLine == null)
            return;

        occurrencesInThisLine.forEach(o -> {
            if (!(ta.isSelectionActive() && ta.getCaretLine() == line)) {
                int lineStart = ta.getLineStartOffset(line);
                int wordStart = o.getStartOffset() - lineStart;
                int wordEnd = o.getStopOffset() - lineStart;
                int x = ta._offsetToX(line, wordStart);
                int w = ta._offsetToX(line, wordEnd) - x;

                gfx.setColor(occurenceColor);
                gfx.fillRect(x, y, w, h);
            }
        });
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
