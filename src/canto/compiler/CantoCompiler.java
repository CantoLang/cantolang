/* Canto Compiler and Runtime Engine
 * 
 * CantoCompiler.java
 *
 * Copyright (c) 2018-2021 by cantolang.org
 * All rights reserved.
 */

package canto.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.Date;

import canto.Version;
import canto.lang.Definition;
import canto.runtime.Context;
import canto.runtime.Log;
import canto.runtime.SiteBuilder;

/**
 * canto compiler.
 * 
 * @author Michael St. Hippolyte
 */


public class CantoCompiler {

    public static boolean verbose = true;

    private static final Log logger = Log.getLogger(CantoCompiler.class.getName());

    public static void main( String args[] ) {
        if ( args.length == 0 ) {
            showHelp();
        } else {
            CantoCompiler bc = new CantoCompiler();
            bc.compile( args );
        }
    }

    private static String durString( long time ) {
        if ( time > 500 ) {
            long sec = time / 1000;
            int hundredths = ( (int)( time - sec * 1000 ) + 5 ) / 10;
            return String.valueOf( sec ) + "." + String.valueOf( hundredths ) + " sec.";
        } else {
            return String.valueOf( time ) + " ms.";
        }
    }

    private static void showHelp() {
        System.out.println("\nUsage:\n        java cantoc [options] sourcepath[:sourcepath...] [target]" );
        System.out.println("\nwhere sourcepath is a file or directory pathname and target is the target" );
        System.out.println(  "target or language to generate.  If target is omitted, source files" );
        System.out.println(  "are compiled and any errors are reported, but no output files are generated." );
        System.out.println("\nThe following options are supported, in any combination:" );
        System.out.println("\n   -f filter   If sourcepath is a directory, load only the files that" );
        System.out.println(  "               match the filter (default: *.canto)" );
        System.out.println("\n   -l dirname  Write logging information to logfile in the dirname" );
        System.out.println(  "               (default: write to console)" );
        System.out.println("\n   -o dirname  Write output files to the dirname directory (default:" );
        System.out.println(  "               write to current directory)" );
        System.out.println("\n   -r          If sourcepath is a directory, recurse through subdirectories" );
        System.out.println("\n   -v          Verbose output" );
        System.out.println("\n   -?          Show this help screen" );
        System.out.println("\nCanto version " + canto.Version.getVersion());
    }

    /**
     * The class and its extension points
     * 
     */
    public CantoCompiler() {
    }
    
    /**
     * createOutputDirectory
     * 
     * Default convention is to use the current directory ".", unless the page
     * Definition overrides this by specifying a String constant
     * "directoryLocation"
     * 
     * @param page
     * @param instance
     * 
     * @return a File indicating the output directory, or null if there is no
     *         output associated with this page and instance.
     */
    protected String DEFAULT_OUTPUT_DIRECTORY = ".";

    File createOutDir( String outDirName ) {
        outDirName = outDirName.replace( '.', File.separatorChar );
        outDirName = outDirName.replace( '/', File.separatorChar );
        outDirName = outDirName.replace( '\\', File.separatorChar );
        File outDir = new File( outDirName );
        if ( !outDir.exists() ) {
            logger.info( "Creating output directory " + outDir.getAbsolutePath() );
            outDir.mkdirs();
        }

        if ( !outDir.isDirectory() ) {
            logger.info( "Output path " + outDir.getAbsolutePath() + " is not a directory." );
            return null;
        } else {
            return outDir;
        }
    }

    void compile( String args[] ) {

        long startTime = System.currentTimeMillis();

        String logFileName = null;
        String inFilter = "*.canto";

        String cantoPath = null;
        String pageName = null;
        boolean recursive = false;
        boolean multiThreaded = false;
        boolean autoLoadCore = true;

        System.out.println("\ncantoc compiler for Canto version " + Version.getVersion());
        System.out.println("Copyright (c) 2018-2024 by cantolang.org\n");
        for ( int i = 0; i < args.length; i++ ) {
            if ( args[ i ].charAt( 0 ) == '-' && args[ i ].length() > 1 ) {
                switch ( args[ i ].charAt( 1 ) ) {
                case 'c':
                    autoLoadCore = false;
                    break;
                case 'f':
                    if ( i < args.length - 1 ) {
                        i++;
                        inFilter = args[ i ];
                    }
                    break;
                case 'l':
                    if ( i < args.length - 1 ) {
                        i++;
                        logFileName = args[ i ];
                    }
                    break;
                case 'm':
                    multiThreaded = true;
                    break;
                case 'o':
                    if ( i < args.length - 1 ) {
                        i++;
                        DEFAULT_OUTPUT_DIRECTORY = args[ i ];
                    }
                    break;
                case 'r':
                    recursive = true;
                    break;
                case 'v':
                    break;
                case '?':
                    showHelp();
                    return;

                }

            } else if ( cantoPath == null ) {
                cantoPath = args[ i ];
            }
        }

        if ( cantoPath == null ) {
            System.out.println( "No path specified, exiting." );
            return;
        }

        SiteBuilder siteBuilder = new SiteBuilder(cantoPath, inFilter, recursive);
    }

}
