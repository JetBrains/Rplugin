package org.jetbrains.r.injections;

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo;
import org.jetbrains.r.hints.parameterInfo.RParameterInfoUtil;
import org.jetbrains.r.psi.api.*;

public class RPatterns {
  /**
   * Used in rplugin/resources/injections/rInjections.xml
   */
  public static @NotNull PsiElementPattern.Capture<RStringLiteralExpression> callArgumentPattern(@NotNull String methodName,
                                                                                                 @NotNull String argumentName) {
    String debugPatternName = String.format("%s(%s)", methodName, argumentName);
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
          if (!(receiver instanceof RIdentifierExpression) || !methodName.equals(receiver.getName())) {
            return false;
          }
          RArgumentInfo argumentInfo = RParameterInfoUtil.INSTANCE.getArgumentInfo(call);
          if (argumentInfo != null) {
            return argumentInfo.getArgumentPassedToParameter(argumentName) == stringLiteral;
          }
        }
        return false;
      }
    });
  }
}
