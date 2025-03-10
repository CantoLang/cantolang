/* Canto Compiler and Runtime Engine
 * 
 * CoreSource.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package cantocore;


/**
 * This is a static convenience class for autoloading core source.
 *
 */
public final class CoreSource {
    public static String[] corePaths = { "core.canto", "core_ui.canto", "core_js.canto", "core_platform_java.canto", "core_sandbox.canto" };

    public static String[] getCorePaths() {
        return corePaths;
    }

}
