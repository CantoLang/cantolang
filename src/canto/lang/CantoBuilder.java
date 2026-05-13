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
            site.initializeTables(core);
            if (site instanceof Core) {
                core.mergeSite(site);
            } else {
                core.addSite(site);
            }
            
            site.resolve(null);
            
            if (!site.validate(site.getParent(), site.getOwner())) {
                LOG.error("Site validation failed for site " + site.getName());
                site = null;
            }
            
        } catch (Exception e) {
            exception = e;
            LOG.error("Error building site", e);
        }
        
        return site;
    }

    public ComplexName buildComplexName() {
        ComplexName name = null;
        try {
            name = (ComplexName) parser.complexName().accept(new CantoVisitor());
        } catch (Exception e) {
            exception = e;
            LOG.error("Error building site", e);
        }
        return name;
    }
}
