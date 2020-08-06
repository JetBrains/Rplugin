/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.settings

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import org.jetbrains.r.interpreter.RInterpreterInfo
import org.jetbrains.r.interpreter.RInterpreterLocation

interface RInterpreterSettingsProvider {
  fun getLocationFromState(state: RSettings.State): RInterpreterLocation?

  fun putLocationToState(state: RSettings.State, location: RInterpreterLocation): Boolean

  fun deserializeLocation(serializable: RSerializableInterpreter): RInterpreterLocation?

  fun serializeLocation(location: RInterpreterLocation, serializable: RSerializableInterpreter): Boolean

  fun getAddInterpreterActionName(): String

  fun showAddInterpreterDialog(existingInterpreters: List<RInterpreterInfo>, onAdded: (RInterpreterInfo) -> Unit)

  fun createSettingsConfigurable(project: Project): UnnamedConfigurable? = null

  fun isEditingSupported(): Boolean = false

  fun canEditInterpreter(info: RInterpreterInfo): Boolean = false

  fun showEditInterpreterDialog(info: RInterpreterInfo, existingInterpreters: List<RInterpreterInfo>,
                                onEdited: (RInterpreterInfo) -> Unit) {}

  fun provideInterpreterForTests(): RInterpreterLocation? = null

  companion object {
    private val EP_NAME: ExtensionPointName<RInterpreterSettingsProvider>
      = ExtensionPointName.create("com.intellij.rInterpreterSettingsProvider")

    fun getProviders(): List<RInterpreterSettingsProvider> = EP_NAME.extensionList
  }
}