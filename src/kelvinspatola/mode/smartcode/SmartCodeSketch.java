package kelvinspatola.mode.smartcode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import kelvinspatola.mode.smartcode.ui.ColorTag;
import kelvinspatola.mode.smartcode.ui.LineBookmarks.Bookmark;
import kelvinspatola.mode.smartcode.ui.LineMarker;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.mode.java.debug.LineID;

public class SmartCodeSketch extends Sketch {
    private SmartCodeEditor editor;
    private boolean renaming;

    // CONSTRUCTOR
    public SmartCodeSketch(String path, Mode mode) {
        super(path, mode);
    }

    // CONSTRUCTOR
    public SmartCodeSketch(String path, SmartCodeEditor editor) throws IOException {
        super(path, editor);
        this.editor = editor;
    }

    @Override
    public void removeCode(SketchCode which) {
        editor.clearBookmarksFromTab(getCurrentCodeIndex());
        super.removeCode(which);
    }

    @Override
    public void handleRenameCode() {
        renaming = true;
        super.handleRenameCode();
        renaming = false;
    }

    @Override
    protected void nameCode(String newName) {
        if (renaming) {
            String oldName = getCurrentCode().getFileName();
            var removedLines = getBookmarksInfoAndRemoveThem(oldName);
            super.nameCode(newName);
            generateBookmarksAt(getCurrentCode().getFileName(), removedLines);

        } else {
            super.nameCode(newName);
        }
    }

    @Override
    public boolean saveAs() throws IOException {
        if (!editor.hasBookmarks()) {
            return super.saveAs();
        }

        String oldName = getCode(0).getFileName();
        var removedLines = getBookmarksInfoAndRemoveThem(oldName);

        boolean saved = super.saveAs();

        String newName = getCode(0).getFileName();
        generateBookmarksAt(newName, removedLines);

        if (saved) {
            for (SketchCode code : getCode()) {
                editor.addBookmarkComments(code.getFileName());
            }
        }
        return saved;
    }

    @Override
    public boolean save() throws IOException {
        if (!editor.hasBookmarks()) {
            return super.save();
        }

        if (super.save()) {
            for (SketchCode code : getCode()) {
                editor.addBookmarkComments(code.getFileName());
            }
            return true;
        }
        return false;
    }

    /**
     * Saves information relative to the line number and highlight color from all
     * bookmarks in the current tab before removing them.
     * 
     * Its purpose is to save bookmark references at a time before the tab name is
     * changed and then serve the {@link #generateBookmarksAt()} method, which
     * restores all bookmarks back once a new name has been assigned to the tab.
     * 
     * @param tabFileName the target {@code SketchCode} filename.
     * 
     * @return a map with the line number and color tag of all bookmarks removed
     *         from the current tab.
     * 
     * @see generateBookmarksAt
     */
    protected Map<Integer, ColorTag> getBookmarksInfoAndRemoveThem(String tabFileName) {
        Map<Integer, ColorTag> result = new HashMap<>();

        for (int i = editor.getBookmarks().size() - 1; i >= 0; i--) {
            Bookmark bm = (Bookmark) editor.getBookmarks().get(i);

            if (bm.getLineID().fileName().equals(tabFileName)) {
                ColorTag tag = bm.getColorTag();
                result.put(bm.getLine(), tag);
                editor.removeBookmark(bm);
            }
        }
        return result;
    }

    /*
     * @param tab the target SketchCode filename
     * 
     * @param lineNumbers a list with the line numbers of bookmarks to be added
     */
    protected void generateBookmarksAt(String tabFileName, Map<Integer, ColorTag> references) {
        references.keySet().stream().forEach(l -> editor.addBookmark(new LineID(tabFileName, l), references.get(l)));
    }
}
