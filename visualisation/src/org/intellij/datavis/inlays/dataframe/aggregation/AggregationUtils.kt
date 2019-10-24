/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.intellij.datavis.inlays.dataframe.aggregation

fun ArrayList<String?>.min(): String? {

    var result: String? = null
    for (i in 0 until this.size) {
        if (this[i] == null)
            continue

        result = if (result == null) {
            this[i]
        } else {
            if (this[i]!! < result) this[i] else result
        }
    }

    return result
}

fun ArrayList<String?>.max(): String? {

    var result: String? = null
    for (i in 0 until this.size) {
        if (this[i] == null)
            continue

        result = if (result == null) {
            this[i]
        } else {
            if (this[i]!! > result) this[i] else result
        }
    }

    return result
}