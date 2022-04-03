package kelvinspatola.mode.smartcode.ui;

import java.awt.Color;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import processing.app.Base;
import processing.app.Messages;
import processing.app.Settings;
import processing.app.ui.Theme;
import processing.core.PApplet;

public class SmartCodeTheme {
    static public final String[] attributes = { 
            "bookmarks.icon.color", // 0
            "bookmarks.linehighlight", // 1
            "bookmarks.linehighlight.color.1", // 2
            "bookmarks.linehighlight.color.2", // 3
            "bookmarks.linehighlight.color.3", // 4
            "bookmarks.linehighlight.color.4", // 5
            "bookmarks.linehighlight.color.5", // 6
            "column.bookmark.color", // 7
            "column.error.color", // 8
            "column.occurrence.color", // 9
            "column.warning.color", // 10
            "occurrences.highlight", // 11
            "occurrences.highlight.color", // 12
    };

    static protected Map<String, String> defaultPrefs = new HashMap<>();
    static {
        defaultPrefs.put(attributes[0], Theme.get("footer.icon.selected.color")); // bookmarks.icon.color
        defaultPrefs.put(attributes[1], "true"); // bookmarks.linehighlight
        defaultPrefs.put(attributes[2], "#9B5DE5"); // bookmarks.linehighlight.color.1
        defaultPrefs.put(attributes[3], "#F15BB5"); // bookmarks.linehighlight.color.2
        defaultPrefs.put(attributes[4], "#FEE440"); // bookmarks.linehighlight.color.3
        defaultPrefs.put(attributes[5], "#00BBF9"); // bookmarks.linehighlight.color.4
        defaultPrefs.put(attributes[6], "#00F5D4"); // bookmarks.linehighlight.color.5
        defaultPrefs.put(attributes[7], "#00FFFF"); // column.bookmark.color
        defaultPrefs.put(attributes[8], "#FF0000"); // column.error.color
        defaultPrefs.put(attributes[9], Theme.get("header.tab.selected.color")); // column.occurrence.color
        defaultPrefs.put(attributes[10], "#FFFF00"); // column.warning.color
        defaultPrefs.put(attributes[11], "true"); // occurrences.highlight
        defaultPrefs.put(attributes[12], Theme.get("header.tab.selected.color")); // occurrences.highlight.color
    }

    static private Settings theme;

    static public void init() {
        try {
            File inputFile = ensureExistence(getThemeFile());
            theme = new Settings(inputFile);
            ensureAttributes();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static public File getThemeFile() {
        return new File(Base.getSketchbookModesFolder(), "SmartCodeMode/theme/theme.txt");
    }

    static private File ensureExistence(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            Messages.log("theme.txt created at " + file.getAbsolutePath());

            try (PrintWriter writer = PApplet.createWriter(file)) {
                String[] keyList = defaultPrefs.keySet().toArray(new String[0]);
                keyList = PApplet.sort(keyList);
                for (String key : keyList) {
                    writer.println(key + " = " + defaultPrefs.get(key));
                }
            }
            Messages.log("Default attributes for theme.txt successfully configured");
        }
        return file;
    }

    private static void ensureAttributes() {
        for (String att : defaultPrefs.keySet()) {
            if (!theme.getMap().containsKey(att) || theme.get(att).isEmpty()) {
                theme.set(att, defaultPrefs.get(att));
                Messages.log("Setting up a default value for: " + att);
            }
        }
        theme.save();
    }
    
    static public Color updateColor(String attribute) {
        String key = "";
        
        if (theme.getMap().containsKey(attribute)) {
            if(attribute.equals("column.occurrence.color") ||
                    attribute.equals("occurrences.highlight.color")) {
                key = "header.tab.selected.color";
                
            } else if (attribute.equals("bookmarks.icon.color")) {
                key = "footer.icon.selected.color";
            }
        }
        return updateColor(attribute, Theme.get(key));
    }
    
    static public Color updateColor(String attribute, String value) {
        if (theme.getMap().containsKey(attribute)) {
            theme.set(attribute, value);
            theme.save();
            
        }
        return theme.getColor(attribute);
    }

    static public boolean getBoolean(String attribute) {
        return theme.getBoolean(attribute);
    }

    static public Color getColor(String attribute) {
        return theme.getColor(attribute);
    }
}
