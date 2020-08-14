// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.parsing;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.r.psi.RElementType;
import org.jetbrains.r.psi.RElementTypeFactory;
import org.jetbrains.r.psi.impl.*;

public interface RElementTypes {

  IElementType R_AND_OPERATOR = new RElementType("R_AND_OPERATOR");
  IElementType R_ARGUMENT_LIST = new RElementType("R_ARGUMENT_LIST");
  IElementType R_ASSIGNMENT_STATEMENT = RElementTypeFactory.getElementTypeByName("R_ASSIGNMENT_STATEMENT");
  IElementType R_ASSIGN_OPERATOR = new RElementType("R_ASSIGN_OPERATOR");
  IElementType R_AT_EXPRESSION = new RElementType("R_AT_EXPRESSION");
  IElementType R_AT_OPERATOR = new RElementType("R_AT_OPERATOR");
  IElementType R_BLOCK_EXPRESSION = new RElementType("R_BLOCK_EXPRESSION");
  IElementType R_BOOLEAN_LITERAL = new RElementType("R_BOOLEAN_LITERAL");
  IElementType R_BOUNDARY_LITERAL = new RElementType("R_BOUNDARY_LITERAL");
  IElementType R_BREAK_STATEMENT = new RElementType("R_BREAK_STATEMENT");
  IElementType R_CALL_EXPRESSION = RElementTypeFactory.getElementTypeByName("R_CALL_EXPRESSION");
  IElementType R_COLON_OPERATOR = new RElementType("R_COLON_OPERATOR");
  IElementType R_COMPARE_OPERATOR = new RElementType("R_COMPARE_OPERATOR");
  IElementType R_EMPTY_EXPRESSION = new RElementType("R_EMPTY_EXPRESSION");
  IElementType R_EXPRESSION = new RElementType("R_EXPRESSION");
  IElementType R_EXP_OPERATOR = new RElementType("R_EXP_OPERATOR");
  IElementType R_FOR_STATEMENT = new RElementType("R_FOR_STATEMENT");
  IElementType R_FUNCTION_EXPRESSION = new RElementType("R_FUNCTION_EXPRESSION");
  IElementType R_HELP_EXPRESSION = new RElementType("R_HELP_EXPRESSION");
  IElementType R_IDENTIFIER_EXPRESSION = new RElementType("R_IDENTIFIER_EXPRESSION");
  IElementType R_IF_STATEMENT = new RElementType("R_IF_STATEMENT");
  IElementType R_INFIX_OPERATOR = new RElementType("R_INFIX_OPERATOR");
  IElementType R_INVALID_LITERAL = new RElementType("R_INVALID_LITERAL");
  IElementType R_LIST_SUBSET_OPERATOR = new RElementType("R_LIST_SUBSET_OPERATOR");
  IElementType R_MEMBER_EXPRESSION = new RElementType("R_MEMBER_EXPRESSION");
  IElementType R_MULDIV_OPERATOR = new RElementType("R_MULDIV_OPERATOR");
  IElementType R_NAMED_ARGUMENT = new RElementType("R_NAMED_ARGUMENT");
  IElementType R_NAMESPACE_ACCESS_EXPRESSION = new RElementType("R_NAMESPACE_ACCESS_EXPRESSION");
  IElementType R_NA_LITERAL = new RElementType("R_NA_LITERAL");
  IElementType R_NEXT_STATEMENT = new RElementType("R_NEXT_STATEMENT");
  IElementType R_NOT_OPERATOR = new RElementType("R_NOT_OPERATOR");
  IElementType R_NO_COMMA_TAIL = new RElementType("R_NO_COMMA_TAIL");
  IElementType R_NULL_LITERAL = new RElementType("R_NULL_LITERAL");
  IElementType R_NUMERIC_LITERAL_EXPRESSION = new RElementType("R_NUMERIC_LITERAL_EXPRESSION");
  IElementType R_OPERATOR_EXPRESSION = new RElementType("R_OPERATOR_EXPRESSION");
  IElementType R_OR_OPERATOR = new RElementType("R_OR_OPERATOR");
  IElementType R_PARAMETER = RElementTypeFactory.getElementTypeByName("R_PARAMETER");
  IElementType R_PARAMETER_LIST = new RElementType("R_PARAMETER_LIST");
  IElementType R_PARENTHESIZED_EXPRESSION = new RElementType("R_PARENTHESIZED_EXPRESSION");
  IElementType R_PLUSMINUS_OPERATOR = new RElementType("R_PLUSMINUS_OPERATOR");
  IElementType R_REPEAT_STATEMENT = new RElementType("R_REPEAT_STATEMENT");
  IElementType R_STRING_LITERAL_EXPRESSION = new RElementType("R_STRING_LITERAL_EXPRESSION");
  IElementType R_SUBSCRIPTION_EXPRESSION = new RElementType("R_SUBSCRIPTION_EXPRESSION");
  IElementType R_TILDE_EXPRESSION = new RElementType("R_TILDE_EXPRESSION");
  IElementType R_TILDE_OPERATOR = new RElementType("R_TILDE_OPERATOR");
  IElementType R_UNARY_NOT_EXPRESSION = new RElementType("R_UNARY_NOT_EXPRESSION");
  IElementType R_UNARY_PLUSMINUS_EXPRESSION = new RElementType("R_UNARY_PLUSMINUS_EXPRESSION");
  IElementType R_UNARY_TILDE_EXPRESSION = new RElementType("R_UNARY_TILDE_EXPRESSION");
  IElementType R_WHILE_STATEMENT = new RElementType("R_WHILE_STATEMENT");

