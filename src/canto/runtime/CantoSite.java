/* Canto Compiler and Runtime Engine
 * 
 * CantoSite.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.*;
import java.util.*;

import canto.lang.*;
import canto.util.SingleItemList;

/**
 */
public class CantoSite extends CantoDomain {

    private static final Log LOG = Log.getLogger(CantoSite.class);

    private static boolean LOG_MEMORY = false;
    private static String currentSiteName = "(unknown)";
    private static PrintStream mps = null;
    private static void mlog(String string) {
        if (LOG_MEMORY) {
            if (mps == null) {
                try {
                    mps = new PrintStream(new FileOutputStream("canto_mem.log", true));
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    mps = System.err;
                }
                Date now = new Date();
                mps.println("\n=========== begin logging for site " + currentSiteName + " on " + now.toString() + " ============");
            }
            mps.println(string);
        }
    }

    private long startingConsumedMemory;
    private long loadedConsumedMemory;

    private String siteName;
    private String cantoPath = null;

    private Map<String, Integer> pageTracker;
    private Map<String, Integer> fileTracker;
    private Map<String, Integer> redirectTracker;
    private long loadTime;
    private boolean hasGeneralResponse = false;
    private Set<String> ignoreExtensions = null;
    private Set<String> handleAsObjectExtensions = null;

    /** Constructs a new CantoSite object, which can load and compile Canto source code
     *  defining a group of related site objects and respond to queries.
     *
     *  The name parameter specifies the main site, which is the site queried for names
     *  that do not explicitly specify a site.  The sitename may be null or an empty string,
     *  in which case the name of the site is obtained from the main_site Canto object,
     *  loaded from the default site.
     */
    public CantoSite(String name, canto_server server) {
        super(name, server);

        siteName = name;
        currentSiteName = siteName;
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        startingConsumedMemory = runtime.totalMemory() - runtime.freeMemory();
    }

    
    public String toString() {
        String str = "Canto Site " + (siteName != null ? siteName : "(no name)") + '\n';
        if (getSite() != null) {
            str += getSite().toString();
        } else {
            str += "(empty)";
        }
        str += '\n';
        return str;
    }
    

    public boolean loadPath(String cantoPath, String inFilter) {
        this.cantoPath = cantoPath;
        
        clearStats();
        if (load(cantoPath, inFilter)) {
            loadTime = System.currentTimeMillis();
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            loadedConsumedMemory = runtime.totalMemory() - runtime.freeMemory();
            LOG.info("Loading site consumed " + (loadedConsumedMemory - startingConsumedMemory) + " bytes.");
            return true;
        } else {
            return false;
        }
    }


    void globalInit() {
        super.globalInit();
    }
    
    void siteInit() {
        hasGeneralResponse = isDefined("general_response");
        List<? extends Object> exts = getPropertyList("ignore_extensions");
        if (exts != null && exts.size() > 0) {
            ignoreExtensions = new TreeSet<String>();
            Iterator<? extends Object> it = exts.iterator();
            while (it.hasNext()) {
                ignoreExtensions.add(it.next().toString());
            }
            
        }
        exts = getPropertyList("always_handle_extensions");
        if (exts != null && exts.size() > 0) {
            handleAsObjectExtensions = new TreeSet<String>();
            Iterator<? extends Object> it = exts.iterator();
            while (it.hasNext()) {
                handleAsObjectExtensions.add(it.next().toString());
            }
        }
    }
    
    public boolean isDebuggingEnabled() {
        return debuggingEnabled;
    }

    public void enableDebugging(boolean enabled) {
        debuggingEnabled = enabled;
    }

    public void clearStats() {
        pageTracker = new TreeMap<String, Integer>();
        fileTracker = new TreeMap<String, Integer>();
        redirectTracker = new TreeMap<String, Integer>();
    }

