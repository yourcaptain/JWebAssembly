/*
   Copyright 2020 Volker Berlin (i-net software)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/
package de.inetsoftware.jwebassembly.module;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.inetsoftware.classparser.ClassFile;
import de.inetsoftware.classparser.WeakValueCache;

/**
 * Cache and manager for the loaded ClassFiles
 * 
 * @author Volker Berlin
 */
public class ClassFileLoader {

    private final HashMap<String, ClassFile>        replace   = new HashMap<>();

    private final WeakValueCache<String, ClassFile> weakCache = new WeakValueCache<>();

    private final ClassLoader                       loader;

    private final ClassLoader                       bootLoader;

    /**
     * Create a new instance
     * 
     * @param loader
     *            the classloader to find the *.class files
     */
    public ClassFileLoader( ClassLoader loader ) {
        this.loader = loader;
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        do {
            ClassLoader parent = cl.getParent();
            if( parent == null ) {
                bootLoader = cl;
                break;
            }
            cl = parent;
        } while( true );
    }

    /**
     * Get the ClassFile from cache or load it.
     * 
     * @param className
     *            the class name
     * @return the ClassFile or null
     * @throws IOException
     *             If any I/O error occur
     */
    @Nullable
    public ClassFile get( String className ) throws IOException {
        ClassFile classFile = replace.get( className );
        if( classFile != null ) {
            return classFile;
        }
        classFile = weakCache.get( className );
        if( classFile != null ) {
            if( "java/lang/String".equals( className ) ) {
                System.err.println( className + " from cache" );
            }
            return classFile;
        }
        InputStream stream = loader.getResourceAsStream( className + ".class" );
        if( stream != null ) {
            if( "java/lang/String".equals( className ) ) {
                System.err.println( className + " from URL " + loader.getResource( className + ".class" ) );
            }
            classFile = new ClassFile( stream );
            weakCache.put( className, classFile );
        }
        return classFile;
    }

    /**
     * Add a class file to the weak cache.
     * 
     * @param classFile
     *            the class file
     */
    public void cache( @Nonnull ClassFile classFile ) {
        String name = classFile.getThisClass().getName();
        if( bootLoader.getResource( name + ".class" ) != null ) {
            // if the same resource is exist in the JVM self then we need to hold the reference permanently
            replace.put( name, classFile );
        } else {
            weakCache.put( name, classFile );
        }
    }

    /**
     * Replace the class in the cache with the given instance.
     * 
     * @param className
     *            the name of the class to replace
     * @param classFile
     *            the replasing ClassFile
     */
    public void replace( String className, ClassFile classFile ) {
        classFile = new ClassFile( className, classFile );
        replace.put( className, classFile );
    }
}