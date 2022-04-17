package kelvinspatola.mode.smartcode;

import java.util.HashMap;
import java.util.Map;

import processing.app.Preferences;

public class SmartCodePreferences {
    static public boolean AUTOFORMAT_COMMENTS;
    static public int AUTOFORMAT_COMMENTS_LENGTH;
    static public boolean AUTOFORMAT_STRINGS;
    static public int  AUTOFORMAT_STRINGS_LENGTH;
    static public boolean BRACKETS_AUTO_CLOSE;
    static public boolean BRACKETS_REPLACE_TOKEN;
    static public boolean MOVE_LINES_AUTO_INDENT;
    static public boolean TEMPLATES_ENABLED;
    
    static public final String[] attributes = { 
            "SmartCode.autoformat.comments", // 0
            "SmartCode.autoformat.comments.length", // 1
            "SmartCode.autoformat.strings", // 2
            "SmartCode.autoformat.strings.length", // 3
            "SmartCode.brackets.auto_close", // 4
            "SmartCode.brackets.replace_token", // 5
            "SmartCode.movelines.auto_indent", // 6
            "SmartCode.templates.enabled" // 7
    };

    static protected final Map<String, String> defaultPrefs = new HashMap<>();
    static {
        defaultPrefs.put(attributes[0], "true"); // autoformat.comments
        defaultPrefs.put(attributes[1], "80"); // autoformat.comments.length
        defaultPrefs.put(attributes[2], "true"); // autoformat.strings
        defaultPrefs.put(attributes[3], "80"); // autoformat.strings.length
        defaultPrefs.put(attributes[4], "true"); // brackets.auto_close
        defaultPrefs.put(attributes[5], "true"); // brackets.replace_token        
        defaultPrefs.put(attributes[6], "true"); // movelines.auto_indent
        defaultPrefs.put(attributes[7], "true"); // templates.enabled
    }

    static public void init() {
        checkDefaultPreferences();
        load();
    }
    
    static public void load() {
        AUTOFORMAT_COMMENTS = Preferences.getBoolean(attributes[0]);
        AUTOFORMAT_COMMENTS_LENGTH = Preferences.getInteger(attributes[1]);
        AUTOFORMAT_STRINGS = Preferences.getBoolean(attributes[2]);
        AUTOFORMAT_STRINGS_LENGTH = Preferences.getInteger(attributes[3]);
        BRACKETS_AUTO_CLOSE = Preferences.getBoolean(attributes[4]);
        BRACKETS_REPLACE_TOKEN = Preferences.getBoolean(attributes[5]);
        MOVE_LINES_AUTO_INDENT = Preferences.getBoolean(attributes[6]);
        TEMPLATES_ENABLED = Preferences.getBoolean(attributes[7]);
    }

    private static void checkDefaultPreferences() {
        for (String att : defaultPrefs.keySet()) {
            if (Preferences.get(att) == null) {
                Preferences.set(att, defaultPrefs.get(att));
            }
        }
    }
}
