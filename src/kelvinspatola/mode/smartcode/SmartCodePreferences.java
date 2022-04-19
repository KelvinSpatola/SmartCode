package kelvinspatola.mode.smartcode;

import java.util.HashMap;
import java.util.Map;

import processing.app.Preferences;

public class SmartCodePreferences {
    static public boolean AUTOCLOSE_BLOCK_COMMENTS;
    static public boolean AUTOCLOSE_BRACKETS;
    static public boolean AUTOCLOSE_QUOTES;
    static public boolean AUTOCLOSE_WRAP_TEXT;
    static public boolean AUTOCLOSE_WRAP_REPLACE;
    static public boolean AUTOFORMAT_COMMENTS;
    static public int     AUTOFORMAT_COMMENTS_LENGTH;
    static public boolean AUTOFORMAT_STRINGS;
    static public int     AUTOFORMAT_STRINGS_LENGTH;
    static public boolean MOVE_LINES_AUTO_INDENT;
    static public boolean TEMPLATES_ENABLED;
    
    static public final String[] attributes = { 
            "SmartCode.auto_close.block_comments", // 0
            "SmartCode.auto_close.brackets", // 1
            "SmartCode.auto_close.quotes", // 2
            "SmartCode.auto_close.wrap_text", // 3
            "SmartCode.auto_close.wrap_text.replace", // 4
            "SmartCode.autoformat.comments", // 5
            "SmartCode.autoformat.comments.length", // 6
            "SmartCode.autoformat.strings", // 7
            "SmartCode.autoformat.strings.length", // 8
            "SmartCode.movelines.auto_indent", // 9
            "SmartCode.templates.enabled" // 10
    };

    static protected final Map<String, String> defaultPrefs = new HashMap<>();
    static {
        defaultPrefs.put(attributes[0], "true"); // auto_close.block_comments
        defaultPrefs.put(attributes[1], "true"); // auto_close.brackets
        defaultPrefs.put(attributes[2], "true"); // auto_close.quotes
        defaultPrefs.put(attributes[3], "true"); // auto_close.wrap_text
        defaultPrefs.put(attributes[4], "true"); // auto_close.wrap_text.replace     
        defaultPrefs.put(attributes[5], "true"); // autoformat.comments
        defaultPrefs.put(attributes[6], "80"); // autoformat.comments.length
        defaultPrefs.put(attributes[7], "true"); // autoformat.strings
        defaultPrefs.put(attributes[8], "80"); // autoformat.strings.length
        defaultPrefs.put(attributes[9], "true"); // movelines.auto_indent
        defaultPrefs.put(attributes[10], "true"); // templates.enabled
    }

    static public void init() {
        checkDefaultPreferences();
        load();
    }
    
    static public void load() {
        AUTOCLOSE_BLOCK_COMMENTS = Preferences.getBoolean(attributes[0]);
        AUTOCLOSE_BRACKETS = Preferences.getBoolean(attributes[1]);
        AUTOCLOSE_QUOTES = Preferences.getBoolean(attributes[2]);
        AUTOCLOSE_WRAP_TEXT = Preferences.getBoolean(attributes[3]);
        AUTOCLOSE_WRAP_REPLACE = Preferences.getBoolean(attributes[4]);
        AUTOFORMAT_COMMENTS = Preferences.getBoolean(attributes[5]);
        AUTOFORMAT_COMMENTS_LENGTH = Preferences.getInteger(attributes[6]);
        AUTOFORMAT_STRINGS = Preferences.getBoolean(attributes[7]);
        AUTOFORMAT_STRINGS_LENGTH = Preferences.getInteger(attributes[8]);
        MOVE_LINES_AUTO_INDENT = Preferences.getBoolean(attributes[9]);
        TEMPLATES_ENABLED = Preferences.getBoolean(attributes[10]);
    }

    private static void checkDefaultPreferences() {
        for (String att : defaultPrefs.keySet()) {
            if (Preferences.get(att) == null) {
                Preferences.set(att, defaultPrefs.get(att));
            }
        }
    }
}
