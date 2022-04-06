package kelvinspatola.mode.smartcode.ui;

import java.awt.EventQueue;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import processing.app.Base;
import processing.app.Language;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SmartCodePreferencesFrame {
    
    Base base;
    JFrame frame;
    private JPanel contentPane;
    GroupLayout layout;
    
    JCheckBox checkBookmarkHighlighting;
    JCheckBox checkOccurrencesHighlighting;
    JButton okButton;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    SmartCodePreferencesFrame SmartPrefs = new SmartCodePreferencesFrame();
                    SmartPrefs.showFrame();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    
    public SmartCodePreferencesFrame() {
        this(null);
    }
    
    public SmartCodePreferencesFrame(Base base) {
        this.base = base;
        
//        Toolkit.setIcon(this);
        frame = new JFrame("SmartCode Preferences");
        frame.setResizable(false);
        frame.setBounds(100, 100, 300, 300);
        frame.setLocationRelativeTo(null);

        
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.setContentPane(contentPane);
        
        checkBookmarkHighlighting = new JCheckBox("Highlight bookmarked lines");
        checkOccurrencesHighlighting = new JCheckBox("Highlight occurrences");
        
        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                applyPrefs();
                disposeFrame();
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> disposeFrame());
                
        
        
        
        final int buttonWidth = 80;//Toolkit.getButtonWidth();
        layout = new GroupLayout(contentPane);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(checkBookmarkHighlighting, GroupLayout.PREFERRED_SIZE, 186, GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(Alignment.TRAILING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(checkOccurrencesHighlighting, GroupLayout.PREFERRED_SIZE, 157, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(ComponentPlacement.RELATED, 19, Short.MAX_VALUE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(okButton, 80, 80, 80)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(cancelButton, 80, 80, 80)))
                            .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(checkBookmarkHighlighting)
                    .addGap(3)
                    .addComponent(checkOccurrencesHighlighting)
                    .addPreferredGap(ComponentPlacement.RELATED, 179, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cancelButton)
                        .addComponent(okButton)))
        );
        contentPane.setLayout(layout);
        
        // closing the window is same as hitting cancel button
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                disposeFrame();
            }
        });
        
        ActionListener disposer = actionEvent -> disposeFrame();

//        Toolkit.registerWindowCloseKeys(frame.getRootPane(), disposer);
//        Toolkit.setIcon(frame);
        frame.pack();

//        // handle window closing commands for ctrl/cmd-W or hitting ESC.
//        contentPane.addKeyListener(new KeyAdapter() {
//            public void keyPressed(KeyEvent e) {
//                KeyStroke wc = Toolkit.WINDOW_CLOSE_KEYSTROKE;
//                if ((e.getKeyCode() == KeyEvent.VK_ESCAPE) || (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
//                    disposeFrame();
//                }
//            }
//        });
    }

    protected void disposeFrame() {
        frame.dispose();
    }
    
    public void showFrame() {
        checkBookmarkHighlighting.setSelected(SmartCodeTheme.BOOKMARKS_HIGHLIGHT);
        checkOccurrencesHighlighting.setSelected(SmartCodeTheme.OCCURRENCES_HIGHLIGHT);


        frame.getRootPane().setDefaultButton(okButton);
        frame.pack();
        frame.setVisible(true);
    }
    
    protected void applyPrefs() {
        SmartCodeTheme.setBoolean("bookmarks.linehighlight", checkBookmarkHighlighting.isSelected());
        SmartCodeTheme.setBoolean("occurrences.highlight", checkOccurrencesHighlighting.isSelected());
        
        SmartCodeTheme.save();
        
        for (Editor editor : base.getEditors()) {
            editor.applyPreferences();
        }
    }
}
