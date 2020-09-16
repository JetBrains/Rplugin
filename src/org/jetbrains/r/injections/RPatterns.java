package org.jetbrains.r.injections;

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil;
import org.jetbrains.r.psi.api.*;

public class RPatterns {
  /**
   * Used in rplugin/resources/injections/rInjections.xml
   */
  public static @NotNull PsiElementPattern.Capture<RStringLiteralExpression> callArgumentPattern(@NotNull String methodExpression,
                                                                                                 @NotNull String argumentName) {
    String debugPatternName = String.format("%s(%s)", methodExpression, argumentName);
    return PlatformPatterns.psiElement(RStringLiteralExpression.class).with(new PatternCondition<>(debugPatternName) {
      @Override
      public boolean accepts(@NotNull final RStringLiteralExpression stringLiteral, final ProcessingContext context) {
        PsiElement parent = stringLiteral.getParent();
        if (parent instanceof RNamedArgument) {
          parent = parent.getParent();
        }
        if (parent instanceof RArgumentList) {
          RArgumentList argumentList = (RArgumentList) parent;

          RCallExpression call = (RCallExpression)argumentList.getParent();
          RExpression receiver = call.getExpression();
          String[] methodNames = methodExpression.split("\\|");
          if (!(receiver instanceof RIdentifierExpression) || !ArrayUtil.contains(receiver.getName(), methodNames)) {
            return false;
          }
          RExpression argumentValue = RParameterInfoUtil.INSTANCE.getArgumentByName(call, argumentName);
          return argumentValue == stringLiteral;
        }
        return false;
      }
    });
  }
}
