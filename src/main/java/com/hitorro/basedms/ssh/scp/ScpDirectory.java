/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.basedms.ssh.scp;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: rmonaghan Date: Nov 30, 2006 Time: 10:01:59 AM
 */
public class ScpDirectory {

    private File directory;
    private ArrayList childDirectories;
    private ArrayList files;
    private ScpDirectory parent;

    /**
     * Constructor for a Directory.
     *
     * @param directory a directory.
     */
    public ScpDirectory(File directory) {
        this(directory, null);
    }

    /**
     * Constructor for a Directory.
     *
     * @param directory a directory
     * @param parent    a parent Directory
     */
    public ScpDirectory(File directory, ScpDirectory parent) {
        this.parent = parent;
        this.childDirectories = new ArrayList();
        this.files = new ArrayList();
        this.directory = directory;
    }

    /**
     * Convert a file path to an array of path components. This uses File.sepatator to split the file path string.
     *
     * @param thePath the file path string to convertToPdf
     * @return an array of path components
     */
    public static String[] getPath(String thePath) {
        StringTokenizer tokenizer = new StringTokenizer(thePath,
                File.separator);
        String[] path = new String[tokenizer.countTokens()];

        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            path[i] = tokenizer.nextToken();
            i++;
        }

        return path;
    }

    /**
     * Add a directory to the child directories.
     *
     * @param directory a Directory
     */
    public void addDirectory(ScpDirectory directory) {
        if (!childDirectories.contains(directory)) {
            childDirectories.add(directory);
        }
    }

    /**
     * Add a file to the listFiles of files.
     *
     * @param file a file to put
     */
    public void addFile(File file) {
        files.add(file);
    }

    /**
     * Get an iterator over the child Directories.
     *
     * @return an iterator
     */
    public Iterator directoryIterator() {
        return childDirectories.iterator();
    }

    /**
     * Get an iterator over the files.
     *
     * @return an iterator
     */
    public Iterator filesIterator() {
        return files.iterator();
    }

    /**
     * Get the parent Directory.
     *
     * @return the parent Directory.
     */
    public ScpDirectory getParent() {
        return parent;
    }

    /**
     * Is this a root Directory?
     *
     * @return true if there is no parent Directory
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Get the directory file.
     *
     * @return the directory file
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Get a child directory of this directory.
     *
     * @param dir the directory to look for
     * @return the child directory, or null if not found
     */
    public ScpDirectory getChild(File dir) {
        for (int i = 0; i < childDirectories.size(); i++) {
            ScpDirectory current = (ScpDirectory) childDirectories.get(i);
            if (current.getDirectory().equals(dir)) {
                return current;
            }
        }

        return null;
    }

    /**
     * The equality method. This checks if the directory field is the same.
     *
     * @param obj the object to compare to
     * @return true if this object has an equal directory field as the other object
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ScpDirectory)) {
            return false;
        }

        ScpDirectory d = (ScpDirectory) obj;

        return this.directory.equals(d.directory);
    }

    /**
     * The hashcode method.
     *
     * @return the hash code of the directory field
     */
    public int hashCode() {
        return directory.hashCode();
    }

    /**
     * Get the path components of this directory.
     *
     * @return the path components as an array of strings.
     */
    public String[] getPath() {
        return getPath(directory.getAbsolutePath());
    }

    /**
     * Get the number of files in the files attribute.
     *
     * @return the number of files
     */
    public int fileSize() {
        return files.size();
    }
}
