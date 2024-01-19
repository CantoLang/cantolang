/* Canto Compiler and Runtime Engine
 * 
 * Log.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.lang.System.Logger;
import java.util.ResourceBundle;

public class Log implements Logger {

    public static Log getLogger(String name) {
        return new Log();
    }
    
    private Logger logger;
    
    private Log() {
        logger = System.getLogger("canto");
    }
    
    public void debug(String message) {
        logger.log(Level.DEBUG, message);
    }
    
    public void info(String message) {
        logger.log(Level.INFO, message);
    }
    
    public void warning(String message) {
        logger.log(Level.WARNING, message);
    }
    
    public void error(String message) {
        logger.log(Level.ERROR, message);
    }
    
    
    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isLoggable(Level arg0) {
        return logger.isLoggable(arg0);
    }

    @Override
    public void log(Level arg0, ResourceBundle arg1, String arg2, Throwable arg3) {
        logger.log(arg0, arg1, arg2, arg3);
    }

    @Override
    public void log(Level arg0, ResourceBundle arg1, String arg2, Object... arg3) {
        logger.log(arg0, arg1, arg2, arg3);
    }
}
