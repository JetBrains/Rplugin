/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.lang.Language
import com.intellij.psi.templateLanguages.TemplateLanguage
import org.intellij.plugins.markdown.lang.MarkdownLanguage

object RMarkdownLanguage : Language(MarkdownLanguage.INSTANCE,"RMarkdown"), TemplateLanguage