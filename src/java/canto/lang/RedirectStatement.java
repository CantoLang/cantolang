/* Canto Compiler and Runtime Engine
 * 
 * RedirectStatement.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * A directive to redirect the client to a different page.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public class RedirectStatement extends Construction {

    public RedirectStatement() {
        super();
        setDynStat(false, false);
    }

    public Redirection getRedirection(Context context) {
        CantoNode child = getChild(0);
        if (child instanceof Instantiation) {
            return new Redirection((Instantiation) child, context);
        } else if (child instanceof Name) {
            return new Redirection(child.getName());
        } else {
            throw new IllegalArgumentException("Non-dynamic redirection requires a name");
        }
    }
    
    
    public boolean getBoolean(Context context) {
        throw new UnsupportedOperationException();
    }

    public String getText(Context context) throws Redirection {
        throw new UnsupportedOperationException();
    }

    public Object getData(Context context) throws Redirection {
        throw new UnsupportedOperationException();
    }

    public Object getData(Context context, Definition def) throws Redirection {
        throw new UnsupportedOperationException();
    }

    public boolean isAbstract(Context context) {
        CantoNode child = getChild(0);
        if (child instanceof Construction) {
            return ((Construction) child).isAbstract(context);
        }
        return false;
    }
    
    /** Returns the type of this construction in the specified context. */
    public Type getType(Context context, boolean generate) {
        throw new UnsupportedOperationException();
    }

    /** Returns the name of the definition being constructed */
    public String getDefinitionName() {
        throw new UnsupportedOperationException();
    }
    
    /** Return the construction that this construction resolves to, if it
     *  is a wrapper or alias of some sort, or else return this construction.
     *  This class is not a wrapper or alias, so it returns this construction.
     */
    public Construction getUltimateConstruction(Context context) {
        return this;
    }

    public String getString(Context context) throws Redirection {
        throw new UnsupportedOperationException();
    }

    public byte getByte(Context context) {
        throw new UnsupportedOperationException();
    }

    public char getChar(Context context) {
        throw new UnsupportedOperationException();
    }

    public int getInt(Context context) {
        throw new UnsupportedOperationException();
    }

    public long getLong(Context context) {
        throw new UnsupportedOperationException();
    }

    public double getDouble(Context context) {
        throw new UnsupportedOperationException();
    }

    public Value getValue(Context context) throws Redirection {
        throw new UnsupportedOperationException();
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        sb.append("redirect ");
        sb.append(getChild(0).toString());
        sb.append(" ? ");
        sb.append(getChild(1).toString());
        sb.append(" : ");
        sb.append(getChild(2).toString());
        sb.append(')');
        return sb.toString();
    }

    @Override
    public Object generateData(Context context, Definition def) throws Redirection {
        // TODO Auto-generated method stub
        return null;
    }
}
