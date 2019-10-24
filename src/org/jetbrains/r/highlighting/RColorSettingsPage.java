// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova


/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.r.RIconsKt;

import javax.swing.*;
import java.util.Map;

/**
 * RColorSettingsPage implementation
 * Created on 7/23/14.
 *
 * @author HongKee Moon
 */
final public class RColorSettingsPage implements ColorSettingsPage {
  /**
   * The path to the sample .R file
   */
  private static final String SAMPLE_R_SCRIPT = "\n" +
          "for (i in names(list)) {\n" +
          "   if(true)\n" +
          "   {\n" +
          "      # line comment\n" +
          "      names[,i] = 0\n" +
          "      names[,i+1] = test()\n" +
          "      names[,'added'] = \"string\"\n" +
          "   }\n" +
          "}\n";


  /**
   * The sample .R document shown in the colors settings dialog
   */
  private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
          new AttributesDescriptor("Comment", RHighlighterColors.LINE_COMMENT),
          new AttributesDescriptor("Keyword", RHighlighterColors.KEYWORD),
          new AttributesDescriptor("Parenthesis", RHighlighterColors.PARENTHESES),
          new AttributesDescriptor("Braces", RHighlighterColors.BRACES),
          new AttributesDescriptor("Brackets", RHighlighterColors.BRACKETS),
          new AttributesDescriptor("Number", RHighlighterColors.NUMBER),
          new AttributesDescriptor("String ...", RHighlighterColors.STRING),
          new AttributesDescriptor("Function Call", RHighlighterColors.FUNCTION_CALL),
          new AttributesDescriptor("Namespace", RHighlighterColors.NAMESPACE),
          new AttributesDescriptor("Parameter", RHighlighterColors.PARAMETER),
          new AttributesDescriptor("Local variable", RHighlighterColors.LOCAL_VARIABLE),
          new AttributesDescriptor("Global variable", RHighlighterColors.GLOBAL_VARIABLE),
          new AttributesDescriptor("Closure", RHighlighterColors.CLOSURE),
          new AttributesDescriptor("Named argument", RHighlighterColors.NAMED_ARGUMENT),
  };

  @Nullable
  @Override
  public Icon getIcon() {
    return RIconsKt.R_LOGO_16;
  }

  @NotNull
  @Override
  public SyntaxHighlighter getHighlighter() {
    return new RSyntaxHighlighter();
  }

  @NotNull
  @Override
  public String getDemoText() {
    return SAMPLE_R_SCRIPT;
  }

  @Nullable
  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }

  @NotNull
  @Override
  public AttributesDescriptor[] getAttributeDescriptors() {
    return DESCRIPTORS;
  }

  @NotNull
  @Override
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "R";
  }
}