  IElementType R_AND = new RElementType("&");
  IElementType R_ANDAND = new RElementType("&&");
  IElementType R_AT = new RElementType("@");
  IElementType R_BREAK = new RElementType("break");
  IElementType R_COLON = new RElementType(":");
  IElementType R_COMMA = new RElementType(",");
  IElementType R_COMPLEX = new RElementType("COMPLEX");
  IElementType R_DIV = new RElementType("/");
  IElementType R_DOUBLECOLON = new RElementType("::");
  IElementType R_ELSE = new RElementType("else");
  IElementType R_EQ = new RElementType("=");
  IElementType R_EQEQ = new RElementType("==");
  IElementType R_EXP = new RElementType("^");
  IElementType R_FALSE = new RElementType("FALSE");
  IElementType R_FOR = new RElementType("for");
  IElementType R_FUNCTION = new RElementType("function");
  IElementType R_GE = new RElementType(">=");
  IElementType R_GT = new RElementType(">");
  IElementType R_HELP = new RElementType("help");
  IElementType R_IDENTIFIER = new RElementType("identifier");
  IElementType R_IF = new RElementType("if");
  IElementType R_IN = new RElementType("in");
  IElementType R_INF = new RElementType("Inf");
  IElementType R_INFIX_OP = new RElementType("INFIX_OP");
  IElementType R_INTEGER = new RElementType("INTEGER");
  IElementType R_INVALID_STRING = new RElementType("invalid string");
  IElementType R_LBRACE = new RElementType("{");
  IElementType R_LBRACKET = new RElementType("[");
  IElementType R_LDBRACKET = new RElementType("[[");
  IElementType R_LE = new RElementType("<=");
  IElementType R_LEFT_ASSIGN = new RElementType("<-");
  IElementType R_LEFT_ASSIGN_OLD = new RElementType(":=");
  IElementType R_LEFT_COMPLEX_ASSIGN = new RElementType("<<-");
  IElementType R_LIST_SUBSET = new RElementType("$");
  IElementType R_LPAR = new RElementType("(");
  IElementType R_LT = new RElementType("<");
  IElementType R_MINUS = new RElementType("-");
  IElementType R_MULT = new RElementType("*");
  IElementType R_NA = new RElementType("NA");
  IElementType R_NAN = new RElementType("NaN");
  IElementType R_NA_CHARACTER_ = new RElementType("NA_character_");
  IElementType R_NA_COMPLEX_ = new RElementType("NA_complex_");
  IElementType R_NA_INTEGER_ = new RElementType("NA_integer_");
  IElementType R_NA_REAL_ = new RElementType("NA_real_");
  IElementType R_NEXT = new RElementType("next");
  IElementType R_NOT = new RElementType("!");
  IElementType R_NOTEQ = new RElementType("!=");
  IElementType R_NULL = new RElementType("NULL");
  IElementType R_NUMERIC = new RElementType("NUMERIC");
  IElementType R_OR = new RElementType("|");
  IElementType R_OROR = new RElementType("||");
  IElementType R_PLUS = new RElementType("+");
  IElementType R_RBRACE = new RElementType("}");
  IElementType R_RBRACKET = new RElementType("]");
  IElementType R_RDBRACKET = new RElementType("]]");
  IElementType R_REPEAT = new RElementType("repeat");
  IElementType R_RIGHT_ASSIGN = new RElementType("->");
  IElementType R_RIGHT_COMPLEX_ASSIGN = new RElementType("->>");
  IElementType R_RPAR = new RElementType(")");
  IElementType R_SEMI = new RElementType(";");
  IElementType R_STRING = new RElementType("STRING");
  IElementType R_TICK = new RElementType("`");
  IElementType R_TILDE = new RElementType("~");
  IElementType R_TRIPLECOLON = new RElementType(":::");
  IElementType R_TRIPLE_DOTS = new RElementType("TRIPLE_DOTS");
  IElementType R_TRUE = new RElementType("TRUE");
  IElementType R_UNDERSCORE = new RElementType("_");
  IElementType R_WHILE = new RElementType("while");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == R_AND_OPERATOR) {
        return new RAndOperatorImpl(node);
      }
      else if (type == R_ARGUMENT_LIST) {
        return new RArgumentListImpl(node);
      }
      else if (type == R_ASSIGNMENT_STATEMENT) {
        return new RAssignmentStatementImpl(node);
      }
      else if (type == R_ASSIGN_OPERATOR) {
        return new RAssignOperatorImpl(node);
      }
      else if (type == R_AT_EXPRESSION) {
        return new RAtExpressionImpl(node);
      }
      else if (type == R_AT_OPERATOR) {
        return new RAtOperatorImpl(node);
      }
      else if (type == R_BLOCK_EXPRESSION) {
        return new RBlockExpressionImpl(node);
      }
      else if (type == R_BOOLEAN_LITERAL) {
        return new RBooleanLiteralImpl(node);
      }
      else if (type == R_BOUNDARY_LITERAL) {
        return new RBoundaryLiteralImpl(node);
      }
      else if (type == R_BREAK_STATEMENT) {
        return new RBreakStatementImpl(node);
      }
      else if (type == R_CALL_EXPRESSION) {
        return new RCallExpressionImpl(node);
      }
      else if (type == R_COLON_OPERATOR) {
        return new RColonOperatorImpl(node);
      }
      else if (type == R_COMPARE_OPERATOR) {
        return new RCompareOperatorImpl(node);
      }
      else if (type == R_EMPTY_EXPRESSION) {
        return new REmptyExpressionImpl(node);
      }
      else if (type == R_EXP_OPERATOR) {
        return new RExpOperatorImpl(node);
      }
      else if (type == R_FOR_STATEMENT) {
        return new RForStatementImpl(node);
      }
      else if (type == R_FUNCTION_EXPRESSION) {
        return new RFunctionExpressionImpl(node);
      }
      else if (type == R_HELP_EXPRESSION) {
        return new RHelpExpressionImpl(node);
      }
      else if (type == R_IDENTIFIER_EXPRESSION) {
        return new RIdentifierExpressionImpl(node);
      }
      else if (type == R_IF_STATEMENT) {
        return new RIfStatementImpl(node);
      }
      else if (type == R_INFIX_OPERATOR) {
        return new RInfixOperatorImpl(node);
      }
      else if (type == R_INVALID_LITERAL) {
        return new RInvalidLiteralImpl(node);
      }
      else if (type == R_LIST_SUBSET_OPERATOR) {
        return new RListSubsetOperatorImpl(node);
      }
      else if (type == R_MEMBER_EXPRESSION) {
        return new RMemberExpressionImpl(node);
      }
      else if (type == R_MULDIV_OPERATOR) {
        return new RMuldivOperatorImpl(node);
      }
      else if (type == R_NAMED_ARGUMENT) {
        return new RNamedArgumentImpl(node);
      }
      else if (type == R_NAMESPACE_ACCESS_EXPRESSION) {
        return new RNamespaceAccessExpressionImpl(node);
      }
      else if (type == R_NA_LITERAL) {
        return new RNaLiteralImpl(node);
      }
      else if (type == R_NEXT_STATEMENT) {
        return new RNextStatementImpl(node);
      }
      else if (type == R_NOT_OPERATOR) {
        return new RNotOperatorImpl(node);
      }
      else if (type == R_NO_COMMA_TAIL) {
        return new RNoCommaTailImpl(node);
      }
      else if (type == R_NULL_LITERAL) {
        return new RNullLiteralImpl(node);
      }
      else if (type == R_NUMERIC_LITERAL_EXPRESSION) {
        return new RNumericLiteralExpressionImpl(node);
      }
      else if (type == R_OPERATOR_EXPRESSION) {
        return new ROperatorExpressionImpl(node);
      }
      else if (type == R_OR_OPERATOR) {
        return new ROrOperatorImpl(node);
      }
      else if (type == R_PARAMETER) {
        return new RParameterImpl(node);
      }
      else if (type == R_PARAMETER_LIST) {
        return new RParameterListImpl(node);
      }
      else if (type == R_PARENTHESIZED_EXPRESSION) {
        return new RParenthesizedExpressionImpl(node);
      }
      else if (type == R_PLUSMINUS_OPERATOR) {
        return new RPlusminusOperatorImpl(node);
      }
      else if (type == R_REPEAT_STATEMENT) {
        return new RRepeatStatementImpl(node);
      }
      else if (type == R_STRING_LITERAL_EXPRESSION) {
        return new RStringLiteralExpressionImpl(node);
      }
      else if (type == R_SUBSCRIPTION_EXPRESSION) {
        return new RSubscriptionExpressionImpl(node);
      }
      else if (type == R_TILDE_EXPRESSION) {
        return new RTildeExpressionImpl(node);
      }
      else if (type == R_TILDE_OPERATOR) {
        return new RTildeOperatorImpl(node);
      }
      else if (type == R_UNARY_NOT_EXPRESSION) {
        return new RUnaryNotExpressionImpl(node);
      }
      else if (type == R_UNARY_PLUSMINUS_EXPRESSION) {
        return new RUnaryPlusminusExpressionImpl(node);
      }
      else if (type == R_UNARY_TILDE_EXPRESSION) {
        return new RUnaryTildeExpressionImpl(node);
      }
      else if (type == R_WHILE_STATEMENT) {
        return new RWhileStatementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
