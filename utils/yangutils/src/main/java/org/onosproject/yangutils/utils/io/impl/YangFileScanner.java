/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.yangutils.utils.io.impl;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Represents utility for searching the files in a directory.
 */
public final class YangFileScanner {

    private static final String JAVA_FILE_EXTENTION = ".java";
    private static final String YANG_FILE_EXTENTION = ".yang";

    /**
     * Creates an instance of YANG file scanner.
     */
    private YangFileScanner() {
    }

    /**
     * Returns the list of java files.
     *
     * @param root specified directory
     * @return list of java files
     * @throws NullPointerException when no files are there.
     * @throws IOException          when files get deleted while performing the
     *                              operations
     */
    public static List<String> getJavaFiles(String root) throws IOException {

        return getFiles(root, JAVA_FILE_EXTENTION);
    }

    /**
     * Returns the list of YANG file.
     *
     * @param root specified directory
     * @return list of YANG file information
     * @throws NullPointerException when no files are there
     * @throws IOException          when files get deleted while performing the
     *                              operations
     */
    public static List<String> getYangFiles(String root) throws IOException {

        return getFiles(root, YANG_FILE_EXTENTION);
    }

    /**
     * Returns the list of required files.
     *
     * @param root      specified directory
     * @param extension file extension
     * @return list of required files
     * @throws NullPointerException when no file is there
     * @throws IOException          when files get deleted while performing the operations
     */
    public static List<String> getFiles(String root, String extension) throws IOException {

        List<String> store = new LinkedList<>();
        Stack<String> stack = new Stack<>();
        stack.push(root);
        File file;
        File[] filelist;
        try {
            while (!stack.empty()) {
                root = stack.pop();
                file = new File(root);
                filelist = file.listFiles();
                if ((filelist == null) || (filelist.length == 0)) {
                    continue;
                }
                for (File current : filelist) {
                    if (current.isDirectory()) {
                        stack.push(current.toString());
                    } else {
                        String yangFile = current.getCanonicalPath();
                        if (yangFile.endsWith(extension)) {
                            store.add(yangFile);
                        }
                    }
                }
            }
            return store;
        } catch (IOException e) {
            throw new IOException("No File found of " + extension + " extension in " + root + " directory.");
        }
    }
}
