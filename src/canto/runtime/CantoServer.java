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
import canto.lang.Context;
import canto.lang.Definition;
import canto.lang.ExternalDefinition;
import canto.lang.Instantiation;
import canto.lang.Redirection;
import canto.lang.canto_domain;
import canto.lang.canto_server;
import canto.lang.site_config;
import canto.runtime.CantoStandaloneServer;
import canto.runtime.Log;
import canto.runtime.CantoServer.RequestState;

import org.antlr.v4.runtime.RuntimeMetaData;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;


/**
 * 
 */
public class CantoServer implements canto_server {
    private static final Log LOG = Log.getLogger(CantoServer.class);
    
    public static final String NAME = "CantoServer";
    public static final String VERSION = Version.VERSION;
    public static final String NAME_AND_VERSION = NAME + " " + VERSION;

    public static final String REQUEST_STATE_ATTRIBUTE = "canto_request_state";

    /** Status codes **/
    
    public static final int OK = 200;
    public static final int NO_CONTENT = 204;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    public static final int SERVER_ERROR = 500;
    public static final int TIMEOUT = 504;

    /** Default port for the server */
    public static final int DEFAULT_PORT = 8080;
    
    /** This enum signifies the stage in the lifecycle of the request 
     *  containing it.
     */
     
    public enum RequestState
    {
        STARTING,
        STARTED,
        COMPLETE,
        ERROR,
        EXPIRED
    }
    
    public static final String SERVER_STARTED = "STARTED";
    public static final String SERVER_STOPPED = "STOPPED";
    public static final String SERVER_FAILED = "FAILED";

    protected Exception exception = null;
    protected CantoSite mainSite = null;
    protected Map<String, CantoSite> sites = new HashMap<String, CantoSite>();

    private String siteName = null;
    private String virtualHost = null;
    private String address = null;
    private int port = DEFAULT_PORT;
    private boolean initedOk = false;
    private String stateFileName = null;
    private String logFileName = null;
    private boolean appendToLog = true;
    private String cantoPath = ".";
    private boolean debuggingEnabled = false;
    private boolean verbose = false;
    protected String fileHandlerName = null;
    private long asyncTimeout = 0l;

    private CantoStandaloneServer standaloneServer = null;
    private HashMap<String, CantoServer> serverMap = new HashMap<String, CantoServer>();


