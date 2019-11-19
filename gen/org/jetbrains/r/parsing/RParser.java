// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.jetbrains.r.parsing.RElementTypes.*;
import static org.jetbrains.r.parsing.RParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class RParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return root(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(R_AND_OPERATOR, R_ASSIGN_OPERATOR, R_AT_OPERATOR, R_COLON_OPERATOR,
      R_COMPARE_OPERATOR, R_EXP_OPERATOR, R_INFIX_OPERATOR, R_LIST_SUBSET_OPERATOR,
      R_MULDIV_OPERATOR, R_NOT_OPERATOR, R_OR_OPERATOR, R_PLUSMINUS_OPERATOR,
      R_TILDE_OPERATOR),
    create_token_set_(R_ASSIGNMENT_STATEMENT, R_BLOCK_EXPRESSION, R_BOOLEAN_LITERAL, R_BOUNDARY_LITERAL,
      R_BREAK_STATEMENT, R_CALL_EXPRESSION, R_EMPTY_EXPRESSION, R_EXPRESSION,
      R_FOR_STATEMENT, R_FUNCTION_EXPRESSION, R_HELP_EXPRESSION, R_IDENTIFIER_EXPRESSION,
      R_IF_STATEMENT, R_MEMBER_EXPRESSION, R_NAMESPACE_ACCESS_EXPRESSION, R_NA_LITERAL,
      R_NEXT_STATEMENT, R_NULL_LITERAL, R_NUMERIC_LITERAL_EXPRESSION, R_OPERATOR_EXPRESSION,
      R_PARENTHESIZED_EXPRESSION, R_REPEAT_STATEMENT, R_STRING_LITERAL_EXPRESSION, R_SUBSCRIPTION_EXPRESSION,
      R_TILDE_EXPRESSION, R_UNARY_NOT_EXPRESSION, R_UNARY_PLUSMINUS_EXPRESSION, R_UNARY_TILDE_EXPRESSION,
      R_WHILE_STATEMENT),
  };

  /* ********************************************************** */
  // '&' | '&&'
  public static boolean and_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_operator")) return false;
    if (!nextTokenIs(b, "<and operator>", R_AND, R_ANDAND)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_AND_OPERATOR, "<and operator>");
    r = consumeToken(b, R_AND);
    if (!r) r = consumeToken(b, R_ANDAND);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // named_argument | expression | external_empty_expression
  static boolean arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arg")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = named_argument(b, l + 1);
    if (!r) r = expression(b, l + 1, -1);
    if (!r) r = parseEmptyExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '(' nl* ')' | '(' nl* arg nl* (',' nl* arg nl*)* ')'
  public static boolean argument_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list")) return false;
    if (!nextTokenIs(b, R_LPAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = argument_list_0(b, l + 1);
    if (!r) r = argument_list_1(b, l + 1);
    exit_section_(b, m, R_ARGUMENT_LIST, r);
    return r;
  }

  // '(' nl* ')'
  private static boolean argument_list_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_LPAR);
    r = r && argument_list_0_1(b, l + 1);
    r = r && consumeToken(b, R_RPAR);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean argument_list_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "argument_list_0_1", c)) break;
    }
    return true;
  }

  // '(' nl* arg nl* (',' nl* arg nl*)* ')'
  private static boolean argument_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_LPAR);
    r = r && argument_list_1_1(b, l + 1);
    r = r && arg(b, l + 1);
    r = r && argument_list_1_3(b, l + 1);
    r = r && argument_list_1_4(b, l + 1);
    r = r && consumeToken(b, R_RPAR);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean argument_list_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "argument_list_1_1", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean argument_list_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "argument_list_1_3", c)) break;
    }
    return true;
  }

  // (',' nl* arg nl*)*
  private static boolean argument_list_1_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!argument_list_1_4_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "argument_list_1_4", c)) break;
    }
    return true;
  }

  // ',' nl* arg nl*
  private static boolean argument_list_1_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_COMMA);
    r = r && argument_list_1_4_0_1(b, l + 1);
    r = r && arg(b, l + 1);
    r = r && argument_list_1_4_0_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean argument_list_1_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1_4_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "argument_list_1_4_0_1", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean argument_list_1_4_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1_4_0_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "argument_list_1_4_0_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '@'
  public static boolean at_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "at_operator")) return false;
    if (!nextTokenIs(b, R_AT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_AT);
    exit_section_(b, m, R_AT_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // ':'
  public static boolean colon_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "colon_operator")) return false;
    if (!nextTokenIs(b, R_COLON)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_COLON);
    exit_section_(b, m, R_COLON_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // '>' | '>=' | '<' | '<=' | '==' | '!='
  public static boolean compare_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_operator")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_COMPARE_OPERATOR, "<compare operator>");
    r = consumeToken(b, R_GT);
    if (!r) r = consumeToken(b, R_GE);
    if (!r) r = consumeToken(b, R_LT);
    if (!r) r = consumeToken(b, R_LE);
    if (!r) r = consumeToken(b, R_EQEQ);
    if (!r) r = consumeToken(b, R_NOTEQ);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '='
  public static boolean eq_assign_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "eq_assign_operator")) return false;
    if (!nextTokenIs(b, R_EQ)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_EQ);
    exit_section_(b, m, R_ASSIGN_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // '^'
  public static boolean exp_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "exp_operator")) return false;
    if (!nextTokenIs(b, R_EXP)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_EXP);
    exit_section_(b, m, R_EXP_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // expression? (semicolon+ expression?)*
  static boolean expression_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression_list_0(b, l + 1);
    r = r && expression_list_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expression?
  private static boolean expression_list_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_0")) return false;
    expression(b, l + 1, -1);
    return true;
  }

  // (semicolon+ expression?)*
  private static boolean expression_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expression_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expression_list_1", c)) break;
    }
    return true;
  }

  // semicolon+ expression?
  private static boolean expression_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression_list_1_0_0(b, l + 1);
    r = r && expression_list_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // semicolon+
  private static boolean expression_list_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_1_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = semicolon(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!semicolon(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expression_list_1_0_0", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // expression?
  private static boolean expression_list_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_1_0_1")) return false;
    expression(b, l + 1, -1);
    return true;
  }

  /* ********************************************************** */
  // expression nl* else nl* expression
  static boolean if_with_else(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_with_else")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = expression(b, l + 1, -1);
    r = r && if_with_else_1(b, l + 1);
    r = r && consumeToken(b, R_ELSE);
    p = r; // pin = 3
    r = r && report_error_(b, if_with_else_3(b, l + 1));
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // nl*
  private static boolean if_with_else_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_with_else_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "if_with_else_1", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean if_with_else_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_with_else_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "if_with_else_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // INFIX_OP
  public static boolean infix_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "infix_operator")) return false;
    if (!nextTokenIs(b, R_INFIX_OP)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_INFIX_OP);
    exit_section_(b, m, R_INFIX_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // NA_integer_ | NA_real_ | NA_complex_ | NA_character_
  //   TRIPLE_DOTS | if | else | repeat | while |
  //   function | for | in | next | break
  static boolean keyword(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keyword")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_NA_INTEGER_);
    if (!r) r = consumeToken(b, R_NA_REAL_);
    if (!r) r = consumeToken(b, R_NA_COMPLEX_);
    if (!r) r = parseTokens(b, 0, R_NA_CHARACTER_, R_TRIPLE_DOTS);
    if (!r) r = consumeToken(b, R_IF);
    if (!r) r = consumeToken(b, R_ELSE);
    if (!r) r = consumeToken(b, R_REPEAT);
    if (!r) r = consumeToken(b, R_WHILE);
    if (!r) r = consumeToken(b, R_FUNCTION);
    if (!r) r = consumeToken(b, R_FOR);
    if (!r) r = consumeToken(b, R_IN);
    if (!r) r = consumeToken(b, R_NEXT);
    if (!r) r = consumeToken(b, R_BREAK);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '<-' | '<<-' | ':='
  public static boolean left_assign_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "left_assign_operator")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_ASSIGN_OPERATOR, "<left assign operator>");
    r = consumeToken(b, R_LEFT_ASSIGN);
    if (!r) r = consumeToken(b, R_LEFT_COMPLEX_ASSIGN);
    if (!r) r = consumeToken(b, R_LEFT_ASSIGN_OLD);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '$'
  public static boolean list_subset_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_subset_operator")) return false;
    if (!nextTokenIs(b, R_LIST_SUBSET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_LIST_SUBSET);
    exit_section_(b, m, R_LIST_SUBSET_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // primitive_expression | namespace_access_expression | parenthesized_expression
  static boolean member_tag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "member_tag")) return false;
    boolean r;
    r = expression(b, l + 1, 29);
    if (!r) r = namespace_access_expression(b, l + 1);
    if (!r) r = parenthesized_expression(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // '*' | '/'
  public static boolean muldiv_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "muldiv_operator")) return false;
    if (!nextTokenIs(b, "<muldiv operator>", R_DIV, R_MULT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_MULDIV_OPERATOR, "<muldiv operator>");
    r = consumeToken(b, R_MULT);
    if (!r) r = consumeToken(b, R_DIV);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (identifier_expression | string_literal_expression) eq_assign_operator nl* expression
  public static boolean named_argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "named_argument")) return false;
    if (!nextTokenIsSmart(b, R_IDENTIFIER, R_STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_NAMED_ARGUMENT, "<named argument>");
    r = named_argument_0(b, l + 1);
    r = r && eq_assign_operator(b, l + 1);
    r = r && named_argument_2(b, l + 1);
    r = r && expression(b, l + 1, -1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // identifier_expression | string_literal_expression
  private static boolean named_argument_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "named_argument_0")) return false;
    boolean r;
    r = identifier_expression(b, l + 1);
    if (!r) r = string_literal_expression(b, l + 1);
    return r;
  }

  // nl*
  private static boolean named_argument_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "named_argument_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "named_argument_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '!'
  public static boolean not_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_operator")) return false;
    if (!nextTokenIs(b, R_NOT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_NOT);
    exit_section_(b, m, R_NOT_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // '|' | '||'
  public static boolean or_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_operator")) return false;
    if (!nextTokenIs(b, "<or operator>", R_OR, R_OROR)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_OR_OPERATOR, "<or operator>");
    r = consumeToken(b, R_OR);
    if (!r) r = consumeToken(b, R_OROR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // identifier_expression ('=' expression)?
  public static boolean parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter")) return false;
    if (!nextTokenIsSmart(b, R_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = identifier_expression(b, l + 1);
    r = r && parameter_1(b, l + 1);
    exit_section_(b, m, R_PARAMETER, r);
    return r;
  }

  // ('=' expression)?
  private static boolean parameter_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_1")) return false;
    parameter_1_0(b, l + 1);
    return true;
  }

  // '=' expression
  private static boolean parameter_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_EQ);
    r = r && expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ('(' ')') | ('(' nl* parameter nl* (',' nl* parameter nl*)* ')')
  public static boolean parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list")) return false;
    if (!nextTokenIs(b, R_LPAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parameter_list_0(b, l + 1);
    if (!r) r = parameter_list_1(b, l + 1);
    exit_section_(b, m, R_PARAMETER_LIST, r);
    return r;
  }

  // '(' ')'
  private static boolean parameter_list_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, R_LPAR, R_RPAR);
    exit_section_(b, m, null, r);
    return r;
  }

  // '(' nl* parameter nl* (',' nl* parameter nl*)* ')'
  private static boolean parameter_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_LPAR);
    r = r && parameter_list_1_1(b, l + 1);
    r = r && parameter(b, l + 1);
    r = r && parameter_list_1_3(b, l + 1);
    r = r && parameter_list_1_4(b, l + 1);
    r = r && consumeToken(b, R_RPAR);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean parameter_list_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_1_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "parameter_list_1_1", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean parameter_list_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_1_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "parameter_list_1_3", c)) break;
    }
    return true;
  }

  // (',' nl* parameter nl*)*
  private static boolean parameter_list_1_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_1_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parameter_list_1_4_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameter_list_1_4", c)) break;
    }
    return true;
  }

  // ',' nl* parameter nl*
  private static boolean parameter_list_1_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_1_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_COMMA);
    r = r && parameter_list_1_4_0_1(b, l + 1);
    r = r && parameter(b, l + 1);
    r = r && parameter_list_1_4_0_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean parameter_list_1_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_1_4_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "parameter_list_1_4_0_1", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean parameter_list_1_4_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_1_4_0_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "parameter_list_1_4_0_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '+' | '-'
  public static boolean plusminus_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "plusminus_operator")) return false;
    if (!nextTokenIs(b, "<plusminus operator>", R_MINUS, R_PLUS)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_PLUSMINUS_OPERATOR, "<plusminus operator>");
    r = consumeToken(b, R_PLUS);
    if (!r) r = consumeToken(b, R_MINUS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '->' | '->>'
  public static boolean right_assign_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "right_assign_operator")) return false;
    if (!nextTokenIs(b, "<right assign operator>", R_RIGHT_ASSIGN, R_RIGHT_COMPLEX_ASSIGN)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_ASSIGN_OPERATOR, "<right assign operator>");
    r = consumeToken(b, R_RIGHT_ASSIGN);
    if (!r) r = consumeToken(b, R_RIGHT_COMPLEX_ASSIGN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // expression_list
  static boolean root(PsiBuilder b, int l) {
    return expression_list(b, l + 1);
  }

  /* ********************************************************** */
  // ';' | nl
  static boolean semicolon(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "semicolon")) return false;
    if (!nextTokenIs(b, "", R_NL, R_SEMI)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_SEMI);
    if (!r) r = consumeToken(b, R_NL);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // named_argument | expression | external_empty_expression
  static boolean subscription_expr_elem(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_elem")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = named_argument(b, l + 1);
    if (!r) r = expression(b, l + 1, -1);
    if (!r) r = parseEmptyExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // subscription_expr_elem nl* (',' nl* subscription_expr_elem nl*)*
  static boolean subscription_expr_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = subscription_expr_elem(b, l + 1);
    r = r && subscription_expr_list_1(b, l + 1);
    r = r && subscription_expr_list_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean subscription_expr_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "subscription_expr_list_1", c)) break;
    }
    return true;
  }

  // (',' nl* subscription_expr_elem nl*)*
  private static boolean subscription_expr_list_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_list_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!subscription_expr_list_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "subscription_expr_list_2", c)) break;
    }
    return true;
  }

  // ',' nl* subscription_expr_elem nl*
  private static boolean subscription_expr_list_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_list_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_COMMA);
    r = r && subscription_expr_list_2_0_1(b, l + 1);
    r = r && subscription_expr_elem(b, l + 1);
    r = r && subscription_expr_list_2_0_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean subscription_expr_list_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_list_2_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "subscription_expr_list_2_0_1", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean subscription_expr_list_2_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_list_2_0_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "subscription_expr_list_2_0_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '~'
  public static boolean tilde_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tilde_operator")) return false;
    if (!nextTokenIs(b, R_TILDE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_TILDE);
    exit_section_(b, m, R_TILDE_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // Expression root: expression
  // Operator priority table:
  // 0: ATOM(if_statement)
  // 1: ATOM(while_statement)
  // 2: ATOM(for_statement)
  // 3: ATOM(repeat_statement)
  // 4: ATOM(break_statement)
  // 5: ATOM(next_statement)
  // 6: ATOM(block_expression)
  // 7: ATOM(help_expression)
  // 8: PREFIX(parenthesized_expression)
  // 9: PREFIX(function_expression)
  // 10: BINARY(left_assign_expression)
  // 11: POSTFIX(eq_assign_expression)
  // 12: BINARY(right_assign_expression)
  // 13: PREFIX(unary_tilde_expression)
  // 14: BINARY(tilde_expression)
  // 15: BINARY(or_expression)
  // 16: BINARY(and_expression)
  // 17: PREFIX(unary_not_expression)
  // 18: BINARY(compare_expression)
  // 19: BINARY(plusminus_expression)
  // 20: BINARY(muldiv_expression)
  // 21: BINARY(infix_expression)
  // 22: BINARY(colon_expression)
  // 23: PREFIX(unary_plusminus_expression)
  // 24: BINARY(exp_expression)
  // 25: POSTFIX(subscription_expression)
  // 26: POSTFIX(call_expression)
  // 27: POSTFIX(member_expression)
  // 28: BINARY(at_expression)
  // 29: PREFIX(namespace_access_expression)
  // 30: ATOM(string_literal_expression) ATOM(numeric_literal_expression) ATOM(boolean_literal) ATOM(na_literal)
  //    ATOM(null_literal) ATOM(boundary_literal) ATOM(identifier_expression)
  public static boolean expression(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "expression")) return false;
    addVariant(b, "<expression>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expression>");
    r = if_statement(b, l + 1);
    if (!r) r = while_statement(b, l + 1);
    if (!r) r = for_statement(b, l + 1);
    if (!r) r = repeat_statement(b, l + 1);
    if (!r) r = break_statement(b, l + 1);
    if (!r) r = next_statement(b, l + 1);
    if (!r) r = block_expression(b, l + 1);
    if (!r) r = help_expression(b, l + 1);
    if (!r) r = parenthesized_expression(b, l + 1);
    if (!r) r = function_expression(b, l + 1);
    if (!r) r = unary_tilde_expression(b, l + 1);
    if (!r) r = unary_not_expression(b, l + 1);
    if (!r) r = unary_plusminus_expression(b, l + 1);
    if (!r) r = namespace_access_expression(b, l + 1);
    if (!r) r = string_literal_expression(b, l + 1);
    if (!r) r = numeric_literal_expression(b, l + 1);
    if (!r) r = boolean_literal(b, l + 1);
    if (!r) r = na_literal(b, l + 1);
    if (!r) r = null_literal(b, l + 1);
    if (!r) r = boundary_literal(b, l + 1);
    if (!r) r = identifier_expression(b, l + 1);
    p = r;
    r = r && expression_0(b, l + 1, g);
    exit_section_(b, l, m, null, r, p, null);
    return r || p;
  }

  public static boolean expression_0(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "expression_0")) return false;
    boolean r = true;
    while (true) {
      Marker m = enter_section_(b, l, _LEFT_, null);
      if (g < 10 && left_assign_expression_0(b, l + 1)) {
        r = expression(b, l, 9);
        exit_section_(b, l, m, R_ASSIGNMENT_STATEMENT, r, true, null);
      }
      else if (g < 11 && eq_assign_expression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, R_ASSIGNMENT_STATEMENT, r, true, null);
      }
      else if (g < 12 && right_assign_expression_0(b, l + 1)) {
        r = expression(b, l, 12);
        exit_section_(b, l, m, R_ASSIGNMENT_STATEMENT, r, true, null);
      }
      else if (g < 14 && tilde_expression_0(b, l + 1)) {
        r = expression(b, l, 14);
        exit_section_(b, l, m, R_TILDE_EXPRESSION, r, true, null);
      }
      else if (g < 15 && or_expression_0(b, l + 1)) {
        r = expression(b, l, 15);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 16 && and_expression_0(b, l + 1)) {
        r = expression(b, l, 16);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 18 && compare_expression_0(b, l + 1)) {
        r = expression(b, l, 18);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 19 && plusminus_expression_0(b, l + 1)) {
        r = expression(b, l, 19);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 20 && muldiv_expression_0(b, l + 1)) {
        r = expression(b, l, 20);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 21 && infix_expression_0(b, l + 1)) {
        r = expression(b, l, 21);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 22 && colon_expression_0(b, l + 1)) {
        r = expression(b, l, 22);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 24 && exp_expression_0(b, l + 1)) {
        r = expression(b, l, 24);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 25 && subscription_expression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, R_SUBSCRIPTION_EXPRESSION, r, true, null);
      }
      else if (g < 26 && argument_list(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, R_CALL_EXPRESSION, r, true, null);
      }
      else if (g < 27 && member_expression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, R_MEMBER_EXPRESSION, r, true, null);
      }
      else if (g < 28 && at_expression_0(b, l + 1)) {
        r = expression(b, l, 28);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else {
        exit_section_(b, l, m, null, false, false, null);
        break;
      }
    }
    return r;
  }

  // if nl* '(' nl* expression nl* ')' nl* (if_with_else | expression )
  public static boolean if_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement")) return false;
    if (!nextTokenIsSmart(b, R_IF)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_IF_STATEMENT, null);
    r = consumeTokenSmart(b, R_IF);
    p = r; // pin = 1
    r = r && report_error_(b, if_statement_1(b, l + 1));
    r = p && report_error_(b, consumeToken(b, R_LPAR)) && r;
    r = p && report_error_(b, if_statement_3(b, l + 1)) && r;
    r = p && report_error_(b, expression(b, l + 1, -1)) && r;
    r = p && report_error_(b, if_statement_5(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, R_RPAR)) && r;
    r = p && report_error_(b, if_statement_7(b, l + 1)) && r;
    r = p && if_statement_8(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // nl*
  private static boolean if_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "if_statement_1", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean if_statement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "if_statement_3", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean if_statement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_5")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "if_statement_5", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean if_statement_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_7")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "if_statement_7", c)) break;
    }
    return true;
  }

  // if_with_else | expression
  private static boolean if_statement_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_8")) return false;
    boolean r;
    r = if_with_else(b, l + 1);
    if (!r) r = expression(b, l + 1, -1);
    return r;
  }

  // while nl* '(' nl* expression nl* ')' nl* expression
  public static boolean while_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "while_statement")) return false;
    if (!nextTokenIsSmart(b, R_WHILE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_WHILE_STATEMENT, null);
    r = consumeTokenSmart(b, R_WHILE);
    p = r; // pin = 1
    r = r && report_error_(b, while_statement_1(b, l + 1));
    r = p && report_error_(b, consumeToken(b, R_LPAR)) && r;
    r = p && report_error_(b, while_statement_3(b, l + 1)) && r;
    r = p && report_error_(b, expression(b, l + 1, -1)) && r;
    r = p && report_error_(b, while_statement_5(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, R_RPAR)) && r;
    r = p && report_error_(b, while_statement_7(b, l + 1)) && r;
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // nl*
  private static boolean while_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "while_statement_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "while_statement_1", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean while_statement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "while_statement_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "while_statement_3", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean while_statement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "while_statement_5")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "while_statement_5", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean while_statement_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "while_statement_7")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "while_statement_7", c)) break;
    }
    return true;
  }

  // for nl* '(' nl* identifier_expression 'in' (nl* expression) ')' nl* expression
  public static boolean for_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement")) return false;
    if (!nextTokenIsSmart(b, R_FOR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_FOR_STATEMENT, null);
    r = consumeTokenSmart(b, R_FOR);
    p = r; // pin = 1
    r = r && report_error_(b, for_statement_1(b, l + 1));
    r = p && report_error_(b, consumeToken(b, R_LPAR)) && r;
    r = p && report_error_(b, for_statement_3(b, l + 1)) && r;
    r = p && report_error_(b, identifier_expression(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, "in")) && r;
    r = p && report_error_(b, for_statement_6(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, R_RPAR)) && r;
    r = p && report_error_(b, for_statement_8(b, l + 1)) && r;
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // nl*
  private static boolean for_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "for_statement_1", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean for_statement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "for_statement_3", c)) break;
    }
    return true;
  }

  // nl* expression
  private static boolean for_statement_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement_6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = for_statement_6_0(b, l + 1);
    r = r && expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean for_statement_6_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement_6_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "for_statement_6_0", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean for_statement_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement_8")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "for_statement_8", c)) break;
    }
    return true;
  }

  // repeat (nl* expression)
  public static boolean repeat_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "repeat_statement")) return false;
    if (!nextTokenIsSmart(b, R_REPEAT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_REPEAT_STATEMENT, null);
    r = consumeTokenSmart(b, R_REPEAT);
    p = r; // pin = 1
    r = r && repeat_statement_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // nl* expression
  private static boolean repeat_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "repeat_statement_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = repeat_statement_1_0(b, l + 1);
    r = r && expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean repeat_statement_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "repeat_statement_1_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "repeat_statement_1_0", c)) break;
    }
    return true;
  }

  // break
  public static boolean break_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "break_statement")) return false;
    if (!nextTokenIsSmart(b, R_BREAK)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_BREAK);
    exit_section_(b, m, R_BREAK_STATEMENT, r);
    return r;
  }

  // next
  public static boolean next_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "next_statement")) return false;
    if (!nextTokenIsSmart(b, R_NEXT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_NEXT);
    exit_section_(b, m, R_NEXT_STATEMENT, r);
    return r;
  }

  // '{' nl* expression_list? nl* '}'
  public static boolean block_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_expression")) return false;
    if (!nextTokenIsSmart(b, R_LBRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_BLOCK_EXPRESSION, null);
    r = consumeTokenSmart(b, R_LBRACE);
    p = r; // pin = 1
    r = r && report_error_(b, block_expression_1(b, l + 1));
    r = p && report_error_(b, block_expression_2(b, l + 1)) && r;
    r = p && report_error_(b, block_expression_3(b, l + 1)) && r;
    r = p && consumeToken(b, R_RBRACE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // nl*
  private static boolean block_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_expression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "block_expression_1", c)) break;
    }
    return true;
  }

  // expression_list?
  private static boolean block_expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_expression_2")) return false;
    expression_list(b, l + 1);
    return true;
  }

  // nl*
  private static boolean block_expression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_expression_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "block_expression_3", c)) break;
    }
    return true;
  }

  // help (nl* help)? nl* (keyword | expression)
  public static boolean help_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_expression")) return false;
    if (!nextTokenIsSmart(b, R_HELP)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_HELP);
    r = r && help_expression_1(b, l + 1);
    r = r && help_expression_2(b, l + 1);
    r = r && help_expression_3(b, l + 1);
    exit_section_(b, m, R_HELP_EXPRESSION, r);
    return r;
  }

  // (nl* help)?
  private static boolean help_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_expression_1")) return false;
    help_expression_1_0(b, l + 1);
    return true;
  }

  // nl* help
  private static boolean help_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = help_expression_1_0_0(b, l + 1);
    r = r && consumeToken(b, R_HELP);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean help_expression_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_expression_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "help_expression_1_0_0", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean help_expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_expression_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "help_expression_2", c)) break;
    }
    return true;
  }

  // keyword | expression
  private static boolean help_expression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_expression_3")) return false;
    boolean r;
    r = keyword(b, l + 1);
    if (!r) r = expression(b, l + 1, -1);
    return r;
  }

  public static boolean parenthesized_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression")) return false;
    if (!nextTokenIsSmart(b, R_LPAR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = parenthesized_expression_0(b, l + 1);
    p = r;
    r = p && expression(b, l, 8);
    r = p && report_error_(b, parenthesized_expression_1(b, l + 1)) && r;
    exit_section_(b, l, m, R_PARENTHESIZED_EXPRESSION, r, p, null);
    return r || p;
  }

  // '(' nl*
  private static boolean parenthesized_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_LPAR);
    r = r && parenthesized_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean parenthesized_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "parenthesized_expression_0_1", c)) break;
    }
    return true;
  }

  // nl* ')'
  private static boolean parenthesized_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parenthesized_expression_1_0(b, l + 1);
    r = r && consumeToken(b, R_RPAR);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean parenthesized_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression_1_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "parenthesized_expression_1_0", c)) break;
    }
    return true;
  }

  public static boolean function_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_expression")) return false;
    if (!nextTokenIsSmart(b, R_FUNCTION)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = function_expression_0(b, l + 1);
    p = r;
    r = p && expression(b, l, 9);
    exit_section_(b, l, m, R_FUNCTION_EXPRESSION, r, p, null);
    return r || p;
  }

  // function parameter_list nl*
  private static boolean function_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_FUNCTION);
    r = r && parameter_list(b, l + 1);
    r = r && function_expression_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean function_expression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_expression_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "function_expression_0_2", c)) break;
    }
    return true;
  }

  // left_assign_operator nl*
  private static boolean left_assign_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "left_assign_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = left_assign_operator(b, l + 1);
    r = r && left_assign_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean left_assign_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "left_assign_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "left_assign_expression_0_1", c)) break;
    }
    return true;
  }

  // eq_assign_operator nl* (expression | external_empty_expression)
  private static boolean eq_assign_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "eq_assign_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = eq_assign_operator(b, l + 1);
    r = r && eq_assign_expression_0_1(b, l + 1);
    r = r && eq_assign_expression_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean eq_assign_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "eq_assign_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "eq_assign_expression_0_1", c)) break;
    }
    return true;
  }

  // expression | external_empty_expression
  private static boolean eq_assign_expression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "eq_assign_expression_0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1, -1);
    if (!r) r = parseEmptyExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // right_assign_operator nl*
  private static boolean right_assign_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "right_assign_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = right_assign_operator(b, l + 1);
    r = r && right_assign_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean right_assign_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "right_assign_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "right_assign_expression_0_1", c)) break;
    }
    return true;
  }

  public static boolean unary_tilde_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_tilde_expression")) return false;
    if (!nextTokenIsSmart(b, R_TILDE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = unary_tilde_expression_0(b, l + 1);
    p = r;
    r = p && expression(b, l, 13);
    exit_section_(b, l, m, R_UNARY_TILDE_EXPRESSION, r, p, null);
    return r || p;
  }

  // tilde_operator nl*
  private static boolean unary_tilde_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_tilde_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = tilde_operator(b, l + 1);
    r = r && unary_tilde_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean unary_tilde_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_tilde_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "unary_tilde_expression_0_1", c)) break;
    }
    return true;
  }

  // tilde_operator nl*
  private static boolean tilde_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tilde_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = tilde_operator(b, l + 1);
    r = r && tilde_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean tilde_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tilde_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "tilde_expression_0_1", c)) break;
    }
    return true;
  }

  // nl* or_operator nl*
  private static boolean or_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = or_expression_0_0(b, l + 1);
    r = r && or_operator(b, l + 1);
    r = r && or_expression_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean or_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "or_expression_0_0", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean or_expression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "or_expression_0_2", c)) break;
    }
    return true;
  }

  // nl* and_operator nl*
  private static boolean and_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = and_expression_0_0(b, l + 1);
    r = r && and_operator(b, l + 1);
    r = r && and_expression_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean and_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "and_expression_0_0", c)) break;
    }
    return true;
  }

  // nl*
  private static boolean and_expression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "and_expression_0_2", c)) break;
    }
    return true;
  }

  public static boolean unary_not_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_not_expression")) return false;
    if (!nextTokenIsSmart(b, R_NOT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = unary_not_expression_0(b, l + 1);
    p = r;
    r = p && expression(b, l, 17);
    exit_section_(b, l, m, R_UNARY_NOT_EXPRESSION, r, p, null);
    return r || p;
  }

  // not_operator nl*
  private static boolean unary_not_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_not_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = not_operator(b, l + 1);
    r = r && unary_not_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean unary_not_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_not_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "unary_not_expression_0_1", c)) break;
    }
    return true;
  }

  // compare_operator nl*
  private static boolean compare_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = compare_operator(b, l + 1);
    r = r && compare_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean compare_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "compare_expression_0_1", c)) break;
    }
    return true;
  }

  // plusminus_operator nl*
  private static boolean plusminus_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "plusminus_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = plusminus_operator(b, l + 1);
    r = r && plusminus_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean plusminus_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "plusminus_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "plusminus_expression_0_1", c)) break;
    }
    return true;
  }

  // muldiv_operator nl*
  private static boolean muldiv_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "muldiv_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = muldiv_operator(b, l + 1);
    r = r && muldiv_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean muldiv_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "muldiv_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "muldiv_expression_0_1", c)) break;
    }
    return true;
  }

  // infix_operator nl*
  private static boolean infix_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "infix_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = infix_operator(b, l + 1);
    r = r && infix_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean infix_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "infix_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "infix_expression_0_1", c)) break;
    }
    return true;
  }

  // colon_operator nl*
  private static boolean colon_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "colon_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = colon_operator(b, l + 1);
    r = r && colon_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean colon_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "colon_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "colon_expression_0_1", c)) break;
    }
    return true;
  }

  public static boolean unary_plusminus_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_plusminus_expression")) return false;
    if (!nextTokenIsSmart(b, R_MINUS, R_PLUS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = unary_plusminus_expression_0(b, l + 1);
    p = r;
    r = p && expression(b, l, 23);
    exit_section_(b, l, m, R_UNARY_PLUSMINUS_EXPRESSION, r, p, null);
    return r || p;
  }

  // plusminus_operator nl*
  private static boolean unary_plusminus_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_plusminus_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = plusminus_operator(b, l + 1);
    r = r && unary_plusminus_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean unary_plusminus_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_plusminus_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "unary_plusminus_expression_0_1", c)) break;
    }
    return true;
  }

  // exp_operator nl*
  private static boolean exp_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "exp_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = exp_operator(b, l + 1);
    r = r && exp_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean exp_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "exp_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "exp_expression_0_1", c)) break;
    }
    return true;
  }

  // '[' nl* ']' | '[' nl* subscription_expr_list ']' |
  //   '[[' nl* ']]' | '[[' nl* subscription_expr_list ']]'
  private static boolean subscription_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = subscription_expression_0_0(b, l + 1);
    if (!r) r = subscription_expression_0_1(b, l + 1);
    if (!r) r = subscription_expression_0_2(b, l + 1);
    if (!r) r = subscription_expression_0_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '[' nl* ']'
  private static boolean subscription_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_LBRACKET);
    r = r && subscription_expression_0_0_1(b, l + 1);
    r = r && consumeToken(b, R_RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean subscription_expression_0_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "subscription_expression_0_0_1", c)) break;
    }
    return true;
  }

  // '[' nl* subscription_expr_list ']'
  private static boolean subscription_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_LBRACKET);
    r = r && subscription_expression_0_1_1(b, l + 1);
    r = r && subscription_expr_list(b, l + 1);
    r = r && consumeToken(b, R_RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean subscription_expression_0_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_1_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "subscription_expression_0_1_1", c)) break;
    }
    return true;
  }

  // '[[' nl* ']]'
  private static boolean subscription_expression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_LDBRACKET);
    r = r && subscription_expression_0_2_1(b, l + 1);
    r = r && consumeToken(b, R_RDBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean subscription_expression_0_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_2_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "subscription_expression_0_2_1", c)) break;
    }
    return true;
  }

  // '[[' nl* subscription_expr_list ']]'
  private static boolean subscription_expression_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_LDBRACKET);
    r = r && subscription_expression_0_3_1(b, l + 1);
    r = r && subscription_expr_list(b, l + 1);
    r = r && consumeToken(b, R_RDBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean subscription_expression_0_3_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_3_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "subscription_expression_0_3_1", c)) break;
    }
    return true;
  }

  // list_subset_operator nl* member_tag
  private static boolean member_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "member_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = list_subset_operator(b, l + 1);
    r = r && member_expression_0_1(b, l + 1);
    r = r && member_tag(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean member_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "member_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "member_expression_0_1", c)) break;
    }
    return true;
  }

  // at_operator nl*
  private static boolean at_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "at_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = at_operator(b, l + 1);
    r = r && at_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // nl*
  private static boolean at_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "at_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, R_NL)) break;
      if (!empty_element_parsed_guard_(b, "at_expression_0_1", c)) break;
    }
    return true;
  }

  public static boolean namespace_access_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_access_expression")) return false;
    if (!nextTokenIsSmart(b, R_IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = namespace_access_expression_0(b, l + 1);
    p = r;
    r = p && expression(b, l, 29);
    exit_section_(b, l, m, R_NAMESPACE_ACCESS_EXPRESSION, r, p, null);
    return r || p;
  }

  // identifier ('::' | ':::')
  private static boolean namespace_access_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_access_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_IDENTIFIER);
    r = r && namespace_access_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '::' | ':::'
  private static boolean namespace_access_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_access_expression_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_DOUBLECOLON);
    if (!r) r = consumeTokenSmart(b, R_TRIPLECOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // STRING
  public static boolean string_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_literal_expression")) return false;
    if (!nextTokenIsSmart(b, R_STRING)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_STRING);
    exit_section_(b, m, R_STRING_LITERAL_EXPRESSION, r);
    return r;
  }

  // INTEGER | NUMERIC | COMPLEX
  public static boolean numeric_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "numeric_literal_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_NUMERIC_LITERAL_EXPRESSION, "<numeric literal expression>");
    r = consumeTokenSmart(b, R_INTEGER);
    if (!r) r = consumeTokenSmart(b, R_NUMERIC);
    if (!r) r = consumeTokenSmart(b, R_COMPLEX);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TRUE | FALSE
  public static boolean boolean_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boolean_literal")) return false;
    if (!nextTokenIsSmart(b, R_FALSE, R_TRUE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_BOOLEAN_LITERAL, "<boolean literal>");
    r = consumeTokenSmart(b, R_TRUE);
    if (!r) r = consumeTokenSmart(b, R_FALSE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // NA | NA_integer_ | NA_real_ | NA_complex_ | NA_character_
  public static boolean na_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "na_literal")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_NA_LITERAL, "<na literal>");
    r = consumeTokenSmart(b, R_NA);
    if (!r) r = consumeTokenSmart(b, R_NA_INTEGER_);
    if (!r) r = consumeTokenSmart(b, R_NA_REAL_);
    if (!r) r = consumeTokenSmart(b, R_NA_COMPLEX_);
    if (!r) r = consumeTokenSmart(b, R_NA_CHARACTER_);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // NULL
  public static boolean null_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "null_literal")) return false;
    if (!nextTokenIsSmart(b, R_NULL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_NULL);
    exit_section_(b, m, R_NULL_LITERAL, r);
    return r;
  }

  // Inf | NaN
  public static boolean boundary_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boundary_literal")) return false;
    if (!nextTokenIsSmart(b, R_INF, R_NAN)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_BOUNDARY_LITERAL, "<boundary literal>");
    r = consumeTokenSmart(b, R_INF);
    if (!r) r = consumeTokenSmart(b, R_NAN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // identifier
  public static boolean identifier_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "identifier_expression")) return false;
    if (!nextTokenIsSmart(b, R_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_IDENTIFIER);
    exit_section_(b, m, R_IDENTIFIER_EXPRESSION, r);
    return r;
  }

}
