package org.jetbrains.r.quarto

import com.intellij.lang.Language
import com.intellij.psi.templateLanguages.TemplateLanguage
import org.intellij.plugins.markdown.lang.MarkdownLanguage

object QuartoLanguage : Language(MarkdownLanguage.INSTANCE, "Quarto"), TemplateLanguage