    /** Main entry point, if CantoServer is run as a standalone application.  The following
     *  flags are recognized (in any order).  All flags are optional.
     *  <table><tr><th>argument</th><th>default</th><th>effect</th></tr><tr>
     *
     *  <td>  -site <name>                    </td><td>  name defined elsewhere </td><td> Specifies the site name.  If present, this must correspond to the name of a site
     *                                                                                    defined in the canto code.  If not present, the site name must be defined
     *                                                                                    elsewhere, e.g. in site_config.can. </td>
     *  <td>  -a <address>[:<port>]           </td><td>  localhost:80           </td><td> Sets the address and port which the server listens on.  </td>
     *  <td>  -p <port>                       </td><td>  80                     </td><td> Sets the port which the server listens on.  </td>
     *  <td>  -host <virtual host name>       </td><td>  all local hosts        </td><td> Name of virtual host.  </td>
     *  <td>  -cantopath <path>[<sep><path>]* </td><td>  current directory      </td><td> Sets the initial cantopath, which is a string of pathnames separated by the
     *                                                                                    platform-specific path separator character (e.g., colon on Unix and semicolon
     *                                                                                    on Windows).  Pathnames may specify either files or directories.  At startup,
     *                                                                                    for each pathname, the Canto server loads either the indicated file (if the
     *                                                                                    pathname specifies a file) or all the files with a .can extension in the
     *                                                                                    indicated directory (if the pathname specifies a directory).
     *  <td>  -log <path>                     </td><td>  no logging             </td><td> All output messages are logged in the specified file.  The file is overwritten
     *                                                                                    if it already exists.  </td>
     *  <td>  -log.append <path>              </td><td>  no logging             </td><td> All output messages are logged in the specified file.  If the file exists, the
     *                                                                                    current content is preserved, and messages are appended to the end of the file.  </td>
     *  <td>  -verbose                        </td><td>  not verbose            </td><td> Verbose output messages for debugging.  </td>.
     *  <td>  -debug                          </td><td>  debugging not enabled  </td><td> Enable the built-in debugger.  </td>.
     *
     */
    public static void main(String[] args) {

        boolean noProblems = true;
        
        Map<String, String> initParams = paramsFromArgs(args);
        
        String problems = initParams.get("problems");
        if (problems != null && !problems.equals("0")) {
            noProblems = false;
        }
        
        if (noProblems) {
            CantoServer server = new CantoServer(initParams);
            if (server.initedOk) {
                try {
                    server.startServer();
                    
                } catch (Exception e) {
                    noProblems = false;
                }
            } else {
                noProblems = false;
            }
            if (server.exception != null) {
                noProblems = false;
            }
            
        } else {
            System.out.println("Usage:");
            System.out.println("          java -jar canto.jar [flags]\n");
            System.out.println("where the optional flags are among the following (in any order):\n");
            System.out.println("Flag                           Effect");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("-s, --site <name>              Specifies the site name.  If present, this must");
            System.out.println("                               correspond to the name of a site defined in the");
            System.out.println("                               canto code.  If not present, the site name must");
            System.out.println("                               be defined elsewhere, e.g. in site_config.canto.\n");
            System.out.println("-a, --address <addr>[:<port>]  Sets the address and port which the server");
            System.out.println("                               listens on.\n");
            System.out.println("-p, --port <port>              Sets the port which the server listens on.\n");
            System.out.println("-h, --host <hostname>          Name of virtual host; if not present, all local");
            System.out.println("                               hosts are handled.\n");
            System.out.println("-t, --timeout <millisecs>      Sets the length of time the server must process");
            System.out.println("                               a request by before returning a timeout error.");
            System.out.println("                               A zero or -1 means the request will never time");
            System.out.println("                               out.  The default value is zero.\n");
            System.out.println("-cp, --cantopath <pathnames>   Sets the initial cantopath, which is a string");
            System.out.println("                               of pathnames separated by the platform-specific");
            System.out.println("                               path separator character (e.g., colon on Unix");
            System.out.println("                               and semicolon on Windows).  Pathnames may");
            System.out.println("                               specify either files or directories.  At");
            System.out.println("                               startup, for each pathname, the Canto server");
            System.out.println("                               loads either the indicated file (if the pathname");
            System.out.println("                               specifies a file) or all the files with a .canto");
            System.out.println("                               extension in the indicated directory (if the");
            System.out.println("                               pathname specifies a directory).\n");
            System.out.println("-sf, --statefile <filename>    Instructs the server to write state intormation");
            System.out.println("                               to a file.\n");
            System.out.println("-l, --log <path>               All output messages are logged in the specified");
            System.out.println("                               file.  The file is overwritten if it already");
            System.out.println("                               exists.\n");
            System.out.println("-la, --log.append <path>       All output messages are logged in the specified");
            System.out.println("                               file.  If the file exists, the current content");
            System.out.println("                               is preserved, and messages are appended to the");
            System.out.println("                               end of the file./n");
            System.out.println("-v, --verbose                  Verbose output messages for debugging.\n");
            System.out.println("--debug                        Enable the built-in debugger.\n");
            System.out.println("-?                             This screen.\n\n");
            System.out.println("Flags may be abbreviated to their initial letters, e.g. -a instead of -address,");
            System.out.println("or -la instead of -log.append.\n");
        }
    }

    private void startServer() throws Exception {
        standaloneServer.startServer();
        standaloneServer.join();
    }

    /** Constructor used when Canto is run as a standalone server */
    public CantoServer(Map<String, String> initParams) {
        initedOk = init(initParams);
        if (initedOk) {
            try {
                loadSite();

                InetSocketAddress addr = address == null ? new InetSocketAddress(port) : new InetSocketAddress(address, port);
                standaloneServer = new CantoJettyServer(addr, this);
                standaloneServer.setVirtualHost(virtualHost);

            } catch (Exception e) {
                recordState("FAILED");
                exception = e;
                LOG.error("Exception starting CantoServer: " + e);
                System.err.println("Exception starting CantoServer: " + e);
                e.printStackTrace(System.err);
            }
        }
    }

