/* Canto Compiler and Runtime Engine
 * 
 * CantoJettyServer.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.net.InetSocketAddress;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.session.*;
import org.eclipse.jetty.util.*;


/**
 * Canto server based on the Jetty HTTP server.
 */

public class CantoJettyServer extends Server implements CantoStandaloneServer {

    private String virtualHost = null;
    private CantoServer cantoServer = null;
    
	public CantoJettyServer(InetSocketAddress addr, CantoServer server) throws Exception {
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
	        public boolean handle(Request request, Response response, Callback callback)
	        {
                try {
                    server.handle(request, response, callback);
                    // Succeed the callback to signal that the
                    // request/response processing is complete.
                    callback.succeeded();
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.failed(e);
                }
	            return true;
	        }
	    });
	    
	    // Set a session manager
	    DefaultSessionIdManager idMgr = new DefaultSessionIdManager(this);
	    idMgr.setWorkerName("cantoserver");
	    addBean(idMgr, true);

	    HouseKeeper houseKeeper = new HouseKeeper();
	    houseKeeper.setSessionIdManager(idMgr);
	    //set the frequency of scavenge cycles
	    houseKeeper.setIntervalSec(3600L);
	    idMgr.setSessionHouseKeeper(houseKeeper);
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
