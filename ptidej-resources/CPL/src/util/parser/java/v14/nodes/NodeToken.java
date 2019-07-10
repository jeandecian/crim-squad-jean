/*******************************************************************************
 * Copyright (c) 2014 Yann-Ga�l Gu�h�neuc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Yann-Ga�l Gu�h�neuc and others, see in file; API and its implementation
 ******************************************************************************/
//
// Generated by JTB 1.2.2
//

package util.parser.java.v14.nodes;

import java.util.*;
/**
 * Represents a single token in the grammar.  If the "-tk" option
 * is used, also contains a Vector of preceding special tokens.
 */
public class NodeToken implements Node {
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

public NodeToken(String s) {
      this(s, -1, -1, -1, -1, -1);    }

   public NodeToken(String s, int kind, int beginLine, int beginColumn, int endLine, int endColumn) {
      this.tokenImage = s;
      this.specialTokens = null;
      this.kind = kind;
      this.beginLine = beginLine;
      this.beginColumn = beginColumn;
      this.endLine = endLine;
      this.endColumn = endColumn;
   }

   public NodeToken getSpecialAt(int i) {
      if ( this.specialTokens == null )
         throw new NoSuchElementException("No specials in token");
      return (NodeToken)this.specialTokens.elementAt(i);
   }

   public int numSpecials() {
      if ( this.specialTokens == null ) return 0;
      return this.specialTokens.size();
   }

   public void addSpecial(NodeToken s) {
      if ( this.specialTokens == null ) this.specialTokens = new Vector<NodeToken>();
      this.specialTokens.addElement(s);
   }

   public void trimSpecials() {
      if ( this.specialTokens == null ) return;
      this.specialTokens.trimToSize();
   }

   public String toString()     { return this.tokenImage; }

   public String withSpecials() {
      if ( this.specialTokens == null )
          return this.tokenImage;

       StringBuffer buf = new StringBuffer();

       for ( Enumeration<NodeToken> e = this.specialTokens.elements(); e.hasMoreElements(); )
          buf.append(e.nextElement().toString());

       buf.append(this.tokenImage);
       return buf.toString();
   }

   public void accept(util.parser.java.v14.visitors.Visitor v) {
      v.visit(this);
   }
   public Object accept(util.parser.java.v14.visitors.ObjectVisitor v, Object argu) {
      return v.visit(this,argu);
   }

   public String tokenImage;

   // Stores a list of NodeTokens
   public Vector<NodeToken> specialTokens;

   // -1 for these ints means no position info is available.
   public int beginLine, beginColumn, endLine, endColumn;

   // Equal to the JavaCC token "kind" integer.
   // -1 if not available.
   public int kind;
}

