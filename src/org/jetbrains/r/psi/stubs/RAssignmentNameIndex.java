// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.psi.stubs;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.psi.api.RAssignmentStatement;

import java.util.Collection;

public class RAssignmentNameIndex extends StringStubIndexExtension<RAssignmentStatement> {
    private static final StubIndexKey<String, RAssignmentStatement> KEY = StubIndexKey.createIndexKey("R.function.shortName");

    @Override
    public @NotNull StubIndexKey<String, RAssignmentStatement> getKey() {
        return KEY;
    }


    public static Collection<RAssignmentStatement> find(String name, Project project, GlobalSearchScope scope) {
        return StubIndex.getElements(KEY, name, project, scope, RAssignmentStatement.class);
    }

    public static void sink(@NotNull IndexSink sink, @NotNull String name) {
        sink.occurrence(RAssignmentNameIndex.KEY, name);
    }
}
