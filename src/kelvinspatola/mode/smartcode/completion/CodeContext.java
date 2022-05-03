package kelvinspatola.mode.smartcode.completion;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import static org.eclipse.jdt.core.dom.ASTNode.*;

import java.util.*;
import java.util.stream.IntStream;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import kelvinspatola.mode.smartcode.SmartCodeEditor;
import processing.app.syntax.InputHandler;
import processing.mode.java.ASTUtils;
import processing.mode.java.PreprocService;
import processing.mode.java.PreprocSketch;

public class CodeContext implements CaretListener {
    private SmartCodeEditor editor;
    private PreprocService pps;

    private String code;
    private String prevCandidate;
    private int lastCodeLength = -1;

    // CONSTRUCTOR
    public CodeContext(SmartCodeEditor editor, PreprocService pps) {
        this.editor = editor;
        this.pps = pps;
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        int caret = e.getDot();
        final int length = editor.getText().length();

        // update 'code' only if we move the caret from one line to another
        if (length != lastCodeLength) {
            code = editor.getText();
            lastCodeLength = length;
        }
        // In case we 'touch' a word on the right side
        if (caret > 0 && caret < code.length() && !isValidChar(caret) && isValidChar(caret - 1)) {
            caret--;
        }

        // if ((caret < code.length()) && isValidChar(caret)) {
//            int startOffset = findWordStart(code, caret);
//            int stopOffset = findWordEnd(code, caret);
        int startOffset = caret;
        int stopOffset = caret;

        String candidate = "" + caret;
//            if (startOffset == -1 || stopOffset == -1) {
//                startOffset = caret;
//                stopOffset = caret;
//                candidate = "";                
//            } else {
//                candidate = code.substring(startOffset, stopOffset);                
//            }

        if (caret == length - 1) {
            usedNodes.stream().forEach(n -> System.out.println(n));
        }

        // if (!candidate.equals(prevCandidate)) {
        // System.err.println("\n *** candidate => " + candidate + " ***");
        int start = startOffset;
        int stop = stopOffset;
        pps.whenDoneBlocking(ps -> getContext(ps, start, stop));
        prevCandidate = candidate;
        // }
//        } else {
//            prevCandidate = null;
//        }
    }

    Set<String> usedNodes = new HashSet<>();

    private void getContext(PreprocSketch ps, int startTabOffset, int stopTabOffset) {
        // Map offsets
        int tab = editor.getSketch().getCurrentCodeIndex();
        int startJavaOffset = ps.tabOffsetToJavaOffset(tab, startTabOffset);
        int stopJavaOffset = ps.tabOffsetToJavaOffset(tab, stopTabOffset);

        CompilationUnit root = ps.compilationUnit;
        if (root.types().isEmpty()) {
            System.out.println("No Type found in CU");
            return;
        }

        SimpleName simpleName = getSimpleNameAt(root, startJavaOffset, stopJavaOffset);
//        System.err.println("\nsimpleName: " + simpleName);
//        if (simpleName == null)
//            return;
//        System.out.println("node type: " + simpleName.getNodeType());

        ASTNode node = findASTNodeAt(root, startJavaOffset, stopJavaOffset);
        //editor.getConsole().clear();
        // System.out.println("node: " + node);
        String nodeType = getTypeName(node);
        // System.out.println("node type: " + nodeType);
        usedNodes.add(nodeType);

        // Find binding
//        IBinding binding = ASTUtils.resolveBinding(simpleName);
//        System.out.println("binding: " + binding);

//        String key = binding.getKey();
//        ASTNode node = ps.compilationUnit.findDeclaringNode(key);
//        System.out.println("node.getNodeType: " + node.getNodeType());
//        
//        if(binding.getKind() == IBinding.METHOD) {
//            IMethodBinding method = (IMethodBinding) binding;
//            System.err.println("isArray: " + method.getReturnType().isArray());
//        }
//        
//        SimpleName declName = null;
//        switch (binding.getKind()) {
//          case IBinding.TYPE: declName = ((TypeDeclaration) decl).getName(); break;
//          case IBinding.METHOD: declName = ((MethodDeclaration) decl).getName(); break;
//          case IBinding.VARIABLE: declName = ((VariableDeclaration) decl).getName(); break;
//        }
//        System.err.println("declName: " + declName);

//        switch (binding.getKind()) {
//        case IBinding.METHOD:
//            if (((IMethodBinding) binding).isConstructor()) {
//                System.out.println("Constructor");
//            } else {
//                System.out.println("Method");
//            }
//            IMethodBinding method = (IMethodBinding) binding;
//            System.out.println("methodBinding: " + method);
//            System.out.println("parent: " + method.getDeclaringClass().getName());
//            System.out.print("params: ");
//            for (ITypeBinding type : method.getParameterTypes()) {
//                System.out.print(type.getName() + ", ");
//            }
//            System.out.println("\nreturn: " + method.getReturnType().getName());
//            break;
//            
//        case IBinding.VARIABLE:
//            IVariableBinding variable = (IVariableBinding) binding;
//            
//            if (variable.isField())
//                System.out.println("Field");
//            else if (variable.isParameter())
//                System.out.println("Parameter");
//            else if (variable.isEnumConstant())
//                System.out.println("Enum constant");
//            else
//                System.out.println("Local variable");
//            
//            System.out.println("declaring method: " + variable.getDeclaringMethod());
//            break;
//            
//        case IBinding.TYPE:
//            ITypeBinding type = (ITypeBinding) binding;
//            
//            System.out.println("Type");
//            if (type.isTypeVariable())
//                System.out.println("Enum constant");
//            break;
//        }
    }

