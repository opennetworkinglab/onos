/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.cord.gui;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides support for fake data.
 */
public class FakeUtils {
    private static final ClassLoader CL = FakeUtils.class.getClassLoader();
    private static final String ROOT_PATH = "/org/onosproject/cord/gui/";
    private static final String UTF_8 = "UTF-8";

    /**
     * Returns the contents of a local file as a string.
     *
     * @param path file path name
     * @return contents of file as a string
     */
    public static String slurp(String path) {
        String result = null;
        InputStream is = CL.getResourceAsStream(ROOT_PATH + path);
        if (is != null) {
            try {
                result = IOUtils.toString(is, UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
