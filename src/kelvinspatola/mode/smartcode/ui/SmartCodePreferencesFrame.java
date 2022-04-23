package kelvinspatola.mode.smartcode.ui;

import java.awt.EventQueue;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import processing.app.Base;
import processing.app.Language;
import processing.app.ui.ColorChooser;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;
import processing.core.PApplet;
import processing.app.Preferences;

import static kelvinspatola.mode.smartcode.SmartCodePreferences.*;
import static kelvinspatola.mode.smartcode.ui.SmartCodeTheme.*;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.Component;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;


public class SmartCodePreferencesFrame {
    private Base base;
    private JFrame frame;
    private JPanel mainPane;
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
    private JCheckBox closeQuotesBox;
    private JCheckBox closeBlockCommentsBox;
    private JCheckBox wrapSelectedTextBox;
    private JRadioButton replaceRadio;
    private JRadioButton stackRadio;
    private JTextField indentField;
    private JCheckBox indentMovingLinesBox;
    private JLabel maxStringWidthLabel;
    private JLabel maxCommentWidthLabel;
    
    private JCheckBox bookmarkHighlightingBox;
    private JTextField colorField_1;
    private JTextField colorField_2;
    private JTextField colorField_3;
    private JTextField colorField_4;
    private JTextField colorField_5;
    private JTextField iconColorField;
    private JTextField colorPalette_1;
    private JTextField colorPalette_2;
    private JTextField colorPalette_3;
    private JTextField colorPalette_4;
    private JTextField colorPalette_5;
    private JTextField iconColorPalette;
    private ColorChooser iconSelector;
    private ColorChooser colorSelector_1;
    private ColorChooser colorSelector_2;
    private ColorChooser colorSelector_3;
    private ColorChooser colorSelector_4;
    private ColorChooser colorSelector_5;
    
    private int currTabSize;
    private JPanel tabOccurrences;
    private JCheckBox occurrencesHighlightingBox;
    

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    SmartCodePreferencesFrame SmartPrefs = new SmartCodePreferencesFrame();
                    //SmartPrefs.showFrame();
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
        tabbedPane.addTab("General", null, tabGeneral, null);
        
