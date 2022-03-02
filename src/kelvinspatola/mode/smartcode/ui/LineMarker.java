package kelvinspatola.mode.smartcode.ui;

public interface LineMarker {
    int getTabIndex();

    int getLine();

    int getStartOffset();

    int getStopOffset();

    String getText();
}
