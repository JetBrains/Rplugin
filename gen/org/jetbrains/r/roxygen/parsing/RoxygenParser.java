// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.roxygen.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.jetbrains.r.roxygen.parsing.RoxygenElementTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class RoxygenParser implements PsiParser, LightPsiParser {

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
    create_token_set_(ROXYGEN_PARAM_TAG, ROXYGEN_TAG),
    create_token_set_(ROXYGEN_EXPRESSION, ROXYGEN_IDENTIFIER_EXPRESSION, ROXYGEN_NAMESPACE_ACCESS_EXPRESSION, ROXYGEN_PARAMETER),
  };

  /* ********************************************************** */
  // "<" AUTOLINK_URI ">"
  public static boolean autolink(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "autolink")) return false;
    if (!nextTokenIs(b, ROXYGEN_LANGLE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, ROXYGEN_LANGLE, ROXYGEN_AUTOLINK_URI, ROXYGEN_RANGLE);
    exit_section_(b, m, ROXYGEN_AUTOLINK, r);
    return r;
  }

  /* ********************************************************** */
  // ws* "#'" (content)*
  static boolean comment_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comment_line")) return false;
    if (!nextTokenIs(b, "", ROXYGEN_DOC_PREFIX, ROXYGEN_WS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comment_line_0(b, l + 1);
    r = r && consumeToken(b, ROXYGEN_DOC_PREFIX);
    r = r && comment_line_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ws*
  private static boolean comment_line_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comment_line_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, ROXYGEN_WS)) break;
      if (!empty_element_parsed_guard_(b, "comment_line_0", c)) break;
    }
    return true;
  }

  // (content)*
  private static boolean comment_line_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comment_line_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comment_line_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "comment_line_2", c)) break;
    }
    return true;
  }

  // (content)
  private static boolean comment_line_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comment_line_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = content(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // param_tag | tag | help_page_link | link_destination | autolink | text
  static boolean content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "content")) return false;
    boolean r;
    r = param_tag(b, l + 1);
    if (!r) r = tag(b, l + 1);
    if (!r) r = help_page_link(b, l + 1);
    if (!r) r = link_destination(b, l + 1);
    if (!r) r = autolink(b, l + 1);
    if (!r) r = text(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // namespace_access_expression | identifier_expression
  public static boolean expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression")) return false;
    if (!nextTokenIs(b, ROXYGEN_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, ROXYGEN_EXPRESSION, null);
    r = namespace_access_expression(b, l + 1);
    if (!r) r = identifier_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // "[" expression ("(" ")")? "]"
  public static boolean help_page_link(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_page_link")) return false;
    if (!nextTokenIs(b, ROXYGEN_LBRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ROXYGEN_LBRACKET);
    r = r && expression(b, l + 1);
    r = r && help_page_link_2(b, l + 1);
    r = r && consumeToken(b, ROXYGEN_RBRACKET);
    exit_section_(b, m, ROXYGEN_HELP_PAGE_LINK, r);
    return r;
  }

  // ("(" ")")?
  private static boolean help_page_link_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_page_link_2")) return false;
    help_page_link_2_0(b, l + 1);
    return true;
  }

  // "(" ")"
  private static boolean help_page_link_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "help_page_link_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, ROXYGEN_LPAR, ROXYGEN_RPAR);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean identifier_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "identifier_expression")) return false;
    if (!nextTokenIs(b, ROXYGEN_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ROXYGEN_IDENTIFIER);
    exit_section_(b, m, ROXYGEN_IDENTIFIER_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // "(" text* ")"
  public static boolean link_destination(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "link_destination")) return false;
    if (!nextTokenIs(b, ROXYGEN_LPAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ROXYGEN_LPAR);
    r = r && link_destination_1(b, l + 1);
    r = r && consumeToken(b, ROXYGEN_RPAR);
    exit_section_(b, m, ROXYGEN_LINK_DESTINATION, r);
    return r;
  }

  // text*
  private static boolean link_destination_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "link_destination_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!text(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "link_destination_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER "::" identifier_expression
  public static boolean namespace_access_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_access_expression")) return false;
    if (!nextTokenIs(b, ROXYGEN_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, ROXYGEN_IDENTIFIER, ROXYGEN_DOUBLECOLON);
    r = r && identifier_expression(b, l + 1);
    exit_section_(b, m, ROXYGEN_NAMESPACE_ACCESS_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // TAG_NAME (ws+ parameter ("," parameter)*)?
  public static boolean param_tag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "param_tag")) return false;
    if (!nextTokenIs(b, ROXYGEN_TAG_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ROXYGEN_TAG_NAME);
    r = r && param_tag_1(b, l + 1);
    exit_section_(b, m, ROXYGEN_PARAM_TAG, r);
    return r;
  }

  // (ws+ parameter ("," parameter)*)?
  private static boolean param_tag_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "param_tag_1")) return false;
    param_tag_1_0(b, l + 1);
    return true;
  }

  // ws+ parameter ("," parameter)*
  private static boolean param_tag_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "param_tag_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = param_tag_1_0_0(b, l + 1);
    r = r && parameter(b, l + 1);
    r = r && param_tag_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ws+
  private static boolean param_tag_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "param_tag_1_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ROXYGEN_WS);
    while (r) {
      int c = current_position_(b);
      if (!consumeToken(b, ROXYGEN_WS)) break;
      if (!empty_element_parsed_guard_(b, "param_tag_1_0_0", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // ("," parameter)*
  private static boolean param_tag_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "param_tag_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!param_tag_1_0_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "param_tag_1_0_2", c)) break;
    }
    return true;
  }

  // "," parameter
  private static boolean param_tag_1_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "param_tag_1_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ROXYGEN_COMMA);
    r = r && parameter(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter")) return false;
    if (!nextTokenIs(b, ROXYGEN_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ROXYGEN_IDENTIFIER);
    exit_section_(b, m, ROXYGEN_PARAMETER, r);
    return r;
  }

  /* ********************************************************** */
  // comment_line (nl comment_line)*
  static boolean root(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root")) return false;
    if (!nextTokenIs(b, "", ROXYGEN_DOC_PREFIX, ROXYGEN_WS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comment_line(b, l + 1);
    r = r && root_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (nl comment_line)*
  private static boolean root_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!root_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "root_1", c)) break;
    }
    return true;
  }

  // nl comment_line
  private static boolean root_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ROXYGEN_NL);
    r = r && comment_line(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // TAG_NAME
  public static boolean tag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag")) return false;
    if (!nextTokenIs(b, ROXYGEN_TAG_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ROXYGEN_TAG_NAME);
    exit_section_(b, m, ROXYGEN_TAG, r);
    return r;
  }

  /* ********************************************************** */
  // TEXT | ws
  static boolean text(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "text")) return false;
    if (!nextTokenIs(b, "", ROXYGEN_TEXT, ROXYGEN_WS)) return false;
    boolean r;
    r = consumeToken(b, ROXYGEN_TEXT);
    if (!r) r = consumeToken(b, ROXYGEN_WS);
    return r;
  }

}