        restoreBtn = new JButton("Restore defaults");
        restoreBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                
            }
        });
        
        JLabel formattingLabel = new JLabel("formatting");
        formattingLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        
        /* string formatting */
        
        formatStringsBox = new JCheckBox("Enable string formatting");
        formatStringsBox.addChangeListener(a -> {
            maxStringWidthLabel.setEnabled(formatStringsBox.isSelected());
            stringWidthField.setEnabled(formatStringsBox.isSelected());
        });
        
        maxStringWidthLabel = new JLabel("max width:");
        
        stringWidthField = new JTextField();
        stringWidthField.setColumns(3);
        
        
        /* block comment formatting */
        
        formatCommentsBox = new JCheckBox("Enable block comment formatting");
        formatCommentsBox.addChangeListener(a -> {
            maxCommentWidthLabel.setEnabled(formatCommentsBox.isSelected());
            commentWidthField.setEnabled(formatCommentsBox.isSelected());
        });
        
        maxCommentWidthLabel = new JLabel("max width:");
        
        commentWidthField = new JTextField();
        commentWidthField.setColumns(3);
        
        /* AUTO-CLOSE */
        
        JSeparator sep1 = new JSeparator();
        
        JLabel autoCloseLabel = new JLabel("auto-close");
        autoCloseLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        
        closeBracketsBox = new JCheckBox("{brace}, (parentesis), [square] and <angle> brackets");
        closeQuotesBox = new JCheckBox("\"strings\" and 'chars'");
        closeBlockCommentsBox = new JCheckBox("Javadoc and comment regions");
        
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
        
        /* INDENTATION */
        
        JSeparator sep2 = new JSeparator();
        
        JLabel indentationLabel = new JLabel("indentation");
        indentationLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        
        JLabel indentLabel = new JLabel("Indent size:");
        JLabel spacesLabel = new JLabel("spaces");
        
        indentField = new JTextField();
        indentField.setColumns(3);
        
        indentMovingLinesBox = new JCheckBox("Automatically indent when moving text lines");
        
        
        int gap = 13; //Toolkit.BORDER;
                
        GroupLayout gl_tabGeneral = new GroupLayout(tabGeneral);
        gl_tabGeneral.setHorizontalGroup(
            gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_tabGeneral.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                        .addComponent(sep1, GroupLayout.PREFERRED_SIZE, 300, GroupLayout.PREFERRED_SIZE)
                        .addComponent(formattingLabel)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                                .addComponent(formatCommentsBox)
                                .addComponent(formatStringsBox))
                            .addGap(gap)
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                                .addComponent(maxCommentWidthLabel)
                                .addComponent(maxStringWidthLabel))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                                .addComponent(commentWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(stringWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                        .addComponent(wrapSelectedTextBox)
                        .addComponent(closeBlockCommentsBox)
                        .addComponent(closeQuotesBox)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addGap(gap)
                            .addComponent(replaceRadio)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(stackRadio))
                        .addComponent(closeBracketsBox)
                        .addComponent(autoCloseLabel)
                        .addComponent(indentMovingLinesBox)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addComponent(indentLabel)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(indentField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(spacesLabel))
                        .addComponent(indentationLabel)
                        .addGroup(gl_tabGeneral.createParallelGroup(Alignment.TRAILING)
                            .addComponent(restoreBtn)
                            .addComponent(sep2, GroupLayout.PREFERRED_SIZE, 300, GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
        );
        gl_tabGeneral.setVerticalGroup(
            gl_tabGeneral.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_tabGeneral.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_tabGeneral.createParallelGroup(Alignment.TRAILING)
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addComponent(formattingLabel)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(formatStringsBox)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(formatCommentsBox))
                        .addGroup(gl_tabGeneral.createSequentialGroup()
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                                .addComponent(maxStringWidthLabel)
                                .addComponent(stringWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                                .addComponent(commentWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(maxCommentWidthLabel))))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGap(gap)
                    .addComponent(sep1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(autoCloseLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(closeBracketsBox)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(closeQuotesBox)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(closeBlockCommentsBox)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(wrapSelectedTextBox)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_tabGeneral.createParallelGroup(Alignment.BASELINE)
                        .addComponent(replaceRadio)
                        .addComponent(stackRadio))
                    .addGap(gap)
                    .addComponent(sep2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(restoreBtn))
        );
        gl_tabGeneral.setAutoCreateGaps(true);
        gl_tabGeneral.setAutoCreateContainerGaps(true);
        tabGeneral.setLayout(gl_tabGeneral);
        
        
        /*
         * BOOKMARKS TAB
         * 
         */
        
        JPanel tabBookmarks = new JPanel();
        tabbedPane.addTab("Bookmarks", null, tabBookmarks, null);
        
        bookmarkHighlightingBox = new JCheckBox("Highlight bookmarked lines");
        bookmarkHighlightingBox.addChangeListener(a -> {
//            maxStringWidthLabel.setEnabled(checkBookmarkHighlighting.isSelected());
        });
        
        JLabel colorLabel_1 = new JLabel("Color 1:  #");
        colorPalette_1 = createColorPalette();
        colorField_1 = createColorField(colorPalette_1);

        colorSelector_1 = new ColorChooser(frame, false, colorPalette_1.getBackground(),
                Language.text("prompt.ok"), e -> {
                    String colorValue = colorSelector_1.getHexColor().substring(1); // remove the #
                    colorField_1.setText(colorValue);
                    colorPalette_1.setBackground(new Color(PApplet.unhex(colorValue)));
                    colorSelector_1.hide();
                });
        setupMouseListener(colorPalette_1, colorSelector_1);
        
        
        JLabel colorLabel_2 = new JLabel("Color 2:  #");
        colorPalette_2 = createColorPalette();
        colorField_2 = createColorField(colorPalette_2);
        
        colorSelector_2 = new ColorChooser(frame, false, colorPalette_2.getBackground(),
                Language.text("prompt.ok"), e -> {
                    String colorValue = colorSelector_2.getHexColor().substring(1); // remove the #
                    colorField_2.setText(colorValue);
                    colorPalette_2.setBackground(new Color(PApplet.unhex(colorValue)));
                    colorSelector_2.hide();
                });
        setupMouseListener(colorPalette_2, colorSelector_2);
        
        
        JLabel colorLabel_3 = new JLabel("Color 3:  #");
        colorPalette_3 = createColorPalette();
        colorField_3 = createColorField(colorPalette_3);    
        
        colorSelector_3 = new ColorChooser(frame, false, colorPalette_3.getBackground(),
                Language.text("prompt.ok"), e -> {
                    String colorValue = colorSelector_3.getHexColor().substring(1); // remove the #
                    colorField_3.setText(colorValue);
                    colorPalette_3.setBackground(new Color(PApplet.unhex(colorValue)));
                    colorSelector_3.hide();
                });
        setupMouseListener(colorPalette_3, colorSelector_3);
        
        
        JLabel colorLabel_4 = new JLabel("Color 4:  #");
        colorPalette_4 = createColorPalette();
        colorField_4 = createColorField(colorPalette_4);    
        
        colorSelector_4 = new ColorChooser(frame, false, colorPalette_4.getBackground(),
                Language.text("prompt.ok"), e -> {
                    String colorValue = colorSelector_4.getHexColor().substring(1); // remove the #
                    colorField_4.setText(colorValue);
                    colorPalette_4.setBackground(new Color(PApplet.unhex(colorValue)));
                    colorSelector_4.hide();
                });
        setupMouseListener(colorPalette_4, colorSelector_4);
        
        
        JLabel colorLabel_5 = new JLabel("Color 5:  #");
        colorPalette_5 = createColorPalette();
        colorField_5 = createColorField(colorPalette_5);    
        
        colorSelector_5 = new ColorChooser(frame, false, colorPalette_5.getBackground(),
                Language.text("prompt.ok"), e -> {
                    String colorValue = colorSelector_5.getHexColor().substring(1); // remove the #
                    colorField_5.setText(colorValue);
                    colorPalette_5.setBackground(new Color(PApplet.unhex(colorValue)));
                    colorSelector_5.hide();
                });
        setupMouseListener(colorPalette_5, colorSelector_5);
        
        JSeparator separator = new JSeparator(); // ---------------------------------------------------
        
        JLabel iconColorLabel = new JLabel("Icon color:  #");
        iconColorPalette = createColorPalette();
        iconColorField = createColorField(iconColorPalette);
        
        iconSelector = new ColorChooser(frame, false, iconColorPalette.getBackground(),
                Language.text("prompt.ok"), a -> {
                    String colorValue = iconSelector.getHexColor().substring(1); // remove the #
                    iconColorField.setText(colorValue);
                    iconColorPalette.setBackground(new Color(PApplet.unhex(colorValue)));
                    iconSelector.hide();
                });
        setupMouseListener(iconColorPalette, iconSelector);
        
        
        GroupLayout gl_tabBookmarks = new GroupLayout(tabBookmarks);
        gl_tabBookmarks.setHorizontalGroup(
            gl_tabBookmarks.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_tabBookmarks.createSequentialGroup()
                    .addGroup(gl_tabBookmarks.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_tabBookmarks.createParallelGroup(Alignment.LEADING, false)
                            .addGroup(gl_tabBookmarks.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(separator, GroupLayout.PREFERRED_SIZE, 300, GroupLayout.PREFERRED_SIZE)
                                .addGap(10))
                            .addGroup(gl_tabBookmarks.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(gl_tabBookmarks.createParallelGroup(Alignment.LEADING, false)
                                    .addComponent(bookmarkHighlightingBox)
                                    .addGroup(gl_tabBookmarks.createSequentialGroup()
                                        .addComponent(colorLabel_1, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorField_1, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorPalette_1, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED))
                                    .addGroup(gl_tabBookmarks.createSequentialGroup()
                                        .addComponent(colorLabel_2, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorField_2, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorPalette_2, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED))
                                    .addGroup(gl_tabBookmarks.createSequentialGroup()
                                        .addComponent(colorLabel_3, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorField_3, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorPalette_3, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED))
                                    .addGroup(gl_tabBookmarks.createSequentialGroup()
                                        .addComponent(colorLabel_4, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorField_4, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorPalette_4, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED))
                                    .addGroup(gl_tabBookmarks.createSequentialGroup()
                                        .addComponent(colorLabel_5, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorField_5, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(colorPalette_5, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                                        .addGap(13)))))
                        .addGroup(gl_tabBookmarks.createSequentialGroup()
                            .addComponent(iconColorLabel, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(iconColorField, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(iconColorPalette, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED)))
                    .addContainerGap(161, Short.MAX_VALUE))
        );
        gl_tabBookmarks.setVerticalGroup(
            gl_tabBookmarks.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_tabBookmarks.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(bookmarkHighlightingBox)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(gl_tabBookmarks.createParallelGroup(Alignment.BASELINE)
                        .addComponent(colorLabel_1)
                        .addComponent(colorField_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(colorPalette_1, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_tabBookmarks.createParallelGroup(Alignment.BASELINE)
                        .addComponent(colorLabel_2)
                        .addComponent(colorField_2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(colorPalette_2, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_tabBookmarks.createParallelGroup(Alignment.BASELINE)
                        .addComponent(colorLabel_3)
                        .addComponent(colorField_3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(colorPalette_3, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_tabBookmarks.createParallelGroup(Alignment.BASELINE)
                        .addComponent(colorLabel_4)
                        .addComponent(colorField_4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(colorPalette_4, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_tabBookmarks.createParallelGroup(Alignment.BASELINE)
                        .addComponent(colorLabel_5)
                        .addComponent(colorField_5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(colorPalette_5, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_tabBookmarks.createParallelGroup(Alignment.BASELINE)
                        .addComponent(iconColorLabel)
                        .addComponent(iconColorField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(iconColorPalette, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(157, Short.MAX_VALUE))
        );
        gl_tabBookmarks.setAutoCreateContainerGaps(true);
        gl_tabBookmarks.setAutoCreateGaps(true);
        tabBookmarks.setLayout(gl_tabBookmarks);
        mainPane.add(tabbedPane);
        
        
        /*
         * BOOKMARKS TAB
         * 
         */
        
        tabOccurrences = new JPanel();
        tabbedPane.addTab("Occurrences", null, tabOccurrences, null);
        
        occurrencesHighlightingBox = new JCheckBox("Highlight occurrences");
        occurrencesHighlightingBox.setHorizontalAlignment(SwingConstants.LEFT);
        
        JSlider slider = new JSlider();
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int val = slider.getValue();
                System.out.println("val: " + val);
            }
        });
        slider.setPaintTicks(true);
        slider.setSnapToTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        GroupLayout gl_tabOccurrences = new GroupLayout(tabOccurrences);
        gl_tabOccurrences.setHorizontalGroup(
            gl_tabOccurrences.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_tabOccurrences.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_tabOccurrences.createParallelGroup(Alignment.LEADING)
                        .addComponent(occurrencesHighlightingBox)
                        .addComponent(slider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(109, Short.MAX_VALUE))
        );
        gl_tabOccurrences.setVerticalGroup(
            gl_tabOccurrences.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_tabOccurrences.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(occurrencesHighlightingBox)
                    .addGap(34)
                    .addComponent(slider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(253, Short.MAX_VALUE))
        );
        tabOccurrences.setLayout(gl_tabOccurrences);
        
        
        /*
         * FOOTER BUTTONS PANEL
         * 
         */
        
        
        final int buttonWidth = 80; // Toolkit.getButtonWidth();

        JPanel mainButtonsPane = new JPanel();
        mainPane.add(mainButtonsPane);
        
        applyBtn = new JButton("Apply");
        applyBtn.setPreferredSize(new Dimension(buttonWidth, 23));
        applyBtn.addActionListener(e -> {
            applyPrefs();
            disposeFrame();
        });
        mainButtonsPane.add(applyBtn);
        
        cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(buttonWidth, 23));
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
        // *** GENERAL *** //
        
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
        
        /* auto-close brackets, quotes and block comments */
        closeBracketsBox.setSelected(AUTOCLOSE_BRACKETS);
        closeQuotesBox.setSelected(AUTOCLOSE_QUOTES);
        closeBlockCommentsBox.setSelected(AUTOCLOSE_BLOCK_COMMENTS);
        
        /* wrap selected text buttons */
        wrapSelectedTextBox.setSelected(AUTOCLOSE_WRAP_TEXT);
        replaceRadio.setEnabled(wrapSelectedTextBox.isSelected());
        stackRadio.setEnabled(wrapSelectedTextBox.isSelected());
        replaceRadio.setSelected(AUTOCLOSE_WRAP_REPLACE);
        stackRadio.setSelected(!AUTOCLOSE_WRAP_REPLACE);
        
        /* indentation */
        currTabSize = Preferences.getInteger("editor.tabs.size");
        indentField.setText(String.valueOf(currTabSize));
        indentMovingLinesBox.setSelected(MOVE_LINES_AUTO_INDENT);
        
        
        // *** BOOKMARKS *** //      
        
        bookmarkHighlightingBox.setSelected(BOOKMARKS_HIGHLIGHT);
        
        colorPalette_1.setBackground(SmartCodeTheme.getColor("bookmarks.linehighlight.color.1"));
        colorField_1.setText(SmartCodeTheme.get("bookmarks.linehighlight.color.1"));
        
        colorPalette_2.setBackground(SmartCodeTheme.getColor("bookmarks.linehighlight.color.2"));
        colorField_2.setText(SmartCodeTheme.get("bookmarks.linehighlight.color.2"));
        
        colorPalette_3.setBackground(SmartCodeTheme.getColor("bookmarks.linehighlight.color.3"));
        colorField_3.setText(SmartCodeTheme.get("bookmarks.linehighlight.color.3"));
        
        colorPalette_4.setBackground(SmartCodeTheme.getColor("bookmarks.linehighlight.color.4"));
        colorField_4.setText(SmartCodeTheme.get("bookmarks.linehighlight.color.4"));
        
        colorPalette_5.setBackground(SmartCodeTheme.getColor("bookmarks.linehighlight.color.5"));
        colorField_5.setText(SmartCodeTheme.get("bookmarks.linehighlight.color.5"));
        
        /* icon */
        iconColorPalette.setBackground(SmartCodeTheme.getColor("bookmarks.icon.color"));
        iconColorField.setText(SmartCodeTheme.get("bookmarks.icon.color"));
        
        
        occurrencesHighlightingBox.setSelected(OCCURRENCES_HIGHLIGHT);
        
        frame.getRootPane().setDefaultButton(applyBtn);
        frame.pack();
        frame.setVisible(true);
    }
        
    protected void applyPrefs() {
     // *** GENERAL *** //
        
        /* formatting */
        Preferences.setBoolean("SmartCode.autoformat.strings", formatStringsBox.isSelected());
        Preferences.setInteger("SmartCode.autoformat.strings.length",
                Integer.parseInt(stringWidthField.getText().trim()));

        Preferences.setBoolean("SmartCode.autoformat.comments", formatCommentsBox.isSelected());
        Preferences.setInteger("SmartCode.autoformat.comments.length",
                Integer.parseInt(commentWidthField.getText().trim()));
        
        /* auto-close */
        Preferences.setBoolean("SmartCode.auto_close.brackets", closeBracketsBox.isSelected());
        Preferences.setBoolean("SmartCode.auto_close.quotes", closeQuotesBox.isSelected());
        Preferences.setBoolean("SmartCode.auto_close.block_comments", closeBlockCommentsBox.isSelected());
        Preferences.setBoolean("SmartCode.auto_close.wrap_text", wrapSelectedTextBox.isSelected());
        Preferences.setBoolean("SmartCode.auto_close.wrap_text.replace", replaceRadio.isSelected());
        
        /* indentation */
        int newTabSize = Integer.parseInt(indentField.getText().trim());
        Preferences.setInteger("editor.tabs.size", newTabSize);
        Preferences.setBoolean("SmartCode.movelines.auto_indent", indentMovingLinesBox.isSelected());
        
        
        // *** BOOKMARKS *** //        
        
        SmartCodeTheme.setBoolean("bookmarks.linehighlight", bookmarkHighlightingBox.isSelected());
        
        SmartCodeTheme.setColor("bookmarks.linehighlight.color.1", colorPalette_1.getBackground());
        SmartCodeTheme.setColor("bookmarks.linehighlight.color.2", colorPalette_2.getBackground());
        SmartCodeTheme.setColor("bookmarks.linehighlight.color.3", colorPalette_3.getBackground());
        SmartCodeTheme.setColor("bookmarks.linehighlight.color.4", colorPalette_4.getBackground());
        SmartCodeTheme.setColor("bookmarks.linehighlight.color.5", colorPalette_5.getBackground());
        /* icon */
        SmartCodeTheme.setColor("bookmarks.icon.color", iconColorPalette.getBackground());

        
        
        
        SmartCodeTheme.setBoolean("occurrences.highlight", occurrencesHighlightingBox.isSelected());
        
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
    


    private JTextField createColorPalette() {
        JTextField colorPalette = new JTextField("      ");
        colorPalette.setOpaque(true);
        colorPalette.setEnabled(true);
        colorPalette.setEditable(false);
        Border cb = new CompoundBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, new Color(195, 195, 195)),
                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(54, 54, 54)));
        colorPalette.setBorder(cb);
        return colorPalette;
    }
    
    private JTextField createColorField(JTextField colorPalette) {
        JTextField colorField = new JTextField();
        colorField.setColumns(6);
        colorField.setDocument(new HexNumberFormatDocument());
        colorField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                final String colorValue = colorField.getText();

                if (colorValue.length() == 7) {
                    EventQueue.invokeLater(() -> colorField.setText(colorValue.substring(1)));
                }

                if (colorValue.length() == 6 && colorValue.matches("[A-F0-9]*")) {
                    colorPalette.setBackground(new Color(PApplet.unhex(colorValue)));
                    
                    if (!colorValue.equals(colorField.getText())) { 
                        EventQueue.invokeLater(() -> colorField.setText(colorValue));
                    }
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                final String colorValue = colorField.getText();

                if (colorValue.length() == 7) {
                    EventQueue.invokeLater(() -> colorField.setText(colorValue.substring(1)));
                }

                if (colorValue.length() == 6 && colorValue.matches("[A-F0-9]*")) {
                    colorPalette.setBackground(new Color(PApplet.unhex(colorValue)));
                    
                    if (!colorValue.equals(colorField.getText())) { 
                        EventQueue.invokeLater(() -> colorField.setText(colorValue));
                    }
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        
        return colorField;
    }
    
    static class HexNumberFormatDocument extends PlainDocument {
        int maxLength = 6;
        String hexNumber;

        @Override
            public void insertString(int offset, String str, AttributeSet att) throws BadLocationException {
            if (str == null)
                return;
            
            str = str.toUpperCase();

            if (offset == 0)
                maxLength = 6;

            if (offset == 0 && str.matches("\\#.*")) {
                hexNumber = "#[A-F0-9]*";
                maxLength = 7;
            } else {
                hexNumber = "[A-F0-9]*";
            }

            if (str.matches(hexNumber) && (getLength() + str.length()) <= maxLength) {
                super.insertString(offset, str, att);
            }
        }
    }
    
    void setupMouseListener(JTextField colorPalette, ColorChooser colorChooser) {
        colorPalette.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                colorChooser.setColor(colorPalette.getBackground());
                colorChooser.show();
            }
        });
    }
}
