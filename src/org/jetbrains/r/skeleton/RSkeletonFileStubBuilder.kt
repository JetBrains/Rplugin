/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.BinaryFileStubBuilder
import com.intellij.psi.stubs.Stub
import com.intellij.util.indexing.FileContent
import org.jetbrains.r.hints.parameterInfo.RExtraNamedArgumentsInfo
import org.jetbrains.r.packages.LibrarySummary
import org.jetbrains.r.parsing.RParserDefinition
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStub
import org.jetbrains.r.skeleton.psi.RSkeletonElementTypes.R_SKELETON_ASSIGNMENT_STATEMENT
import org.jetbrains.r.skeleton.psi.RSkeletonFileStub
import java.io.ByteArrayInputStream

class RSkeletonFileStubBuilder : BinaryFileStubBuilder {
  override fun getStubVersion(): Int = RParserDefinition.FILE.stubVersion

  override fun buildStubTree(fileContent: FileContent): Stub {
    val skeletonFileStub = RSkeletonFileStub()
    val content = fileContent.getContent()
    val binPackage: LibrarySummary.RLibraryPackage = ByteArrayInputStream(content).use {
      LibrarySummary.RLibraryPackage.parseFrom(it)
    }
    for (symbol in binPackage.symbolsList) {
      val extraNamedArguments = symbol.extraNamedArguments
      RSkeletonAssignmentStub(skeletonFileStub,
                              R_SKELETON_ASSIGNMENT_STATEMENT,
                              symbol.name,
                              symbol.type,
                              symbol.parameters,
                              symbol.exported,
                              RExtraNamedArgumentsInfo(extraNamedArguments.argNamesList, extraNamedArguments.funArgNamesList))

    }
    return skeletonFileStub
  }

  override fun acceptsFile(file: VirtualFile): Boolean = true
}
