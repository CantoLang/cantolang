/* Canto Compiler and Runtime Engine
 * 
 * CantoBuilder.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import canto.parser.CantoLexer;
import canto.parser.CantoParser;
import canto.runtime.Log;

/**
 * 
 */
public class CantoBuilder {

    private static final Log LOG = Log.getLogger(CantoBuilder.class);
    
    private CantoParser parser;
    private Exception exception = null;

    public CantoBuilder(Object source) throws IOException {
        this.parser = getCantoParser(source);
    }
    
    private CantoParser getCantoParser(Object source) throws IOException {
        CharStream cs = null;
        if (source instanceof Reader) {
            cs = CharStreams.fromReader((Reader) source);
        } else if (source instanceof String) {
            cs = CharStreams.fromString((String) source);
        } else if (source instanceof InputStream) {
            cs = CharStreams.fromStream((InputStream) source);
        } else if (source instanceof File) {
            cs = CharStreams.fromStream(new FileInputStream((File) source));
        } else if (source instanceof URL) {
            cs = CharStreams.fromStream(((URL) source).openStream());
        } else {
            throw new IOException("Invalid source type: " + source.getClass());
        }
        CantoLexer lexer = new CantoLexer(cs);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new CantoParser(tokens);
    }

    public Exception getException() {
        return exception;
    }

    public Site buildSite(Core core) {
        Site site = null;
        try {
            site = (Site) parser.compilationUnit().accept(new CantoVisitor());
            if (!site.validate(null, site)) {
                LOG.error("Site validation failed for site " + site.getName());
                site = null;
            }
            
        } catch (Exception e) {
            exception = e;
            LOG.error("Error building site", e);
        }
        if (site == null) {
            return null;
        }
        
        if (site instanceof Core) {
            core.mergeSite(site);
        } else {
            String name = site.getName();
            Map<String, DefinitionTable> defTableTable = core.getDefTableTable();
            DefinitionTable defTable = (DefinitionTable) defTableTable.get(name);
            if (defTable != null) {
                site.setDefinitionTable(defTable);
            } else {
                defTable = site.setNewDefinitionTable();
                defTableTable.put(name, defTable);
            }
            Map<String, Map<String, Object>> globalKeepTable = core.getGlobalKeepTable();
            Map<String, Object> globalKeep = globalKeepTable.get(name);
            if (globalKeep == null) {
                globalKeep = new HashMap<String, Object>();
                globalKeepTable.put(name,  globalKeep);
            }
            site.setGlobalKeep(globalKeep);
            core.addSite(site);
        }
        return site;
    }

    public ComplexName buildComplexName() {
        ComplexName name = null;
        try {
            name = (ComplexName) parser.compilationUnit().accept(new CantoVisitor());
        } catch (Exception e) {
            exception = e;
            LOG.error("Error building site", e);
        }
        return name;
    }
}
