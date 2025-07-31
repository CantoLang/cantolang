/* Canto Compiler and Runtime Engine
 * 
 * DefParameter.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.CantoObjectWrapper;
import canto.util.EmptyList;

/**
 * DefParameter is a formal parameter declaration in a parameterized definition or a
 * for statement.
 */

public class DefParameter extends NamedDefinition {

    private List<Construction> list = null;

    public DefParameter() {
        super();
        setAccess(Access.LOCAL);
    }

    /** Construct an explicitly named parameter.  */
    public DefParameter(NameNode name) {
        super();
        setName(name);
        setType(DefaultType.TYPE);
        setAccess(Access.LOCAL);
    }

    /** Construct an unnamed primitive parameter */
    public DefParameter(Class<?> c) {
        super();
        setType(new PrimitiveType(c));
        setAccess(Access.LOCAL);
    }

    /** Construct an named primitive parameter */
    public DefParameter(NameNode name, Class<?> c) {
        super();
        setName(name);
        setType(new PrimitiveType(c));
        setAccess(Access.LOCAL);
    }

    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    /** A parameter cannot be abstract, so this always returns false.
     */
    public boolean isAbstract(Context context) {
        return false;
    }

    /** Returns true, since this is indeed a formal parameter definition. */
    public boolean isFormalParam() {
        return true;
    }

    /** Returns <code>true</code> if this parameter is defined in a <code>for</code>
     *  expression.
     */
    public boolean isInFor() {
        return (getParent() instanceof ForStatement.IteratorValues);
    }

    /** Get a definition for this parameter */
    public Definition getDefinitionFor(Context context, Object obj) {
        Definition def;
        DefParameter paramInstance;
        
        if (obj == null) {
            paramInstance = (DefParameter) clone();
            paramInstance.list = new EmptyList<Construction>();
            def = paramInstance;
            
        } else if (obj instanceof ResolvedInstance) {
            // already resolved, no need for rigamarole
            return ((ResolvedInstance) obj).getDefinition();
            
        } else if (obj instanceof Instantiation) {
            Instantiation arg = (Instantiation) obj;
            Definition argOwner = arg.getOwner();
            int numUnpushes = 0;
            try {
                if (arg.isParameterKind()) {
                    Scope scope = context.peek();
                    while (!scope.covers(argOwner)) {
                        Scope link = entry.getPrevious();
                        if (link == null || link.equals(context.getRootEntry())) {
                            while (numUnpushes-- > 0) {
                                context.repush();
                            }
                            break;
                        }
                        numUnpushes++;
                        entry = link;
                        context.unpush();
                    }
                    def = arg.getDefinition(context);
                } else if (argOwner != null && argOwner.isCollection() && ((CollectionDefinition) argOwner).isHonestCollection()) { 
                    def = new ElementDefinition(argOwner, arg);
                } else {
                    def = arg.getDefinition(context);
                }
            } finally {
                while (numUnpushes-- > 0) {
                    context.repush();
                }
            }

        } else if (obj instanceof ConstructionContainer) {
            paramInstance = (DefParameter) clone();
            paramInstance.list = ((ConstructionContainer) obj).getConstructions(context);
            def = paramInstance;
            
        // don't intercept Values here, let the next else take care of them.  In
        // particular, PrimitiveValues are both ValueGenerators and Values
        } else if (obj instanceof ValueGenerator && !(obj instanceof Value)) {
            paramInstance = (DefParameter) clone();
            paramInstance.list = Context.newArrayList(1, Construction.class);
            paramInstance.list.add(0, (Construction) obj);
            def = paramInstance;

        } else {
            if (obj instanceof Value) {
                obj = ((Value) obj).getData();
            }
            
            if (obj instanceof AliasedDefinition) {
                def = (Definition) obj;
            } else if (obj instanceof NamedDefinition) {
                NamedDefinition embeddedDef = (NamedDefinition) obj;
                def = new AliasedDefinition(embeddedDef, embeddedDef.getNameNode());
            } else if (obj instanceof CantoObjectWrapper) {
                def = ((CantoObjectWrapper) obj).getDefinition();
            } else {
                def = new ExternalDefinition(getNameNode(), getParent(), getOwner(), getType(), getAccess(), getDurability(), obj, null);
            }
        }
        return def;
    }

    public List<Construction> getConstructions(Context context) {
         if (list == null) {
            list = new EmptyList<Construction>();
        }
        return list;
    }

    public String getReferenceName() {
        if (list != null && list.size() == 1) {
            Object item = list.get(0);
            if (item instanceof Instantiation) {
                CantoNode ref = ((Instantiation) item).getReference();
                if (ref instanceof Name) {
                    return ((Name) ref).getName();
                }
            }
        }
        return "";
    }
    
//    protected String getTypeAndName() {
//        StringBuffer sb = new StringBuffer();
//        
//        Type type = getSuper();
//        if (type != null) {
//            sb.append(type.getName());
//            sb.append(' ');
//        }
//
//        String name = getReferenceName();
//        if (name != null && name.length() > 0) {
//            sb.append(name);
//            List<Dim> dims = getDims();
//            if (dims != null && dims.size() > 0) { 
//                Iterator<Dim> it = dims.iterator();
//                while (it.hasNext()) {
//                    it.next().toString();
//                }
//            }
//            sb.append(' ');
//        }
//        return sb.toString();        
//    }
}
