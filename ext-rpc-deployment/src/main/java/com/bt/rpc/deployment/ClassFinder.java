package com.bt.rpc.deployment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.bt.rpc.annotation.RpcService;

/**
 * <p>This class implements the capability to search over the current classpath
 * retrieving classes that implement a certain interface.</p>
 *
 * Copyright 2001 Sapient
 * @since carbon 1.0
 * @author Greg Hinkle, June 2001
 * @version $Revision: 1.10 $($Author: dvoet $ / $Date: 2003/05/05 21:21:23 $)
 */
public class ClassFinder {

    /**
     * Tracks the count of classes found that match the
     * provided criteria.
     */
    protected long foundClasses = 0;

    /**
     * The super class criteria
     */
    protected Class superClass = null;
    /**
     * The required substring path criteria for this searcher
    // */
    //protected String requiredPathSubstring = null;
    /////**
    // * The set of classes found matching the provided criteria.
    // */
    protected Set<String> classes = new HashSet(2000);


    //String path;
    /**
     * <p>Instantiates the type of MBeanHarvester that will return all classes
     * in the entire classpath.</p>
     */
    public ClassFinder() {

    }

    /**
     * <p>Instantiates the type of MBeanHarvester that will return all classes
     * that are assignable to the supplied class. This would include all
     * implementations of it, if it is an interface or it and all subclasses
     * of it if it's a class.</p>
     *
     * @param superClass the Class that should be searched for along with
     *   implementations and subclasses
     */
    public ClassFinder(Class superClass) {
        this.superClass = superClass;

    }

    public ClassFinder(String pkg) {

        var path  = pkg.replace(".", "/");

        try {
            final Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(path);
            while (urls.hasMoreElements()) {
                var file = urls.nextElement().getFile();
                int index =file.lastIndexOf(path);
                processFile(file.substring(0, index),path);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //
    //
    //static List<URL> foundUrls(String resourceName, ClassLoader... loaders) {
    //    List<URL>  result = new ArrayList<>();
    //    for (ClassLoader classLoader : loaders) {
    //        try {
    //            final Enumeration<URL> urls = classLoader.getResources(resourceName);
    //            while (urls.hasMoreElements()) {
    //                final URL url = urls.nextElement();
    //                int index = url.toExternalForm().lastIndexOf(resourceName);
    //                if (index != -1) {
    //                    // Add old url as contextUrl to support exotic url handlers
    //                    result.add(new URL(url, url.toExternalForm().substring(0, index)));
    //                } else {
    //                    result.add(url);
    //                }
    //            }
    //        } catch (IOException e) {
    //            e.printStackTrace();
    //        }
    //    }
    //    return result;
    //}

    /**
     * <p>Adds a class name to the list of found classes if and only if it meets
     * the configured requirements.</p>
     *
     * @param className the FQN String name of the class to add
     */
    protected void addClassName(String className) {

        // Only add this class if we're not checking for a particular
        // substring of the FQN or we find that substring
        //if ((this.requiredPathSubstring == null) ||
        //    (className.indexOf(this.requiredPathSubstring) >= 0)) {

            if (this.superClass == null) {
                this.classes.add(className);
            } else {
                try {

                    // TODO: GH - add a way to search other classpaths and the
                    // system classpath.
                    Class theClass =
                        Class.forName(
                            className,
                            false,
                            this.getClass().getClassLoader());

                    if (this.superClass.isAssignableFrom(theClass)) {
                        this.classes.add(className);
                    }
                } catch (ClassNotFoundException cnfe) {
                    // Used to catch mis-parsed classnames
                } catch (Throwable t) {
                    // Used to catch JVM security and linkage errors
                }
            }
        //}
    }


    /**
     * <p>Used to retrieve the results <code>Set</code> from this harvester's
     * search.</p>
     *
     * @return Set the set of classes that meet this harvester's requirements
     */
    //public Set getClasses() {
    //
    //    // 1) tokenize classpath
    //    String classpath = System.getProperty("java.class.path");
    //    String pathSeparator = System.getProperty("path.separator");
    //
    //    StringTokenizer st = new StringTokenizer(classpath,pathSeparator);
    //
    //    // 2) for each element in the classpath
    //    while (st.hasMoreTokens()) {
    //        File currentDirectory = new File(st.nextToken());
    //
    //        processFile(currentDirectory.getAbsolutePath(),"");
    //
    //    }
    //
    //    return this.classes;
    //}

    public Set<String> getClasses() {


        return classes;
    }


    /**
     * Recursively search through Directories with special checks to recognize
     * zip and jar files. (Zip and Jar files return true from
     * &lt;File&gt;.isDirectory())
     * @param base the base file path to search
     * @param current the current recursively searched file path being searched
     */
    private void processFile(String base, String current) {
        File currentDirectory = new File(base + File.separatorChar + current);

        // Handle special for archives
        if (isArchive(currentDirectory.getName())) {
            try {
                processZip(new ZipFile(currentDirectory));
            } catch (Exception e) {
                // The directory was not found so the classpath was probably in
                // error or we don't have rights to it
            }
            return;
        } else {

            Set directories = new HashSet();

            File[] children = currentDirectory.listFiles();

            // if no children, return
            if (children == null || children.length == 0) {
                return;
            }

            // check for classfiles
            for (int i = 0; i < children.length; i++) {
                File child = children[i];
                if (child.isDirectory()) {
                    directories.add(children[i]);
                } else {
                    if (child.getName().endsWith(".class")) {
                        String className =
                            getClassName(
                                current +
                                ((current == "") ? "" : File.separator) +
                                child.getName());
                        addClassName(className);
                        this.foundClasses++;
                    }
                }
            }

            //call process file on each directory.  This is an iterative call!!
            for (Iterator i = directories.iterator(); i.hasNext(); ) {
                processFile(base, current + ((current=="")?"":File.separator) +
                    ((File)i.next()).getName());
            }
        }
    }


    /**
     * <p>Looks at the name of a file to determine if it is an archive</p>
     * @param name the name of a file
     * @return true if a file in the classpath is an archive
     * such as a Jar or Zip file
     */
    protected boolean isArchive(String name) {
        if ((name.endsWith(".jar") ||
            (name.endsWith(".zip")))) {

            return true;
        } else {
            return false;
        }
    }

    /**
     * <p>Returns the Fully Qualified Class name of a class from it's path
     * @param fileName the full path to a class
     * @return the FQN of a class
     */
    protected String getClassName(String fileName) {
        String newName =  fileName.replace(File.separatorChar,'.');
        // Because zipfiles don't have platform specific seperators
        newName =  newName.replace('/','.');
        return newName.substring(0, fileName.length() - 6);
    }


    /**
     * <P>Iterates through the files in a zip looking for files that may be
     * classes. This is not recursive as zip's in zip's are not searched by the
     * classloader either.</p>
     *
     * @param file The ZipFile to be searched
     */
    protected void processZip(ZipFile file) {
        Enumeration files = file.entries();

        while (files.hasMoreElements()) {
            Object tfile = files.nextElement();
            ZipEntry child = (ZipEntry) tfile;
            if (child.getName().endsWith(".class")) {
                addClassName(getClassName(child.getName()));

                this.foundClasses++;
            }
        }
    }
}