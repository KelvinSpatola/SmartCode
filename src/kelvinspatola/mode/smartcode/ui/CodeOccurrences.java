package kelvinspatola.mode.smartcode.ui;

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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import kelvinspatola.mode.smartcode.SmartCodeEditor;
import processing.app.syntax.InputHandler;
import processing.app.ui.EditorStatus;
import processing.mode.java.ASTUtils;
import processing.mode.java.PreprocService;
import processing.mode.java.PreprocSketch;
import processing.mode.java.SketchInterval;

public class CodeOccurrences implements CaretListener {
    private SmartCodeEditor editor;
    private PreprocService pps;

    private IBinding binding;
    private PreprocSketch ps;

    private int prevLine = -1;
    private String code;
    private String prevWord;

    private List<LineMarker> occurrences = new ArrayList<>();

    // CONSTRUCTOR
    public CodeOccurrences(SmartCodeEditor editor, PreprocService pps) {
        this.editor = editor;
        this.pps = pps;
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        final int caret = e.getDot();
        final int line = editor.getTextArea().getCaretLine();

        // update 'code' only if we move the caret from one line to the other
        if (line != prevLine) {
            code = editor.getText();
            prevLine = line;
        }

        if ((caret < code.length()) && Character.isLetterOrDigit(code.charAt(caret))) {
            int tabIndex = editor.getSketch().getCurrentCodeIndex();
            int startOffset = InputHandler.findWordStart(code, caret + 1, null);
            int stopOffset = InputHandler.findWordEnd(code, caret, null);
            String candidate = code.substring(startOffset, stopOffset);

            if (!candidate.equals(prevWord)) {
                pps.whenDoneBlocking(ps -> handleCollectOccurrences(ps, tabIndex, startOffset, stopOffset));
                prevWord = candidate;
            }
            System.out.println("size: " + occurrences.size());

        } else {
            occurrences.clear();
        }
    }

    private void handleCollectOccurrences(PreprocSketch ps, int tabIndex, int startTabOffset, int stopTabOffset) {
        CompilationUnit root = ps.compilationUnit;

        // Map offsets
        int startJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, startTabOffset);
        int stopJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, stopTabOffset);

        // Find the node
        SimpleName name = ASTUtils.getSimpleNameAt(root, startJavaOffset, stopJavaOffset);
        if (name == null) {
            editor.statusMessage("Highlight the class/function/variable name first", EditorStatus.NOTICE);
            return;
        }

        // Find binding
        IBinding binding = ASTUtils.resolveBinding(name);
        if (binding == null) {
            editor.statusMessage(name.getIdentifier() + " isn't defined in this sketch, " + "so it cannot be renamed",
                    EditorStatus.ERROR);
            return;
        }

        // Renaming constructor should rename class
        if (binding.getKind() == IBinding.METHOD) {
            IMethodBinding method = (IMethodBinding) binding;
            if (method.isConstructor()) {
                binding = method.getDeclaringClass();
            }
        }

        ASTNode decl = root.findDeclaringNode(binding.getKey());
        if (decl == null) {
            editor.statusMessage("decl not found, showing usage instead", EditorStatus.NOTICE);
        }

        List<SimpleName> names = new ArrayList<>(findAllOccurrences(root, binding.getKey()));

        List<SketchInterval> intervals = names.stream().map(ps::mapJavaToSketch).collect(Collectors.toList());

        for (SketchInterval in : intervals) {
            try {
//                final Field tab = in.getClass().getDeclaredField("tabIndex");
//                tab.setAccessible(true);
                final Field startTabOffsetInterval = in.getClass().getDeclaredField("startTabOffset");
                startTabOffsetInterval.setAccessible(true);
                final Field stopTabOffsetInterval = in.getClass().getDeclaredField("stopTabOffset");
                stopTabOffsetInterval.setAccessible(true);

//                int tabIndex = (Integer) tab.get(in);
                int startOffset = (Integer) startTabOffsetInterval.get(in);
                int stopOffset = (Integer) stopTabOffsetInterval.get(in);
                
                int line = editor.getTextArea().getLineOfOffset(startOffset);
                
                occurrences.add(new Occurrence(tabIndex, line, startOffset, stopOffset, binding.getKey()));

            } catch (final ReflectiveOperationException e) {
                System.err.println(e);
            }
        }

//        if (occurrences.isEmpty()) {
//            occurrences.add(0, new Occurrence(tabIndex, line, startOffset, stopOffset, candidate));
//        } else {
//            occurrences.set(0, new Occurrence(tabIndex, line, startOffset, stopOffset, candidate));                    
//        }

//        System.out.println("decl: " + decl);
//        System.out.println("names: " + names);
//        System.out.println("intervals: " + intervals);

    }

    static List<SimpleName> findAllOccurrences(ASTNode root, String bindingKey) {
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

    public List<LineMarker> getOccurrences() {
        return occurrences;
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
    }
}
