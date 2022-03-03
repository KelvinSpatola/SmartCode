package kelvinspatola.mode.smartcode;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import processing.app.Preferences;
import processing.app.ui.Theme;

public class SmartCodePreferences {
    static private final String[] attributes = { 
            "SmartCode.autoformat.comments", // 0
            "SmartCode.autoformat.comments.length", // 1
            "SmartCode.autoformat.strings", // 2
            "SmartCode.autoformat.strings.length", // 3
            "SmartCode.bookmarks.linehighlight", // 4
            "SmartCode.bookmarks.linehighlight.color", // 5
            "SmartCode.brackets.auto_close", // 6
            "SmartCode.brackets.replace_token", // 7
            "SmartCode.column.bookmark.color", // 8
            "SmartCode.column.error.color", // 9
            "SmartCode.column.occurrence.color", // 10
            "SmartCode.column.warning.color", // 11
            "SmartCode.movelines.auto_indent", // 12
            "SmartCode.occurrences.highlight", // 13
            "SmartCode.occurrences.highlight.color", // 14
            "SmartCode.templates.enabled" // 15
    };

    static protected Map<String, String> defaultPrefs = new HashMap<>();
    static {
        defaultPrefs.put(attributes[0], "true"); // autoformat.comments
        defaultPrefs.put(attributes[1], "80"); // autoformat.comments.length
        defaultPrefs.put(attributes[2], "true"); // autoformat.strings
        defaultPrefs.put(attributes[3], "80"); // autoformat.strings.length
        defaultPrefs.put(attributes[4], "true"); // bookmarks.linehighlight
        defaultPrefs.put(attributes[5], "#FF8288"); // bookmarks.linehighlight.color
        defaultPrefs.put(attributes[6], "true"); // brackets.auto_close
        defaultPrefs.put(attributes[7], "true"); // brackets.replace_token
        defaultPrefs.put(attributes[8], "#00FFFF"); // column.bookmark.color
        defaultPrefs.put(attributes[9], "#FF0000"); // column.error.color
        defaultPrefs.put(attributes[10], Theme.get("editor.linehighlight.color")); // column.occurrence.color
        defaultPrefs.put(attributes[11], "#FFFF00"); // column.warning.color
        defaultPrefs.put(attributes[12], "true"); // movelines.auto_indent
        defaultPrefs.put(attributes[13], "true"); // occurrences.highlight
        defaultPrefs.put(attributes[14], Theme.get("editor.linehighlight.color")); // occurrences.highlight.color
        defaultPrefs.put(attributes[15], "true"); // templates.templates
    }

    static public boolean AUTOFORMAT_COMMENTS;
    static public int AUTOFORMAT_COMMENTS_LENGTH;
    static public boolean AUTOFORMAT_STRINGS;
    static public int  AUTOFORMAT_STRINGS_LENGTH;
    static public boolean BOOKMARKS_HIGHLIGHT;
    static public Color BOOKMARKS_HIGHLIGHT_COLOR;
    static public boolean BRACKETS_AUTO_CLOSE;
    static public boolean BRACKETS_REPLACE_TOKEN;
    static public Color COLUMN_BOOKMARK_COLOR;
    static public Color COLUMN_ERROR_COLOR;
    static public Color COLUMN_OCCURRENCE_COLOR;
    static public Color COLUMN_WARNING_COLOR;
    static public boolean MOVE_LINES_AUTO_INDENT;
    static public boolean OCCURRENCES_HIGHLIGHT;
    static public Color OCCURRENCES_HIGHLIGHT_COLOR;
    static public boolean TEMPLATES_ENABLED;

    static public void init() {
        checkDefaultPreferences();

        AUTOFORMAT_COMMENTS = Preferences.getBoolean(attributes[0]);
        AUTOFORMAT_COMMENTS_LENGTH = Preferences.getInteger(attributes[1]);
        AUTOFORMAT_STRINGS = Preferences.getBoolean(attributes[2]);
        AUTOFORMAT_STRINGS_LENGTH = Preferences.getInteger(attributes[3]);
        BOOKMARKS_HIGHLIGHT = Preferences.getBoolean(attributes[4]);
        BOOKMARKS_HIGHLIGHT_COLOR = Preferences.getColor(attributes[5]);
        BRACKETS_AUTO_CLOSE = Preferences.getBoolean(attributes[6]);
        BRACKETS_REPLACE_TOKEN = Preferences.getBoolean(attributes[7]);
        COLUMN_BOOKMARK_COLOR = Preferences.getColor(attributes[8]);
        COLUMN_ERROR_COLOR = Preferences.getColor(attributes[9]);
        COLUMN_OCCURRENCE_COLOR = Preferences.getColor(attributes[10]);
        COLUMN_WARNING_COLOR = Preferences.getColor(attributes[11]);
        MOVE_LINES_AUTO_INDENT = Preferences.getBoolean(attributes[12]);
        OCCURRENCES_HIGHLIGHT = Preferences.getBoolean(attributes[13]);
        OCCURRENCES_HIGHLIGHT_COLOR = Preferences.getColor(attributes[14]);
        TEMPLATES_ENABLED = Preferences.getBoolean(attributes[15]);
    }

    private static void checkDefaultPreferences() {
        for (String att : defaultPrefs.keySet()) {
            if (Preferences.get(att) == null) {
                Preferences.set(att, defaultPrefs.get(att));
            }
        }
    }
}
