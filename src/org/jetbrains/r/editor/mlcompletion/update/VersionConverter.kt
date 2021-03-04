package org.jetbrains.r.editor.mlcompletion.update

import com.intellij.util.xmlb.Converter
import org.eclipse.aether.version.InvalidVersionSpecificationException
import org.eclipse.aether.version.Version
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager

object VersionConverter : Converter<Version?>() {
  override fun fromString(value: String): Version? = try {
    ArtifactRepositoryManager.asVersion(value)
  } catch (e: InvalidVersionSpecificationException) {
    null
  }

  override fun toString(value: Version): String = value.toString()
}