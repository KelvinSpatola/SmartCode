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
import processing.app.Platform;
import processing.app.Preferences;
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
        defaultPrefs.put(attributes[0], "footer.icon.selected.color"); // bookmarks.icon.color
        defaultPrefs.put(attributes[1], "true"); // bookmarks.linehighlight
        defaultPrefs.put(attributes[2], "#9B5DE57D"); // bookmarks.linehighlight.color.1
        defaultPrefs.put(attributes[3], "#F15BB57D"); // bookmarks.linehighlight.color.2
        defaultPrefs.put(attributes[4], "#FEE4407D"); // bookmarks.linehighlight.color.3
        defaultPrefs.put(attributes[5], "#00BBF97D"); // bookmarks.linehighlight.color.4
        defaultPrefs.put(attributes[6], "#00F5D47D"); // bookmarks.linehighlight.color.5
        defaultPrefs.put(attributes[7], "#00FFFF"); // column.bookmark.color
        defaultPrefs.put(attributes[8], "#FF0000"); // column.error.color
        defaultPrefs.put(attributes[9], "header.tab.selected.color"); // column.occurrence.color
        defaultPrefs.put(attributes[10], "#FFFF00"); // column.warning.color
        defaultPrefs.put(attributes[11], "true"); // occurrences.highlight
        defaultPrefs.put(attributes[12], "header.tab.selected.color"); // occurrences.highlight.color
    }

    static private Settings theme;
    static private Set<String> pdeThemeAttributes = new HashSet<>();
    static public boolean BOOKMARKS_HIGHLIGHT;
    static public boolean OCCURRENCES_HIGHLIGHT;

    static public void init() {
        try {
            File inputFile = ensureExistence(getThemeFile());
            theme = new Settings(inputFile);
            ensureAttributes();

        } catch (IOException e) {
            e.printStackTrace();
        }
        loadPdeThemeAttributes();
        
        BOOKMARKS_HIGHLIGHT = getBoolean(attributes[1]);
        OCCURRENCES_HIGHLIGHT = getBoolean(attributes[11]);
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
//                    String value = line.substring(equals + 1).trim();
                    pdeThemeAttributes.add(key);
                }
            }
        } else {
            Messages.err(pdeThemeFile + " could not be read");
        }
    }

//    static public Color updateColor(String attribute) {
//        String key = "";
//
//        if (theme.getMap().containsKey(attribute)) {
//            if (attribute.equals("column.occurrence.color") || attribute.equals("occurrences.highlight.color")) {
//                key = "header.tab.selected.color";
//
//            } else if (attribute.equals("bookmarks.icon.color")) {
//                key = "footer.icon.selected.color";
//            }
//        }
//        return updateColor(attribute, Theme.get(key));
//    }

    static public void load() {
        theme.load();
        BOOKMARKS_HIGHLIGHT = getBoolean(attributes[1]);
        OCCURRENCES_HIGHLIGHT = getBoolean(attributes[11]);
    }

    static public String get(String attribute) {
        if (theme.getMap().containsKey(attribute)) {
            String value = theme.get(attribute);

            if (pdeThemeAttributes.contains(value)) {
                return Theme.get(value);
            }
            theme.set(attribute, value);
            theme.save();
        }
        return theme.get(attribute);
    }

    static public boolean getBoolean(String attribute) {
        String value = get(attribute);
        if (value == null) {
            System.err.println("Boolean not found: " + attribute);
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    static public Color getColor(String attribute) {
        Color result = null;
        String hexColor = get(attribute);

        if (hexColor != null) {
            result = hexToColor(hexColor);
        }
        if (result == null) {
            System.err.println("Could not parse color " + hexColor + " for " + attribute);
        }
        return result;
    }

    /**
     * Converts a hex string to a color. If it can't be converted null is returned.
     * 
     * @param hex (i.e. #RRGGBBAA or RRGGBB)
     * @return Color
     */
    static private Color hexToColor(String hex) throws IllegalArgumentException {
        if (!hex.startsWith("#") || !(hex.length() == 7 || hex.length() == 9)) {
            throw new IllegalArgumentException("Hex color string is incorrect!");
        }

        hex = hex.replace("#", "");
        switch (hex.length()) {
        case 6:
            return new Color(Integer.valueOf(hex.substring(0, 2), 16), // red
                    Integer.valueOf(hex.substring(2, 4), 16), // green
                    Integer.valueOf(hex.substring(4, 6), 16)); // blue
        case 8:
            return new Color(Integer.valueOf(hex.substring(0, 2), 16), // red
                    Integer.valueOf(hex.substring(2, 4), 16), // green
                    Integer.valueOf(hex.substring(4, 6), 16), // blue
                    Integer.valueOf(hex.substring(6, 8), 16)); // alpha
        default:
            return null;
        }
    }
}