    public String getPageName(String requestName) {

        String pageName = requestName;
        if (pageName.length() > 0) {
            // strip off leading separator
            char c = pageName.charAt(0);
            if (c == '/' || c == '\\') {
                pageName = pageName.substring(1);
            }
            
            // strip off leading site name
            if (site != null && site.getName() != null) {
                String siteNamePrefix = site.getName() + ".";
                if (pageName.startsWith(siteNamePrefix)) {
                    pageName = pageName.substring(siteNamePrefix.length());
                }
            }

            // strip off html extension
            if (pageName.endsWith(".html") || pageName.endsWith(".HTML")) {
                pageName = pageName.substring(0, pageName.length() - 5);
            }

            // turn separators into dots
            pageName = pageName.replace('/', '.').replace('\\', '.');
            if (pageName.endsWith(".")) {
                pageName = pageName + "index";
            }
        }

        // if that leaves us with nothing, default to "index"
        if (pageName.length() == 0) {
            pageName = "index";
        }

        return pageName;
    }


    public Instantiation getPageInstance(String pageName, ArgumentList[] argLists, Context argContext) {
        return getInstance("response", pageName, argLists, argContext);
    }

    public Instantiation getGeneralResponseInstance(ArgumentList[] argLists, Context argContext) {
        return getInstance(null, "general_response", argLists, argContext);
    }


    public ArgumentList[] getGeneralResponseArgumentLists(String name, Construction requestParams, Construction cantoRequest, Construction cantoSession) {
        ArgumentList[] args = new ArgumentList[4];

        StaticText nameArg = new StaticText(name);

        List<Construction> nameRequestAndSession = new ArrayList<Construction>(3);
        nameRequestAndSession.add(nameArg);
        nameRequestAndSession.add(cantoRequest);
        nameRequestAndSession.add(cantoSession);
        args[0] = new ArgumentList(nameRequestAndSession);

        List<Construction> nameAndRequest = new ArrayList<Construction>(2);
        nameAndRequest.add(nameArg);
        nameAndRequest.add(cantoRequest);
        args[1] = new ArgumentList(nameAndRequest);

        List<Construction> nameAndParams = new ArrayList<Construction>(2);
        nameAndParams.add(nameArg);
        nameAndParams.add(requestParams);
        args[2] = new ArgumentList(nameAndParams);

        List<Construction> nameOnly = new SingleItemList<Construction>(nameArg);
        args[3] = new ArgumentList(nameOnly);

        return args;
    }

    public ArgumentList[] getArgumentLists(Construction requestParams, Construction cantoRequest, Construction cantoSession) {
        ArgumentList[] args = new ArgumentList[4];

        List<Construction> requestAndSession = new ArrayList<Construction>(2);
        requestAndSession.add(cantoRequest);
        requestAndSession.add(cantoSession);
        args[0] = new ArgumentList(requestAndSession);

        List<Construction> paramsOnly = new SingleItemList<Construction>(requestParams);
        args[1] = new ArgumentList(paramsOnly);
        
        List<Construction> requestOnly = new SingleItemList<Construction>(cantoRequest);
        args[2] = new ArgumentList(requestOnly);

        args[3] = null;

        return args;
    }


    public boolean canRespond(String pageName) {
        if (getDefinition(pageName) != null) {
            return true;

        } else if (pageName == null || pageName.charAt(0) == '$') {
            return true;

        // there might be a site prefix before the item request
        } else if (pageName.indexOf(".$") > -1) {
            return true;

        } else if (hasGeneralResponse && !ignoreRequest(pageName)) {
            return true;
        }
        
        return false;
    }

    public boolean ignoreRequest(String name) {
        if (ignoreExtensions != null && name.lastIndexOf('.') > -1) {
            String ext = name.substring(name.lastIndexOf('.') + 1);
            return ignoreExtensions.contains(ext);
        }
        
        return false;
    }
    
    public boolean handleAsObject(String name) {
        if (handleAsObjectExtensions != null && name.lastIndexOf('.') > -1) {
            String ext = name.substring(name.lastIndexOf('.') + 1);
            return handleAsObjectExtensions.contains(ext);
        }
        
        return false;
    }

