/* Canto Compiler and Runtime Engine
 * 
 * site_config.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * This interface corresponds to the site_config object, defined in the
 * default site_config.can file.  It represents a website configuration --
 * name, cantopath, base directory for file-based resources, and
 * network address.
 */
public interface site_config {

    /** Returns the name of the site. **/
    public String name();
    
    /** The directories and/or files containing the Canto source
     *  code for this site.
     **/
    public String cantopath();
    
    /** The directories and/or files containing the Canto source
     *  code for core.
     **/
    public String corepath();
    
    /** The directories and/or files containing the Canto source
     *  code specific to this site (not including core).
     **/
    public String sitepath();

    /** The external interfaces (address and port) that the server should
     *  respond to for this site.  If null the globally defined value is used.
     **/
    public Object[] listen_to();    
}


