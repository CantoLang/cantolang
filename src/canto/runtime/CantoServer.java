/* Canto Compiler and Runtime Engine
 * 
 * CantoServer.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.File;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import canto.lang.Core;
import canto.lang.site_config;
import canto.runtime.CantoStandaloneServer;
import canto.runtime.CantoServer.site_config_wrapper;
import canto.runtime.Log;

import org.antlr.v4.runtime.RuntimeMetaData;


/**
 * 
 */
public class CantoServer {
    private static final long serialVersionUID = 1L;

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
    private String port = null;
    private boolean initedOk = false;
    private String stateFileName = null;
    private String logFileName = null;
    private boolean appendToLog = true;
    private PrintStream log = null;
    private String cantoPath = ".";
    private boolean debuggingEnabled = false;
    private HashMap<String, Object> properties = new HashMap<String, Object>();
    protected String fileHandlerName = null;
    private String contextPath = "";
    private long asyncTimeout = 0l;
    private String baseUrl = null;

    private CantoStandaloneServer standaloneServer = null;
    private HashMap<String, CantoServer> serverMap = new HashMap<String, CantoServer>();
    private List<CantoServerRunner> pendingServerRunners = null;
    

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
            System.out.println("                               A zero or negative value means the request will");
            System.out.println("                               never time out.  The default value is zero.\n");
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

                standaloneServer = new CantoJettyServer();
                
                standaloneServer.setServer(this);
                standaloneServer.setVirtualHost(virtualHost);
                baseUrl = ((CantoJettyServer) standaloneServer).getAddress();

            } catch (Exception e) {
                recordState("FAILED");

                System.err.println("Exception starting CantoServer: " + e);
                exception = e;
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
       
    private boolean init(Map<String, String> initParams) {
        try {    
            initGlobalSettings(initParams);
        } catch (Exception e) {
            exception = e;
            return false;
        }
        return true;
    }

    protected void loadSite() throws Exception {    
        // Load and compile the canto code
        mainSite = load(siteName, cantoPath);
        if (mainSite == null) {
            System.err.println("Unable to load site " + siteName + "; CantoServer not started.");
            return;
        } else if (mainSite.getException() != null) {
            throw mainSite.getException(); 
        }
        mainSite.siteInit();
        siteName = mainSite.getName();

        mainSite.addExternalObject(contextPath, "context_path", null);
        
        if (mainSite.isDefined("file_base")) {
            String newFileBase = mainSite.getProperty("file_base", "");
            if (!fileBase.equals(newFileBase)) {
                fileBase = newFileBase;
                slog("file_base set by site to " + fileBase);
            }
        } else {
            slog("file_base not set by site.");
            mainSite.addExternalObject(fileBase, "file_base", null);
        }
        if (mainSite.isDefined("files_first")) {
            boolean newFilesFirst = mainSite.getBooleanProperty("files_first");
            if (newFilesFirst ^ filesFirst) {
                filesFirst = newFilesFirst;
                slog("files_first set by site to " + filesFirst);
            }
        } else {
            slog("files_first not set by site.");
            mainSite.addExternalObject(new Boolean(filesFirst), "files_first", "boolean");
        }
        if (mainSite.isDefined("share_core")) {
            shareCore = mainSite.getBooleanProperty("share_core");
        }

        if (shareCore) {
            sharedCore = mainSite.getCore();
        }
        Object[] all_sites = mainSite.getPropertyArray("all_sites");
        if (all_sites != null && all_sites.length > 0) {
            for (int i = 0; i < all_sites.length; i++) {
                site_config sc = new site_config_wrapper((CantoObjectWrapper) all_sites[i]);
                String nm = sc.name();
                if (nm.equals(siteName)) {
                    continue;
                }
                String cp = null;
                if (shareCore) {
                    cp = sc.sitepath();
                }
                if (cp == null || cp.length() == 0) {
                    cp = sc.cantopath();
                }
                boolean r = sc.recursive();
                CantoSite s = load(nm, cp, r, false);
                sites.put(nm, s);
            }
        }    
        // have to relink to catch intersite references and unresolved types
        CantoLogger.log("--- SUPERLINK PASS ---");
        link(mainSite.getParseResults());
        if (sites.size() > 0) {
            Iterator<CantoSite> it = sites.values().iterator();
            while (it.hasNext()) {
                link(it.next().getParseResults());
            }
        }

        String showAddress = getNominalAddress();
        slog("             site = " + (siteName == null ? "(no name)" : siteName));
        slog("             cantopath = " + cantoPath);
        slog("             recursive = " + recursive);
        slog("             state file = " + (stateFileName == null ? "(none)" : stateFileName));
        slog("             log file = " + CantoLogger.getLogFile());
        slog("             multithreaded = " + multithreaded);
        slog("             autoloadcore = " + !customCore);
        slog("             sharecore = " + shareCore);
        slog("             current directory = " + (new File(".")).getAbsolutePath());
        slog("             files_first = " + filesFirst);
        slog("             file_base = " + fileBase);
        slog("             address = " + showAddress + (port == null ? "" : (":" + port)));
        slog("             timeout = " + (asyncTimeout > 0 ? Long.toString(asyncTimeout) : "none"));
        slog("             verbosity = " + Integer.toString(CantoLogger.verbosity));
        slog("             debuggingEnabled = " + debuggingEnabled);
        slog("Site " + siteName + " launched at " + (new Date()).toString());
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
                            LOG("Exception running CantoServer: " + e.toString());
                            e.printStackTrace();
                        }
                    }
                });
            t.start();
        }            
                
    }

}
