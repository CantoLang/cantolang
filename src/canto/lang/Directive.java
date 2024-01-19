/* Canto Compiler and Runtime Engine
 * 
 * Directive.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Iterator;


/**
 * 
 */
abstract public class Directive extends CantoNode {

    private NameRange nameRange;

    public static Directive newAdopt(NameRange nameRange) {
        return new AdoptDirective(nameRange);
    }
    
    public static Directive newExtern(String lang, NameRange nameRange) {
        return new ExternDirective(lang, nameRange);
    }

    public static Directive newImport(NameRange nameRange, Name alias) {
        return new ImportDirective(nameRange, alias);
    }

    
    protected Directive(String source, NameRange nameRange) {
        super();
        this.nameRange = nameRange;
    }

    public NameRange getNameRange() {
        return nameRange;
    }
    
    public boolean isAdopt() {
        return false;
    }

    public boolean isExtern() {
        return false;
    }

    public String getLang() {
        return null;
    }

    public boolean isImport() {
        return false;
    }

    public Name getAlias() {
        return null;
    }
    
    @Override
    public CantoNode getChild(int n) {
        throw new IndexOutOfBoundsException("A Directive has no children");
    }

    @Override
    public Iterator<CantoNode> getChildren() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public boolean isDefinition() {
        return false;
    }

    @Override
    public int getNumChildren() {
        return 0;
    }

}

class AdoptDirective extends Directive {

    AdoptDirective(NameRange nameRange) {
        super("adopt " + nameRange.toString(), nameRange);
    }

    @Override
    public boolean isAdopt() {
        return true;
    }
}

class ExternDirective extends Directive {

    private String lang;

    ExternDirective(String lang, NameRange nameRange) {
        super("adopt " + nameRange.toString(), nameRange);
        this.lang = lang;
    }
    
    @Override
    public boolean isExtern() {
        return true;
    }   
    
    public String getLang() {
        return lang;
    }
}

class ImportDirective extends Directive {

    private Name alias;

    ImportDirective(NameRange nameRange, Name alias) {
        super("import " + nameRange.toString(), nameRange);
        this.alias = alias;
    }

    @Override
    public boolean isImport() {
        return true;
    }

    public Name getAlias() {
        return alias;
    }
}


