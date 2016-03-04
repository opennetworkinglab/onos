/*
 * Copyright 2016 Open Networking Laboratory
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
 * Provides the IO services for Yangutils-maven-Plugin.
 */
public final class YangFileScanner {

    /**
     * Default constructor.
     */
    private YangFileScanner() {
    }

    /**
     * Returns the list of java files.
     *
     * @param root specified directory
     * @return list of java files
     * @throws NullPointerException when no files are there.
     * @throws IOException when files get deleted while performing the
     *             operations
     */
    public static List<String> getJavaFiles(String root) throws NullPointerException, IOException {
        return getFiles(root, ".java");
    }

    /**
     * Returns the list of YANG files.
     *
     * @param root specified directory
     * @return list of YANG files
     * @throws NullPointerException when no files are there
     * @throws IOException when files get deleted while performing the
     *             operations
     */
    public static List<String> getYangFiles(String root) throws NullPointerException, IOException {
        return getFiles(root, ".yang");
    }

    /**
     * Returns the list of required files.
     *
     * @param root specified directory
     * @param extension file extension
     * @return list of required files
     * @throws IOException when files get deleted while performing the
     *             operations
     */
    public static List<String> getFiles(String root, String extension) throws  NullPointerException, IOException {
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
                if (filelist.length == 0) {
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
        } catch (NullPointerException e) {
            throw new IOException("NullPointerException occured");
        }
    }
}
