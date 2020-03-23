/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r

import com.intellij.openapi.util.IconLoader

val R_LOGO_16 = load("r.svg")

val R_MARKDOWN = load("rMarkdown.svg")

val RENDER = load("render.svg")

val UPGRADE_ALL = load("packages/upgradeAll.svg")

val R_PACKAGES = load("toolWindow/RPackages.svg")

val R_GRAPH = load("toolWindow/RGraph.svg")

val R_HTML = load("toolWindow/RHtml.svg")

val R_CHANGE_WORKING_DIRECTORY = load("changeWorkingDirectory.svg")

val R_RMARKDOWN_HTML_NOTEBOOK = load("fileTypes/htmlNotebook.svg")
val R_RMARKDOWN_PRESENTATION = load("fileTypes/presentation.svg")
val R_RMARKDOWN_DOCUMENT = load("fileTypes/rMarkdown.svg")
val R_RMARKDOWN_SHINY = load("fileTypes/shiny.svg")

private fun load(path: String) = IconLoader.findIcon("/icons/" + path)