    /** Remove or switch characters that are illegal in a Canto name. */
    private String cleanForCanto(String name) {
        StringBuffer sb = new StringBuffer(name);
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c <= ' ' || c == '-') {
                sb.setCharAt(i, '_');
            }
        }
        return sb.toString();
    }
    
    
    public int respond(String pageName, Construction paramsArg, Construction requestArg, Construction sessionArg, Context context, PrintStream out) throws Redirection {
        ArgumentList[] argLists = getArgumentLists(paramsArg, requestArg, sessionArg);
        Instantiation page = getPageInstance(cleanForCanto(pageName), argLists, context);
        boolean respondWithPage = true;
        
        if (page != null) {
            return respond(page, context, out);

        } else {
            if (pageName == null) {
                return CantoServer.BAD_REQUEST;
            }
            
            boolean handleAsObj = (pageName.charAt(0) == '$' || handleAsObject(pageName));

            if (pageName.equalsIgnoreCase("$stat")) {
                recordRequest("$stat", pageTracker);
                printStatus(out);

            //} else if (pageName.equalsIgnoreCase("$source")) {
            //    recordRequest("$source", pageTracker);
            //    printSource(out);

            } else if (hasGeneralResponse || handleAsObj || pageName.indexOf(".$") > -1) {
                respondWithPage = false;
                Instantiation instance = null;
                String objName = null;
                if (handleAsObj) {
                    if (pageName.charAt(0) == '$') {
                        objName = pageName.substring(1);
                    } else {
                        objName = pageName;
                    }
                    instance = getInstance("", objName, argLists, context);

                } else if (pageName.indexOf(".$") > -1) {
                    int ix = pageName.indexOf(".$");
                    objName = pageName.substring(ix + 2);
                    String ownerName = pageName.substring(0, ix + 1);
                    instance = getInstance("", ownerName + objName, argLists, context);
                    
                } else {
                    objName = pageName;
                    argLists = getGeneralResponseArgumentLists(pageName, paramsArg, requestArg, sessionArg);
                    instance = getGeneralResponseInstance(argLists, context);
                }
                if (instance == null) {
                    LOG.info("Object " + pageName + " is undefined.");
                    return CantoServer.NOT_FOUND;
                }
                // record the page name (which has the $ prefix) rather than the
                // object name in order to preserve the differentiation between
                // pages and objects.
                recordRequest(pageName, pageTracker);
                try {

                    String scopeName = null;
                    if (objName.indexOf('.') > 0) {
                        scopeName = objName.substring(0, objName.indexOf('.'));;
                    }
                    
                    Definition instanceDef = instance.getDefinition(context);
                    if (instanceDef == null || instanceDef.getAccess() != Definition.Access.PUBLIC) {
                        LOG.info("Object " + pageName + " is not public.");
                        return CantoServer.NOT_FOUND;
                    }
                    
                    Site instanceSite = instanceDef.getSite();
                    Object data = null;
                    if (!instanceSite.equals(context.peek().def) && !(instanceSite instanceof Core)) {
                        context.push(instanceSite, null, null, true);
                        Instantiation pageInstance = null;
                        if (scopeName != null) {
                            pageInstance = getInstance("page", cleanForCanto(scopeName), argLists, context);
                        }
                        if (pageInstance != null) {
                            Definition pageDef = pageInstance.getDefinition(context);
                            ArgumentList pageArgs = pageInstance.getArguments();
                            ParameterList pageParams = pageDef.getParamsForArgs(pageArgs, context);
                            context.push(pageDef, pageParams, pageArgs, true);
                            data = instance.getData(context);
                            context.pop();
                        } else {
                            Definition responseDef = instance.getDefinition(context);
                            data = instance.instantiate(context, responseDef);
                        }
                        context.pop();
                    } else {
                        Instantiation pageInstance = null;
                        if (scopeName != null) {
                            pageInstance = getPageInstance(cleanForCanto(scopeName), argLists, context);
                        }
                        if (pageInstance != null) {
                            Definition pageDef = pageInstance.getDefinition(context);
                            ArgumentList pageArgs = pageInstance.getArguments();
                            ParameterList pageParams = pageDef.getParamsForArgs(pageArgs, context);
                            context.push(pageDef, pageParams, pageArgs, true);
                            data = instance.getData(context);
                            context.pop();
                        } else {
                            data = instance.instantiate(context, instanceDef);
                        }
                    }
                    if (data == null) {
                        Type type = instance.getType(context, false);
                        LOG.info(type.getName() + " " + (pageName.charAt(0) == '$' ? pageName.substring(1) : pageName) + " is empty.");
                        return CantoServer.NO_CONTENT;
                    }
                    String str = getStringForData(data);
                    out.println(str);
                    currentSiteName = siteName; //for logging
                    LOG.info("----------------- requested object: " + pageName + " ------------------");
                    LOG.info("Created " + Context.getNumContextsCreated() + " Contexts (" + Context.getNumClonedContexts() + " of them cloned) and " + Context.getNumEntriesCreated() + " entries (" + Context.getNumEntriesCloned() + " of them cloned).");
                    LOG.info("Created " + Context.getNumHashMapsCreated() + " HashMaps.");
                    LOG.info("Created " + Context.getNumArrayListsCreated() + " ArrayLists, " + Context.getTotalListSize() + " total initial allocation.");
                    long consumedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - loadedConsumedMemory;
                    LOG.info(consumedMemory + " additional bytes of memory consumed since site was loaded.");

                } catch (Redirection r) {
                    String location = r.getLocation();
                    if (location == null || location.length() < 1 || location.equals(Redirection.STANDARD_ERROR)) {
                        if (respondWithPage) {
                            location = Redirection.STANDARD_ERROR_PAGE;
                        } else {
                            location = Redirection.STANDARD_ERROR_DIV;
                        }
                        r.setLocation(location);
                    }
                    recordRequest(location, redirectTracker);
                    throw r;
                }
            } else {
                return CantoServer.NOT_FOUND;
            }
        }
        return CantoServer.OK;
    }

    public int respond(Instantiation page, Context context, PrintStream out) throws Redirection {
        
        String pageName = page.getName();
        recordRequest(pageName, pageTracker);
        try {
            Object pageData = null;
            Definition pageDef = page.getDefinition(context);
            if (pageDef == null) {
                LOG.info("Page " + pageName + " is undefined.");
                return CantoServer.NOT_FOUND;
            } else if (pageDef.getAccess() != Definition.Access.PUBLIC) {
                LOG.info("Page " + pageName + " is not public.");
                return CantoServer.NOT_FOUND;
            }
            Site pageSite = pageDef.getSite();
            if (pageSite != null && !pageSite.equals(context.peek().def) && !(pageSite instanceof Core)) {
                context.push(pageSite, null, null, true);
                pageData = pageDef.instantiate(page.getArguments(), null, context);
                context.pop();
            } else {
                pageData = page.instantiate(context, pageDef);
            }
            if (pageData == null) {
                LOG.info("Page " + pageName + " is empty.");
                return CantoServer.NO_CONTENT;
            }
            String str = getStringForData(pageData);

            // for server-to-server communications, we want to send the response exactly as
            // it has been constructed, so we make sure not to add a newline
            if (pageDef != null && pageDef.isSuperType("server_response")) {
                out.print(str);
            } else {
                out.println(str);
            }
            currentSiteName = siteName; //for logging
            LOG.info("----------------- requested page: " + pageName + " ------------------");
            LOG.info("Created " + Context.getNumContextsCreated() + " Contexts (" + Context.getNumClonedContexts() + " of them cloned) and " + Context.getNumEntriesCreated() + " entries (" + Context.getNumEntriesCloned() + " of them cloned).");
            LOG.info("Created " + Context.getNumHashMapsCreated() + " HashMaps.");
            LOG.info("Created " + Context.getNumArrayListsCreated() + " ArrayLists, " + Context.getTotalListSize() + " total initial allocation.");
            long consumedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - loadedConsumedMemory;
            LOG.info(consumedMemory + " bytes of memory consumed since site was loaded.");

        } catch (Redirection r) {
            String location = r.getLocation();
            if (location == null || location.length() < 1 || location.equals(Redirection.STANDARD_ERROR)) {
                location = Redirection.STANDARD_ERROR_PAGE;
                r.setLocation(location);
            }
            recordRequest(location, redirectTracker);
            throw r;
        }
        return CantoServer.OK;
    }

    public boolean respondWithFile(File file, String mimeType, OutputStream out) {
        // Just record the request and return false, indicating that the caller should
        // handle the file transfer using an appropriate default mechanism
        recordRequest(file.getName(), fileTracker);
        return false;
    }
    
    public void run(String request, Writer out) throws Redirection, IOException {
        
        try {
            Object runData = get(request);
            if (runData == null) {
                LOG.info("CantoRequest " + request + " returns null.");
                return;
            }
            String str = getStringForData(runData);
            out.write(str);

        } catch (Redirection r) {
            String location = r.getLocation();
            String message = r.getMessage();
            if (location != null && location.length() > 0) {
                out.write("Redirecting to " + location);
                if (message != null && message.length() > 0) {
                    out.write(": ");
                }
            }
            if (message != null && message.length() > 0) {
                out.write(message);
            }
            throw r;
        } 
    }


    private static String getStringForData(Object data) throws Redirection {
        if (isCollection(data)) {
            return getStringForCollection(data);
        } else if (data instanceof CantoObjectWrapper) {
            return ((CantoObjectWrapper) data).getText();
        } else {
            return data.toString();
        }
        
    }
    
    private static boolean isCollection(Object data) {
        return (data instanceof Map<?,?> || data instanceof List<?> || data.getClass().isArray());
    }

    private static String getStringForCollection(Object data) {
        
        List<?> list = null;
        Map<?,?> map = null;
        
        if (data instanceof List<?>) {
            list = (List<?>) data;
        } else if (data.getClass().isArray()) {
            list = Arrays.asList((Object[]) data);
        } else if (data instanceof Map<?,?>) {
            map = (Map<?,?>) data;
        }
         
        if (list != null) {    
            StringBuffer sb = new StringBuffer();
            sb.append("[ ");
            int len = list.size();
            if (len > 0) {
                Iterator<?> it = list.iterator();
                int n = 0;
                while (it.hasNext()) {
                    if (n++ > 0) {
                        sb.append(", ");
                    }
                    Object element = it.next();
                    if (element instanceof CantoObjectWrapper) {
                        continue;
                    }
                    
                    if (element instanceof Value) {
                        element = ((Value) element).getData();
                    }
                    if (element != null) {
                        if (isCollection(element)) {
                            sb.append(getStringForCollection(element));
                        } else if (element instanceof Number) {
                            sb.append(element.toString());
                        } else {
                            sb.append('"');
                            String str = element.toString();
                            sb.append(str.replace("\"", "\\\""));
                            sb.append('"');
                        }
                    }
                }
            }
            sb.append(" ]");
            return sb.toString();
    
        } else if (map != null) {
            StringBuffer sb = new StringBuffer();
            sb.append("{ ");
            Object[] keys = map.keySet().toArray();
            if (keys.length > 0) {
                Arrays.sort(keys);
                for (int i = 0; i < keys.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    String key = keys[i].toString();
                    sb.append('"');
                    sb.append(key);
                    sb.append('"');
                    sb.append(": ");
                    Object element = map.get(key);
                    if (element == null) {
                        sb.append("null");
                    } else {
                        if (element instanceof Value) {
                            element = ((Value) element).getData();
                        }
                        if (isCollection(element)) {
                            sb.append(getStringForCollection(element));
                        } else if (element instanceof Number) {
                            sb.append(element.toString());
                        } else {
                            sb.append('"');
                            String str = element.toString();
                            sb.append(str.replace("\"", "\\\""));
                            sb.append('"');
                        }
                    }
                }
            }
            sb.append(" }");
            return sb.toString();
           
        } else {
            return "";
        }

    }
    
    
    
    private static final Integer ONE = Integer.valueOf(1);
    private void recordRequest(String name, Map<String, Integer> tracker) {
        LOG.debug("------------------------------------------------------------\nRequesting: " + name);
        Integer hitCount = (Integer) tracker.get(name);
        if (hitCount != null) {
            int n = hitCount.intValue();
            tracker.put(name, Integer.valueOf(n + 1));
        } else {
            tracker.put(name, ONE);
        }
    }

    private void printStatus(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("<html>");
        writer.println("<head><title>Site " + siteName + "</title></head>");
        writer.println("<body bgcolor=\"#ccbb99\">");
        writer.println("<h2><font color=\"#993300\">STATUS</font></h2>");
        writer.println("<hr>");
        writer.println("<h3>Site: " + siteName + "</h3>");
        writer.println("<p>Loaded " + (new Date(loadTime)).toString() + ".</p>");
        writer.println("<h3>Resource Usage</h3>");
        long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        writer.println("<p>" + memory + " bytes of memory in use.<br>");
        writer.println("Created " + Context.getNumContextsCreated() + " Contexts (" + Context.getNumClonedContexts() + " of them cloned) and " + Context.getNumEntriesCreated() + " entries (" + Context.getNumEntriesCloned() + " of them cloned).<br>");
        writer.println("Created " + Context.getNumHashMapsCreated() + " HashMaps.<br>");
        writer.println("Created " + Context.getNumArrayListsCreated() + " ArrayLists, " + Context.getTotalListSize() + " total initial allocation.</p>");

        writer.println("<h3>Input</h3>");
        writer.println("<p>cantopath: " + cantoPath + "</p>");

        int numExceptions = 0;
        int numFiles = 0;
        for (int i = 0; i < sources.length; i++) {
            if (exceptions[i] != null) {
                numExceptions++;
            }
            if (sources[i] instanceof File) {
                numFiles++;
            }

        }
        int numURLs = sources.length - numFiles;
        writer.println("<p>Loaded Canto source from " + numFiles + " file" + (numFiles == 1 ? "" : "s") + ", " + numURLs + " URL" + (numURLs == 1 ? "" : "s") + ".</p>");
        if (numExceptions == 0) {
            writer.println("<p>No exceptions reported.</p>");
        } else {
            writer.println("<p>" + numExceptions + " exceptions encountered:</p><ol>");
            for (int i = 0; i < exceptions.length; i++) {
                if (exceptions[i] != null) {
                   Object source = sources[i];
                   String name = (source instanceof File ? ((File) source).getAbsolutePath() : source.toString());
                   writer.println("<li>Exception processing " + name + ": " + exceptions[i].toString() + "</li><p>");
                }
            }
            writer.println("</ol>");
        }

        writer.println("<h3>Output</h3>");

        writer.println("<p><table border=\"1\" cellpadding=\"8\"><tr><th align=\"left\">Page</th><th align=\"left\">Requests</th></tr>");
        printTrackerRows(writer, pageTracker);
        writer.println("</table></p>");
        writer.println("<p><table border=\"1\" cellpadding=\"8\"><tr><th align=\"left\">File</th><th align=\"left\">Requests</th></tr>");
        printTrackerRows(writer, fileTracker);
        writer.println("</table></p>");
        writer.println("<p><table border=\"1\" cellpadding=\"8\"><tr><th align=\"left\">Redirected to</th><th align=\"left\">Redirections</th></tr>");
        printTrackerRows(writer, redirectTracker);
        writer.println("</table></p>");

        writer.println("<h3>Sites</h3>");
        Iterator<Site> sites = core.getSites();
        int totalDefs = 0;
        if (sites.hasNext()) {
            while (sites.hasNext()) {
                Site site = sites.next();
                int numDefs = site.getNumDefinitions();
                totalDefs += numDefs;
                CantoNode contents = site.getContents();
                int numTLDs = contents.getNumChildren();
                writer.println("<p>site " + site.getName() + ": " + numDefs + " definition" + (numDefs == 1 ? "" : "s") + " (top level: " + numTLDs + ")</p>");
//                out.println("<blockquote>");
//                for (int i = 0; i < numTLDs; i++) {
//                    CantoNode node = contents.getChild(i);
//                    if (node instanceof Definition) {
//                        out.println("<p>TLD " + i + ": " + ((Definition) node).getFullName() + "</p>");
//                    } else {
//                        out.println("<p>child " + i + " (not a definition): " + node.getClass().getName() + "</p>");
//                    }
//                }
//                out.println("</blockquote>");
            }
        } else {
            writer.println("<p>No sites.</p>");
        }

        writer.println("<h3>Objects</h3>");
        writer.println("<p>" + totalDefs + " definition" + (totalDefs == 1 ? "" : "s") + "</p>");
        writer.println("<hr><p><i>" + CantoServer.NAME_AND_VERSION + "</i></p></body></html>");
    }

    private void printTrackerRows(PrintWriter out, Map<String, Integer> tracker) {
        Set<Map.Entry<String, Integer>> entries = tracker.entrySet();
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();
            String key = (String) entry.getKey();
            int hits = ((Integer) entry.getValue()).intValue();
            out.println("<tr><td>" + key + "</td><td>" + hits + "</td></tr>");
        }
    }

}

