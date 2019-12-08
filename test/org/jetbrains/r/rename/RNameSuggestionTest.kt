/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rename

import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import org.jetbrains.r.refactoring.rename.RNameSuggestion

class RNameSuggestionTest : RLightCodeInsightFixtureTestCase() {

  fun testValidName() {
    doVariableTest("valid_name", "valid_name")
    doFunctionTest("valid_name", "valid_name")
  }

  fun testCamelCaseName() {
    doVariableTest("invalidName", "invalid_name")
    doFunctionTest("invalidName", "invalid_name")
  }

  fun testDashName() {
    doVariableTest("dash-name", "dash_name")
    doFunctionTest("dash-name", "dash_name")
  }

  fun testRepetitiveUnderlineName() {
    doVariableTest("rep_Name", "rep_name")
    doFunctionTest("rep_Name", "rep_name")
  }

  fun testFirstUpperCase() {
    doVariableTest("CamelCaseName", "camel_case_name")
    doFunctionTest("CamelCaseName", "camel_case_name")
  }

  fun testDotVariableName() = doVariableTest("..some.another.name", "some_another_name")

  fun testDotFunctionName() = doFunctionTest("...Dot.Name..f", "dot.Name..f", "dot_name_f")

  fun testMixVariableName() = doVariableTest("dot..under_ln__CaMelC__See_..e", "dot_under_ln_ca_mel_c_see_e")

  fun testMixFunctionName() = doFunctionTest("Fun___for__.data.table", "fun_for.data.table", "fun_for_data_table")

  fun testForLoopName() = doForLoopTest("valC-2020", "val_c_2020", "i")

  fun testUnavailableNamesVariable() {
    doVariableTest("abaCaba", "aba_caba1", unavailableNames = setOf("aba_caba"))
    doVariableTest("aba.Caba", "aba_caba2", unavailableNames = setOf("aba_caba", "aba_caba1", "aba_caba3"))
    doVariableTest("..aba.caba", "aba_caba", unavailableNames = setOf("abaCaba"))
  }

  fun testUnavailableNamesFunction() {
    doFunctionTest("abaCaba", "aba_caba1", unavailableNames = setOf("aba_caba"))
    doFunctionTest("abaCaba.data.table", "aba_caba2.data.table", "aba_caba_data_table1",
                   unavailableNames = setOf("aba_caba.data.table", "aba_caba1.data.table", "aba_caba3.data.table", "aba_caba_data_table"))
  }

  fun testUnavailableNamesForLoop() {
    doForLoopTest(".forVar-222", "for_var_2222", "i", unavailableNames = setOf("for_var_222", "for_var_2221"))
    doForLoopTest("varR", "var_r1", "j", unavailableNames = setOf("var_r", "var_r2", "i"))
    doForLoopTest("varR", "var_r1", "k", unavailableNames = setOf("var_r", "var_2", "i", "j"))
    doForLoopTest("varR", "var_r1", "i1", unavailableNames = setOf("var_r", "var_2", "i", "j", "k"))
  }

  private fun doVariableTest(oldName: String, vararg expectedNames: String, unavailableNames: Set<String> = emptySet()) {
    doTest(oldName, *expectedNames, unavailableNames = unavailableNames, suggestionFunction = RNameSuggestion::getVariableSuggestedNames)
  }

  private fun doFunctionTest(oldName: String, vararg expectedNames: String, unavailableNames: Set<String> = emptySet()) {
    doTest(oldName, *expectedNames, unavailableNames = unavailableNames, suggestionFunction = RNameSuggestion::getFunctionSuggestedNames)
  }

  private fun doForLoopTest(oldName: String, vararg expectedNames: String, unavailableNames: Set<String> = emptySet()) {
    doTest(oldName, *expectedNames, unavailableNames = unavailableNames,
           suggestionFunction = RNameSuggestion::getTargetForLoopSuggestedNames)
  }

  private fun doTest(oldName: String,
                     vararg expectedNames: String,
                     unavailableNames: Set<String>,
                     suggestionFunction: (String, MutableSet<String>) -> Set<String>) {
    val mutableUnavailableNames = unavailableNames.toMutableSet()
    val suggestedNames = suggestionFunction(oldName, mutableUnavailableNames)
    assertContainsElements(mutableUnavailableNames, suggestedNames)
    assertEquals(expectedNames.toSet(), suggestedNames)
  }
}