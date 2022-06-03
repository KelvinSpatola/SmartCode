package kelvinspatola.mode.smartcode.ui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import kelvinspatola.mode.smartcode.SmartCodeEditor;
import kelvinspatola.mode.smartcode.ui.LineBookmarks.Bookmark;
import kelvinspatola.mode.smartcode.ui.LineBookmarks.BookmarkListListener;
import processing.app.ui.Toolkit;
import processing.app.ui.ZoomTreeCellRenderer;


public class ShowBookmarks {
    private final List<LineMarker> bookmarks;
    private final SmartCodeEditor editor;
    private final JDialog window;
    private final JTree tree;
    private final DefaultMutableTreeNode rootNode;

    // CONSTRUCTOR
    public ShowBookmarks(SmartCodeEditor editor) {
        this.editor = editor;
        rootNode = new DefaultMutableTreeNode(editor.getSketch().getName());
        bookmarks = editor.lineBookmarks.getMarkers();

        window = new JDialog(editor);
        window.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        window.setSize(Toolkit.zoom(300, 400));
        window.setAutoRequestFocus(false);
        window.setFocusableWindowState(false);
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                tree.setModel(null);
            }
        });
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
        tree.addTreeSelectionListener(e -> {
            Object lastSelectedPathComponent = tree.getLastSelectedPathComponent();

            if (lastSelectedPathComponent != null) {
                Object userObj = ((DefaultMutableTreeNode) lastSelectedPathComponent).getUserObject();

                if (userObj instanceof LineMarker) {
                    editor.highlight((LineMarker) userObj);
                }
            }
        });
        window.add(new JScrollPane(tree));

        editor.lineBookmarks.addBookmarkListListener(new BookmarkListListener() {
            public void bookmarkAdded(Bookmark bm) {
                updateTree(); 
            }

            public void bookmarkRemoved(Bookmark bm) {
                if (bookmarks.isEmpty()) {
                    window.setVisible(false);
                } else {
                    updateTree();
                }
            }
        });
    }

    public void handleShowBookmarks() {
        if (window.isVisible())
            return; // do nothing if the window is already visible.

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int maxX = (int) rect.getMaxX() - window.getWidth();
        int x = Math.min(editor.getX() + editor.getWidth(), maxX);
        int y = (x == maxX) ? 10 : editor.getY();
        window.setLocation(x, y);
        window.setVisible(true);
        window.toFront();

        updateTree();
    }

    public void updateTree() {
        if (!window.isVisible())
            return;

        rootNode.removeAllChildren();

        bookmarks.stream()
                // Group nodes by tab index
                .collect(Collectors.groupingBy(LineMarker::getTabIndex))
                // Stream Map Entries of (tab index, List<LineMarker>)
                .entrySet().stream()
                .map(nodesPerTab -> {
                    int tabIndex = nodesPerTab.getKey();
                    int count = nodesPerTab.getValue().size();

                    String tabName = editor.getSketch().getCode(tabIndex).getPrettyName();
                    String tabTitle = "<html><font color=#222222>" + tabName + "</font> <font color=#999999>" + count
                            + " bookmark" + ((count == 1) ? "" : "s") + "</font></html>";

                    // Create new DefaultMutableTreeNode for this tab
                    DefaultMutableTreeNode tabNode = new DefaultMutableTreeNode(tabTitle);

                    // Stream nodes belonging to this tab
                    nodesPerTab.getValue().stream()
                            // Convert TreeNodes to DefaultMutableTreeNodes
                            .map(DefaultMutableTreeNode::new)
                            // Add all as children of tab node
                            .forEach(tabNode::add);
                    
                    return tabNode;
                }).forEach(rootNode::add);

        // Update tree
        EventQueue.invokeLater(() -> {
            tree.setModel(new DefaultTreeModel(rootNode));
            // Expand all nodes
            for (int i = 0; i < tree.getRowCount(); i++) {
                tree.expandRow(i);
            }
            tree.setRootVisible(true);
            window.setTitle("Bookmarks encoutered: " + bookmarks.size());
        });
    }

    public void dispose() {
        if (window != null) {
            window.dispose();
        }
    }
}