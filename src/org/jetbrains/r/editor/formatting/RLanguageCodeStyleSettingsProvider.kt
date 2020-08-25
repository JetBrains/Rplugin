// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor.formatting

import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizableOptions
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage


private typealias CCSS = CommonCodeStyleSettings

class RLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
  override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
    when (settingsType) {
      SettingsType.SPACING_SETTINGS -> {
        consumer.showStandardOptions(
          CCSS::SPACE_BEFORE_METHOD_CALL_PARENTHESES.name,
          CCSS::SPACE_BEFORE_METHOD_PARENTHESES.name,
          CCSS::SPACE_BEFORE_IF_PARENTHESES.name,
          CCSS::SPACE_BEFORE_WHILE_PARENTHESES.name,
          CCSS::SPACE_BEFORE_FOR_PARENTHESES.name,

          CCSS::SPACE_AROUND_ASSIGNMENT_OPERATORS.name,
          CCSS::SPACE_AROUND_RELATIONAL_OPERATORS.name,
          CCSS::SPACE_AROUND_ADDITIVE_OPERATORS.name,
          CCSS::SPACE_AROUND_MULTIPLICATIVE_OPERATORS.name,
          CCSS::SPACE_AROUND_UNARY_OPERATOR.name,

          CCSS::SPACE_WITHIN_BRACKETS.name,
          CCSS::SPACE_WITHIN_BRACES.name,
          CCSS::SPACE_WITHIN_PARENTHESES.name,

          CCSS::SPACE_BEFORE_METHOD_LBRACE.name,
          CCSS::SPACE_BEFORE_IF_LBRACE.name,
          CCSS::SPACE_BEFORE_WHILE_LBRACE.name,
          CCSS::SPACE_BEFORE_FOR_LBRACE.name,

          CCSS::SPACE_AFTER_COMMA.name,
          CCSS::SPACE_BEFORE_COMMA.name)

        consumer.renameStandardOption(CCSS::SPACE_BEFORE_METHOD_CALL_PARENTHESES.name,
                                      RBundle.message("code.style.label.function.call.parentheses"))
        consumer.renameStandardOption(CCSS::SPACE_BEFORE_METHOD_PARENTHESES.name,
                                      RBundle.message("code.style.label.function.declaration.parentheses"))
        consumer.renameStandardOption(CCSS::SPACE_AROUND_ASSIGNMENT_OPERATORS.name,
                                      RBundle.message("code.style.label.assignment.operators"))
        consumer.renameStandardOption(CCSS::SPACE_AROUND_RELATIONAL_OPERATORS.name,
                                      RBundle.message("code.style.label.comparison.operators"))
        consumer.renameStandardOption(CCSS::SPACE_AROUND_MULTIPLICATIVE_OPERATORS.name,
                                      RBundle.message("code.style.label.multiplicative.operators"))
        consumer.renameStandardOption(CCSS::SPACE_AROUND_UNARY_OPERATOR.name,
                                      RBundle.message("code.style.label.unary.operators"))
        consumer.renameStandardOption(CCSS::SPACE_WITHIN_BRACES.name,
                                      RBundle.message("code.style.label.braces"))
        consumer.renameStandardOption(CCSS::SPACE_WITHIN_PARENTHESES.name,
                                      RBundle.message("code.style.label.parentheses"))
        consumer.renameStandardOption(CCSS::SPACE_BEFORE_METHOD_LBRACE.name,
                                      RBundle.message("code.style.label.function.left.brace"))

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_AROUND_BINARY_TILDE_OPERATOR.name,
                                  RBundle.message("code.style.label.binary.tilde.operator"), CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS)

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_AROUND_DISJUNCTION_OPERATORS.name,
                                  RBundle.message("code.style.label.disjunction.operators"),
                                  CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS)

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_AROUND_CONJUNCTION_OPERATORS.name,
                                  RBundle.message("code.style.label.conjunction.operators"),
                                  CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS)

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_AROUND_INFIX_OPERATOR.name,
                                  RBundle.message("code.style.label.infix.operators"),
                                  CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS)

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_AROUND_COLON_OPERATOR.name,
                                  RBundle.message("code.style.label.colon.operator"), CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS)

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_AROUND_EXPONENTIATION_OPERATOR.name,
                                  RBundle.message("code.style.label.exponentiation.operator"), CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS)

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_AROUND_SUBSET_OPERATOR.name,
                                  RBundle.message("code.style.label.list.subset.operator"), CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS)

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_AROUND_AT_OPERATOR.name,
                                  RBundle.message("code.style.label.at.operator"), CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS)

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_BEFORE_REPEAT_LBRACE.name,
                                  RBundle.message("code.style.label.repeat.left.brace"), CodeStyleSettingsCustomizableOptions.getInstance().SPACES_BEFORE_LEFT_BRACE)

        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::SPACE_BEFORE_LEFT_BRACKET.name,
                                  RBundle.message("code.style.label.space.before"), CodeStyleSettingsCustomizableOptions.getInstance().SPACES_OTHER)
      }
      SettingsType.BLANK_LINES_SETTINGS ->
        consumer.showStandardOptions(CCSS::KEEP_BLANK_LINES_IN_CODE.name)
      SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {
        consumer.showStandardOptions(CCSS::RIGHT_MARGIN.name,
                                     CCSS::WRAP_ON_TYPING.name,
                                     CCSS::KEEP_LINE_BREAKS.name,
                                     CCSS::WRAP_LONG_LINES.name,
                                     CCSS::ALIGN_MULTILINE_PARAMETERS.name,
                                     CCSS::ALIGN_MULTILINE_PARAMETERS_IN_CALLS.name)
        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::ALIGN_ASSIGNMENT_OPERATORS.name,
                                  RBundle.message("code.style.label.align.assignment.operators"), null)
        consumer.showCustomOption(RCodeStyleSettings::class.java, RCodeStyleSettings::ALIGN_COMMENTS.name,
                                  RBundle.message("code.style.label.align.comments"), null)
      }
      else -> {} // do nothing
    }
  }

  override fun getLanguage(): Language = RLanguage.INSTANCE

  override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

  @org.intellij.lang.annotations.Language("R")
  override fun getCodeSample(settingsType: SettingsType): String = """
    normalMean <- function(samples=10,
    otherParameter){
      data <- rnorm(samples)
      mean(data)
    }
    
    someMethod(
    samples = 5, # First parameter
    otherParameter = 10000, # Second parameter
    last = 7 # Last parameter
    )
    
    aLotOfArguments(100, 101,
    102, 103)

    hello <- function() { print("Hello!") ; 42 }
    
    if (debug==!FALSE || no == yes && yes == R) {
      hello()
    }
    
    for (i in 1:10){
      print(i)
    }
    
    while (foo()) {
      boo(x[1])
    }
    
    a <- 10
    bbb <- 20
    cc <- 30
    
    repeat {
      foo(some@oh)
    }
    
    some_value = df[num < 0,
    "campaign.id"
    ]

    #' some roxygen style function documentaiton
    #' @param a arggument one
    #' @param b arggument b
    testFunc <- function(a, b){
      as.numeric(a)^2 + 2*as.numeric(b)
    }

    models <- mtcars %>%
       group_by(cyl) %>%
       do(lm = lm(mpg ~ wt, data = .)) %>%
       summarise(rsq = summary(lm)${"$"}r.squared) %>%
       head()
  """

  override fun customizeDefaults(commonSettings: CommonCodeStyleSettings, indentOptions: CommonCodeStyleSettings.IndentOptions) {
    commonSettings.SPACE_WITHIN_BRACES = true
    commonSettings.ALIGN_MULTILINE_PARAMETERS = true
    commonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = true
    indentOptions.INDENT_SIZE = 2
    indentOptions.CONTINUATION_INDENT_SIZE = 2
    indentOptions.USE_TAB_CHARACTER = false
  }
}

