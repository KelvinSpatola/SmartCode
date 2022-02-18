package kelvinspatola.mode.smartcode;

import processing.app.Preferences;

public final class Constants {
    static public final String COMMENT_TEXT = "^(?!.*\\\".*\\/\\*.*\\\")(?:.*\\/\\*.*|\\h*\\*.*)";
    static public final String STRING_TEXT = "^(?!(.*?(\\*|\\/+).*?\\\".*\\\")).*(?:\\\".*){2}";
    static public final String SPLIT_STRING_TEXT = "^\\h*\\+\\s*(?:\\\".*){2}";
    static public final String BLOCK_OPENING = "^(?!.*?\\/+.*?\\{.*|\\h*\\*.*|.*?\\\".*?\\{.*?\\\".*).*?\\{.*$";
    static public final String BLOCK_CLOSING = "^(?!.*?\\/+.*?\\}.*|.*\\/\\*.*|\\h*\\*.*).*?\\}.*";

    static public final int TAB_SIZE = Preferences.getInteger("editor.tabs.size");
    static public final String TAB = SmartCodeEditor.addSpaces(TAB_SIZE);
    static public final boolean INDENT = Preferences.getBoolean("editor.indent");

    static public final String OPEN_COMMENT = "/*";
    static public final String CLOSE_COMMENT = "*/";
    static public final char OPEN_BRACE = '{';
    static public final char CLOSE_BRACE = '}';
    static public final String LF = "\n";
    
    static public final String PIN_MARKER = "pin";

    private Constants() {
    }

}
