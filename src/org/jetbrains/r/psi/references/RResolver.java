// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.references;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.interpreter.RInterpreter;
import org.jetbrains.r.interpreter.RInterpreterManager;
import org.jetbrains.r.psi.RPsiUtil;
import org.jetbrains.r.psi.api.*;
import org.jetbrains.r.psi.stubs.RAssignmentNameIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class RResolver {

  protected static final Logger LOG = Logger.getInstance("#" + RResolver.class.getName());

  public static void resolveWithNamespace(@NotNull final Project project,
                                          String name,
                                          String namespace,
                                          @NotNull final List<ResolveResult> result) {
    RInterpreter interpreter = RInterpreterManager.Companion.getInstance(project).getInterpreter();
    if (interpreter == null) {
      return;
    }
    PsiFile psiFile = interpreter.getSkeletonFileByPackageName(namespace);
    if (psiFile == null) {
      return;
    }
    Collection<RAssignmentStatement> statements = RAssignmentNameIndex.find(name, project, GlobalSearchScope.fileScope(psiFile));

    for (RAssignmentStatement statement : statements) {
      if (Objects.equals(statement.getName(), name)) {
        result.add(new PsiElementResolveResult(statement));
      }
    }
  }


  public static <T> Predicate<T> not(Predicate<T> t) {
    return t.negate();
  }


  static void resolveNameArgument(@NotNull final PsiElement element,
                                  String elementName,
                                  @NotNull final List<ResolveResult> result) {
    RCallExpression callExpression = PsiTreeUtil.getParentOfType(element, RCallExpression.class);
    if (callExpression != null) {
      RFunctionExpression functionExpression = RPsiUtil.INSTANCE.getFunction(callExpression);
      RParameterList parameterList = PsiTreeUtil.getChildOfType(functionExpression, RParameterList.class);
      if (parameterList != null) {
        for (RParameter parameter : parameterList.getParameterList()) {
          String name = parameter.getName();
          if (name != null && name.equals(elementName)) {
            result.add(0, new PsiElementResolveResult(parameter));
            return;
          }
        }
      }
    }
  }


  // TODO: massive refactoring awaits!!!
  static void resolveFunctionCall(PsiElement myElement, String name, List<ResolveResult> result) {
    PsiElement parent = myElement.getParent();

    if (parent instanceof RCallExpression) {
      RCallExpression call = ((RCallExpression)parent);
      List<RExpression> arguments = call.getArgumentList().getExpressionList();

      List<ResolveResult> myResult = new ArrayList<ResolveResult>();

      resolveInFileOrLibrary(myElement, name, myResult);

      // since the downstream analysis relies on having an argument, we stop if the call does not have any args
      if (call.getExpression().equals(myElement) && !arguments.isEmpty()) {
        result.addAll(myResult);
      }
    }
  }


  public static void resolveInFileOrLibrary(PsiElement element, String name, List<ResolveResult> myResult) {
    resolveFromStubs(element, myResult, name);
  }

  private static void resolveFromStubs(@NotNull final PsiElement element,
                                       @NotNull final List<ResolveResult> result,
                                       @NotNull final String... names) {

    for (String name : names) {
      Collection<RAssignmentStatement> statements =
        RAssignmentNameIndex.find(name, element.getProject(), RSearchScopeUtil.getScope(element));
      addResolveResults(result, statements);
    }
  }

  private static void addResolveResults(@NotNull List<ResolveResult> result, Collection<RAssignmentStatement> statements) {
    for (RAssignmentStatement statement : statements) {
      result.add(new PsiElementResolveResult(statement));
    }
  }
}
