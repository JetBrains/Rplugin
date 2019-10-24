// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova


/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.editor;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;
import org.jetbrains.annotations.NonNls;


/**
 * Add some live templates for R
 *
 * @author Holger Brandl
 */
public class LiveTemplateProvider implements DefaultLiveTemplatesProvider {

    private static final
    @NonNls
    String[] DEFAULT_TEMPLATES = new String[]{
            "/liveTemplates/rtemplates",
            "/liveTemplates/surround"
    };


    public String[] getDefaultLiveTemplateFiles() {
        return DEFAULT_TEMPLATES;
    }


    @Override
    public String[] getHiddenLiveTemplateFiles() {
        return null;
    }
}
