package kelvinspatola.mode.smartcode;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import processing.app.Preferences;
import processing.app.ui.Theme;

public class SmartCodePreferences {
    static public final String[] attributes = { 
            "SmartCode.autoformat.comments", // 0
            "SmartCode.autoformat.comments.length", // 1
            "SmartCode.autoformat.strings", // 2
            "SmartCode.autoformat.strings.length", // 3
            "SmartCode.bookmarks.linehighlight", // 4
            "SmartCode.bookmarks.linehighlight.color.1", // 5
            "SmartCode.bookmarks.linehighlight.color.2", // 6
            "SmartCode.bookmarks.linehighlight.color.3", // 7
            "SmartCode.bookmarks.linehighlight.color.4", // 8
            "SmartCode.bookmarks.linehighlight.color.5", // 9
            "SmartCode.brackets.auto_close", // 10
            "SmartCode.brackets.replace_token", // 11
            "SmartCode.column.bookmark.color", // 12
            "SmartCode.column.error.color", // 13
            "SmartCode.column.occurrence.color", // 14
            "SmartCode.column.warning.color", // 15
            "SmartCode.movelines.auto_indent", // 16
            "SmartCode.occurrences.highlight", // 17
            "SmartCode.occurrences.highlight.color", // 18
            "SmartCode.templates.enabled" // 19
    };

    static protected Map<String, String> defaultPrefs = new HashMap<>();
    static {
        defaultPrefs.put(attributes[0], "true"); // autoformat.comments
        defaultPrefs.put(attributes[1], "80"); // autoformat.comments.length
        defaultPrefs.put(attributes[2], "true"); // autoformat.strings
        defaultPrefs.put(attributes[3], "80"); // autoformat.strings.length
        defaultPrefs.put(attributes[4], "true"); // bookmarks.linehighlight
        defaultPrefs.put(attributes[5], "#9B5DE5"); // bookmarks.linehighlight.color.1
        defaultPrefs.put(attributes[6], "#F15BB5"); // bookmarks.linehighlight.color.2
        defaultPrefs.put(attributes[7], "#FEE440"); // bookmarks.linehighlight.color.3
        defaultPrefs.put(attributes[8], "#00BBF9"); // bookmarks.linehighlight.color.4
        defaultPrefs.put(attributes[9], "#00F5D4"); // bookmarks.linehighlight.color.5
        defaultPrefs.put(attributes[10], "true"); // brackets.auto_close
        defaultPrefs.put(attributes[11], "true"); // brackets.replace_token
        defaultPrefs.put(attributes[12], "#00FFFF"); // column.bookmark.color
        defaultPrefs.put(attributes[13], "#FF0000"); // column.error.color
        defaultPrefs.put(attributes[14], Theme.get("header.tab.selected.color")); // column.occurrence.color
        defaultPrefs.put(attributes[15], "#FFFF00"); // column.warning.color
        defaultPrefs.put(attributes[16], "true"); // movelines.auto_indent
        defaultPrefs.put(attributes[17], "true"); // occurrences.highlight
        defaultPrefs.put(attributes[18], Theme.get("header.tab.selected.color")); // occurrences.highlight.color
        defaultPrefs.put(attributes[19], "true"); // templates.templates
    }

    static public boolean AUTOFORMAT_COMMENTS;
    static public int AUTOFORMAT_COMMENTS_LENGTH;
    static public boolean AUTOFORMAT_STRINGS;
    static public int  AUTOFORMAT_STRINGS_LENGTH;
    static public boolean BOOKMARKS_HIGHLIGHT;
    static public Color BOOKMARKS_HIGHLIGHT_COLOR_1;
    static public Color BOOKMARKS_HIGHLIGHT_COLOR_2;
    static public Color BOOKMARKS_HIGHLIGHT_COLOR_3;
    static public Color BOOKMARKS_HIGHLIGHT_COLOR_4;
    static public Color BOOKMARKS_HIGHLIGHT_COLOR_5;
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
        BOOKMARKS_HIGHLIGHT_COLOR_1 = Preferences.getColor(attributes[5]);
        BOOKMARKS_HIGHLIGHT_COLOR_2 = Preferences.getColor(attributes[6]);
        BOOKMARKS_HIGHLIGHT_COLOR_3 = Preferences.getColor(attributes[7]);
        BOOKMARKS_HIGHLIGHT_COLOR_4 = Preferences.getColor(attributes[8]);
        BOOKMARKS_HIGHLIGHT_COLOR_5 = Preferences.getColor(attributes[9]);
        BRACKETS_AUTO_CLOSE = Preferences.getBoolean(attributes[10]);
        BRACKETS_REPLACE_TOKEN = Preferences.getBoolean(attributes[11]);
        COLUMN_BOOKMARK_COLOR = Preferences.getColor(attributes[12]);
        COLUMN_ERROR_COLOR = Preferences.getColor(attributes[13]);
        COLUMN_OCCURRENCE_COLOR = Preferences.getColor(attributes[14]);
        COLUMN_WARNING_COLOR = Preferences.getColor(attributes[15]);
        MOVE_LINES_AUTO_INDENT = Preferences.getBoolean(attributes[16]);
        OCCURRENCES_HIGHLIGHT = Preferences.getBoolean(attributes[17]);
        OCCURRENCES_HIGHLIGHT_COLOR = Preferences.getColor(attributes[18]);
        TEMPLATES_ENABLED = Preferences.getBoolean(attributes[19]);
    }

    private static void checkDefaultPreferences() {
        for (String att : defaultPrefs.keySet()) {
            if (Preferences.get(att) == null) {
                Preferences.set(att, defaultPrefs.get(att));
            }
        }
    }
}
