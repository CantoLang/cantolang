/* Canto Compiler and Runtime Engine
 * 
 * CantoJettyServer.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.*;
import org.eclipse.jetty.util.thread.*;


/**
 * Canto server based on the Jetty HTTP server.
 *
 * @author Michael St. Hippolyte
 */

public class CantoJettyServer extends Server implements CantoStandaloneServer {

	public CantoJettyServer() {
	    super(new QueuedThreadPool());

	    // Create a ServerConnector to accept connections from clients.
	    Connector connector = new ServerConnector(this);

	    // Add the Connector to the Server
	    addConnector(connector);

	    // Set a simple Handler to handle requests/responses.
	    setHandler(new Handler.Abstract()
	    {
	        @Override
	        public boolean handle(Request request, Response response, Callback callback)
	        {
	            // Succeed the callback to signal that the
	            // request/response processing is complete.
	            callback.succeeded();
	            return true;
	        }
	    });
	}
	
    @Override
    public void startServer() throws Exception {
        start();
    }

    @Override
    public void stopServer() throws Exception {
        stop();
    }

}
