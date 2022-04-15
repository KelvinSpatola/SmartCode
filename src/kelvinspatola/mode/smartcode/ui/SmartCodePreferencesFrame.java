package kelvinspatola.mode.smartcode.ui;

import java.awt.EventQueue;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import processing.app.Base;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;
import processing.app.Preferences;

import kelvinspatola.mode.smartcode.Constants;
import static kelvinspatola.mode.smartcode.SmartCodePreferences.*;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.event.ActionListener;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;


public class SmartCodePreferencesFrame {
    private Base base;
    private JFrame frame;
    private JPanel mainPane;
    private JCheckBox checkOccurrencesHighlighting;
    private JTabbedPane tabbedPane;
    private JPanel tabGeneral;
    private JButton applyBtn;
    private JButton cancelBtn;
    private JButton restoreBtn;
    private JTextField stringWidthField;
    private JTextField commentWidthField;
    private JCheckBox formatCommentsBox;
    private JCheckBox formatStringsBox;
    private JCheckBox closeBracketsBox;
    private JCheckBox closeStringsAndCharsBox;
    private JCheckBox closeCommentRegionsBox;
    private JCheckBox wrapSelectedTextBox;
    private JRadioButton replaceRadio;
    private JRadioButton stackRadio;
    private JCheckBox checkBookmarkHighlighting;
    private JLabel indentationLabel;
    private JTextField indentField;
    private JCheckBox indentMovingLinesBox;

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
        
        frame = new JFrame("SmartCode Preferences");
        frame.setTitle("SmartCode preferences");
        frame.setBounds(100, 100, 433, 346);
        frame.setLocationRelativeTo(null);

        
        mainPane = new JPanel();
        mainPane.setPreferredSize(new Dimension(400, 380));
        mainPane.setBorder(null);
        frame.setContentPane(mainPane);
                
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
        
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        
        
        
        /*
         * GENERAL TAB
         * 
         */
        ButtonGroup wrapButtonsGroup = new ButtonGroup();
        
        /*
         * GRID
         */
        
        tabGeneral = new JPanel();
        tabGeneral.setBorder(null);
        tabGeneral.setBackground(UIManager.getColor("TabbedPane.background"));
        tabbedPane.addTab("General", null, tabGeneral, null);
        
