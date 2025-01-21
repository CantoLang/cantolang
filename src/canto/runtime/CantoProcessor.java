/* Canto Compiler and Runtime Engine
 * 
 * CantoServer.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import canto.lang.CantoNode;
import canto.lang.Construction;
import canto.lang.Definition;
import canto.lang.ExternalDefinition;
import canto.lang.Instantiation;
import canto.lang.Redirection;
import canto.lang.canto_domain;
import canto.lang.canto_processor;
import canto.lang.canto_server;
import canto.lang.site_config;
import canto.runtime.Log;

import org.antlr.v4.runtime.RuntimeMetaData;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;


/**
 * 
 */
public class CantoProcessor implements canto_processor {
    private static final Log LOG = Log.getLogger(CantoProcessor.class);
    

    public CantoProcessor() {}


    /** Compile the Canto source files found at the locations specified in <code>cantopath</code>
     *  and return a CantoDomain object.  If a location is a directory, scan subdirectories
     *  recursively for Canto source files.  If the core definitions required by the system 
     *  cannot be found in the files specified in <code>cantopath</code>, the processor will
     *  attempt to load the core definitions automatically from a known source (e.g. from the
     *  same jar file that the processor was loaded from).
     */
    public canto_domain compile(String siteName, String cantopath) {
        CantoSite site = new CantoSite(siteName, this);
        site.load(cantopath, "*.canto");
        return site;
    }

    /** Compile Canto source code passed in as a string and return a canto_domain object.  If
     *  <code>autoloadCore</code> is true, and the core definitions required by the system cannot
     *  be found in the files specified in <code>cantopath</code>, the processor will attempt to
     *  load the core definitions automatically from a known source (e.g. from the same jar file
     *  that the processor was loaded from).
     */
    public canto_domain compile(String siteName, String cantotext, boolean autoloadCore) {
        return null;
    }

    /** Compile Canto source code passed in as a string and merge the result into the specified
     *  canto_domain.  If there is a fatal error in the code, the result is not merged and
     *  a Redirection is thrown.
     */
    public void compile_into(canto_domain domain, String cantotext) throws Redirection {
        ;
    }

    @Override
    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String version() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> props() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public canto_domain compile(String siteName, String cantopath, boolean recursive, boolean autoloadCore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String domain_type() {
        // TODO Auto-generated method stub
        return null;
    }

}
