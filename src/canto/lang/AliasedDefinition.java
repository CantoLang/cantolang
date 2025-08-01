/* Canto Compiler and Runtime Engine
 * 
 * AliasedDefinition.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;


/**
* An AliasedDefinition is a definition that references another definition.
*/

public class AliasedDefinition extends ExternalDefinition {
    Definition def;

    public AliasedDefinition(Definition def, NameNode alias) {
        super(def.getNameNode(), def.getParent(), def.getOwner(), null, Definition.Access.SITE, Definition.Durability.IN_CONTEXT, def, null);
        this.def = def;
        setName(alias);
        Site site = def.getSite();
        Definition definitionDef = site.getDefinition("definition");
        if (definitionDef != null) {
           Type definitionType = definitionDef.getType();

           setType(TypeList.addTypes(def.getType(),definitionType));
           
           //setSuper(def.getType());
           //setType(definitionType);

           //Type st = def.getSuper();
           //
           //if (st == null) {
           //    setSuper(definitionType);
           //} else {
           //    setSuper(TypeList.addTypes(st, definitionType));
           //}
        }
    }

    /** Returns true, because this definition represents a special name, whose
     *  meaning can change. 
     */
    public boolean isDynamic() {
        return true;
    }

    public Durability getDurability() {
        return Durability.IN_CONTEXT;
    }

    /** Construct this definition with the specified arguments in the specified context. */
    public Object instantiate(ArgumentList args, IndexList indexes, Context context) throws Redirection {
        return def.instantiate(args, indexes, context);
    }

        
    public Object getChild(NameNode node, ArgumentList args, IndexList indexes, ArgumentList parentArgs, Context context, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        
        Object data = def.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
        if (data == null || data == UNDEFINED) {
            data = super.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
        }
        return data; 
    }
    
    public Definition getDefForContext(Context context, ArgumentList args) {
//        if (def instanceof ExternalDefinition) {
//            return ((ExternalDefinition) def).getDefForContext(context, args);
//        } else {
//            return def;
//        }
        return this;
    }

    public Definition getUltimateDefinition(Context context) {
        if (Name.THIS.equals(getName())) {
            // this is to make sure an object wrapper is created
            // when this definition is instantiated
            return this;
        } else if (def instanceof AliasedDefinition) {
            return def.getUltimateDefinition(context);
        } else {
            return def;
        }
    }

    public Definition getAliasedDefinition(Context context) {
        return def;
    }


    public List<ParameterList> getParamLists() {
        return def.getParamLists();
    }
    
    /** Returns the type object for the aliased definition. */
    //public Type getType() {
    //    return def != null ? def.getType() : super.getType();
    //}

    public Site getSite() {
        if (def instanceof Site) {
            return (Site) def;
        } else {
            return super.getSite();
        }
    }
    
    DefinitionTable getDefinitionTable() {
        return def.getDefinitionTable();
    }

    /** Create the type corresponding to this definition.  This is a copy of  
     *  the createType function in NamedDefinition -- i.e., the super super
     *  super definition of this one -- which effectively bypasses the
     *  ExternalDefinition version of createType, which creates an ExternalType.
     *  This way we get a regular, non-external type, which works better
     *  for parameter list matching (see this_type_test.show_c).
     **/
    protected Type createType() {
        NameNode nameNode = getNameNode();
        ComplexType type = new ComplexType(this, nameNode.getName(), nameNode.getDims(), nameNode.getArguments());
        type.setOwner(getOwner());
        return type;
    }
    
}

