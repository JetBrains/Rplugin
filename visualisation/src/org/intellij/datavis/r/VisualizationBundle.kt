/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.r

import com.intellij.CommonBundle
import com.intellij.reference.SoftReference
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.lang.ref.Reference
import java.util.*

object VisualizationBundle {
  private var ourBundle: Reference<ResourceBundle>? = null

  @NonNls
  private const val BUNDLE = "messages.VisualizationBundle"

  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return CommonBundle.message(bundle, key, *params)
  }

  // Cached loading
  private val bundle: ResourceBundle
    get() {
      var bundle = SoftReference.dereference(ourBundle)
      if (bundle == null) {
        bundle = ResourceBundle.getBundle(BUNDLE)!!
        ourBundle = SoftReference<ResourceBundle>(bundle)
      }
      return bundle
    }
}