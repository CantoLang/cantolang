/* Canto Compiler and Runtime Engine
 * 
 * CantoStandaloneServer.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

/**
 * Interface for standalone Canto-programmable HTTP server
 *
 * This allows CantoServer to avoid a dependency on a specifig implementation (e.g. Jetty)
 *
 * @author Michael St. Hippolyte
 */

public interface CantoStandaloneServer {

	public void startServer() throws Exception;

    public void stopServer() throws Exception;
    
    public boolean isRunning();

    public void join() throws InterruptedException;

    public void setVirtualHost(String virtualHost);
}
