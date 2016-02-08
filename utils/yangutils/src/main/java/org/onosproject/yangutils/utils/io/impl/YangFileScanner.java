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
     * Returns the list of yang files.
     *
     * @param root specified directory
     * @return list of yang files.
     * @throws IOException when files get deleted while performing the operations.
     */
    public static List<String> getYangFiles(String root) throws IOException {

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
                if (filelist == null) {
                    continue;
                }
                for (File current : filelist) {
                    if (current.isDirectory()) {
                        stack.push(current.toString());
                    } else {
                        String yangFile = current.getCanonicalPath();
                        if (yangFile.endsWith(".yang")) {
                            store.add(yangFile);
                        }
                    }
                }
            }
            return store;
        } catch (IOException e) {
            throw new IOException("IOException occured");
        }
    }
}
