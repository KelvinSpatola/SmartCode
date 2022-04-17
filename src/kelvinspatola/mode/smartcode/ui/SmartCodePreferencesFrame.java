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
import java.awt.Component;


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
    private JLabel maxStringWidthLabel;
    private JLabel maxCommentWidthLabel;
    
    private int currTabSize;

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
        frame.setResizable(false);
        frame.setTitle("SmartCode preferences");
        frame.setBounds(100, 100, 416, 364);
        frame.setLocationRelativeTo(null);

        
        mainPane = new JPanel();
        mainPane.setPreferredSize(new Dimension(400, 400));
        mainPane.setBorder(null);
        frame.setContentPane(mainPane);
                
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
        
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        
        
        
        /*
         * GENERAL TAB
         * 
         */

        /* FORMATTING */
        
        tabGeneral = new JPanel();
        tabGeneral.setBorder(null);
        tabbedPane.addTab("General", null, tabGeneral, null);
        
        restoreBtn = new JButton("Restore defaults");
        restoreBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            }
        });
        
        JLabel formattingLabel = new JLabel("formatting");
        formattingLabel.setHorizontalAlignment(SwingConstants.LEFT);
        formattingLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        
        /* string formatting */
        
        formatStringsBox = new JCheckBox("Enable string formatting");
        formatStringsBox.addChangeListener(a -> {
            maxStringWidthLabel.setEnabled(formatStringsBox.isSelected());
            stringWidthField.setEnabled(formatStringsBox.isSelected());
        });
        
        maxStringWidthLabel = new JLabel("Max width:");
        
        stringWidthField = new JTextField();
        stringWidthField.setHorizontalAlignment(SwingConstants.LEFT);
        stringWidthField.setColumns(3);
        
        
        /* block comment formatting */
        
        formatCommentsBox = new JCheckBox("Enable block comment formatting");
        formatCommentsBox.addChangeListener(a -> {
            maxCommentWidthLabel.setEnabled(formatCommentsBox.isSelected());
            commentWidthField.setEnabled(formatCommentsBox.isSelected());
        });
        
        maxCommentWidthLabel = new JLabel("Max width:");
        
        commentWidthField = new JTextField();
        commentWidthField.setHorizontalAlignment(SwingConstants.LEFT);
        commentWidthField.setColumns(3);
        
        /* AUTO-CLOSE */
        
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
        ButtonGroup wrapButtonsGroup = new ButtonGroup();
        wrapButtonsGroup.add(replaceRadio);
        wrapButtonsGroup.add(stackRadio);
        
        indentationLabel = new JLabel("indentation");
        indentationLabel.setHorizontalAlignment(SwingConstants.LEFT);
        indentationLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        
        JLabel indentLabel = new JLabel("Indent size:");
        JLabel spacesLabel = new JLabel("spaces");
        
        indentField = new JTextField();
        indentField.setHorizontalAlignment(SwingConstants.LEFT);
        indentField.setEnabled(true);
        indentField.setColumns(1);
        
        indentMovingLinesBox = new JCheckBox("Automatically indent when moving text lines");
        
        JSeparator separator = new JSeparator();
        
        JSeparator separator_1 = new JSeparator();
        
                
        GroupLayout gl_tabGeneral = new GroupLayout(tabGeneral);
        gl_tabGeneral.setHorizontalGroup(
            gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_tabGeneral.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING, false)
                                .addComponent(formattingLabel)
                                .addComponent(formatCommentsBox)
                                .addComponent(formatStringsBox))
                            .addPreferredGap(ComponentPlacement.RELATED, 22, Short.MAX_VALUE)
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.TRAILING)
                                .addGroup(gl_tabGeneral.createSequentialGroup()
                                    .addComponent(maxStringWidthLabel)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(stringWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGroup(gl_tabGeneral.createSequentialGroup()
                                    .addComponent(maxCommentWidthLabel, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(commentWidthField, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)))
                            .addGap(6))
                        .addComponent(separator, GroupLayout.DEFAULT_SIZE, 307, Short.MAX_VALUE)
                        .addComponent(wrapSelectedTextBox)
                        .addComponent(closeCommentRegionsBox)
                        .addComponent(closeStringsAndCharsBox)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addGap(21)
                            .addComponent(replaceRadio)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(stackRadio))
                        .addComponent(closeBracketsBox)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addComponent(autoCloseLabel)
                            .addGap(96))
                        .addComponent(separator_1, GroupLayout.PREFERRED_SIZE, 307, GroupLayout.PREFERRED_SIZE)
                        .addComponent(indentMovingLinesBox)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addComponent(indentLabel)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(indentField, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(spacesLabel))
                        .addComponent(indentationLabel)
                        .addComponent(restoreBtn, Alignment.TRAILING))
                    .addContainerGap())
        );
        gl_tabGeneral.setVerticalGroup(
            gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_tabGeneral.createSequentialGroup()
                    .addGap(6)
                    .addComponent(formattingLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                        .addComponent(formatStringsBox)
                        .addComponent(maxStringWidthLabel)
                        .addComponent(stringWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(6)
                    .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                        .addComponent(formatCommentsBox)
                        .addComponent(maxCommentWidthLabel)
                        .addComponent(commentWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(11)
                    .addComponent(separator, GroupLayout.PREFERRED_SIZE, 2, GroupLayout.PREFERRED_SIZE)
                    .addGap(4)
                    .addComponent(autoCloseLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
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
                    .addGap(7)
                    .addComponent(separator_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(indentationLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                        .addComponent(indentLabel)
                        .addComponent(indentField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(spacesLabel))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(indentMovingLinesBox)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(restoreBtn)
                    .addContainerGap(25, Short.MAX_VALUE))
        );
        gl_tabGeneral.setAutoCreateGaps(true);
        gl_tabGeneral.setAutoCreateContainerGaps(true);
        tabGeneral.setLayout(gl_tabGeneral);
        
        JPanel tabBookmarks = new JPanel();
        FlowLayout flowLayout = (FlowLayout) tabBookmarks.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        tabBookmarks.setBorder(null);
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
        applyBtn.addActionListener(e -> {
            applyPrefs();
            disposeFrame();
        });
        mainButtonsPane.add(applyBtn);
        
        cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> disposeFrame());
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
        maxStringWidthLabel.setEnabled(formatStringsBox.isSelected());
        stringWidthField.setText(String.valueOf(AUTOFORMAT_STRINGS_LENGTH));

        /* block comment formatting*/
        formatCommentsBox.setSelected(AUTOFORMAT_COMMENTS);
        commentWidthField.setEnabled(formatCommentsBox.isSelected());
        maxCommentWidthLabel.setEnabled(formatCommentsBox.isSelected());
        commentWidthField.setText(String.valueOf(AUTOFORMAT_COMMENTS_LENGTH));
        
        /* wrap selected text buttons */
        wrapSelectedTextBox.setSelected(BRACKETS_AUTO_CLOSE);
        replaceRadio.setEnabled(wrapSelectedTextBox.isSelected());
        stackRadio.setEnabled(wrapSelectedTextBox.isSelected());
        replaceRadio.setSelected(BRACKETS_REPLACE_TOKEN);
        stackRadio.setSelected(!BRACKETS_REPLACE_TOKEN);
        
        /* indentation */
        currTabSize = Preferences.getInteger("editor.tabs.size");
        indentField.setText(String.valueOf(currTabSize));
        indentMovingLinesBox.setSelected(MOVE_LINES_AUTO_INDENT);
        
        
        frame.getRootPane().setDefaultButton(applyBtn);
        frame.pack();
        frame.setVisible(true);
    }
    
    protected void applyPrefs() {
        /* formatting */
        Preferences.setBoolean("SmartCode.autoformat.strings", formatStringsBox.isSelected());
        Preferences.setInteger("SmartCode.autoformat.strings.length",
                Integer.parseInt(stringWidthField.getText().trim()));

        Preferences.setBoolean("SmartCode.autoformat.comments", formatCommentsBox.isSelected());
        Preferences.setInteger("SmartCode.autoformat.comments.length",
                Integer.parseInt(commentWidthField.getText().trim()));
        
        /* auto-close */
        
        
        /* indentation */
        int newTabSize = Integer.parseInt(indentField.getText().trim());
        Preferences.setInteger("editor.tabs.size", newTabSize);
        Preferences.setBoolean("SmartCode.movelines.auto_indent", indentMovingLinesBox.isSelected());
        
        
        

        SmartCodeTheme.setBoolean("bookmarks.linehighlight", checkBookmarkHighlighting.isSelected());
        SmartCodeTheme.setBoolean("occurrences.highlight", checkOccurrencesHighlighting.isSelected());

        SmartCodeTheme.save();
        Preferences.save();
        
        boolean handleAutoFormat = (currTabSize != newTabSize);
        
        for (Editor editor : base.getEditors()) {
            editor.applyPreferences();
            if (handleAutoFormat) {
                editor.handleAutoFormat();
            }
        }
    }
}
