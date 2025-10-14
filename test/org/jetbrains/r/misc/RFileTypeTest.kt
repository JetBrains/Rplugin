/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.misc

import com.intellij.openapi.fileTypes.FileTypeManager
import junit.framework.TestCase
import com.intellij.r.psi.RFileType
import org.jetbrains.r.RUsefulTestCase

class RFileTypeTest : RUsefulTestCase() {

  fun testRFileTypes() {
    TestCase.assertEquals(RFileType, FileTypeManager.getInstance ().getFileTypeByFileName("Foo.R"))
    TestCase.assertEquals(RFileType, FileTypeManager.getInstance ().getFileTypeByFileName(".Rprofile"))
    TestCase.assertEquals(RFileType, FileTypeManager.getInstance ().getFileTypeByFileName("Foo.r"))
    TestCase.assertEquals(RFileType, FileTypeManager.getInstance ().getFileTypeByFileName(".rprofile"))
  }
}