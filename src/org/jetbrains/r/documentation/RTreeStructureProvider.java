// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.documentation;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.NamedLibraryElementNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RTreeStructureProvider implements TreeStructureProvider, DumbAware {
    @NotNull
    @Override
    public Collection<AbstractTreeNode> modify(@NotNull AbstractTreeNode parent,
                                               @NotNull Collection<AbstractTreeNode> children,
                                               ViewSettings settings) {
        final Project project = parent.getProject();
        if (parent instanceof NamedLibraryElementNode) {
            List<AbstractTreeNode> newChildren = new ArrayList<AbstractTreeNode>();
            for (final AbstractTreeNode child : children) {
                if (child instanceof PsiDirectoryNode) {
                    newChildren.add(new PsiDirectoryNode(project, ((PsiDirectoryNode) child).getValue(), ((PsiDirectoryNode) child).getSettings()) {

                        @Override
                        protected void updateImpl(PresentationData data) {
                            super.updateImpl(data);
                            final VirtualFile file = ((PsiDirectoryNode) child).getVirtualFile();
                            if (file != null) {
                                final VirtualFile fileParent = file.getParent();
                                if (fileParent != null) {
                                    data.setPresentableText(fileParent.getName());
                                }
                            }
                            data.setLocationString("");
                        }
                    });
                } else {
                    newChildren.add(child);
                }
            }
            return newChildren;
        }
        return children;
    }


    @Override
    public Object getData(Collection<AbstractTreeNode> selected, String dataName) {
        return null;
    }
}
