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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;

/**
 * Represents the license header for the generated files.
 */
public final class CopyrightHeader {

    private static final int EOF = -1;
    private static final String COPYRIGHT_HEADER_FILE = "CopyrightHeader.txt";
    private static final String COPYRIGHTS_FIRST_LINE = "/*\n * Copyright " + Calendar.getInstance().get(Calendar.YEAR)
            + "-present Open Networking Laboratory\n";
    private static final String TEMP_FILE = "temp.txt";
    private static ClassLoader classLoader = CopyrightHeader.class.getClassLoader();

    private static String copyrightHeader;

    /**
     * Creates an instance of copyright header.
     */
    private CopyrightHeader() {
    }

    /**
     * Returns copyright file header.
     *
     * @return copyright file header
     * @throws IOException when fails to parse copyright header
     */
    public static String getCopyrightHeader() throws IOException {

        if (copyrightHeader == null) {
            parseCopyrightHeader();
        }
        return copyrightHeader;
    }

    /**
     * Sets the copyright header.
     *
     * @param header copyright header
     */
    private static void setCopyrightHeader(String header) {

        copyrightHeader = header;
    }

    /**
     * parses Copyright to the temporary file.
     *
     * @throws IOException when fails to get the copyright header
     */
    private static void parseCopyrightHeader() throws IOException {

        File temp = new File(TEMP_FILE);

        try {
            InputStream stream = classLoader.getResourceAsStream(COPYRIGHT_HEADER_FILE);
            OutputStream out = new FileOutputStream(temp);

            int index;
            out.write(COPYRIGHTS_FIRST_LINE.getBytes());
            while ((index = stream.read()) != EOF) {
                out.write(index);
            }
            out.close();
            stream.close();
            getStringFileContent(temp);
            setCopyrightHeader(getStringFileContent(temp));
        } catch (IOException e) {
            throw new IOException("failed to parse the Copyright header");
        } finally {
            temp.delete();
        }
    }

    /**
     * Converts it to string.
     *
     * @param toAppend file to be converted.
     * @return string of file.
     * @throws IOException when fails to convert to string
     */
    private static String getStringFileContent(File toAppend) throws IOException {

        FileReader fileReader = new FileReader(toAppend);
        BufferedReader bufferReader = new BufferedReader(fileReader);
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferReader.readLine();

            while (line != null) {
                stringBuilder.append(line);
                stringBuilder.append(NEW_LINE);
                line = bufferReader.readLine();
            }
            return stringBuilder.toString();
        } finally {
            fileReader.close();
            bufferReader.close();
        }
    }
}
