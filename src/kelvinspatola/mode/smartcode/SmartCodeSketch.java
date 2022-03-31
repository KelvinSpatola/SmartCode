package kelvinspatola.mode.smartcode;

import java.awt.Color;

import java.io.IOException;
import java.util.Map;

import kelvinspatola.mode.smartcode.ui.LineBookmark;

import java.util.HashMap;
import java.util.List;

import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.mode.java.debug.LineID;

public class SmartCodeSketch extends Sketch {
    private final SmartCodeEditor editor;
    private boolean renaming;

    // CONSTRUCTOR
    public SmartCodeSketch(String path, Mode mode) {
        super(path, mode);
        this.editor = null;
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
        if (editor.getBookmarkedLines().isEmpty()) {
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
        if (editor.getBookmarkedLines().isEmpty()) {
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
     * changed and then serve the {@link generateBookmarksAt} method, which restores all
     * bookmarks back once a new name has been assigned to the tab.
     * 
     * @param tab the target SketchCode filename.
     * 
     * @return a map with the line number and color of all bookmarks removed from
     *         the current tab.
     *         
     * @see generateBookmarksAt
     */
    protected Map<Integer, Color> getBookmarksInfoAndRemoveThem(String tabFileName) {
        Map<Integer, Color> result = new HashMap<>();
        List<LineBookmark> bookmarks = editor.getBookmarkedLines();

        for (int i = bookmarks.size() - 1; i >= 0; i--) {
            LineID lineID = bookmarks.get(i).getLineID();

            if (lineID.fileName().equals(tabFileName)) {
                Color color = bookmarks.get(i).getColor();
                result.put(lineID.lineIdx(), color);
                editor.removeLineBookmark(lineID);
            }
        }
        return result;
    }

    /*
     * @param tab the target SketchCode filename
     * 
     * @param lineNumbers a list with the line numbers of bookmarks to be added
     */
    protected void generateBookmarksAt(String tabFileName, Map<Integer, Color> references) {
        for (int line : references.keySet())
            editor.addLineBookmark(new LineID(tabFileName, line), references.get(line));
    }
}
