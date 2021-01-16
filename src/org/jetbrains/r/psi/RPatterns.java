// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi;

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.psi.api.RAssignmentStatement;
import org.jetbrains.r.psi.api.RMemberExpression;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author Holger Brandl
 */
public class RPatterns {

    public static final PsiElementPattern.Capture<PsiElement> MEMBER_ASSIGNMENT_PATTERN = psiElement()
            .withParent(psiElement(RAssignmentStatement.class).with(new PatternCondition<>("isMemberAssignee") {
              @Override
              public boolean accepts(@NotNull RAssignmentStatement psiElement, ProcessingContext processingContext) {
                return psiElement.getAssignee() instanceof RMemberExpression;
              }
            }));
}
