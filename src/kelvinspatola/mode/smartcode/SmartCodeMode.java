package kelvinspatola.mode.smartcode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import processing.app.Base;
import processing.app.Platform;
import processing.app.ui.Editor;
import processing.app.ui.EditorException;
import processing.app.ui.EditorState;
import processing.mode.java.JavaMode;

public class SmartCodeMode extends JavaMode {
    static public boolean bracketClosingEnabled;
    protected File dataFolder;
    protected File snippetsFile;

    public SmartCodeMode(Base base, File folder) {
        super(base, folder);
        SmartCodePreferences.init();

        examplesFolder = Platform.getContentFile("modes/java/examples");
        dataFolder = getContentFile("data");
        loadSnippetsFile();
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

    public final File getJarFile() {
        return getContentFile("mode/SmartCodeMode.jar");
    }

    public File loadSnippetsFile() {
        snippetsFile = getContentFile("data/snippets.json");

        if (!snippetsFile.exists()) {
            System.out.println("Creating a new 'templates.json' file at:\n    " + dataFolder.getAbsolutePath());
            System.out.println("You can modify this file and add your own templates there.");
            snippetsFile = createNewSnippetsFile();
        }
        return snippetsFile;
    }

    private File createNewSnippetsFile() {
        final String FILE_TO_EXTRACT = "data/templates.json";

        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(getJarFile()))) {
            ZipEntry entry = null;

            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(FILE_TO_EXTRACT)) {
                    File newFile = new File(folder, FILE_TO_EXTRACT);

                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int size;
                        while ((size = zip.read(buffer)) > 0) {
                            fos.write(buffer, 0, size);
                        }
                    }
                    return newFile;
                }
            }
            zip.closeEntry();

        } catch (IOException e) {
            System.err.println("Unable to create a new 'templates.json' file.");
            e.printStackTrace();
        }
        return null;
    }

}
