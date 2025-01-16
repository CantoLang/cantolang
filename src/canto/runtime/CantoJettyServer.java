/* Canto Compiler and Runtime Engine
 * 
 * CantoJettyServer.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.*;


/**
 * Canto server based on the Jetty HTTP server.
 *
 * @author Michael St. Hippolyte
 */

public class CantoJettyServer extends Server implements CantoStandaloneServer {

    private String virtualHost = null;
    private CantoServer cantoServer = null;
    
	public CantoJettyServer(InetSocketAddress addr, CantoServer server) {
	    super(addr);
	    this.cantoServer = server;

	    // Create a ServerConnector to accept connections from clients.
	    Connector connector = new ServerConnector(this);

	    // Add the Connector to the Server
	    addConnector(connector);

	    // Set a simple Handler to handle requests/responses.
	    setHandler(new Handler.Abstract()
	    {
	        @Override
	        public boolean handle(CantoRequest request, Response response, Callback callback)
	        {
                try {
                    server.handle(request, response, callback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

    @Override
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

}
