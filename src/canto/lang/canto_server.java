/* Canto Compiler and Runtime Engine
 * 
 * canto_server.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Map;

import canto.runtime.Context;

/**
 * This interface corresponds to the Canto canto_server object, which represents a Canto server.
 */
public interface canto_server {

    /** Returns the URL prefix that this server uses to filter requests.  This allows
     *  an HTTP server to dispatch requests among multiple Canto servers, using the
     *  first part of the URL to differentiate among them.
     *
     *  If null, this server accepts all requests.
     */
    public String base_url();


    /** Returns a table associating paths in the request to specific sites.  By default, a
     *  site's path is simply the site's name, except for the main site, whose path is an
     *  empty string.  If, however, there is a string in this table associated with a site's
     *  name, that string is used as the path for requests to that site.
     */
    public Map<String, String> site_paths();

    
    /** Returns the server address used to identify this server to other
     *  servers.
     */
    public String nominal_address();
    
    
    /** Returns true if the server was successfully started and has not yet
     *  been stopped.
     */
    public boolean is_running();


    /** Launches a new Canto server, initialized with the passed parameters, unless a server
     *  with the passed name has been launched already.
     */
    public canto_server launch_server(String name, Map<String, String> params);

    
    /** Launches a new Canto server, initialized with the passed parameters, after stopping 
     *  any previously launched server with the passed name.
     */
    public canto_server relaunch_server(String name, Map<String, String> params);


    /** Gets the server with the specified name, if such a server was launched
     *  by this server, else null.
     **/
    public canto_server get_server(String name);

    
    /** CantoRequest data from the server. **/
    public String get(Context context, String requestName, Map<String, String> requestParams) throws Redirection;
    
    /** CantoRequest data from the server. **/
    public String get(Context context, String requestName) throws Redirection;
    
}


