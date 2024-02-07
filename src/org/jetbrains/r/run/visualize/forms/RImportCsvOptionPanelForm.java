/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize.forms;

import com.intellij.DynamicBundle;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;

public class RImportCsvOptionPanelForm extends JDialog {
  private JPanel contentPane;
  private JTextField skipTextField;
  private JCheckBox firstRowAsNamesCheckBox;
  private JCheckBox trimSpacesCheckBox;
  private JComboBox delimiterComboBox;
  private JComboBox quotesComboBox;
  private JComboBox escapeComboBox;
  private JComboBox commentComboBox;
  private JComboBox naComboBox;

  @Override
  public JPanel getContentPane() {
    return contentPane;
  }

  public JTextField getSkipTextField() {
    return skipTextField;
  }

  public JCheckBox getFirstRowAsNamesCheckBox() {
    return firstRowAsNamesCheckBox;
  }

  public JCheckBox getTrimSpacesCheckBox() {
    return trimSpacesCheckBox;
  }

  public JComboBox getDelimiterComboBox() {
    return delimiterComboBox;
  }

  public JComboBox getQuotesComboBox() {
    return quotesComboBox;
  }

  public JComboBox getEscapeComboBox() {
    return escapeComboBox;
  }

  public JComboBox getCommentComboBox() {
    return commentComboBox;
  }

  public JComboBox getNaComboBox() {
    return naComboBox;
  }

  public RImportCsvOptionPanelForm() {
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
    contentPane.setLayout(new GridLayoutManager(9, 2, new Insets(0, 0, 0, 0), -1, -1));
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1, DynamicBundle.getResourceBundle(getClass().getClassLoader(), "messages/RPluginBundle").getString("import.data.dialog.form.skip"));
    contentPane.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                                false));
    final Spacer spacer1 = new Spacer();
    contentPane.add(spacer1, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                                 GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    skipTextField = new JTextField();
    skipTextField.setText("0");
    contentPane.add(skipTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                       GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                       new Dimension(150, -1), null, 0, false));
    firstRowAsNamesCheckBox = new JCheckBox();
    firstRowAsNamesCheckBox.setSelected(true);
    this.$$$loadButtonText$$$(firstRowAsNamesCheckBox,
                              DynamicBundle.getResourceBundle(getClass().getClassLoader(), "messages/RPluginBundle").getString("import.data.dialog.form.column.names"));
    contentPane.add(firstRowAsNamesCheckBox, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                 GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                 null, null, null, 0, false));
    trimSpacesCheckBox = new JCheckBox();
    trimSpacesCheckBox.setSelected(true);
    this.$$$loadButtonText$$$(trimSpacesCheckBox,
                              DynamicBundle.getResourceBundle(getClass().getClassLoader(), "messages/RPluginBundle").getString("import.data.dialog.form.trim.spaces"));
    contentPane.add(trimSpacesCheckBox, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label2 = new JLabel();
    this.$$$loadLabelText$$$(label2, DynamicBundle.getResourceBundle(getClass().getClassLoader(), "messages/RPluginBundle").getString("import.data.dialog.form.delimiter"));
    contentPane.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                                false));
    final JLabel label3 = new JLabel();
    this.$$$loadLabelText$$$(label3, DynamicBundle.getResourceBundle(getClass().getClassLoader(), "messages/RPluginBundle").getString("import.data.dialog.form.quotes"));
    contentPane.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                                false));
    final JLabel label4 = new JLabel();
    this.$$$loadLabelText$$$(label4, DynamicBundle.getResourceBundle(getClass().getClassLoader(), "messages/RPluginBundle").getString("import.data.dialog.form.escape"));
    contentPane.add(label4, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                                false));
    final JLabel label5 = new JLabel();
    this.$$$loadLabelText$$$(label5, DynamicBundle.getResourceBundle(getClass().getClassLoader(), "messages/RPluginBundle").getString("import.data.dialog.form.comment"));
    contentPane.add(label5, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                                false));
    delimiterComboBox = new JComboBox();
    contentPane.add(delimiterComboBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                           GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                           null, null, 0, false));
    quotesComboBox = new JComboBox();
    contentPane.add(quotesComboBox, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                        null, 0, false));
    escapeComboBox = new JComboBox();
    contentPane.add(escapeComboBox, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                        null, 0, false));
    commentComboBox = new JComboBox();
    contentPane.add(commentComboBox, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                         GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                         null, 0, false));
    final JLabel label6 = new JLabel();
    this.$$$loadLabelText$$$(label6, DynamicBundle.getResourceBundle(getClass().getClassLoader(), "messages/RPluginBundle").getString("import.data.dialog.form.na"));
    contentPane.add(label6, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                                false));
    naComboBox = new JComboBox();
    contentPane.add(naComboBox, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                    GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null,
                                                    0, false));
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
