/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.ide.actions.ToolWindowTabRenameActionBase
import com.intellij.openapi.project.DumbAware
import com.intellij.r.psi.RBundle

class RConsoleRenameAction : ToolWindowTabRenameActionBase(RConsoleToolWindowFactory.ID, RBundle.message("console.rename.label")),
                              DumbAware
