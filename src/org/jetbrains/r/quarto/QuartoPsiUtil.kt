package org.jetbrains.r.quarto

import java.util.regex.Pattern

object QuartoPsiUtil {

  private val executableFenceLabelPattern: Pattern = Pattern.compile("\\{(\\w+)([^,]*)(,.*)?}", Pattern.DOTALL)

  fun getExecutableFenceLanguage(fullFenceHeader: CharSequence): String? {
    val matcher = executableFenceLabelPattern.matcher(fullFenceHeader)
    if (matcher.matches()) {
      return matcher.group(1).toLowerCase()
    }
    return null
  }
}
