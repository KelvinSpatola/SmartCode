package kelvinspatola.mode.smartcode.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import kelvinspatola.mode.smartcode.LinePainter;
import kelvinspatola.mode.smartcode.SmartCodeEditor;
import kelvinspatola.mode.smartcode.SmartCodePreferences;
import kelvinspatola.mode.smartcode.SmartCodeTextArea;
import processing.mode.java.ASTUtils;
import processing.mode.java.PreprocService;
import processing.mode.java.PreprocSketch;
import processing.mode.java.SketchInterval;

public class CodeOccurrences implements CaretListener, LinePainter {
    private List<LineMarker> occurrences = new ArrayList<>();
    private SmartCodeEditor editor;
    private PreprocService pps;

    private String code;
    private String prevCandidate;
    private int prevLine = -1;
    
    private Color occurenceColor;

//    private float delta;

    // CONSTRUCTOR
    public CodeOccurrences(SmartCodeEditor editor, PreprocService pps) {
        this.editor = editor;
        this.pps = pps;
        updateTheme();
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        int caret = e.getDot();
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
            if (!candidate.equals(prevCandidate)) {
                pps.whenDoneBlocking(ps -> handleCollectOccurrences(ps, startOffset, stopOffset));
                prevCandidate = candidate;
            }
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
        if (name == null)
            return;

        // Find binding
        IBinding binding = ASTUtils.resolveBinding(name);
//        System.err.println("name: " + binding.getName() + " - type: " + getBindingTypeLabel(binding));
//
//        if (binding.getKind() == IBinding.METHOD) {
//            IMethodBinding method = (IMethodBinding) binding;
//            System.out.println("parent: " + method.getDeclaringClass().getName());
//            System.out.print("params: ");                
//            for(ITypeBinding type : method.getParameterTypes()) {
//                System.out.print(type.getName() + ", ");                
//            }
//            System.out.println("\nreturn: " + method.getReturnType().getName());
//        }

        List<SketchInterval> intervals = findAllOccurrences(root, binding.getKey()).stream().map(ps::mapJavaToSketch)
                .collect(Collectors.toList());

        for (SketchInterval si : intervals) {
            try {
                final Field tabIndexInterval = si.getClass().getDeclaredField("tabIndex");
                tabIndexInterval.setAccessible(true);
                final Field startTabOffsetInterval = si.getClass().getDeclaredField("startTabOffset");
                startTabOffsetInterval.setAccessible(true);
                final Field stopTabOffsetInterval = si.getClass().getDeclaredField("stopTabOffset");
                stopTabOffsetInterval.setAccessible(true);

                int tabIndex = (Integer) tabIndexInterval.get(si);
                int startOffset = (Integer) startTabOffsetInterval.get(si);
                int stopOffset = (Integer) stopTabOffsetInterval.get(si);
                int line = editor.getTextArea().getLineOfOffset(startOffset);
                occurrences.add(new Occurrence(tabIndex, line, startOffset, stopOffset, name.toString()));

            } catch (final ReflectiveOperationException e) {
                System.err.println(e);
            }
        }
    }

    static private List<SimpleName> findAllOccurrences(ASTNode root, String bindingKey) {
        List<SimpleName> occurrences = new ArrayList<>();
        root.getRoot().accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName name) {
                IBinding binding = ASTUtils.resolveBinding(name);
                if (binding != null && bindingKey.equals(binding.getKey())) {
                    occurrences.add(name);
                }
                return super.visit(name);
            }
        });
        return occurrences;
    }

    public static int findWordStart(String text, int pos) {
        for (int i = pos; i >= 0; i--) {
            if (!Character.isLetterOrDigit(text.charAt(i)))
                return i + 1;
        }
        return -1;
    }

    public static int findWordEnd(String text, int pos) {
        for (int i = pos; i < text.length(); i++) {
            if (!Character.isLetterOrDigit(text.charAt(i)))
                return i;
        }
        return -1;
    }

    private boolean isValidChar(int offset) {
        return Character.isLetterOrDigit(code.charAt(offset));
    }

    @Override
    public boolean canPaint(Graphics gfx, int line, int y, int h, SmartCodeTextArea ta) {
        if (occurrences.isEmpty() || !SmartCodeTheme.OCCURRENCES_HIGHLIGHT)
            return false;

        int currentTab = editor.getSketch().getCurrentCodeIndex();
        for (LineMarker occurrence : occurrences) {
            if (occurrence.getTabIndex() == currentTab && occurrence.getLine() == line) {
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
    }

    class Occurrence implements LineMarker {
        int tab, line, startOffset, stopOffset;
        String text;

        Occurrence(int tab, int line, int startOffset, int stopOffset, String text) {
            this.tab = tab;
            this.line = line;
            this.startOffset = startOffset;
            this.stopOffset = stopOffset;
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
            return text;
        }

        public Class<?> getParent() {
            return this.getClass();
        }
    }

//    static String getBindingTypeLabel(IBinding binding) {
//        switch (binding.getKind()) {
//        case IBinding.METHOD:
//            if (((IMethodBinding) binding).isConstructor())
//                return "Constructor";
//            return "Method";
//        case IBinding.TYPE:
//            return "Type";
//        case IBinding.VARIABLE:
//            IVariableBinding variable = (IVariableBinding) binding;
//            if (variable.isField())
//                return "Field";
//            else if (variable.isParameter())
//                return "Parameter";
//            else if (variable.isEnumConstant())
//                return "Enum constant";
//            else
//                return "Local variable";
//        }
//        return "none";
//    }
}
