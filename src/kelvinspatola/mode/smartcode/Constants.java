package kelvinspatola.mode.smartcode;

import java.util.regex.Pattern;

import processing.app.Preferences;

public final class Constants {
    static public final Pattern BLOCK_OPENING = Pattern.compile("^(?!.*?\\/+.*?\\{.*|\\h*\\*.*|.*?\\\".*?\\{.*?\\\".*).*?\\{.*$");
    static public final Pattern BLOCK_CLOSING = Pattern.compile("^(?!.*?\\/+.*?\\}.*|.*\\/\\*.*|\\h*\\*.*).*?\\}.*");
    static public final Pattern COMMENT_TEXT = Pattern.compile("^(?!.*\\\".*\\/\\*.*\\\")(?:.*\\/\\*.*|\\h*\\*.*)"); // Editor
    static public final Pattern STRING_TEXT = Pattern.compile("^(?!(.*?(\\*|\\/+).*?\\\".*\\\")).*(?:\\\".*){2}"); // Constants

    static public final String OPEN_COMMENT = "/*";
    static public final String CLOSE_COMMENT = "*/";
    static public final char OPEN_BRACE = '{';
    static public final char CLOSE_BRACE = '}';
    static public final String LF = "\n";
    
    static public final boolean INDENT = Preferences.getBoolean("editor.indent");
    
    static public final String PIN_MARKER = " //<bookmark>//";

    private Constants() { }
}