    public static ASTNode findASTNodeAt(ASTNode root, int startJavaOffset, int stopJavaOffset) {
        int length = stopJavaOffset - startJavaOffset;

        NodeFinder f = new NodeFinder(root, startJavaOffset, length);
        ASTNode node = f.getCoveredNode();
        if (node == null) {
            node = f.getCoveringNode();
        }

        return node;
    }

    public static SimpleName getSimpleNameAt(ASTNode root, int startJavaOffset, int stopJavaOffset) {
        // Find node at offset
        ASTNode node = ASTUtils.getASTNodeAt(root, startJavaOffset, stopJavaOffset);

        SimpleName result = null;
        if (node != null) {
            if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
                result = (SimpleName) node;
            } else {
                // Return SimpleName with highest coverage
                List<SimpleName> simpleNames = getSimpleNameChildren(node);
                if (!simpleNames.isEmpty()) {
                    // Compute coverage <selection x node>
                    int[] coverages = simpleNames.stream().mapToInt(name -> {
                        int start = name.getStartPosition();
                        int stop = start + name.getLength();
                        return Math.min(stop, stopJavaOffset) - Math.max(startJavaOffset, start);
                    }).toArray();
                    // Select node with highest coverage
                    int maxIndex = IntStream.range(0, simpleNames.size())
                            .filter(i -> coverages[i] >= 0)
                            .reduce((i, j) -> coverages[i] > coverages[j] ? i : j)
                            .orElse(-1);
                    if (maxIndex == -1)
                        return null;
                    result = simpleNames.get(maxIndex);
                }
            }
        }

