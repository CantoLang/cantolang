/* Canto Compiler and Runtime Engine
 * 
 * CantoJettyServer.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.*;
import org.eclipse.jetty.util.thread.*;

import canto.lang.*;

import java.io.*;
import java.util.*;

/**
 * Canto server based on the Jetty HTTP server.
 *
 * @author Michael St. Hippolyte
 */

public class CantoJettyServer extends CantoServer {

    private Server server;
    
	public CantoJettyServer() {
	    // Create and configure a ThreadPool.
	    QueuedThreadPool threadPool = new QueuedThreadPool();
	    threadPool.setName("server");

	    // Create a Server instance.
	    server = new Server(threadPool);

	    // Create a ServerConnector to accept connections from clients.
	    Connector connector = new ServerConnector(server);

	    // Add the Connector to the Server
	    server.addConnector(connector);

	    // Set a simple Handler to handle requests/responses.
	    server.setHandler(new Handler.Abstract()
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
	
    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
    
}
