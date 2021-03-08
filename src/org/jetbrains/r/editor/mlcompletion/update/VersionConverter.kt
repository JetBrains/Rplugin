package org.jetbrains.r.editor.mlcompletion.update

import com.intellij.openapi.util.Version
import com.intellij.util.xmlb.Converter

object VersionConverter : Converter<Version?>() {
  override fun fromString(value: String): Version? = Version.parseVersion(value)

  override fun toString(value: Version): String = value.toString()
}