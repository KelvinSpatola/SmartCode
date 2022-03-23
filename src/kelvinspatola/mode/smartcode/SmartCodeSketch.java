package kelvinspatola.mode.smartcode;

import java.io.IOException;
import java.util.ArrayList;
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
            List<Integer> removedLines = removeBookmarksAndRetrieveLineNumbers(oldName);
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
        List<Integer> removedLines = removeBookmarksAndRetrieveLineNumbers(oldName);
        
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

    
    /*
     * @param tab the target SketchCode filename
     * 
     * @return a list with the line numbers of all bookmarks removed from this tab
     */
    protected List<Integer> removeBookmarksAndRetrieveLineNumbers(String tab) {
        List<Integer> lineNumbers = new ArrayList<>();

        for (int i = editor.getBookmarkedLines().size() - 1; i >= 0; i--) {
            LineID lineID = editor.getBookmarkedLines().get(i).getLineID();
            
            if (lineID.fileName().equals(tab)) {
                lineNumbers.add(lineID.lineIdx());
                editor.removeLineBookmark(lineID);
            }
        }
        return lineNumbers;
    }
    

    /*
     * @param tab the target SketchCode filename
     * 
     * @param lineNumbers a list with the line numbers of bookmarks to be added
     */
    protected void generateBookmarksAt(String tab, List<Integer> lineNumbers) {
        for (int line : lineNumbers)
            editor.addLineBookmark(new LineID(tab, line));
    }
}
