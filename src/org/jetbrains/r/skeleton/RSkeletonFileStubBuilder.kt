/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.skeleton

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.stubs.BinaryFileStubBuilder
import com.intellij.psi.stubs.Stub
import com.intellij.util.indexing.FileContent
import org.jetbrains.r.classes.s4.classInfo.RS4ClassInfo
import org.jetbrains.r.classes.s4.classInfo.RS4ClassSlot
import org.jetbrains.r.classes.s4.classInfo.RS4SuperClass
import org.jetbrains.r.classes.s4.methods.RS4GenericInfo
import org.jetbrains.r.classes.s4.methods.RS4GenericSignature
import org.jetbrains.r.classes.s4.methods.RS4MethodParameterInfo
import org.jetbrains.r.classes.s4.methods.RS4RawMethodInfo
import org.jetbrains.r.hints.parameterInfo.RExtraNamedArgumentsInfo
import org.jetbrains.r.packages.LibrarySummary
import org.jetbrains.r.packages.LibrarySummary.RLibrarySymbol.*
import org.jetbrains.r.parsing.RParserDefinition
import org.jetbrains.r.skeleton.psi.RSkeletonAssignmentStub
import org.jetbrains.r.skeleton.psi.RSkeletonCallExpressionStub
import org.jetbrains.r.skeleton.psi.RSkeletonElementTypes.R_SKELETON_ASSIGNMENT_STATEMENT
import org.jetbrains.r.skeleton.psi.RSkeletonElementTypes.R_SKELETON_CALL_EXPRESSION
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
      when (symbol.representationCase) {
        RepresentationCase.S4CLASSREPRESENTATION -> {
          val s4ClassRepresentation = symbol.s4ClassRepresentation
          RSkeletonCallExpressionStub(skeletonFileStub,
                                      R_SKELETON_CALL_EXPRESSION,
                                      RS4ClassInfo(symbol.name,
                                                   s4ClassRepresentation.packageName,
                                                   s4ClassRepresentation.slotsList.map { RS4ClassSlot(it.name, it.type, it.declarationClass)
                                                   },
                                                 s4ClassRepresentation.superClassesList.map {
                                                   RS4SuperClass(it.name, it.distance) },
                                                   s4ClassRepresentation.isVirtual
                                      )
          )
        }
        else -> {
          val functionRepresentation = symbol.functionRepresentation
          val (s4GenericOrMethodInfo, extraNamedArguments) =
          when (symbol.type) {
            Type.S4GENERIC -> {
              val signature = functionRepresentation.s4GenericSignature.let { RS4GenericSignature(it.parametersList, it.valueClassesList, false) }
              RS4GenericInfo(symbol.name, signature) to FunctionRepresentation.ExtraNamedArguments.getDefaultInstance()
            }
            Type.S4METHOD -> {
              val methodsParameters = functionRepresentation.s4ParametersInfo.s4MethodParametersList.map {
                RS4MethodParameterInfo(it.name, it.type)
              }
              RS4RawMethodInfo(symbol.name, methodsParameters) to FunctionRepresentation.ExtraNamedArguments.getDefaultInstance()
            }
            else -> null to functionRepresentation.extraNamedArguments
          }
          RSkeletonAssignmentStub(skeletonFileStub,
                                  R_SKELETON_ASSIGNMENT_STATEMENT,
                                  symbol.name,
                                  symbol.type,
                                  functionRepresentation.parameters,
                                  symbol.exported,
                                  RExtraNamedArgumentsInfo(extraNamedArguments.argNamesList,
                                                           extraNamedArguments.funArgNamesList),
                                s4GenericOrMethodInfo)
        }
      }
    }
    return skeletonFileStub
  }

  override fun getFileFilter(): VirtualFileFilter = VirtualFileFilter.ALL

  override fun acceptsFile(file: VirtualFile): Boolean = true
}