        restoreBtn = new JButton("Restore defaults");
        restoreBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            }
        });
        
        JLabel formattingLabel = new JLabel("formatting");
        formattingLabel.setHorizontalAlignment(SwingConstants.LEFT);
        formattingLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        
        formatStringsBox = new JCheckBox("Enable string formatting");
        formatStringsBox.addChangeListener(a -> stringWidthField.setEnabled(formatStringsBox.isSelected()));
        
        stringWidthField = new JTextField();
        stringWidthField.setHorizontalAlignment(SwingConstants.LEFT);
        stringWidthField.setColumns(3);
        
        formatCommentsBox = new JCheckBox("Enable block comment formatting");
        formatCommentsBox.addChangeListener(a -> commentWidthField.setEnabled(formatCommentsBox.isSelected()));
        
        commentWidthField = new JTextField();
        commentWidthField.setHorizontalAlignment(SwingConstants.LEFT);
        commentWidthField.setColumns(3);
        
        
        JLabel maxStringWidthLabel = new JLabel("Max width:");
        JLabel maxCommentWidthLabel = new JLabel("Max width:");
        
        JLabel autoCloseLabel = new JLabel("auto-close");
        autoCloseLabel.setHorizontalAlignment(SwingConstants.LEFT);
        autoCloseLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        
        closeBracketsBox = new JCheckBox("{brace}, (parentesis), [square] and <angle> brackets");
        closeStringsAndCharsBox = new JCheckBox("\"strings\" and 'chars'");
        closeCommentRegionsBox = new JCheckBox("Javadoc and comment regions");
        
        wrapSelectedTextBox = new JCheckBox("Wrap selected text");
        wrapSelectedTextBox.addChangeListener(a -> {
            replaceRadio.setEnabled(wrapSelectedTextBox.isSelected());
            stackRadio.setEnabled(wrapSelectedTextBox.isSelected());
        });
        
                replaceRadio = new JRadioButton("replace");
                stackRadio = new JRadioButton("stack");
                wrapButtonsGroup.add(replaceRadio);
                wrapButtonsGroup.add(stackRadio);
                
                indentationLabel = new JLabel("indentation");
                indentationLabel.setHorizontalAlignment(SwingConstants.LEFT);
                indentationLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
                
                JLabel indentLabel = new JLabel("indent size:");
                JLabel spacesLabel = new JLabel("spaces");
                
                indentField = new JTextField();
                indentField.setHorizontalAlignment(SwingConstants.LEFT);
                indentField.setEnabled(true);
                indentField.setColumns(1);
                
                indentMovingLinesBox = new JCheckBox("Automatically indent when moving text lines");
                
                
                GroupLayout gl_tabGeneral = new GroupLayout(tabGeneral);
                gl_tabGeneral.setHorizontalGroup(
                    gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                                .addComponent(indentMovingLinesBox)
                                .addGroup(gl_tabGeneral.createSequentialGroup()
                                    .addGap(10)
                                    .addComponent(indentLabel)
                                    .addPreferredGap(ComponentPlacement.UNRELATED)
                                    .addComponent(indentField, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(spacesLabel))
                                .addComponent(wrapSelectedTextBox)
                                .addComponent(closeCommentRegionsBox)
                                .addComponent(closeStringsAndCharsBox)
                                .addComponent(indentationLabel)
                                .addGroup(gl_tabGeneral.createSequentialGroup()
                                    .addGap(21)
                                    .addComponent(replaceRadio)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(stackRadio))
                                .addComponent(closeBracketsBox)
                                .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING, false)
                                    .addComponent(formattingLabel)
                                    .addGroup(gl_tabGeneral.createSequentialGroup()
                                        .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                                            .addComponent(formatCommentsBox)
                                            .addComponent(formatStringsBox))
                                        .addGap(26)
                                        .addGroup(gl_tabGeneral.createParallelGroup(Alignment.TRAILING)
                                            .addGroup(gl_tabGeneral.createSequentialGroup()
                                                .addComponent(maxStringWidthLabel)
                                                .addGap(4)
                                                .addComponent(stringWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                            .addGroup(gl_tabGeneral.createSequentialGroup()
                                                .addComponent(maxCommentWidthLabel, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
                                                .addGap(4)
                                                .addComponent(commentWidthField, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE))))
                                    .addComponent(autoCloseLabel)
                                    .addComponent(restoreBtn, Alignment.TRAILING)))
                            .addGap(12))
                );
                gl_tabGeneral.setVerticalGroup(
                    gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addGap(6)
                            .addComponent(formattingLabel)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                                .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(maxStringWidthLabel)
                                    .addComponent(stringWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addComponent(formatStringsBox))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                                .addComponent(formatCommentsBox)
                                .addComponent(maxCommentWidthLabel)
                                .addComponent(commentWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(ComponentPlacement.UNRELATED)
                            .addComponent(autoCloseLabel)
                            .addPreferredGap(ComponentPlacement.RELATED, 6, Short.MAX_VALUE)
                            .addComponent(closeBracketsBox)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(closeStringsAndCharsBox)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(closeCommentRegionsBox)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(wrapSelectedTextBox)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                                .addComponent(replaceRadio)
                                .addComponent(stackRadio))
                            .addPreferredGap(ComponentPlacement.UNRELATED)
                            .addComponent(indentationLabel)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                                .addComponent(indentLabel)
                                .addComponent(indentField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(spacesLabel))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(indentMovingLinesBox)
                            .addGap(18)
                            .addComponent(restoreBtn)
                            .addContainerGap())
                );
                gl_tabGeneral.setAutoCreateGaps(true);
                gl_tabGeneral.setAutoCreateContainerGaps(true);
                tabGeneral.setLayout(gl_tabGeneral);
        
        JPanel tabBookmarks = new JPanel();
        FlowLayout flowLayout = (FlowLayout) tabBookmarks.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        tabBookmarks.setBorder(null);
        tabBookmarks.setBackground(UIManager.getColor("TabbedPane.background"));
        tabbedPane.addTab("Bookmarks", null, tabBookmarks, null);
        
        checkBookmarkHighlighting = new JCheckBox("Highlight bookmarked lines");
        checkBookmarkHighlighting.setHorizontalAlignment(SwingConstants.LEFT);
        tabBookmarks.add(checkBookmarkHighlighting);
        checkOccurrencesHighlighting = new JCheckBox("Highlight occurrences");
        checkOccurrencesHighlighting.setHorizontalAlignment(SwingConstants.LEFT);
        tabBookmarks.add(checkOccurrencesHighlighting);
        mainPane.add(tabbedPane);
        
        
        final int buttonWidth = 80;

        JPanel mainButtonsPane = new JPanel();
        mainPane.add(mainButtonsPane);
        
        applyBtn = new JButton("Apply");
        mainButtonsPane.add(applyBtn);
        
        cancelBtn = new JButton("Cancel");
        mainButtonsPane.add(cancelBtn);
        
        // closing the window is same as hitting cancel button
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                disposeFrame();
            }
        });
        
        ActionListener disposer = actionEvent -> disposeFrame();

        Toolkit.registerWindowCloseKeys(frame.getRootPane(), disposer);
        Toolkit.setIcon(frame);
        frame.pack();

        // handle window closing commands for ctrl/cmd-W or hitting ESC.
        mainPane.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                KeyStroke wc = Toolkit.WINDOW_CLOSE_KEYSTROKE;
                if ((e.getKeyCode() == KeyEvent.VK_ESCAPE) || (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
                    disposeFrame();
                }
            }
        });
    }

    protected void disposeFrame() {
        frame.dispose();
    }
    
    public void showFrame() {
        /* string formatting*/
        formatStringsBox.setSelected(AUTOFORMAT_STRINGS);
        stringWidthField.setEnabled(formatStringsBox.isSelected());
        stringWidthField.setText(String.valueOf(AUTOFORMAT_STRINGS_LENGTH));

        /* block comment formatting*/
        formatCommentsBox.setSelected(AUTOFORMAT_COMMENTS);
        commentWidthField.setEnabled(formatCommentsBox.isSelected());
        commentWidthField.setText(String.valueOf(AUTOFORMAT_COMMENTS_LENGTH));
        
        /* wrap selected text buttons */
        wrapSelectedTextBox.setSelected(BRACKETS_AUTO_CLOSE);
        replaceRadio.setEnabled(wrapSelectedTextBox.isSelected());
        stackRadio.setEnabled(wrapSelectedTextBox.isSelected());
        replaceRadio.setSelected(BRACKETS_REPLACE_TOKEN);
        stackRadio.setSelected(!BRACKETS_REPLACE_TOKEN);
        
        /* indentation */
        indentField.setText(String.valueOf(Constants.TAB_SIZE));
        indentMovingLinesBox.setSelected(MOVE_LINES_AUTO_INDENT);
        
        
        frame.getRootPane().setDefaultButton(applyBtn);
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
