/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.r.psi.psi.stubs

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.r.psi.psi.api.RParameter
import com.intellij.r.psi.psi.api.RPsiElement

interface RParameterStub : NamedStub<RParameter> {
  override fun getName(): String
}

class RParameterStubImpl(private val name: String,
                         parent: StubElement<in RPsiElement>,
                         stubElementType: IStubElementType<in RParameterStub, in RParameter>)
  : StubBase<RParameter>(parent, stubElementType), RParameterStub {

  override fun getName(): String = name
}