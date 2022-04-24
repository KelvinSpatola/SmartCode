package kelvinspatola.mode.smartcode.ui;

import java.awt.Color;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import processing.app.Base;
import processing.app.Messages;
import processing.app.Settings;
import processing.app.ui.Theme;
import processing.core.PApplet;

public class SmartCodeTheme {
    static public boolean BOOKMARKS_HIGHLIGHT;
    static public boolean OCCURRENCES_HIGHLIGHT;
    static private Set<String> pdeThemeAttributes = new HashSet<>();
    static private Settings settings;
    
    static private final String[] attributes = {
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
            "templates.highlight.color", // 13
    };

    static protected final Map<String, String> defaultPrefs = new HashMap<>();
    static {
        defaultPrefs.put(attributes[0], "footer.icon.selected.color"); // bookmarks.icon.color
        defaultPrefs.put(attributes[1], "true"); // bookmarks.linehighlight
        defaultPrefs.put(attributes[2], "#9B5DE5"); // bookmarks.linehighlight.color.1
        defaultPrefs.put(attributes[3], "#F15BB5"); // bookmarks.linehighlight.color.2
        defaultPrefs.put(attributes[4], "#FEE440"); // bookmarks.linehighlight.color.3
        defaultPrefs.put(attributes[5], "#00BBF9"); // bookmarks.linehighlight.color.4
        defaultPrefs.put(attributes[6], "#00F5D4"); // bookmarks.linehighlight.color.5
        defaultPrefs.put(attributes[7], "#00FFFF"); // column.bookmark.color
        defaultPrefs.put(attributes[8], "#FF0000"); // column.error.color
        defaultPrefs.put(attributes[9], "occurrences.highlight.color"); // column.occurrence.color
        defaultPrefs.put(attributes[10], "#FFFF00"); // column.warning.color
        defaultPrefs.put(attributes[11], "true"); // occurrences.highlight
        defaultPrefs.put(attributes[12], "header.tab.selected.color"); // occurrences.highlight.color
        defaultPrefs.put(attributes[13], "editor.eolmarkers.color"); // templates.highlight.color
    }

    static public void init() {
        try {
            File inputFile = ensureExistence(getThemeFile());
            settings = new Settings(inputFile);
            ensureAttributes();

        } catch (IOException e) {
            e.printStackTrace();
        }
        loadPdeThemeAttributes();
        
        BOOKMARKS_HIGHLIGHT = getBoolean("bookmarks.linehighlight");
        OCCURRENCES_HIGHLIGHT = getBoolean("occurrences.highlight");
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
            if (!settings.getMap().containsKey(att) || settings.get(att).isEmpty()) {
                settings.set(att, defaultPrefs.get(att));
                Messages.log("Setting up a default value for: " + att);
            }
        }
        settings.save();
    }

    static private void loadPdeThemeAttributes() {
        File pdeThemeFile = Theme.getSketchbookFile();

        String[] lines = PApplet.loadStrings(pdeThemeFile);
        if (lines != null) {
            for (String line : lines) {
                if ((line.length() == 0) || (line.charAt(0) == '#')) // comments
                    continue;

                int equals = line.indexOf('=');
                if (equals != -1) {
                    String key = line.substring(0, equals).trim();
                    pdeThemeAttributes.add(key);
                }
            }
        } else {
            Messages.err(pdeThemeFile + " could not be read");
        }
    }

    static public void load() {
        settings.load();
        BOOKMARKS_HIGHLIGHT = getBoolean("bookmarks.linehighlight");
        OCCURRENCES_HIGHLIGHT = getBoolean("occurrences.highlight");
    }
    
    static public void save() {
        settings.save();
    }

    static public String get(String attribute) {
        if (settings.getMap().containsKey(attribute)) {
            String value = settings.get(attribute);

            if (pdeThemeAttributes.contains(value)) {
                return Theme.get(value);
                
            } else if (defaultPrefs.containsKey(value)) {
                return SmartCodeTheme.get(value);
            }
            set(attribute, value);
            save();
        }
        return settings.get(attribute);
    }
    
    static public void set(String attribute, String value) {
        settings.set(attribute, value);
    }

    static public boolean getBoolean(String attribute) {
        String value = get(attribute);
        if (value == null) {
            System.err.println("Boolean not found: " + attribute);
            return false;
        }
        return Boolean.parseBoolean(value);
    }
    
    static public void setBoolean(String attribute, boolean value) {
        set(attribute, value ? "true" : "false");
    }

    static public Color getColor(String attribute) {
        Color result = null;
        String hexColor = get(attribute);

        if (hexColor != null) {
            result = new Color(Integer.parseInt(hexColor.substring(1), 16));
        }
        if (result == null) {
            System.err.println("Could not parse color " + hexColor + " for " + attribute);
        }
        return result;
    }
    
    static public void setColor(String attr, Color what) {
        set(attr, "#" + PApplet.hex(what.getRGB() & 0xffffff, 6)); //$NON-NLS-1$
    }
}
