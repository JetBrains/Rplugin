// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2011-2012 Gregory Shrago
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

package org.jetbrains.r.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import org.jetbrains.annotations.NotNull;


/**
 * @author gregsh
 */
public class RStringLiteralEscaper extends LiteralTextEscaper<RStringInjectHost> {

    public RStringLiteralEscaper(RStringInjectHost element) {
        super(element);
    }


    @Override
    public boolean decode(@NotNull final TextRange rangeInsideHost, @NotNull final StringBuilder outChars) {
        // todo implement proper java-like string escapes support
        TextRange.assertProperRange(rangeInsideHost);
        outChars.append(myHost.getText(), rangeInsideHost.getStartOffset(), rangeInsideHost.getEndOffset());
        return true;
    }


    @Override
    public int getOffsetInHost(final int offsetInDecoded, @NotNull final TextRange rangeInsideHost) {
        TextRange.assertProperRange(rangeInsideHost);
        int offset = offsetInDecoded;
        // todo implement proper java-like string escapes support
        offset += rangeInsideHost.getStartOffset();
        if (offset < rangeInsideHost.getStartOffset()) offset = rangeInsideHost.getStartOffset();
        if (offset > rangeInsideHost.getEndOffset()) offset = rangeInsideHost.getEndOffset();
        return offset;
    }


    @Override
    public boolean isOneLine() {
        return true;
    }
}

