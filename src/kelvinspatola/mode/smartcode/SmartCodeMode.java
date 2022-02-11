package kelvinspatola.mode.smartcode;

import processing.app.Base;
import processing.app.Platform;
import processing.app.ui.Editor;
import processing.app.ui.EditorException;
import processing.app.ui.EditorState;
import processing.mode.java.JavaMode;

import java.io.File;



public class SmartCodeMode extends JavaMode {  
    static public boolean bracketClosingEnabled;

    public SmartCodeMode(Base base, File folder) {
        super(base, folder);
        examplesFolder = Platform.getContentFile("modes/java/examples");
        SmartCodePreferences.init();
    }

    /**
     * Gets the display name of this mode.
     * 
     * @return the display name of this mode
     */
    @Override
    public String getTitle() {
        return "SmartCode";
    }

    @Override
    public Editor createEditor(Base base, String path, EditorState state) throws EditorException {
        return new SmartCodeEditor(base, path, state, this);
    }
    
    public File checkTemplateFolder() {
        return checkSketchbookTemplate();
    }    

}
