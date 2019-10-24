/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.bin

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.BinaryFileStubBuilder
import com.intellij.psi.stubs.Stub
import com.intellij.util.indexing.FileContent
import org.jetbrains.r.bin.psi.RBinAssignmentStub
import org.jetbrains.r.bin.psi.RBinElementTypes.R_BIN_ASSIGNMENT_STATEMENT
import org.jetbrains.r.bin.psi.RBinFileStub
import org.jetbrains.r.packages.LibrarySummary
import org.jetbrains.r.parsing.RParserDefinition
import java.io.ByteArrayInputStream

class RBinFileStubBuilder : BinaryFileStubBuilder {
  //TODO: Do I need RBinFileElementType at all?
  override fun getStubVersion(): Int = RParserDefinition.FILE.stubVersion

  override fun buildStubTree(fileContent: FileContent): Stub {
    val rBinFileStub = RBinFileStub()
    val content = fileContent.getContent()
    val binPackage: LibrarySummary.RLibraryPackage = ByteArrayInputStream(content).use {
      LibrarySummary.RLibraryPackage.parseFrom(it)
    }
    for (symbol in binPackage.symbolsList) {
      if (!symbol.exported) {
        continue
      }

      RBinAssignmentStub(rBinFileStub,
                         R_BIN_ASSIGNMENT_STATEMENT,
                         symbol.name,
                         symbol.type == LibrarySummary.RLibrarySymbol.Type.FUNCTION,
                         symbol.parameters)

    }
    return rBinFileStub
  }

  override fun acceptsFile(file: VirtualFile): Boolean = true
}
