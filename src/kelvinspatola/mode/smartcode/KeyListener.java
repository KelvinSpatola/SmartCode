package kelvinspatola.mode.smartcode;

import java.awt.event.KeyEvent;

public interface KeyListener {
    boolean handlePressed(KeyEvent e);
    boolean handleTyped(KeyEvent e);
}