    private static Map<String, String> paramsFromArgs(String[] args) {
        Map<String, String> initParams = new HashMap<String, String>();
        int numProblems = 0;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String nextArg = (i + 1 < args.length ? args[i + 1] : null);
            boolean noNextArg = (nextArg == null || nextArg.startsWith("-"));
            if (arg.equals("--site") || arg.equals("-s")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "site name not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("site", nextArg);
                    i++;
                }

            } else if (arg.equals("--address") || arg.equals("-a")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "address not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("address", nextArg);
                    i++;
                }

            } else if (arg.equals("--port") || arg.equals("-p")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "port not provided";
                    initParams.put("problem" + numProblems, msg);
                } else if (!isPositiveNumber(nextArg)) {
                    numProblems++;
                    String msg = "port must be a positive number";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("port", nextArg);
                    i++;
                }

            } else if (arg.equals("--host") || arg.equals("-h")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "host not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("host", nextArg);
                    i++;
                }

            } else if (arg.equals("--timeout") || arg.equals("-t")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "timeout value not provided";
                    initParams.put("problem" + numProblems, msg);
                } else if (!isMinusOneOrAbove(nextArg)) {
                    numProblems++;
                    String msg = "timeout must be -1, 0, or a positive number";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("timeout", nextArg);
                    i++;
                }

            } else if (arg.equals("--cantopath") || arg.equals("-cp")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "cantopath not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("cantopath", nextArg);
                    i++;
                }

            } else if (arg.equals("--statefile") || arg.equals("-sf")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "state filename not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("statefile", nextArg);
                    i++;
                }

            } else if (arg.equals("--log") || arg.equals("-l")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "log file not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("log", nextArg);
                    i++;
                }

            } else if (arg.equals("--log.append") || arg.equals("-la")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "log.append file not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("log", nextArg);
                    initParams.put("log.append", "true");
                    i++;
                }

            } else if (arg.equals("--verbose") || arg.equals("-v")) {
                initParams.put("verbose", "true");

            } else if (arg.equals("--debug")) {
                initParams.put("debug", "true");

            } else {
                numProblems++;
                String msg = "unrecognized option: " + arg;
                initParams.put("problem" + numProblems, msg);
            }
        }
        initParams.put("problems", Integer.toString(numProblems));
        
        return initParams;
    }
       
    private static boolean isPositiveNumber(String str)
    {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private static boolean isMinusOneOrAbove(String str)
    {
        try {
            long n = Long.parseLong(str);
            return n >= -1L;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean init(Map<String, String> initParams) {
        try {    
            initGlobalSettings(initParams);
        } catch (Exception e) {
            exception = e;
            return false;
        }
        return true;
    }

    protected void initGlobalSettings(Map<String, String> initParams) throws Exception {

        String param;
        
        param = initParams.get("verbose");
        if ("true".equalsIgnoreCase(param)) {
            verbose  = true;
        }

        siteName = initParams.get("site");
        if (siteName == null) {
            siteName = canto.lang.Name.DEFAULT;
        }

        address = initParams.get("address");
        port = Integer.parseInt(initParams.get("port"));
        String timeout = initParams.get("timeout");
        if (timeout != null) {
            asyncTimeout = Long.parseLong(timeout);
        } else {
            asyncTimeout = 0L;
        }
        
        stateFileName = initParams.get("statefile");
        
        logFileName = initParams.get("log");
        String appendLog = initParams.get("log.append");
        appendToLog = isTrue(appendLog);
        if (logFileName != null) {
            Log.setLogFile(logFileName, appendToLog);
        }

        cantoPath = initParams.get("cantopath");
        if (cantoPath == null) {
            cantoPath = ".";
        }

        debuggingEnabled = isTrue(initParams.get("debug"));
    }

    /** Compile the Canto source files found at the locations specified in <code>cantopath</code>
     *  and return a CantoDomain object.  If a location is a directory, scan subdirectories
     *  recursively for Canto source files.  If the core definitions required by the system 
     *  cannot be found in the files specified in <code>cantopath</code>, the processor will
     *  attempt to load the core definitions automatically from a known source (e.g. from the
     *  same jar file that the processor was loaded from).
     */
    public canto_domain compile(String siteName, String cantopath) {
        CantoSite site = new CantoSite(siteName, this);
        site.loadPath(cantopath, "*.canto");
        return site;
    }

     /** Compile Canto source code passed in as a string and merge the result into the specified
     *  canto_domain.  If there is a fatal error in the code, the result is not merged and
     *  a Redirection is thrown.
     */
    public void compile_into(canto_domain domain, String cantotext) throws Redirection {
        ;
    }

    /** Load the site files */
    private CantoSite loadPath(String sitename, String cantoPath) throws Exception {
        CantoSite site = null;

        LOG.info(NAME_AND_VERSION);
        LOG.info("Loading site " + (sitename == null ? "(no name yet)" : sitename) + " from path " + cantoPath);
        site = (CantoSite) compile(sitename, cantoPath);
        Exception e = site.getException();
        if (e != null) {
            LOG.error("Exception loading site " + site.getName() + ": " + e);
            throw e;
        }
        return site;
    }

    public String getCantoPath() {
        return cantoPath;
    }
    
    /** Returns true if this server was successfully started and has not yet been stopped. */
    public boolean is_running() {
        return (standaloneServer != null && standaloneServer.isRunning());
    }

    String getNominalAddress() {
        String showAddress = address;
        if (showAddress == null) { 
            Object serverAddr[] = null;
            site_config sc = mainSite.getSiteConfig();
            if (sc != null) {
                serverAddr = sc.listen_to();
            }
            if (serverAddr == null || serverAddr.length == 0) {
                serverAddr = mainSite.getPropertyArray("listen_to");
            }
            if (serverAddr != null && serverAddr.length > 0) {
                for (int i = 0; i < serverAddr.length; i++) {
                    String addr = serverAddr[i].toString();
                    if (showAddress == null) {
                        showAddress = addr;
                    } else {
                        showAddress = showAddress + ", " + addr;
                    }
                }
            }
        }
        return showAddress;
    }


    public void handle(Request request, Response response, Callback callback) throws IOException {
        String contextPath = Request.getContextPath(request);
        String ru = request.getHttpURI().asString();
        
        RequestState state = (RequestState) request.getAttribute(REQUEST_STATE_ATTRIBUTE);
        if (state != null && state == RequestState.EXPIRED) {
            Response.writeError(request, response, callback, 408, "Unable to process request in a timely manner");
            return;
        }
        request.setAttribute(REQUEST_STATE_ATTRIBUTE, RequestState.STARTING);

        if (contextPath != null && ru != null && ru.startsWith(contextPath)) {
            ru = ru.substring(contextPath.length());
        }

        if (ru == null || ru.length() == 0) {
            Response.sendRedirect(request, response, callback, "index.html");
            return;
        }

        CantoSite site = mainSite; 
        if (sites != null) {
            int ix = ru.indexOf('/');
            while (ix == 0) {
                ru = ru.substring(1);
                ix = ru.indexOf('/');
            }
            if (ix < 0) {
                if (sites.containsKey(ru)) {
                    site = (CantoSite) sites.get(ru);
                }
            } else if (ix > 0) {
                String siteName = ru.substring(0, ix);
                if (sites.containsKey(siteName)) {
                    site = (CantoSite) sites.get(siteName);
                }
            }
        }
        
        continueResponse(site, contextPath, request, response, callback);
    }
        
    /**
     * @throws IOException
     */
    private void continueResponse(final CantoSite site, final String contextPath, final Request request, final Response response, final Callback callback) throws IOException {
        String pageName = site.getPageName(contextPath);
        if (site.canRespond(pageName)) {
            try {
                respond(site, pageName, request, response, callback);
            } catch (Exception e) {
                LOG.error("Exception handling request: " + e.toString());
                callback.failed(e);
            }
        } else {
            LOG.error("Cannot respond to " + contextPath);
            Response.writeError(request, response, callback, 404, contextPath + " is undefined");
            callback.succeeded();
        }
    }

    public void respond(CantoSite site, String pageName, Request request, Response response, Callback callback) throws IOException {

        Session session = request.getSession(true);
        Construction cantoRequest = createRequestArg(site, new CantoRequest(request));
        Construction cantoSession = createSessionArg(site, new CantoSession(session));
        Map<String, String> params = new HashMap<String, String>();
        Request.getParameters(request).stream().forEach(p -> params.put(p.getName(), p.getValue()));
        Construction requestParams = createParamsArg(site, params);

        // contexts are stored under a name that is not a legal name
        // in Canto, so that it won't collide with cached Canto values.
        CantoContext cantoContext = (CantoContext) session.getAttribute("@");

        int status = 500;
        try {
            OutputStream out = Response.asBufferedOutputStream(request, response);
            
            // if the CantoContext for this session is null, then it's a new
            // session; create a new context, save it in the current session
            // and call session_init. 
            //
            // Otherwise, this is an existing session.  Use the saved context
            // if it's not in use; otherwise create a new context and use that.
            
            if (cantoContext == null) {
                cantoContext = (CantoContext) site.context();
                site.getPropertyInContext("session_init", cantoContext.getContext());
                session.setAttribute("@", cantoContext);
            }

            cantoContext = new CantoContext(cantoContext);
            
            Context context = null;
            synchronized (cantoContext) {
                cantoContext.setInUse(true);
                context = cantoContext.getContext();
            }
            
            synchronized (context) {
                status = site.respond(pageName, requestParams, cantoRequest, cantoSession, context, out);
            }
            response.setStatus(status);                
            callback.succeeded();

        } catch (Exception e) {
            status = CantoServer.SERVER_ERROR;
            response.setStatus(status);
            Response.writeError(request, response, callback, status, "Server error", e);

        } catch (Redirection r) {
            status = r.getStatus();
            String location = r.getLocation();
            String message = r.getMessage();
            if (location != null) {
                if (isStatusCode(location)) {
                    status = Integer.parseInt(location);
                }
                if (status >= 400) {
                    Response.writeError(request, response, callback, status, message);

                } else {
                    if (message != null) {
                        location = location + "?message=" + message; 
                    }
                    Response.sendRedirect(request, response, callback, location);
                }
            } else {
                response.setStatus(status);
                callback.succeeded();
            }

        } finally {
            synchronized (cantoContext) {
                cantoContext.setInUse(false);
            }
        }
    }
    
    private static Construction createParamsArg(CantoSite site, Map<String, String> params) {
        Definition parent = site.getMainOwner();
        CantoNode owner = (CantoNode) parent;
        ExternalDefinition def = new ExternalDefinition("params", owner, parent, null, Definition.PUBLIC_ACCESS, Definition.IN_CONTEXT, params, null);
        return new Instantiation(def);
    }
    
    private static Construction createRequestArg(CantoSite site, CantoRequest request) {
        Definition requestDef = site.getDefinition("request");
        if (requestDef == null) {
            throw new RuntimeException("core not loaded (can't find definition for 'request')");
        }
        Definition parent = site.getMainOwner();
        CantoNode owner = (CantoNode) parent;
        ExternalDefinition def = new ExternalDefinition("request", owner, parent, requestDef.getType(), requestDef.getAccess(), requestDef.getDurability(), request, null);
        return new Instantiation(def);
    }
    
    private static Construction createSessionArg(CantoSite site, CantoSession session) {
        Definition sessionDef = site.getDefinition("session");
        if (sessionDef == null) {
            throw new RuntimeException("core not loaded (can't find definition for 'session')");
        }
        Definition parent = site.getMainOwner();
        CantoNode owner = (CantoNode) parent;
        ExternalDefinition def = new ExternalDefinition("session", owner, parent, sessionDef.getType(), sessionDef.getAccess(), sessionDef.getDurability(), session, null);
        return new Instantiation(def);
    }

    
    
    /** Returns true if the passed string is a valid parameter representation
     *  of true.
     */
    private static boolean isTrue(String param) {
        return ("true".equalsIgnoreCase(param) || "yes".equalsIgnoreCase(param) || "1".equalsIgnoreCase(param));
    }

    public void recordState(String state) {
        if (stateFileName != null) {
            try {
                PrintStream ps = new PrintStream(new FileOutputStream(stateFileName, false));
                Date now = new Date();
                ps.println(state + " " + now.toString());
                ps.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void loadSite() throws Exception {    
        // Load and compile the canto code
        mainSite = loadPath(siteName, cantoPath);
        if (mainSite == null) {
            System.err.println("Unable to load site " + siteName + "; CantoServer not started.");
            return;
        } else if (mainSite.getException() != null) {
            throw mainSite.getException(); 
        }
        mainSite.siteInit();
        siteName = mainSite.getName();

        mainSite.addExternalObject(cantoPath, "canto_path", null);
        
        sharedCore = mainSite.getCore();

        Object[] all_sites = mainSite.getPropertyArray("all_sites");
        if (all_sites != null && all_sites.length > 0) {
            for (int i = 0; i < all_sites.length; i++) {
                site_config sc = new site_config_wrapper((CantoObjectWrapper) all_sites[i]);
                String nm = sc.name();
                if (nm.equals(siteName)) {
                    continue;
                }
                String cp = sc.sitepath();
                if (cp == null || cp.length() == 0) {
                    cp = sc.cantopath();
                }
                CantoSite s = loadPath(nm, cp);
                sites.put(nm, s);
            }
        }    
        // have to relink to catch intersite references and unresolved types
        LOG.info("--- SUPERLINK PASS ---");
        link(mainSite.getParseResults());
        if (sites.size() > 0) {
            Iterator<CantoSite> it = sites.values().iterator();
            while (it.hasNext()) {
                link(it.next().getParseResults());
            }
        }

        String showAddress = getNominalAddress();
        LOG.info("             site = " + (siteName == null ? "(no name)" : siteName));
        LOG.info("             cantopath = " + cantoPath);
        LOG.info("             state file = " + (stateFileName == null ? "(none)" : stateFileName));
        LOG.info("             current directory = " + (new File(".")).getAbsolutePath());
        LOG.info("             address = " + showAddress + (port > 0 ? "" : (":" + Integer.toString(port))));
        LOG.info("             timeout = " + (asyncTimeout > 0 ? Long.toString(asyncTimeout) : "none"));
        LOG.info("             debuggingEnabled = " + debuggingEnabled);
        LOG.info("Site " + siteName + " launched at " + (new Date()).toString());
    }
    
    public static class CantoServerRunner {
        
        private CantoServer server;
        
        public CantoServerRunner(CantoServer server) {
            this.server = server;
        }
        
        public CantoServer getServer() {
            return server;
        }

        public void start() {
            Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            server.startServer();
                        } catch (Exception e) {
                            LOG.error("Exception running CantoServer: " + e.toString());
                            e.printStackTrace();
                        }
                    }
                });
            t.start();
        }            
    }

    /** Class to provide Java access to Canto site_config object. **/
    public static class site_config_wrapper implements site_config {
        CantoObjectWrapper site_config;
        
        public site_config_wrapper(CantoObjectWrapper site_config) {
            this.site_config = site_config;
        }

        /** Returns the name of the site. **/
        public String name() {
            return site_config.getChildText("name");
        }
        
        /** The directories and/or files containing the Canto source
         *  code for this site.
         **/
        public String cantopath() {
            return site_config.getChildText("cantopath");
            
        }

        /** The directories and/or files containing the Canto source
         *  code for core.
         **/
        public String corepath() {
            return site_config.getChildText("corepath");
        }
        
        /** The directories and/or files containing the Canto source
         *  code specific to this site (not including core).
         **/
        public String sitepath() {
            return site_config.getChildText("sitepath");
            
        }
        
        /** The external interfaces (address and port) that the server should
         *  respond to for this site.  If null the globally defined value is used.
         **/
        public Object[] listen_to() {
            return site_config.getChildArray("listen_to");
        };    
    }
    
    
    public CantoSite getMainSite() {
        return mainSite;
    }

    @Override
    public String base_url() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> site_paths() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String nominal_address() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public canto_server launch_server(String name, Map<String, String> params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public canto_server relaunch_server(String name, Map<String, String> params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public canto_server get_server(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String get(Context context, String requestName, Map<String, String> requestParams) throws Redirection {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String get(Context context, String requestName) throws Redirection {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String get(Context context, String requestName, Map<String, String> requestParams) throws Redirection {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String get(Context context, String requestName) throws Redirection {
        // TODO Auto-generated method stub
        return null;
    }

}
