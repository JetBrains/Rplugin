/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r.inlays.components.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class GraphicsAdvancedExportDialogForm extends JDialog {
  private JPanel contentPane;
  private JTextField fileNameTextField;
  private JComboBox formatComboBox;
  private JCheckBox openAfterSavingCheckBox;
  private JPanel directoryFieldPanel;
  private JPanel widthInputPanel;
  private JPanel keepAspectRatioButtonPanel;
  private JPanel heightInputPanel;
  private JPanel resolutionInputPanel;
  private JPanel southPanel;
  private JPanel okCancelButtonsPanel;
  private JPanel graphicsContentPanel;
  private JPanel refreshButtonPanel;
  private JCheckBox autoResizeCheckBox;

  @Override
  public JPanel getContentPane() {
    return contentPane;
  }

  public JTextField getFileNameTextField() {
    return fileNameTextField;
  }

  public JComboBox getFormatComboBox() {
    return formatComboBox;
  }

  public JCheckBox getOpenAfterSavingCheckBox() {
    return openAfterSavingCheckBox;
  }

  public JPanel getDirectoryFieldPanel() {
    return directoryFieldPanel;
  }

  public JPanel getWidthInputPanel() {
    return widthInputPanel;
  }

  public JPanel getKeepAspectRatioButtonPanel() {
    return keepAspectRatioButtonPanel;
  }

  public JPanel getHeightInputPanel() {
    return heightInputPanel;
  }

  public JPanel getResolutionInputPanel() {
    return resolutionInputPanel;
  }

  public JPanel getOkCancelButtonsPanel() {
    return okCancelButtonsPanel;
  }

  public JPanel getGraphicsContentPanel() {
    return graphicsContentPanel;
  }

  public JPanel getRefreshButtonPanel() {
    return refreshButtonPanel;
  }

  public JCheckBox getAutoResizeCheckBox() {
    return autoResizeCheckBox;
  }

  public GraphicsAdvancedExportDialogForm() {
    setContentPane(contentPane);
    setModal(true);
  }

  {
    // GUI initializer generated by IntelliJ IDEA GUI Designer
    // >>> IMPORTANT!! <<<
    // DO NOT EDIT OR ADD ANY CODE HERE!
    $$$setupUI$$$();
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    contentPane = new JPanel();
    contentPane.setLayout(new GridLayoutManager(5, 1, new JBInsets(8, 12, 8, 12), -1, -1));
    southPanel = new JPanel();
    southPanel.setLayout(new GridLayoutManager(1, 2, new JBInsets(0, 0, 0, 0), -1, -1));
    contentPane.add(southPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null,
                                                    null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    southPanel.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridBagLayout());
    southPanel.add(panel1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                               GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                               GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null,
                                               null, 0, false));
    openAfterSavingCheckBox = new JCheckBox();
    this.$$$loadButtonText$$$(openAfterSavingCheckBox, ResourceBundle.getBundle("messages/VisualizationBundle")
      .getString("inlay.output.image.export.dialog.open.image"));
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    panel1.add(openAfterSavingCheckBox, gbc);
    okCancelButtonsPanel = new JPanel();
    okCancelButtonsPanel.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new JBInsets(0, 20, 0, 0);
    panel1.add(okCancelButtonsPanel, gbc);
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridBagLayout());
    contentPane.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("messages/VisualizationBundle")
      .getString("inlay.output.image.export.dialog.save.as"));
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    panel2.add(label1, gbc);
    fileNameTextField = new JTextField();
    fileNameTextField.setText("Rplot01");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 0.25;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new JBInsets(0, 8, 0, 0);
    panel2.add(fileNameTextField, gbc);
    formatComboBox = new JComboBox();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new JBInsets(0, 8, 0, 0);
    panel2.add(formatComboBox, gbc);
    final JLabel label2 = new JLabel();
    this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("messages/VisualizationBundle")
      .getString("inlay.output.image.export.dialog.save.in"));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new JBInsets(0, 8, 0, 8);
    panel2.add(label2, gbc);
    directoryFieldPanel = new JPanel();
    directoryFieldPanel.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.weightx = 0.75;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel2.add(directoryFieldPanel, gbc);
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new GridBagLayout());
    contentPane.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    this.$$$loadLabelText$$$(label3,
                             ResourceBundle.getBundle("messages/VisualizationBundle").getString("inlay.output.image.export.dialog.width"));
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new JBInsets(0, 28, 0, 8);
    panel3.add(label3, gbc);
    final JLabel label4 = new JLabel();
    this.$$$loadLabelText$$$(label4,
                             ResourceBundle.getBundle("messages/VisualizationBundle").getString("inlay.output.image.export.dialog.height"));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new JBInsets(0, 8, 0, 8);
    panel3.add(label4, gbc);
    final JLabel label5 = new JLabel();
    this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("messages/VisualizationBundle")
      .getString("inlay.output.image.export.dialog.resolution"));
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new JBInsets(0, 28, 0, 8);
    panel3.add(label5, gbc);
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    panel3.add(panel4, gbc);
    widthInputPanel = new JPanel();
    widthInputPanel.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel4.add(widthInputPanel, gbc);
    keepAspectRatioButtonPanel = new JPanel();
    keepAspectRatioButtonPanel.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new JBInsets(0, 6, 0, 0);
    panel4.add(keepAspectRatioButtonPanel, gbc);
    heightInputPanel = new JPanel();
    heightInputPanel.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    panel3.add(heightInputPanel, gbc);
    refreshButtonPanel = new JPanel();
    refreshButtonPanel.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 7;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new JBInsets(0, 6, 0, 0);
    panel3.add(refreshButtonPanel, gbc);
    resolutionInputPanel = new JPanel();
    resolutionInputPanel.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 6;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    panel3.add(resolutionInputPanel, gbc);
    autoResizeCheckBox = new JCheckBox();
    autoResizeCheckBox.setSelected(true);
    this.$$$loadButtonText$$$(autoResizeCheckBox,
                              ResourceBundle.getBundle("messages/VisualizationBundle").getString("graphics.settings.auto.resize"));
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    panel3.add(autoResizeCheckBox, gbc);
    final JPanel panel5 = new JPanel();
    panel5.setLayout(new GridLayoutManager(1, 1, new JBInsets(8, 0, 8, 0), -1, -1));
    contentPane.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null,
                                                null, 0, false));
    graphicsContentPanel = new JPanel();
    graphicsContentPanel.setLayout(new BorderLayout(0, 0));
    panel5.add(graphicsContentPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                         null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    contentPane.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                                 GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, JBUI.scale(8)), null, 0, false));
  }

  /**
   * @noinspection ALL
   */
  private void $$$loadLabelText$$$(JLabel component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) break;
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setDisplayedMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /**
   * @noinspection ALL
   */
  private void $$$loadButtonText$$$(AbstractButton component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) break;
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() { return contentPane; }
}