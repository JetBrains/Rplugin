// This is a generated file. Not intended for manual editing.
package org.jetbrains.r.roxygen.parsing;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.r.roxygen.psi.RoxygenElementType;
import org.jetbrains.r.roxygen.psi.impl.*;

public interface RoxygenElementTypes {

  IElementType ROXYGEN_AUTOLINK = new RoxygenElementType("ROXYGEN_AUTOLINK");
  IElementType ROXYGEN_EXPRESSION = new RoxygenElementType("ROXYGEN_EXPRESSION");
  IElementType ROXYGEN_HELP_PAGE_LINK = new RoxygenElementType("ROXYGEN_HELP_PAGE_LINK");
  IElementType ROXYGEN_IDENTIFIER_EXPRESSION = new RoxygenElementType("ROXYGEN_IDENTIFIER_EXPRESSION");
  IElementType ROXYGEN_LINK_DESTINATION = new RoxygenElementType("ROXYGEN_LINK_DESTINATION");
  IElementType ROXYGEN_NAMESPACE_ACCESS_EXPRESSION = new RoxygenElementType("ROXYGEN_NAMESPACE_ACCESS_EXPRESSION");
  IElementType ROXYGEN_PARAMETER = new RoxygenElementType("ROXYGEN_PARAMETER");
  IElementType ROXYGEN_PARAM_TAG = new RoxygenElementType("ROXYGEN_PARAM_TAG");
  IElementType ROXYGEN_TAG = new RoxygenElementType("ROXYGEN_TAG");

  IElementType ROXYGEN_AUTOLINK_URI = new RoxygenElementType("AUTOLINK_URI");
  IElementType ROXYGEN_COMMA = new RoxygenElementType(",");
  IElementType ROXYGEN_DOC_PREFIX = new RoxygenElementType("#'");
  IElementType ROXYGEN_DOUBLECOLON = new RoxygenElementType("::");
  IElementType ROXYGEN_IDENTIFIER = new RoxygenElementType("IDENTIFIER");
  IElementType ROXYGEN_LANGLE = new RoxygenElementType("<");
  IElementType ROXYGEN_LBRACKET = new RoxygenElementType("[");
  IElementType ROXYGEN_LPAR = new RoxygenElementType("(");
  IElementType ROXYGEN_NL = new RoxygenElementType("nl");
  IElementType ROXYGEN_RANGLE = new RoxygenElementType(">");
  IElementType ROXYGEN_RBRACKET = new RoxygenElementType("]");
  IElementType ROXYGEN_RPAR = new RoxygenElementType(")");
  IElementType ROXYGEN_TAG_NAME = new RoxygenElementType("TAG_NAME");
  IElementType ROXYGEN_TEXT = new RoxygenElementType("TEXT");
  IElementType ROXYGEN_WS = new RoxygenElementType("ws");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ROXYGEN_AUTOLINK) {
        return new RoxygenAutolinkImpl(node);
      }
      else if (type == ROXYGEN_HELP_PAGE_LINK) {
        return new RoxygenHelpPageLinkImpl(node);
      }
      else if (type == ROXYGEN_IDENTIFIER_EXPRESSION) {
        return new RoxygenIdentifierExpressionImpl(node);
      }
      else if (type == ROXYGEN_LINK_DESTINATION) {
        return new RoxygenLinkDestinationImpl(node);
      }
      else if (type == ROXYGEN_NAMESPACE_ACCESS_EXPRESSION) {
        return new RoxygenNamespaceAccessExpressionImpl(node);
      }
      else if (type == ROXYGEN_PARAMETER) {
        return new RoxygenParameterImpl(node);
      }
      else if (type == ROXYGEN_PARAM_TAG) {
        return new RoxygenParamTagImpl(node);
      }
      else if (type == ROXYGEN_TAG) {
        return new RoxygenTagImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
