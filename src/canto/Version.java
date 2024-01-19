/* Canto Compiler and Runtime Engine
 * 
 * Version.java
 *
 * Copyright (c) 2019-2024 by cantolang.org
 * All rights reserved.
 */

package canto;

/**
 * Canto version.
 *
 * @author Michael St. Hippolyte
 */

public class Version {
    public static final String MAJOR_VERSION = "1.1";
    public static final String MINOR_VERSION = "0";
    public static final String VERSION = MAJOR_VERSION + "." + MINOR_VERSION;

    public static String getVersion() {
        return VERSION;
    }
}
