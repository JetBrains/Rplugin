package org.jetbrains.r.injections;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.r.hints.parameterInfo.RArgumentInfo;
import org.jetbrains.r.psi.api.*;

public final class RPatterns {
  private static final Logger LOG = Logger.getInstance(RPatterns.class);

  /**
   * Used in rplugin/resources/injections/rInjections.xml
   */
  public static @NotNull PsiElementPattern.Capture<RStringLiteralExpression> identifierCallArgument(@NotNull String filter,
                                                                                                    @NotNull String argumentName) {

    return callArgumentPattern(filter, argumentName, (receiver, pattern) -> receiver instanceof RIdentifierExpression &&
                                                                            ((RIdentifierExpression)receiver).getName().equals(pattern));
  }

  /**
   * Used in rplugin/resources/injections/rInjections.xml
   */
  public static @NotNull PsiElementPattern.Capture<RStringLiteralExpression> memberCallArgument(@NotNull String filter,
                                                                                                @NotNull String argumentName) {
    return callArgumentPattern(filter, argumentName, new RInjectionPatternFilter() {
      @Override
      public boolean accepts(@NotNull PsiElement receiver, @NotNull String pattern) {
        String[] tagAndMember = pattern.split("\\$", 2);
        if (tagAndMember.length < 2) {
          LOG.warn(String.format("Invalid pattern %s", pattern));
          return false;
        }
        if (!(receiver instanceof RMemberExpression)) {
          return false;
        }

        String tag = ((RMemberExpression)receiver).getTag();
        if (!tag.equals(tagAndMember[0])) {
          return false;
        }

        if ("*".equals(tagAndMember[1])) {
          return true;
        }
        RExpression right = ((RMemberExpression)receiver).getRightExpr();
        if (right instanceof RIdentifierExpression) {
          return StringUtil.equals(right.getName(), tagAndMember[1]);
        }
        return false;
      }
    });
  }

  private static @NotNull PsiElementPattern.Capture<RStringLiteralExpression> callArgumentPattern(
    @NotNull String injectionCallPattern,
    @NotNull String argumentName,
    @NotNull RInjectionPatternFilter patternFilter) {

    return PlatformPatterns.psiElement(RStringLiteralExpression.class).with(new PatternCondition<>(argumentName) {
      @Override
      public boolean accepts(final @NotNull RStringLiteralExpression stringLiteral, final ProcessingContext context) {
        PsiElement parent = stringLiteral.getParent();
        if (parent instanceof RNamedArgument) {
          parent = parent.getParent();
        }
        if (!(parent instanceof RArgumentList argumentList)) {
          return false;
        }

        RCallExpression call = (RCallExpression)argumentList.getParent();
        RExpression receiver = call.getExpression();

        RExpression argumentValue = RArgumentInfo.getArgumentByName(call, argumentName);
        if (argumentValue != stringLiteral) {
          return false;
        }

        String[] callPatterns = injectionCallPattern.split("\\|");
        for (String pattern : callPatterns) {
          if (patternFilter.accepts(receiver, pattern)) {
            return true;
          }
        }
        return false;
      }
    });
  }

  private interface RInjectionPatternFilter {
    boolean accepts(@NotNull PsiElement receiver, @NotNull String pattern);
  }
}
