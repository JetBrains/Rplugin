/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.roxygen.lexer;

import com.intellij.lexer.FlexLexer;
import java.util.Stack;
import com.intellij.psi.tree.IElementType;

import static org.jetbrains.r.roxygen.parsing.RoxygenElementTypes.*;

%%

%class _RoxygenLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

%state COMMENT_LINE_START, COMMENT_LINE_CONTINUE
%state PARAM_LIST_START, PARAM_LIST_CONTINUE
%state HELP_LINK_OR_LINK_TEXT, LINK_DESTINATION, AUTOLINK

// See org/jetbrains/r/lexer/R.flex for details
LETTER = [a-zA-Z]
IDENT_START = {LETTER}|"."{LETTER}|"._"|".."
IDENT_CONTINUE = {LETTER}|[0-9_"."]
QUOTED_IDENTIFIER = "`" ([^\\\`] | {ANY_ESCAPE_SEQUENCE})* "`"
IDENTIFIER = {IDENT_START}{IDENT_CONTINUE}** | {QUOTED_IDENTIFIER} | "."
ANY_ESCAPE_SEQUENCE = \\[^]

WS=[\ \t]

TAG_PREFIX="@"
TAG_ID=[:jletter:] [:jletterdigit:]*
TAG = {TAG_PREFIX} {TAG_ID}
PARAM_TAG = {TAG_PREFIX} "param"

DOC_COMMENT_PREFIX = "#'"

SCHEME = {LETTER} ({LETTER} | [0-9] | "+" | "-" | ".")*
AUTOLINK = {SCHEME} ":" [^\ \t\n\r\f<>]+

%{
  private Stack<IElementType> myExpectedBracketsStack = new Stack<>();
%}

%%
<YYINITIAL> {
  {DOC_COMMENT_PREFIX}          { yybegin(COMMENT_LINE_START); return ROXYGEN_DOC_PREFIX; }
}

<COMMENT_LINE_CONTINUE, HELP_LINK_OR_LINK_TEXT> {
  [\n]                          { yybegin(YYINITIAL); return ROXYGEN_NL; }
  (\\\\) ("[" | "]")            { return ROXYGEN_TEXT; }                   // See roxygen2::double_escape_md
  {ANY_ESCAPE_SEQUENCE}         { return ROXYGEN_TEXT; }
}

<YYINITIAL, COMMENT_LINE_START, COMMENT_LINE_CONTINUE, HELP_LINK_OR_LINK_TEXT> {
  {WS}+                         { return ROXYGEN_WS; }
}

// Tags must start at the beginning of a line
<COMMENT_LINE_START> {
  {PARAM_TAG}                   { yybegin(PARAM_LIST_START); return ROXYGEN_TAG_NAME; }
  {TAG}                         { yybegin(COMMENT_LINE_CONTINUE); return ROXYGEN_TAG_NAME; }
  [^]                           { yybegin(COMMENT_LINE_CONTINUE); yypushback(1); }
}

<COMMENT_LINE_CONTINUE> {
  "["                           { yybegin(HELP_LINK_OR_LINK_TEXT); return ROXYGEN_LBRACKET; }
  "<"                           { yybegin(AUTOLINK); return ROXYGEN_LANGLE; }
  [^\ \t\n\r\f\[<]+             { return ROXYGEN_TEXT; }
}

<PARAM_LIST_START> {
  {WS}*                         { yybegin(PARAM_LIST_CONTINUE); if (yylength() > 0) return ROXYGEN_WS; }
}

<PARAM_LIST_CONTINUE> {
  ","                           { return ROXYGEN_COMMA; }
  {IDENTIFIER}                  { return ROXYGEN_IDENTIFIER; }
  [^]                           { yybegin(COMMENT_LINE_CONTINUE); yypushback(1); }
}

<HELP_LINK_OR_LINK_TEXT> {
   "["                          { return ROXYGEN_LBRACKET; }
   {IDENTIFIER}                 { return ROXYGEN_IDENTIFIER; }
   "::"                         { return ROXYGEN_DOUBLECOLON; }
   "]" / "("                    { yybegin(LINK_DESTINATION); return ROXYGEN_RBRACKET; }
   "]"                          { yybegin(COMMENT_LINE_CONTINUE); return ROXYGEN_RBRACKET; }
   "("                          { return ROXYGEN_LPAR; }
   ")"                          { return ROXYGEN_RPAR; }
   "<"                          { yybegin(AUTOLINK); return ROXYGEN_LANGLE; }
}

<LINK_DESTINATION> {
   [^\(\)\n\r\f]+               { return ROXYGEN_TEXT; }
   "("                          { IElementType returnValue = myExpectedBracketsStack.isEmpty() ? ROXYGEN_LPAR : ROXYGEN_TEXT;
                                  myExpectedBracketsStack.add(ROXYGEN_LPAR);
                                  return returnValue;
                                }
   ")"                          { myExpectedBracketsStack.pop();
                                  IElementType returnValue;
                                  if (myExpectedBracketsStack.isEmpty()) {
                                    yybegin(COMMENT_LINE_CONTINUE);
                                    returnValue = ROXYGEN_RPAR;
                                  }
                                  else returnValue = ROXYGEN_TEXT;
                                  return returnValue;
                                }
}

<AUTOLINK> {
   {AUTOLINK} / ">"             { return ROXYGEN_AUTOLINK_URI; }
   "<"                          { return ROXYGEN_LANGLE; }
   ">"                          { yybegin(COMMENT_LINE_CONTINUE); return ROXYGEN_RANGLE; }
   "["                          { yybegin(HELP_LINK_OR_LINK_TEXT); return ROXYGEN_LBRACKET; }
   [^]                          { yybegin(COMMENT_LINE_CONTINUE); yypushback(1); }
}

[^]                             { return ROXYGEN_TEXT; }
