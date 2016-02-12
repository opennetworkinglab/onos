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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

/**
 * Provides the license header for the generated files.
 */
public final class CopyrightHeader {

    private static final Logger log = getLogger(CopyrightHeader.class);
    private static final int NULL = -1;
    private static ClassLoader classLoader = CopyrightHeader.class.getClassLoader();

    /**
     * Default constructor.
     */
    private CopyrightHeader() {
    }

    /**
     * Returns Copyright file header.
     *
     * @return Copyright file header
     */
    public static String getCopyrightHeader() {
        return parseOnosHeader();
    }

    /**
     * parse Copyright to the temporary file.
     *
     * @param file generated file
     * @param stream input stream
     */
    private static String parseOnosHeader() {

        File temp = new File("temp.txt");

        try {
            InputStream stream = classLoader.getResourceAsStream("CopyrightHeader.txt");
            OutputStream out = new FileOutputStream(temp);
            int index;
            while ((index = stream.read()) != NULL) {
                out.write(index);
            }
            out.close();
            return convertToString(temp.toString());
        } catch (IOException e) {
            log.info("Failed to insert onos header in files.");
        } finally {
            temp.delete();
        }
        return "";
    }

    /**
     * Converts it to string.
     *
     * @param toAppend file to be converted.
     * @return string of file.
     * @throws IOException when fails to convert to string
     */
    private static String convertToString(String toAppend) throws IOException {
        BufferedReader bufferReader = new BufferedReader(new FileReader(toAppend));
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferReader.readLine();

            while (line != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
                line = bufferReader.readLine();
            }
            return stringBuilder.toString();
        } finally {
            bufferReader.close();
        }
    }
}
