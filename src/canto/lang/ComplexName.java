/* Canto Compiler and Runtime Engine
 * 
 * ComplexName.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

import canto.parser.CantoLexer;
import canto.parser.CantoParser;
import canto.runtime.Log;
import canto.util.SingleItemList;

import java.io.StringReader;

/**
 * Base class for compound names.
 */

public class ComplexName extends NameNode implements Name, Initializable {
    private static final Log LOG = Log.getLogger(ComplexName.class);

    public ComplexName() {
        super();
    }

    public ComplexName(NameNode node, int start, int end) {
        super();
        int len = end - start;
        children = new CantoNode[len];
        for (int i = 0; i < len; i++) {
            children[i] = node.getPart(start + i);
            children[i].parent = this;
        }
        super.setName(computeName());
    }

    /** Combines two names into a new single name */
    public ComplexName(NameNode prefix, NameNode suffix) {
        super();
        int plen = (prefix instanceof ComplexName ? prefix.children.length : 1);
        int slen = (suffix instanceof ComplexName ? suffix.children.length : 1);

        children = new CantoNode[plen + slen];

        if (prefix instanceof ComplexName) {
            for (int i = 0; i < plen; i++) {
                children[i] = (CantoNode) prefix.children[i].clone();
            }
        } else {
            children[0] = (CantoNode) prefix.clone();
        }

        if (suffix instanceof ComplexName) {
            for (int i = 0; i < slen; i++) {
                children[plen + i] = (CantoNode) suffix.children[i].clone();
            }
        } else {
            children[plen] = (CantoNode) suffix.clone();
        }
        
        super.setName(computeName());
    }

    public ComplexName(NameNode node) {
        super();
        if (node instanceof ComplexName) {
            copyChildren(node);
        } else {
            setChild(0, (CantoNode) node.clone());
        }
        super.setName(node.getName());
    }


    public ComplexName(String name) {
        super(name);
        parseName(name);
    }

    public void init() {
        super.setName(computeName());
    }

    public void setName(String name) {
        super.setName(name);
        parseName(name);
    }

    private void parseName(String name) {
        try {
            CantoLexer lexer = new CantoLexer(CharStreams.fromString(name));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CantoParser parser = new CantoParser(tokens);
            ComplexName generatedName = parser.parseComplexName();
            copyChildren(generatedName);
        } catch (Exception e) {
            System.out.println("Exception parsing name in ComplexName: " + e);
            children = new CantoNode[1];
            name = '(' + name + ')';
            children[0] = new NameNode(name);
            children[0].parent = this;
        }
    }

    
    private String computeName() {
        String name = null;
        Iterator<CantoNode> it = getChildren();
        CantoNode node = null;
        while (it.hasNext()) {
            node = it.next();
//            if (it.hasNext() && node instanceof NameWithIndexes) {
//                String n = node.toString();
//                if (name == null) {
//                    name = n;
//                } else {
//                    name = name + '.' + n;
//                }
//            } else 
            	if (node instanceof Name) {
                String n = ((Name) node).getName();
                if (name == null) {
                    name = n;
                } else {
                    name = name + '.' + n;
                }
            } else if (node instanceof Dim) {
                name = name + "[]";
            }
        }
        return name;
    }

