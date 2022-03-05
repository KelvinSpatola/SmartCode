package kelvinspatola.mode.smartcode.ui;

public interface LineMarker {
    Class<?> getParent();
    
    int getTabIndex();

    int getLine();

    int getStartOffset();

    int getStopOffset();

    String getText();
}
