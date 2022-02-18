package kelvinspatola.mode.smartcode;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import processing.app.Preferences;

public class SmartCodePreferences {
    static private final String[] attributes = { 
            "SmartCode.autoformat.comments", 
            "SmartCode.autoformat.strings",
            "SmartCode.autoformat.line_length", 
            "SmartCode.brackets.auto_close", 
            "SmartCode.brackets.replace_token",
            "SmartCode.editor.bookmark.line_highlight", 
            "SmartCode.editor.bookmark.line_color", 
            "SmartCode.move_lines_auto_indent", 
            "SmartCode.templates.enabled" 
            };

    static protected Map<String, String> defaultPrefs = new HashMap<>();
    static {
        defaultPrefs.put(attributes[0], "true"); // comments
        defaultPrefs.put(attributes[1], "true"); // strings
        defaultPrefs.put(attributes[2],   "80"); // line_length
        defaultPrefs.put(attributes[3], "true"); // auto_close
        defaultPrefs.put(attributes[4], "true"); // replace_token
        defaultPrefs.put(attributes[5], "true"); // line_highlight
        defaultPrefs.put(attributes[6], "#FF82D2"); // line_color
        defaultPrefs.put(attributes[7], "true"); // auto_indent
        defaultPrefs.put(attributes[8], "true"); // templates
    }

    static public boolean AUTOFORMAT_COMMENTS;
    static public boolean AUTOFORMAT_STRINGS;
    static public int     AUTOFORMAT_LINE_LENGTH;
    static public boolean BRACKETS_AUTO_CLOSE;
    static public boolean BRACKETS_REPLACE_TOKEN;
    static public boolean BOOKMARK_HIGHLIGHT;
    static public Color   BOOKMARK_LINE_COLOR;
    static public boolean MOVE_LINES_AUTO_INDENT;
    static public boolean TEMPLATES_ENABLED;

    static public void init() {
        checkDefaultPreferences();

        AUTOFORMAT_COMMENTS = Preferences.getBoolean(attributes[0]);
        AUTOFORMAT_STRINGS = Preferences.getBoolean(attributes[1]);
        AUTOFORMAT_LINE_LENGTH = Preferences.getInteger(attributes[2]);
        BRACKETS_AUTO_CLOSE = Preferences.getBoolean(attributes[3]);
        BRACKETS_REPLACE_TOKEN = Preferences.getBoolean(attributes[4]);
        BOOKMARK_HIGHLIGHT = Preferences.getBoolean(attributes[5]);
        BOOKMARK_LINE_COLOR = Preferences.getColor(attributes[6]);
        MOVE_LINES_AUTO_INDENT = Preferences.getBoolean(attributes[7]);
        TEMPLATES_ENABLED = Preferences.getBoolean(attributes[8]);
    }

    private static void checkDefaultPreferences() {
        for (String att : defaultPrefs.keySet()) {
            if (Preferences.get(att) == null) {
                Preferences.set(att, defaultPrefs.get(att));
            }
        }
    }
}