    public String getName() {
        String nm = super.getName();
        if (nm == null) {
            System.out.println("getName returns null name!!!!");
        }
        return nm;
    }
   
    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    /** Returns <code>true</code> if the name has dynamic arguments in any
     *  part of the name, else <code>false</code>.
     */
    public boolean isDynamic() {
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof NameNode) {
                if (((NameNode) children[i]).isDynamic()) {
                    return true;
                }
            } else if (children[i] instanceof ArgumentList) {
                if (((ArgumentList) children[i]).isDynamic()) {
                    return true;
                }
            }
        }
        return false;
    }

    
    /** Returns <code>true</code> indicating that this is a complex name, i.e., its
     *  children are names.
     */
    public boolean isComplex() {
        return true;
    }

    public int numParts() {
        int n = getNumChildren();
        if (n == 1 && ((NameNode) getChild(0)).isComplex()) {
            n = ((NameNode) getChild(0)).numParts();
        }
        return n;        
    }

    /** Returns the first part of the name. */
    public NameNode getFirstPart() {
        NameNode firstNode = (NameNode) getChild(0);
        if (firstNode.isComplex()) {
            return firstNode.getFirstPart();
        } else {
            return firstNode;
        }
    }

    /** Returns the last part of the name. */
    public NameNode getLastPart() {
        NameNode lastNode = (NameNode) getChild(getNumChildren() - 1);
        if (lastNode.isComplex()) {
            return lastNode.getLastPart();
        } else {
            return lastNode;
        }
    }

    /** Returns the nth part of the name.  Tthrows an IndexOutOfBounds exception if
     *   there are fewer than n + 1 parts.
     */
    public NameNode getPart(int n) {
        if (n < getNumChildren()) {
            return (NameNode) getChild(n);
        } else {
            NameNode firstNode = (NameNode) getChild(0);
            if (firstNode.isComplex()) {
                return firstNode.getPart(n);
            } else {
                throw new IndexOutOfBoundsException("part number " + n + " is too big for name");
            }
        }
    }
    
        /** Returns true if this name has one or more arguments, else false. */
    public boolean hasArguments() {
        return (getArguments() != null);
    }


    /** Returns the list of arguments associated with this name. */
    public ArgumentList getArguments() {
        // this name has arguments if the last child name has arguments, or
        // is followed by arguments.
        int n = children.length;
        for (int i = n - 1; i >= 0; i--) {
            if (children[i] instanceof NameNode) {
                return ((NameNode) children[i]).getArguments();
            } else if (children[i] instanceof ArgumentList) {
                return (ArgumentList) children[i];
            }
        }
        return null;
    }


    /** Returns true if this name has one or more indexes, else false. */
    public boolean hasIndexes() {
        // this name has indexes if any child has indexes, or is an index.
        int n = children.length;
        for (int i = 0; i < n; i++) {
            if (children[i] instanceof NameNode && ((NameNode) children[i]).hasIndexes()) {
                return true;
            } else if (children[i] instanceof Index) {
                return true;
            }
        }
        return false;
    }

    /** Returns a list of indexes associated with this name, or null if none. */
    public IndexList getIndexes() {
        // this name has indexes if the last child name has indexes, or
        // is followed by indexes.
        int n = children.length;
        for (int i = n - 1; i >= 0; i--) {
            if (children[i] instanceof NameNode) {
                return ((NameNode) children[i]).getIndexes();
            } else if (children[i] instanceof Index) {
                CantoNode[] indexes = new CantoNode[1];
                indexes[0] = children[i];
                return new IndexList(indexes);
            }
        }
        return null;
    }

    public boolean matches(String name) {
        Iterator<CantoNode> it = getChildren();
        StringTokenizer tok = new StringTokenizer(name, ".");

        CantoNode node = null;
        String namePart = null;
        while (it.hasNext() && tok.hasMoreTokens()) {
            node = (CantoNode) it.next();
            namePart = tok.nextToken();
            if (node instanceof Name) {
                Name nm = (Name) node;
                if (nm instanceof RegExp) {

                    RegExp regexp = (RegExp) nm;

                    if (!regexp.matches(namePart)) {
                        return false;
                    }
                    if (nm instanceof AnyAny) {
                        if (it.hasNext()) {
                            node = (CantoNode) it.next();
                            if (!(node instanceof Name)) {
                                throw new RuntimeException("only a Name may follow an AnyAny");
                            }
                            String thisNamePart = ((Name) node).getName();
                            while (tok.hasMoreTokens() && !thisNamePart.equals(namePart)) {
                                namePart = tok.nextToken();
                            }
                        }
                    }

                } else {
                    if (!nm.getName().equals(namePart)) {
                        return false;
                    }
                }
            }
        }
        if (it.hasNext()) {
            return false;
        } else if (tok.hasMoreTokens()) {
            return (node instanceof AnyAny);
        } else {
            return true;
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof ComplexName || obj instanceof ComplexType) {
            Iterator<CantoNode> it = getChildren();
            Iterator<CantoNode> otherIt = ((CantoNode) obj).getChildren();

            CantoNode node = null;
            CantoNode otherNode = null;
            while (it.hasNext() && otherIt.hasNext()) {
                node = it.next();
                otherNode = otherIt.next();
                if (node instanceof Name && otherNode instanceof Name) {
                    Name name = (Name) node;
                    Name otherName = (Name) otherNode;
                    if (name instanceof RegExp) {

                        if (otherName instanceof RegExp) {
                            throw new IllegalArgumentException("Cannot compare two names with regexps");
                        }

                        if (!((RegExp) name).matches(otherName.getName())) {
                            return false;
                        }
                        if (name instanceof AnyAny) {
                            if (it.hasNext()) {
                                node = it.next();
                                while (!node.equals(otherNode) && otherIt.hasNext()) {
                                    otherNode = otherIt.next();
                                }
                            }
                        }

                    } else if (otherName instanceof RegExp) {
                        if (!((RegExp) otherName).matches(name.getName())) {
                            return false;
                        }
                        if (otherName instanceof AnyAny) {
                            if (otherIt.hasNext()) {
                                otherNode = otherIt.next();
                                while (!otherNode.equals(node) && it.hasNext()) {
                                    node = it.next();
                                }
                            }
                        }
                    } else {
                        if (!name.getName().equals(otherName.getName())) {
                            return false;
                        }
                    }
                }
            }
            if (it.hasNext()) {
                return (otherNode instanceof AnyAny);
            } else if (otherIt.hasNext()) {
                return (node instanceof AnyAny);
            } else {
                return true;
            }

        } else if (obj instanceof Name) {
            Name otherName = (Name) obj;
            String name = getName();
            if (name == null) {
                return (otherName.getName() == null);
            } else {
                return name.equals(otherName.getName());
            }
        }
        return false;
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        
        Iterator<CantoNode> it = getChildren();
        CantoNode node = null;
        while (it.hasNext()) {
            node = (CantoNode) it.next();
            sb.append(node.toString(""));
            if (it.hasNext()) {
                sb.append('.');
            }
        }
        return sb.toString();
    }


}
