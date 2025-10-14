// This is a generated file. Not intended for manual editing.
package com.intellij.r.psi.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.intellij.r.psi.parsing.RElementTypes.*;
import static com.intellij.r.psi.parsing.RParserUtil.*;
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
      R_COMPARE_OPERATOR, R_EXP_OPERATOR, R_FORWARD_PIPE_OPERATOR, R_INFIX_OPERATOR,
      R_LIST_SUBSET_OPERATOR, R_MULDIV_OPERATOR, R_NOT_OPERATOR, R_OR_OPERATOR,
      R_PLUSMINUS_OPERATOR, R_TILDE_OPERATOR),
    create_token_set_(R_ASSIGNMENT_STATEMENT, R_AT_EXPRESSION, R_BLOCK_EXPRESSION, R_BOOLEAN_LITERAL,
      R_BOUNDARY_LITERAL, R_BREAK_STATEMENT, R_CALL_EXPRESSION, R_EMPTY_EXPRESSION,
      R_EXPRESSION, R_FOR_STATEMENT, R_FUNCTION_EXPRESSION, R_HELP_EXPRESSION,
      R_IDENTIFIER_EXPRESSION, R_IF_STATEMENT, R_INVALID_LITERAL, R_MEMBER_EXPRESSION,
      R_NAMESPACE_ACCESS_EXPRESSION, R_NA_LITERAL, R_NEXT_STATEMENT, R_NULL_LITERAL,
      R_NUMERIC_LITERAL_EXPRESSION, R_OPERATOR_EXPRESSION, R_PARENTHESIZED_EXPRESSION, R_REPEAT_STATEMENT,
      R_STRING_LITERAL_EXPRESSION, R_SUBSCRIPTION_EXPRESSION, R_TILDE_EXPRESSION, R_UNARY_NOT_EXPRESSION,
      R_UNARY_PLUSMINUS_EXPRESSION, R_UNARY_TILDE_EXPRESSION, R_WHILE_STATEMENT),
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
  // non_empty_arg | external_empty_expression
  static boolean arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arg")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = non_empty_arg(b, l + 1);
    if (!r) r = parseEmptyExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (',' arg )*
  static boolean args_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "args_tail")) return false;
    while (true) {
      int c = current_position_(b);
      if (!args_tail_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "args_tail", c)) break;
    }
    return true;
  }

  // ',' arg
  private static boolean args_tail_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "args_tail_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, R_COMMA);
    p = r; // pin = 1
    r = r && arg(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '(' &<<incBrackets>> ( non_empty_first_arg | empty_first_arg )? ')' &<<decBrackets>>
  public static boolean argument_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list")) return false;
    if (!nextTokenIs(b, R_LPAR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_ARGUMENT_LIST, null);
    r = consumeToken(b, R_LPAR);
    p = r; // pin = 1
    r = r && report_error_(b, argument_list_1(b, l + 1));
    r = p && report_error_(b, argument_list_2(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, R_RPAR)) && r;
    r = p && argument_list_4(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // &<<incBrackets>>
  private static boolean argument_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = incBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( non_empty_first_arg | empty_first_arg )?
  private static boolean argument_list_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_2")) return false;
    argument_list_2_0(b, l + 1);
    return true;
  }

  // non_empty_first_arg | empty_first_arg
  private static boolean argument_list_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_2_0")) return false;
    boolean r;
    r = non_empty_first_arg(b, l + 1);
    if (!r) r = empty_first_arg(b, l + 1);
    return r;
  }

  // &<<decBrackets>>
  private static boolean argument_list_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_4")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = decBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
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
  // external_empty_expression ',' arg args_tail
  static boolean empty_first_arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "empty_first_arg")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = parseEmptyExpression(b, l + 1);
    r = r && consumeToken(b, R_COMMA);
    p = r; // pin = 2
    r = r && report_error_(b, arg(b, l + 1));
    r = p && args_tail(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
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
  // ';'* statement_expression* expression?
  static boolean expression_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression_list_0(b, l + 1);
    r = r && expression_list_1(b, l + 1);
    r = r && expression_list_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'*
  private static boolean expression_list_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_SEMI)) break;
      if (!empty_element_parsed_guard_(b, "expression_list_0", c)) break;
    }
    return true;
  }

  // statement_expression*
  private static boolean expression_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!statement_expression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expression_list_1", c)) break;
    }
    return true;
  }

  // expression?
  private static boolean expression_list_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_2")) return false;
    expression(b, l + 1, -1);
    return true;
  }

  /* ********************************************************** */
  // '|>'
  public static boolean forward_pipe_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "forward_pipe_operator")) return false;
    if (!nextTokenIs(b, R_FORWARD_PIPE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_FORWARD_PIPE);
    exit_section_(b, m, R_FORWARD_PIPE_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // function | shorthand_function
  static boolean function_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_literal")) return false;
    if (!nextTokenIs(b, "", R_FUNCTION, R_SHORTHAND_FUNCTION)) return false;
    boolean r;
    r = consumeToken(b, R_FUNCTION);
    if (!r) r = consumeToken(b, R_SHORTHAND_FUNCTION);
    return r;
  }

  /* ********************************************************** */
  // expression else expression
  static boolean if_with_else(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_with_else")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = expression(b, l + 1, -1);
    r = r && consumeToken(b, R_ELSE);
    p = r; // pin = 2
    r = r && expression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
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
    r = expression(b, l + 1, 30);
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
  // (identifier_expression | string_literal_expression) eq_assign_operator (expression | external_empty_expression)
  public static boolean named_argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "named_argument")) return false;
    if (!nextTokenIs(b, "<named argument>", R_IDENTIFIER, R_STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_NAMED_ARGUMENT, "<named argument>");
    r = named_argument_0(b, l + 1);
    r = r && eq_assign_operator(b, l + 1);
    r = r && named_argument_2(b, l + 1);
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

  // expression | external_empty_expression
  private static boolean named_argument_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "named_argument_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1, -1);
    if (!r) r = parseEmptyExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (named_argument | expression)+
  public static boolean no_comma_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "no_comma_tail")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, R_NO_COMMA_TAIL, "<no comma tail>");
    r = no_comma_tail_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!no_comma_tail_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "no_comma_tail", c)) break;
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // named_argument | expression
  private static boolean no_comma_tail_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "no_comma_tail_0")) return false;
    boolean r;
    r = named_argument(b, l + 1);
    if (!r) r = expression(b, l + 1, -1);
    return r;
  }

  /* ********************************************************** */
  // (named_argument | expression ) (no_comma_tail?)
  static boolean non_empty_arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "non_empty_arg")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = non_empty_arg_0(b, l + 1);
    r = r && non_empty_arg_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // named_argument | expression
  private static boolean non_empty_arg_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "non_empty_arg_0")) return false;
    boolean r;
    r = named_argument(b, l + 1);
    if (!r) r = expression(b, l + 1, -1);
    return r;
  }

  // no_comma_tail?
  private static boolean non_empty_arg_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "non_empty_arg_1")) return false;
    no_comma_tail(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // non_empty_arg args_tail
  static boolean non_empty_first_arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "non_empty_first_arg")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = non_empty_arg(b, l + 1);
    p = r; // pin = 1
    r = r && args_tail(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
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
    if (!nextTokenIs(b, R_IDENTIFIER)) return false;
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
  // '(' &<<incBrackets>> (parameter (',' parameter )*)? ')' &<<decBrackets>>
  public static boolean parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list")) return false;
    if (!nextTokenIs(b, R_LPAR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_PARAMETER_LIST, null);
    r = consumeToken(b, R_LPAR);
    p = r; // pin = 1
    r = r && report_error_(b, parameter_list_1(b, l + 1));
    r = p && report_error_(b, parameter_list_2(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, R_RPAR)) && r;
    r = p && parameter_list_4(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // &<<incBrackets>>
  private static boolean parameter_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = incBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (parameter (',' parameter )*)?
  private static boolean parameter_list_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_2")) return false;
    parameter_list_2_0(b, l + 1);
    return true;
  }

  // parameter (',' parameter )*
  private static boolean parameter_list_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_2_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = parameter(b, l + 1);
    p = r; // pin = 1
    r = r && parameter_list_2_0_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (',' parameter )*
  private static boolean parameter_list_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_2_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parameter_list_2_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameter_list_2_0_1", c)) break;
    }
    return true;
  }

  // ',' parameter
  private static boolean parameter_list_2_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_2_0_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, R_COMMA);
    p = r; // pin = 1
    r = r && parameter(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // &<<decBrackets>>
  private static boolean parameter_list_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_4")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = decBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
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
  // &';' | &<<isNewLine>>
  static boolean semicolon(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "semicolon")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = semicolon_0(b, l + 1);
    if (!r) r = semicolon_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &';'
  private static boolean semicolon_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "semicolon_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, R_SEMI);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &<<isNewLine>>
  private static boolean semicolon_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "semicolon_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // expression semicolon ';'*
  static boolean statement_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_expression")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1, -1);
    r = r && semicolon(b, l + 1);
    r = r && statement_expression_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'*
  private static boolean statement_expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_expression_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, R_SEMI)) break;
      if (!empty_element_parsed_guard_(b, "statement_expression_2", c)) break;
    }
    return true;
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
  // subscription_expr_elem (',' subscription_expr_elem )*
  static boolean subscription_expr_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = subscription_expr_elem(b, l + 1);
    r = r && subscription_expr_list_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' subscription_expr_elem )*
  private static boolean subscription_expr_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!subscription_expr_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "subscription_expr_list_1", c)) break;
    }
    return true;
  }

  // ',' subscription_expr_elem
  private static boolean subscription_expr_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expr_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_COMMA);
    r = r && subscription_expr_elem(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
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
  // 9: ATOM(function_expression)
  // 10: BINARY(left_assign_expression)
  // 11: BINARY(eq_assign_expression)
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
  // 22: BINARY(forward_pipe_expression)
  // 23: BINARY(colon_expression)
  // 24: PREFIX(unary_plusminus_expression)
  // 25: BINARY(exp_expression)
  // 26: POSTFIX(subscription_expression)
  // 27: POSTFIX(call_impl_expression)
  // 28: POSTFIX(member_expression)
  // 29: BINARY(at_expression)
  // 30: PREFIX(namespace_access_expression)
  // 31: ATOM(string_literal_expression) ATOM(numeric_literal_expression) ATOM(boolean_literal) ATOM(na_literal)
  //    ATOM(null_literal) ATOM(boundary_literal) ATOM(invalid_literal) ATOM(identifier_expression)
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
    if (!r) r = invalid_literal(b, l + 1);
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
        r = expression(b, l, 10);
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
      else if (g < 22 && forward_pipe_expression_0(b, l + 1)) {
        r = expression(b, l, 22);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 23 && colon_expression_0(b, l + 1)) {
        r = expression(b, l, 23);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 25 && exp_expression_0(b, l + 1)) {
        r = expression(b, l, 25);
        exit_section_(b, l, m, R_OPERATOR_EXPRESSION, r, true, null);
      }
      else if (g < 26 && subscription_expression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, R_SUBSCRIPTION_EXPRESSION, r, true, null);
      }
      else if (g < 27 && call_impl_expression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, R_CALL_EXPRESSION, r, true, null);
      }
      else if (g < 28 && member_expression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, R_MEMBER_EXPRESSION, r, true, null);
      }
      else if (g < 29 && at_expression_0(b, l + 1)) {
        r = expression(b, l, 29);
        exit_section_(b, l, m, R_AT_EXPRESSION, r, true, null);
      }
      else {
        exit_section_(b, l, m, null, false, false, null);
        break;
      }
    }
    return r;
  }

  // if '(' &<<incBrackets>> expression ')' &<<decBrackets>> (if_with_else | expression )
  public static boolean if_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement")) return false;
    if (!nextTokenIsSmart(b, R_IF)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_IF_STATEMENT, null);
    r = consumeTokensSmart(b, 1, R_IF, R_LPAR);
    p = r; // pin = 1
    r = r && report_error_(b, if_statement_2(b, l + 1));
    r = p && report_error_(b, expression(b, l + 1, -1)) && r;
    r = p && report_error_(b, consumeToken(b, R_RPAR)) && r;
    r = p && report_error_(b, if_statement_5(b, l + 1)) && r;
    r = p && if_statement_6(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // &<<incBrackets>>
  private static boolean if_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_2")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = incBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &<<decBrackets>>
  private static boolean if_statement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_5")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = decBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // if_with_else | expression
  private static boolean if_statement_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_6")) return false;
    boolean r;
    r = if_with_else(b, l + 1);
    if (!r) r = expression(b, l + 1, -1);
    return r;
  }

  // while '(' &<<incBrackets>> expression ')' &<<decBrackets>> expression
  public static boolean while_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "while_statement")) return false;
    if (!nextTokenIsSmart(b, R_WHILE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_WHILE_STATEMENT, null);
    r = consumeTokensSmart(b, 1, R_WHILE, R_LPAR);
    p = r; // pin = 1
    r = r && report_error_(b, while_statement_2(b, l + 1));
    r = p && report_error_(b, expression(b, l + 1, -1)) && r;
    r = p && report_error_(b, consumeToken(b, R_RPAR)) && r;
    r = p && report_error_(b, while_statement_5(b, l + 1)) && r;
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // &<<incBrackets>>
  private static boolean while_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "while_statement_2")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = incBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &<<decBrackets>>
  private static boolean while_statement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "while_statement_5")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = decBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // for '(' &<<incBrackets>> identifier_expression 'in' expression ')' &<<decBrackets>> expression
  public static boolean for_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement")) return false;
    if (!nextTokenIsSmart(b, R_FOR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_FOR_STATEMENT, null);
    r = consumeTokensSmart(b, 1, R_FOR, R_LPAR);
    p = r; // pin = 1
    r = r && report_error_(b, for_statement_2(b, l + 1));
    r = p && report_error_(b, identifier_expression(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, "in")) && r;
    r = p && report_error_(b, expression(b, l + 1, -1)) && r;
    r = p && report_error_(b, consumeToken(b, R_RPAR)) && r;
    r = p && report_error_(b, for_statement_7(b, l + 1)) && r;
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // &<<incBrackets>>
  private static boolean for_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement_2")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = incBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &<<decBrackets>>
  private static boolean for_statement_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement_7")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = decBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // repeat expression
  public static boolean repeat_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "repeat_statement")) return false;
    if (!nextTokenIsSmart(b, R_REPEAT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_REPEAT_STATEMENT, null);
    r = consumeTokenSmart(b, R_REPEAT);
    p = r; // pin = 1
    r = r && expression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
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

  // '{' &<<resetBrackets>> expression_list? '}' &<<decBrackets>>
  public static boolean block_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_expression")) return false;
    if (!nextTokenIsSmart(b, R_LBRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, R_BLOCK_EXPRESSION, null);
    r = consumeTokenSmart(b, R_LBRACE);
    p = r; // pin = 1
    r = r && report_error_(b, block_expression_1(b, l + 1));
    r = p && report_error_(b, block_expression_2(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, R_RBRACE)) && r;
    r = p && block_expression_4(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // &<<resetBrackets>>
  private static boolean block_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_expression_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = resetBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // expression_list?
  private static boolean block_expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_expression_2")) return false;
    expression_list(b, l + 1);
    return true;
  }

  // &<<decBrackets>>
  private static boolean block_expression_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_expression_4")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = decBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // help ( help)? (keyword | expression)
  public static boolean help_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_expression")) return false;
    if (!nextTokenIsSmart(b, R_HELP)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_HELP);
    r = r && help_expression_1(b, l + 1);
    r = r && help_expression_2(b, l + 1);
    exit_section_(b, m, R_HELP_EXPRESSION, r);
    return r;
  }

  // ( help)?
  private static boolean help_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_expression_1")) return false;
    consumeTokenSmart(b, R_HELP);
    return true;
  }

  // keyword | expression
  private static boolean help_expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_expression_2")) return false;
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

  // '(' &<<incBrackets>>
  private static boolean parenthesized_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_LPAR);
    r = r && parenthesized_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &<<incBrackets>>
  private static boolean parenthesized_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = incBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ')' &<<decBrackets>>
  private static boolean parenthesized_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_RPAR);
    r = r && parenthesized_expression_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &<<decBrackets>>
  private static boolean parenthesized_expression_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = decBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // function_literal parameter_list expression
  public static boolean function_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_expression")) return false;
    if (!nextTokenIsSmart(b, R_FUNCTION, R_SHORTHAND_FUNCTION)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _COLLAPSE_, R_FUNCTION_EXPRESSION, "<function expression>");
    r = function_literal(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, parameter_list(b, l + 1));
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !<<isNewLine>> left_assign_operator
  private static boolean left_assign_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "left_assign_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = left_assign_expression_0_0(b, l + 1);
    r = r && left_assign_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean left_assign_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "left_assign_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> eq_assign_operator
  private static boolean eq_assign_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "eq_assign_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = eq_assign_expression_0_0(b, l + 1);
    r = r && eq_assign_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean eq_assign_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "eq_assign_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> right_assign_operator
  private static boolean right_assign_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "right_assign_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = right_assign_expression_0_0(b, l + 1);
    r = r && right_assign_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean right_assign_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "right_assign_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  public static boolean unary_tilde_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_tilde_expression")) return false;
    if (!nextTokenIsSmart(b, R_TILDE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = tilde_operator(b, l + 1);
    p = r;
    r = p && expression(b, l, 13);
    exit_section_(b, l, m, R_UNARY_TILDE_EXPRESSION, r, p, null);
    return r || p;
  }

  // !<<isNewLine>> tilde_operator
  private static boolean tilde_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tilde_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = tilde_expression_0_0(b, l + 1);
    r = r && tilde_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean tilde_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tilde_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> or_operator
  private static boolean or_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = or_expression_0_0(b, l + 1);
    r = r && or_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean or_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> and_operator
  private static boolean and_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = and_expression_0_0(b, l + 1);
    r = r && and_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean and_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  public static boolean unary_not_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_not_expression")) return false;
    if (!nextTokenIsSmart(b, R_NOT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = not_operator(b, l + 1);
    p = r;
    r = p && expression(b, l, 17);
    exit_section_(b, l, m, R_UNARY_NOT_EXPRESSION, r, p, null);
    return r || p;
  }

  // !<<isNewLine>> compare_operator
  private static boolean compare_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = compare_expression_0_0(b, l + 1);
    r = r && compare_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean compare_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> plusminus_operator
  private static boolean plusminus_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "plusminus_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = plusminus_expression_0_0(b, l + 1);
    r = r && plusminus_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean plusminus_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "plusminus_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> muldiv_operator
  private static boolean muldiv_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "muldiv_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = muldiv_expression_0_0(b, l + 1);
    r = r && muldiv_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean muldiv_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "muldiv_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> infix_operator
  private static boolean infix_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "infix_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = infix_expression_0_0(b, l + 1);
    r = r && infix_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean infix_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "infix_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> forward_pipe_operator
  private static boolean forward_pipe_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "forward_pipe_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = forward_pipe_expression_0_0(b, l + 1);
    r = r && forward_pipe_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean forward_pipe_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "forward_pipe_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> colon_operator
  private static boolean colon_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "colon_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = colon_expression_0_0(b, l + 1);
    r = r && colon_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean colon_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "colon_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  public static boolean unary_plusminus_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_plusminus_expression")) return false;
    if (!nextTokenIsSmart(b, R_MINUS, R_PLUS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = plusminus_operator(b, l + 1);
    p = r;
    r = p && expression(b, l, 24);
    exit_section_(b, l, m, R_UNARY_PLUSMINUS_EXPRESSION, r, p, null);
    return r || p;
  }

  // !<<isNewLine>> exp_operator
  private static boolean exp_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "exp_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = exp_expression_0_0(b, l + 1);
    r = r && exp_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean exp_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "exp_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> ('[' ']' | '[' &<<incBrackets>> subscription_expr_list ']' &<<decBrackets>> |
  //   '[[' ']]' | '[[' &<<incBrackets>> subscription_expr_list ']]' &<<decBrackets>>)
  private static boolean subscription_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = subscription_expression_0_0(b, l + 1);
    r = r && subscription_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean subscription_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '[' ']' | '[' &<<incBrackets>> subscription_expr_list ']' &<<decBrackets>> |
  //   '[[' ']]' | '[[' &<<incBrackets>> subscription_expr_list ']]' &<<decBrackets>>
  private static boolean subscription_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parseTokensSmart(b, 0, R_LBRACKET, R_RBRACKET);
    if (!r) r = subscription_expression_0_1_1(b, l + 1);
    if (!r) r = parseTokensSmart(b, 0, R_LDBRACKET, R_RDBRACKET);
    if (!r) r = subscription_expression_0_1_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '[' &<<incBrackets>> subscription_expr_list ']' &<<decBrackets>>
  private static boolean subscription_expression_0_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_LBRACKET);
    r = r && subscription_expression_0_1_1_1(b, l + 1);
    r = r && subscription_expr_list(b, l + 1);
    r = r && consumeToken(b, R_RBRACKET);
    r = r && subscription_expression_0_1_1_4(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &<<incBrackets>>
  private static boolean subscription_expression_0_1_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_1_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = incBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &<<decBrackets>>
  private static boolean subscription_expression_0_1_1_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_1_1_4")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = decBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '[[' &<<incBrackets>> subscription_expr_list ']]' &<<decBrackets>>
  private static boolean subscription_expression_0_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_1_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_LDBRACKET);
    r = r && subscription_expression_0_1_3_1(b, l + 1);
    r = r && subscription_expr_list(b, l + 1);
    r = r && consumeToken(b, R_RDBRACKET);
    r = r && subscription_expression_0_1_3_4(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &<<incBrackets>>
  private static boolean subscription_expression_0_1_3_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_1_3_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = incBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &<<decBrackets>>
  private static boolean subscription_expression_0_1_3_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression_0_1_3_4")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = decBrackets(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> argument_list
  private static boolean call_impl_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "call_impl_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = call_impl_expression_0_0(b, l + 1);
    r = r && argument_list(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean call_impl_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "call_impl_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> list_subset_operator member_tag
  private static boolean member_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "member_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = member_expression_0_0(b, l + 1);
    r = r && list_subset_operator(b, l + 1);
    r = r && member_tag(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean member_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "member_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<isNewLine>> at_operator
  private static boolean at_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "at_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = at_expression_0_0(b, l + 1);
    r = r && at_operator(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean at_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "at_expression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  public static boolean namespace_access_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_access_expression")) return false;
    if (!nextTokenIsSmart(b, R_IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = namespace_access_expression_0(b, l + 1);
    p = r;
    r = p && expression(b, l, 30);
    exit_section_(b, l, m, R_NAMESPACE_ACCESS_EXPRESSION, r, p, null);
    return r || p;
  }

  // identifier !<<isNewLine>> ('::' | ':::')
  private static boolean namespace_access_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_access_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_IDENTIFIER);
    r = r && namespace_access_expression_0_1(b, l + 1);
    r = r && namespace_access_expression_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<isNewLine>>
  private static boolean namespace_access_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_access_expression_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !isNewLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '::' | ':::'
  private static boolean namespace_access_expression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_access_expression_0_2")) return false;
    boolean r;
    r = consumeTokenSmart(b, R_DOUBLECOLON);
    if (!r) r = consumeTokenSmart(b, R_TRIPLECOLON);
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

  // INVALID_STRING
  public static boolean invalid_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "invalid_literal")) return false;
    if (!nextTokenIsSmart(b, R_INVALID_STRING)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, R_INVALID_STRING);
    exit_section_(b, m, R_INVALID_LITERAL, r);
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