        return result;
    }

    public static List<SimpleName> getSimpleNameChildren(ASTNode node) {
        List<SimpleName> simpleNames = new ArrayList<>();
        node.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName simpleName) {
                System.out.println(" -> " + simpleName);
                simpleNames.add(simpleName);
                return super.visit(simpleName);
            }
        });
        System.out.println("size: " + simpleNames.size());
        return simpleNames;
    }

    /*
     * for testing purposes
     */
    static String getTypeName(ASTNode node) {
        return nodeType.get(node.getNodeType());
    }

    static final Map<Integer, String> nodeType = new HashMap<>();
    static {
        nodeType.put(1, "MALFORMED or ANONYMOUS_CLASS_DECLARATION");
        nodeType.put(2, "ORIGINALorARRAY_ACCESS");
        nodeType.put(3, "ARRAY_CREATION");
        nodeType.put(4, "ARRAY_INITIALIZER or PROTECT");
        nodeType.put(5, "ARRAY_TYPE");
        nodeType.put(6, "ASSERT_STATEMENT");
        nodeType.put(7, "ASSIGNMENT");
        nodeType.put(8, "BLOCK or RECOVERED");
        nodeType.put(9, "BOOLEAN_LITERAL");
        nodeType.put(10, "BREAK_STATEMENT");
        nodeType.put(11, "CAST_EXPRESSION");
        nodeType.put(12, "CATCH_CLAUSE");
        nodeType.put(13, "CHARACTER_LITERAL");
        nodeType.put(14, "CLASS_INSTANCE_CREATION");
        nodeType.put(15, "COMPILATION_UNIT");
        nodeType.put(16, "CONDITIONAL_EXPRESSION");
        nodeType.put(17, "CONSTRUCTOR_INVOCATION");
        nodeType.put(18, "CONTINUE_STATEMENT");
        nodeType.put(19, "DO_STATEMENT");
        nodeType.put(20, "EMPTY_STATEMENT");
        nodeType.put(21, "EXPRESSION_STATEMENT");
        nodeType.put(22, "FIELD_ACCESS");
        nodeType.put(23, "FIELD_DECLARATION");
        nodeType.put(24, "FOR_STATEMENT");
        nodeType.put(25, "IF_STATEMENT");
        nodeType.put(26, "IMPORT_DECLARATION");
        nodeType.put(27, "INFIX_EXPRESSION");
        nodeType.put(28, "INITIALIZER");
        nodeType.put(29, "JAVADOC");
        nodeType.put(30, "LABELED_STATEMENT");
        nodeType.put(31, "METHOD_DECLARATION");
        nodeType.put(32, "METHOD_INVOCATION");
        nodeType.put(33, "NULL_LITERAL");
        nodeType.put(34, "NUMBER_LITERAL");
        nodeType.put(35, "PACKAGE_DECLARATION");
        nodeType.put(36, "PARENTHESIZED_EXPRESSION");
        nodeType.put(37, "POSTFIX_EXPRESSION");
        nodeType.put(38, "PREFIX_EXPRESSION");
        nodeType.put(39, "PRIMITIVE_TYPE");
        nodeType.put(40, "QUALIFIED_NAME");
        nodeType.put(41, "RETURN_STATEMENT");
        nodeType.put(42, "SIMPLE_NAME");
        nodeType.put(43, "SIMPLE_TYPE");
        nodeType.put(44, "SINGLE_VARIABLE_DECLARATION");
        nodeType.put(45, "STRING_LITERAL");
        nodeType.put(46, "SUPER_CONSTRUCTOR_INVOCATION");
        nodeType.put(47, "SUPER_FIELD_ACCESS");
        nodeType.put(48, "SUPER_METHOD_INVOCATION");
        nodeType.put(49, "SWITCH_CASE");
        nodeType.put(50, "SWITCH_STATEMENT");
        nodeType.put(51, "SYNCHRONIZED_STATEMENT");
        nodeType.put(52, "THIS_EXPRESSION");
        nodeType.put(53, "THROW_STATEMENT");
        nodeType.put(54, "TRY_STATEMENT");
        nodeType.put(55, "TYPE_DECLARATION");
        nodeType.put(56, "TYPE_DECLARATION_STATEMENT");
        nodeType.put(57, "TYPE_LITERAL");
        nodeType.put(58, "VARIABLE_DECLARATION_EXPRESSION");
        nodeType.put(59, "VARIABLE_DECLARATION_FRAGMENT");
        nodeType.put(60, "VARIABLE_DECLARATION_STATEMENT");
        nodeType.put(61, "WHILE_STATEMENT");
        nodeType.put(62, "INSTANCEOF_EXPRESSION");
        nodeType.put(63, "LINE_COMMENT");
        nodeType.put(64, "BLOCK_COMMENT");
        nodeType.put(65, "TAG_ELEMENT");
        nodeType.put(66, "TEXT_ELEMENT");
        nodeType.put(67, "MEMBER_REF");
        nodeType.put(68, "METHOD_REF");
        nodeType.put(69, "METHOD_REF_PARAMETER");
        nodeType.put(70, "ENHANCED_FOR_STATEMENT");
        nodeType.put(71, "ENUM_DECLARATION");
        nodeType.put(72, "ENUM_CONSTANT_DECLARATION");
        nodeType.put(73, "TYPE_PARAMETER");
        nodeType.put(74, "PARAMETERIZED_TYPE");
        nodeType.put(75, "QUALIFIED_TYPE");
        nodeType.put(76, "WILDCARD_TYPE");
        nodeType.put(77, "NORMAL_ANNOTATION");
        nodeType.put(78, "MARKER_ANNOTATION");
        nodeType.put(79, "SINGLE_MEMBER_ANNOTATION");
        nodeType.put(80, "MEMBER_VALUE_PAIR");
        nodeType.put(81, "ANNOTATION_TYPE_DECLARATION");
        nodeType.put(82, "ANNOTATION_TYPE_MEMBER_DECLARATION");
        nodeType.put(83, "MODIFIER");
        nodeType.put(84, "UNION_TYPE");
        nodeType.put(85, "DIMENSION");
        nodeType.put(86, "LAMBDA_EXPRESSION");
        nodeType.put(87, "INTERSECTION_TYPE");
        nodeType.put(88, "NAME_QUALIFIED_TYPE");
        nodeType.put(89, "CREATION_REFERENCE");
        nodeType.put(90, "EXPRESSION_METHOD_REFERENCE");
        nodeType.put(91, "SUPER_METHOD_REFERENCE");
        nodeType.put(92, "TYPE_METHOD_REFERENCE");
    }

    public int findWordStart(String text, int pos) {
//        for (int i = pos; i >= 0; i--) {
//            if (!Character.isLetterOrDigit(text.charAt(i)))
//                return i + 1;
//        }
//        return -1;

        String noWordSep = (String) editor.getTextArea().getDocument().getProperty("noWordSep");
        return InputHandler.findWordStart(text, pos, noWordSep);
    }

    public int findWordEnd(String text, int pos) {
//        for (int i = pos; i < text.length(); i++) {
//            if (!Character.isLetterOrDigit(text.charAt(i)))
//                return i;
//        }
//        return -1;

        String noWordSep = (String) editor.getTextArea().getDocument().getProperty("noWordSep");
        return InputHandler.findWordEnd(text, pos, noWordSep);
    }

    private boolean isValidChar(int offset) {
        return Character.isLetterOrDigit(code.charAt(offset));
    }
}
