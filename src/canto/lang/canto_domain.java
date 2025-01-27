/* Canto Compiler and Runtime Engine
 * 
 * canto_domain.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Map;

/**
 * This interface corresponds to the canto_domain object, defined in core and representing
 * a Canto domain, which consists of Canto code loaded under a particular set of restrictions.
 * Multiple canto_domains may be combined together into a single Canto application, but only by
 * dividing the application into multiple sites, since a site can only have one immediate
 * parent domain.
 */
public interface canto_domain {

    /** Returns the name of this domain.
     **/
    public String name();

    /** Returns the domain type.  The default for the primary domain is "site".
     */
    public String domain_type();

    /** Sites in this domain (keyed on site name). **/
    public Map<String, Site> sites();

    /** The name of the main site.  The main site is the first site to be queried when a name
     *  does not explicitly specify a site (followed by the default site and core).
     **/
    public String main_site();

    /** Returns the definition table associated with this domain. **/
    public Map<String, Definition> defs();

    /** Creates a canto_context object which can be used to construct Canto objects.  The
     *  canto_context will be able to construct objects whose definitions are in any of the
     *  sites in this domain.
     */
    public canto_context context();

    /** Retrieve data from this domain */
    public Object get(String expr) throws Redirection;
    public Definition get_definition(String expr) throws Redirection;
    public Object get_instance(String expr) throws Redirection;
    public Object[] get_array(String expr) throws Redirection;
    public Map<String, Object> get_table(String expr) throws Redirection;
    
    /** Returns the existing child domain with a given name, or null if it does not exist. **/
    public canto_domain child_domain(String name);
    
    /** Creates a new domain which is a child of this domain. **/
    public canto_domain child_domain(String name, String type, String src, boolean isUrl);
    public canto_domain child_domain(String name, String type, String path, String filter, boolean recursive);

    /** Compile the Canto source files found at the locations specified in <code>cantopath</code>
     *  and return a canto_domain object.  If a location is a directory and <code>recursive</code>
     *  is true, scan subdirectories recursively for Canto source files.  If <code>autoloadCore</code>
     *  is true, and the core definitions required by the system cannot be found in the files
     *  specified in <code>cantopath</code>, the processor will attempt to load the core
     *  definitions automatically from a known source (e.g. from the same jar file that the
     *  processor was loaded from).
     *
     *  <code>siteName</code> is the name of the main site; it may be null, in which case the
     *  default site must contain a definition for <code>main_site</code>, which must yield the
     *  name of the main site.
     */
    public canto_domain compile(String siteName, String cantopath, boolean recursive, boolean autoloadCore);

    /** Compile Canto source code passed in as a string and return a canto_domain object.  If
     *  <code>autoloadCore</code> is true, and the core definitions required by the system cannot
     *  be found in the passed text, the processor will attempt to load the core definitions
     *  automatically from a known source (e.g. from the same jar file that the processor was
     *  loaded from).
     *
     *  <code>siteName</code> is the name of the main site; it may be null, in which case the
     *  default site must contain a definition for <code>main_site</code>, which must yield the
     *  name of the main site.
     */
    public canto_domain compile(String siteName, String cantotext, boolean autoloadCore);

    /** Compile Canto source code passed in as a string and merge the result into the specified
     *  canto_domain.  If there is a fatal error in the code, the result is not merged and
     *  a Redirection is thrown.
     */
    public void compile_into(canto_domain domain, String cantotext) throws Redirection;
    
}


