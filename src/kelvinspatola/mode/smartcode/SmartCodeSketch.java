package kelvinspatola.mode.smartcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kelvinspatola.mode.smartcode.ui.LineBookmark;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;

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
        System.out.println("renaming!");
        if (renaming) {
            List<Integer> removedLines = new ArrayList<>();

            for (int i = editor.getBookmarkedLines().size() - 1; i >= 0; i--) {
                LineBookmark bm = editor.getBookmarkedLines().get(i);
                if (bm.getTabIndex() == getCurrentCodeIndex()) {
                    removedLines.add(bm.getLineID().lineIdx());
                    editor.removeLineBookmark(bm.getLineID());
                }
            }
            super.nameCode(newName);

            for (Integer line : removedLines)
                editor.addLineBookmark(editor.getLineIDInCurrentTab(line));

        } else {
            super.nameCode(newName);
        }

    }

//    @Override
//    public boolean saveAs() throws IOException {
//        if (editor.getBookmarkedLines().isEmpty()) {
//            System.out.println("No bookmarks. Saving now");
//            return super.saveAs();
//        }
//        
//        String oldName = getCode(0).getFileName();
//        
//        boolean save = super.saveAs();
//
//        System.out.println("Saved bitch!");
//
//        return save;
//    }

}
