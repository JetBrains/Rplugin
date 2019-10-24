// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2011-present Greg Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.r.refactoring;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.ui.UsageViewDescriptorAdapter;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.psi.RElementFactory;
import org.jetbrains.r.psi.api.RAssignmentStatement;
import org.jetbrains.r.psi.api.RExpression;
import org.jetbrains.r.psi.api.RIdentifierExpression;
import org.jetbrains.r.psi.api.RPsiElement;

import java.util.List;


/**
 * Inlining of assignment statements.
 *
 * @author Holger Brandl
 */
public class InlineAssignmentProcessor extends BaseRefactoringProcessor {
    private static final Logger LOG = Logger.getInstance(InlineAssignmentProcessor.class);

    private RAssignmentStatement myExpression;
    private final PsiReference myReference;

    private final boolean myInlineThisOnly;


    @SuppressWarnings("WeakerAccess")
    public InlineAssignmentProcessor(RAssignmentStatement rule, Project project, PsiReference ref, boolean isInlineThisOnly) {
        super(project);
        myExpression = rule;
        myReference = ref;
        myInlineThisOnly = isInlineThisOnly;
    }


    @NotNull
    protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
        return new UsageViewDescriptorAdapter() {
            @NotNull
            @Override
            public PsiElement[] getElements() {
                return new PsiElement[]{myExpression};
            }


            @Override
            public String getProcessedElementsHeader() {
                return "Expression";
            }
        };
//        return new BnfInlineViewDescriptor(myExpression);
    }


    protected String getCommandName() {
        return "Inline rule '" + myExpression.getName() + "'";
    }


    @NotNull
    protected UsageInfo[] findUsages() {
        RExpression assignee = myExpression.getAssignee();
        if (assignee == null) {
            return UsageInfo.EMPTY_ARRAY;
        }
        if (myInlineThisOnly) return new UsageInfo[]{new UsageInfo(myReference.getElement())};

        List<UsageInfo> result = ContainerUtil.newArrayList();

        for (PsiReference reference : ReferencesSearch.search(assignee, myExpression.getUseScope(), false)) {
            PsiElement element = reference.getElement();
            result.add(new UsageInfo(element));
        }
        return result.toArray(new UsageInfo[0]);
    }


    protected void refreshElements(@NotNull PsiElement[] elements) {
        LOG.assertTrue(elements.length == 1 && elements[0] instanceof RAssignmentStatement);

        myExpression = (RAssignmentStatement) elements[0];
    }


    protected void performRefactoring(@NotNull UsageInfo[] usages) {
        CommonRefactoringUtil.sortDepthFirstRightLeftOrder(usages);
        if (myExpression.getAssignedValue() == null) {
            return;
        }

        for (UsageInfo info : usages) {
            try {
                if (!(info.getElement() instanceof RIdentifierExpression)) {
                    continue;
                }
                final RIdentifierExpression element = (RIdentifierExpression) info.getElement();
                inlineExpressionUsage(element, myExpression.getAssignedValue());
            } catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }

        if (!myInlineThisOnly) {
            try {
                myExpression.delete();
            } catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }


    private static void inlineExpressionUsage(PsiElement place, RPsiElement ruleExpr) throws IncorrectOperationException {
        PsiElement replacement = RElementFactory.createLeafFromText(ruleExpr.getProject(), ruleExpr.getText());
        place.replace(replacement);
    }
}
