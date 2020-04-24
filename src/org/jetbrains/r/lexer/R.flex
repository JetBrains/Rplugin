/* It's an automatically generated code. Do not modify it. */
/* Use 'ant' command in src/org/jetbrains/r/lexer directory to regenerate the lexer */
package org.jetbrains.r.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import java.util.Stack;
import org.jetbrains.r.parsing.RElementTypes;
import org.jetbrains.r.parsing.RParserDefinition;

import static org.jetbrains.r.parsing.RElementTypes.*;
import static org.jetbrains.r.parsing.RParserDefinition.*;

%%

%class _RLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

WHITE_SPACE_CHAR=[\ \n\r\t\f]

// Identifiers consist of a sequence of letters, digits, the period (‘.’) and the underscore.
// They must not start with a digit or an underscore, or with a period followed by a digit.
// The definition of a letter depends on the current locale: the precise set of characters allowed
// is given by the C expression (isalnum(c) || c == ’.’ || c == ’_’)
//
// So lets support letters from the default C locale for now
LETTER = [a-zA-Z] //
IDENT_START = {LETTER}|"."{LETTER}|"._"|".."
IDENT_CONTINUE = {LETTER}|[0-9_"."]
QUOTED_IDENTIFIER = "`" ([^\\\`]|{ANY_ESCAPE_SEQUENCE})* "`"
IDENTIFIER = {IDENT_START}{IDENT_CONTINUE}** | {QUOTED_IDENTIFIER} | "."

END_OF_LINE_COMMENT="#"[^\r\n]*


// numeric constants
DIGIT = [0-9]
NONZERO_DIGIT = [1-9]
HEX_DIGIT = [0-9A-Fa-f]
OCT_DIGIT = [0-7]
NONZERO_OCT_DIGIT = [1-7]
HEX_INTEGER = 0[Xx]({HEX_DIGIT})+
DECIMAL_INTEGER = ({DIGIT}({DIGIT})*)
INTEGER = {DECIMAL_INTEGER}|{HEX_INTEGER}                                  // essential

INT_PART = ({DIGIT})+
FRACTION = \.({DIGIT})+
EXPONENT = [eE][+\-]?({DIGIT})+
BINARY_EXPONENT = [pP][+\-]?({DIGIT})+
POINT_FLOAT=(({INT_PART})?{FRACTION})|({INT_PART}\.)
EXPONENT_HEX = ({HEX_INTEGER}|({HEX_INTEGER}{FRACTION})){BINARY_EXPONENT}
EXPONENT_FLOAT=(({INT_PART})|({POINT_FLOAT})){EXPONENT}
FLOAT_NUMBER=({POINT_FLOAT})|({EXPONENT_FLOAT})|({EXPONENT_HEX})             // essential

// integer constants
LONG_INTEGER = ({INTEGER} | {FLOAT_NUMBER})[Ll]                                              // essential

// complex constants
COMPLEX_NUMBER=(({FLOAT_NUMBER})|({INT_PART}))[i]             // essential

// string constants
QUOTED_LITERAL="'"([^\\\']|{ANY_ESCAPE_SEQUENCE})*?("'")?
DOUBLE_QUOTED_LITERAL=\"([^\\\"]|{ANY_ESCAPE_SEQUENCE})*?(\")?
ANY_ESCAPE_SEQUENCE = \\[^]
STRING=({QUOTED_LITERAL} | {DOUBLE_QUOTED_LITERAL})
//ESCAPE_SEQUENCE=\\([rntbafv\'\"\\]|{NONZERO_OCT_DIGIT}|{OCT_DIGIT}{2,3}|"x"{HEX_DIGIT}{1,2}|"u"{HEX_DIGIT}{1,4}|"u{"{HEX_DIGIT}{1,4}"}"|"U"{HEX_DIGIT}{1,8}|"U{"{HEX_DIGIT}{1,8}"}")

%{
private Stack<IElementType> myExpectedBracketsStack = new Stack<>();
%}

%xstate RAW_STRING_STATE

%%

<YYINITIAL> {
[\n]                        { return R_NL; }
{END_OF_LINE_COMMENT}       { return END_OF_LINE_COMMENT; }
[\ ]                        { return SPACE; }
[\t]                        { return TAB; }
[\f]                        { return FORMFEED; }

// logical constants
"TRUE"                      { return R_TRUE; }
"FALSE"                     { return R_FALSE; }
// disabled because they are not reserved keywords in r but just default assignments to TRUE and FALSE
//"T"                         { return R_TRUE; }
//"F"                         { return R_FALSE; }

// numeric constants
{INTEGER}                   { return R_NUMERIC; }
{FLOAT_NUMBER}              { return R_NUMERIC; }

// complex constants
{COMPLEX_NUMBER}            { return R_COMPLEX; }

// integer constants
{LONG_INTEGER}              { return R_INTEGER; }

// string constants
{STRING}                    { return R_STRING; }

r\"\(                       { yybegin(RAW_STRING_STATE); }

// special constants
"NULL"                      { return R_NULL; }
"NA"                        { return R_NA; }
"Inf"                       { return R_INF; }
"NaN"                       { return R_NAN; }

"NA_integer_"               { return R_NA_INTEGER_; }
"NA_real_"                  { return R_NA_REAL_; }
"NA_complex_"               { return R_NA_COMPLEX_; }
"NA_character_"             { return R_NA_CHARACTER_; }

"if"                        { return R_IF; }
"else"                      { return R_ELSE; }
"repeat"                    { return R_REPEAT; }
"while"                     { return R_WHILE; }
"function"                  { return R_FUNCTION; }
"for"                       { return R_FOR; }
"in"                        { return R_IN; }
"next"                      { return R_NEXT; }
"break"                     { return R_BREAK; }

{IDENTIFIER}                { return R_IDENTIFIER; }

// R allows user-defined infix operators. These have the form of a string of characters delimited
// by the ‘%’ character. The string can contain any printable character except ‘%’. The escape
// sequences for strings do not apply here.
"%"[^%]*"%"                 { return R_INFIX_OP; }

// Infix and prefix operators
":::"                       { return R_TRIPLECOLON; }
"::"                        { return R_DOUBLECOLON; }
"@"                         { return R_AT; }
"&&"                        { return R_ANDAND; }
"||"                        { return R_OROR; }


//arithmetic
"-"                         { return R_MINUS; }
"+"                         { return R_PLUS; }
"*"                         { return R_MULT; }
"/"                         { return R_DIV; }
"^"                         { return R_EXP; }
"**"                        { return R_EXP; } // deprecated form

// relational
"<"                         { return R_LT; }
">"                         { return R_GT; }
"=="                        { return R_EQEQ; }
">="                        { return R_GE; }
"<="                        { return R_LE; }
"!="                        { return R_NOTEQ; }

// logical
"!"                         { return R_NOT; }
"|"                         { return R_OR; }
"&"                         { return R_AND; }

// model formulae
"~"                         { return R_TILDE; }

// assign
"<<-"                       { return R_LEFT_COMPLEX_ASSIGN; }
"->>"                       { return R_RIGHT_COMPLEX_ASSIGN; }
"<-"                        { return R_LEFT_ASSIGN; }
":="                        { return R_LEFT_ASSIGN_OLD; }
"->"                        { return R_RIGHT_ASSIGN; }
"="                         { return R_EQ; }

// list indexing
"$"                         { return R_LIST_SUBSET; }

// sequence
":"                         { return R_COLON; }

// grouping
"("                         { return R_LPAR; }
")"                         { return R_RPAR; }
"{"                         { return R_LBRACE; }
"}"                         { return R_RBRACE; }

// indexing
"[["                        { myExpectedBracketsStack.add(R_RDBRACKET); return R_LDBRACKET; }
"]]"                        {
                              if (myExpectedBracketsStack.isEmpty()) return R_RDBRACKET;
                              final IElementType expectedBracket = myExpectedBracketsStack.pop();
                              if (expectedBracket == R_RDBRACKET) {
                                return R_RDBRACKET;
                              }
                              else {
                                yypushback(1);
                                return R_RBRACKET;
                              }
                              }
"["                         { myExpectedBracketsStack.add(R_RBRACKET); return R_LBRACKET; }
"]"                         {
                              if (myExpectedBracketsStack.isEmpty()) return R_RBRACKET;
                              myExpectedBracketsStack.pop();
                              return R_RBRACKET; }

// separators
","                         { return R_COMMA; }
";"                         { return R_SEMI; }

"?"                         { return R_HELP; }
.                           { return BAD_CHARACTER; }

}

<RAW_STRING_STATE> {
\)\"                        { yybegin(YYINITIAL); return R_STRING; }
[^]                         {}
<<EOF>>                     { yybegin(YYINITIAL); return R_INVALID_STRING; }
}
