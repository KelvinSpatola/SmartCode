package kelvinspatola.mode.smartcode;

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
            "SmartCode.move_lines_auto_indent", 
            "SmartCode.templates.enabled" 
            };

    static protected Map<String, String> defaultPrefs = new HashMap<>();
    static {
        defaultPrefs.put(attributes[0], "true");
        defaultPrefs.put(attributes[1], "true");
        defaultPrefs.put(attributes[2], "80");
        defaultPrefs.put(attributes[3], "true");
        defaultPrefs.put(attributes[4], "true");
        defaultPrefs.put(attributes[5], "true");
        defaultPrefs.put(attributes[6], "true");
    }

    static public boolean AUTOFORMAT_COMMENTS;
    static public boolean AUTOFORMAT_STRINGS;
    static public int     AUTOFORMAT_LINE_LENGTH;
    static public boolean BRACKETS_AUTO_CLOSE;
    static public boolean BRACKETS_REPLACE_TOKEN;
    static public boolean MOVE_LINES_AUTO_INDENT;
    static public boolean TEMPLATES_ENABLED;

    static public void init() {
        checkDefaultPreferences();

        AUTOFORMAT_COMMENTS = Preferences.getBoolean(attributes[0]);
        AUTOFORMAT_STRINGS = Preferences.getBoolean(attributes[1]);
        AUTOFORMAT_LINE_LENGTH = Preferences.getInteger(attributes[2]);
        BRACKETS_AUTO_CLOSE = Preferences.getBoolean(attributes[3]);
        BRACKETS_REPLACE_TOKEN = Preferences.getBoolean(attributes[4]);
        MOVE_LINES_AUTO_INDENT = Preferences.getBoolean(attributes[5]);
        TEMPLATES_ENABLED = Preferences.getBoolean(attributes[6]);
    }

    protected static void checkDefaultPreferences() {
        for (String att : defaultPrefs.keySet()) {
            if (Preferences.get(att) == null) {
                Preferences.set(att, defaultPrefs.get(att));
            }
        }
    }
}
