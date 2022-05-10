package kelvinspatola.mode.smartcode.ui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import kelvinspatola.mode.smartcode.SmartCodeEditor;
import processing.app.ui.Toolkit;
import processing.app.ui.ZoomTreeCellRenderer;

public class ShowBookmarks {
    private List<LineMarker> bookmarks = new ArrayList<>();
    private SmartCodeEditor editor;
    private boolean showWindow;

    private JDialog window;
    private JTree tree;

    // CONSTRUCTOR
    public ShowBookmarks(SmartCodeEditor editor) {
        this.editor = editor;
        bookmarks = editor.lineBookmarks.getMarkers();

        // Show Usage window
        window = new JDialog(editor);
        window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        window.setAutoRequestFocus(false);
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                tree.setModel(null);
                showWindow = false;
            }

            @Override
            public void componentShown(ComponentEvent e) {
                updateTree();
            }
        });
        window.setSize(Toolkit.zoom(300, 400));
        window.setFocusableWindowState(false);
        Toolkit.setIcon(window);

        ZoomTreeCellRenderer renderer = new ZoomTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        renderer.setBackgroundSelectionColor(new Color(228, 248, 246));
        renderer.setBorderSelectionColor(Color.BLACK);
        renderer.setTextSelectionColor(Color.BLACK);
        tree = new JTree();
        tree.setCellRenderer(renderer);

        JScrollPane sp = new JScrollPane();
        sp.setViewportView(tree);
        window.add(sp);

        tree.addTreeSelectionListener(e -> {
            if (tree.getLastSelectedPathComponent() != null) {
                DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                if (tnode.getUserObject() instanceof BookmarkTreeNode) {
                    BookmarkTreeNode node = (BookmarkTreeNode) tnode.getUserObject();
                    editor.highlight(node.tabIndex, node.startOffset, node.stopOffset);
                }
            }
        });
        
        editor.lineBookmarks.notifyChanges(b -> updateTree());
    }

    public void handleShowBookmarks() {
        showWindow = true;
        updateTree();
    }

    public void updateTree() {
        // Create root node
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(editor.getSketch().getName());

        bookmarks.stream().map(BookmarkTreeNode::new)
                // Group nodes by tab index
                .collect(Collectors.groupingBy(marker -> marker.tabIndex))
                // Stream Map Entries of (tab index) <-> (List<BookmarkTreeNode>)
                .entrySet().stream().map(entry -> {
                    List<BookmarkTreeNode> nodes = entry.getValue(); // bookmarks per tab

                    int count = nodes.size();
                    String bookmarkLabel = (count == 1) ? "bookmark" : "bookmarks";
                    String tabName = editor.getSketch().getCode(entry.getKey()).getPrettyName();

                    // Create new DefaultMutableTreeNode for this tab
                    String tabLabel = "<html><font color=#222222>" + tabName + "</font> <font color=#999999>" + count
                            + " " + bookmarkLabel + "</font></html>";

                    DefaultMutableTreeNode tabNode = new DefaultMutableTreeNode(tabLabel);

                    // Stream nodes belonging to this tab
                    nodes.stream()
                            //.sorted((n1, n2) -> n1.startOffset - n2.startOffset)
                            // Convert TreeNodes to DefaultMutableTreeNodes
                            .map(DefaultMutableTreeNode::new)
                            // Add all as children of tab node
                            .forEach(tabNode::add);
                    return tabNode;

                }).forEach(rootNode::add);

        if (showWindow) {
            // Update tree
            EventQueue.invokeLater(() -> {
                tree.setModel(new DefaultTreeModel(rootNode));

                // Expand all nodes
                for (int i = 0; i < tree.getRowCount(); i++) {
                    tree.expandRow(i);
                }

                tree.setRootVisible(true);

                if (!window.isVisible()) {
                    window.setVisible(true);
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
                    Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
                    int maxX = (int) rect.getMaxX() - window.getWidth();
                    int x = Math.min(editor.getX() + editor.getWidth(), maxX);
                    int y = (x == maxX) ? 10 : editor.getY();
                    window.setLocation(x, y);
                }
                window.toFront();
                window.setTitle("Bookmarks encoutered: " + bookmarks.size());
            });
        }
    }

    public void dispose() {
        if (window != null) {
            window.dispose();
        }
    }

    class BookmarkTreeNode {
        final int tabIndex;
        final int startOffset;
        final int stopOffset;
        final String text;

        BookmarkTreeNode(LineMarker b) {
            tabIndex = b.getTabIndex();
            startOffset = b.getStartOffset();
            stopOffset = b.getStopOffset();
            text = b.getText();
        }

        @Override
        public String toString() {
            return text;
        }
    }
}