/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.visualize

import org.jetbrains.r.rinterop.Service
import javax.swing.RowFilter

class RRowFilter(val proto: Service.DataFrameFilterRequest.Filter): RowFilter<Any?, Any?>() {
  override fun include(p0: Entry<out Any?, out Any?>?): Nothing {
    throw NotImplementedError()
  }
}