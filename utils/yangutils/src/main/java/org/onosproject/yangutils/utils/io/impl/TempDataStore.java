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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides storage for Temp data while traversing data model tree for code
 * generation.
 */
public final class TempDataStore {

    /**
     * Data Store types.
     */
    public static enum TempDataStoreType {

        /**
         * Methods.
         */
        GETTER_METHODS,

        /**
         * Methods.
         */
        BUILDER_METHODS,

        /**
         * Methods.
         */
        BUILDER_INTERFACE_METHODS,

        /**
         * Methods.
         */
        IMPL_METHODS,

        /**
         * Attributes.
         */
        ATTRIBUTE,

        /**
         * Imports.
         */
        IMPORT
    }

    /**
     * File name string for Temp files of methods.
     */
    private static final String GETTER_METHOD_FILE_NAME = "TempGetterMethodDataStore";

    /**
     * File name string for Temp files of methods.
     */
    private static final String BUILDER_METHOD_FILE_NAME = "TempBuilderMethodDataStore";

    /**
     * File name string for Temp files of methods.
     */
    private static final String BUILDER_INTERFACE_METHOD_FILE_NAME = "TempBuilderInterfaceMethodDataStore";

    /**
     * File name string for Temp files of methods.
     */
    private static final String IMPL_METHOD_FILE_NAME = "TempImplMethodDataStore";

    /**
     * File name string for Temp files of attributes.
     */
    private static final String ATTRIBUTE_FILE_NAME = "TempAttributeDataStore";

    /**
     * File name string for Temp files of imports.
     */
    private static final String IMPORT_FILE_NAME = "TempImportDataStore";

    /**
     * File extension of Temp files.
     */
    private static final String FILE_EXTENSION = ".tmp";

    /**
     * Directory for generating Temp files.
     */
    private static final String GEN_DIR = "target/";

    /**
     * Buffer size.
     */
    private static final int BUFFER_SIZE = 8 * 1024;

    /**
     * Default constructor.
     */
    private TempDataStore() {
    }

    /**
     * Writes specific info to a Temp file.
     *
     * @param data data to be stored
     * @param type type of Temp data store
     * @param className class name
     * @throws IOException when fails to create a Temp data file
     */
    public static void setTempData(String data, TempDataStoreType type, String className) throws IOException {

        String fileName = "";
        if (type.equals(TempDataStoreType.ATTRIBUTE)) {
            fileName = ATTRIBUTE_FILE_NAME;
        } else if (type.equals(TempDataStoreType.GETTER_METHODS)) {
            fileName = GETTER_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreType.BUILDER_INTERFACE_METHODS)) {
            fileName = BUILDER_INTERFACE_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreType.BUILDER_METHODS)) {
            fileName = BUILDER_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreType.IMPL_METHODS)) {
            fileName = IMPL_METHOD_FILE_NAME;
        } else {
            fileName = IMPORT_FILE_NAME;
        }

        File dir = new File(GEN_DIR + className + File.separator);
        dir.mkdirs();
        try {
            OutputStream file = new FileOutputStream(GEN_DIR + className + File.separator + fileName + FILE_EXTENSION);
            OutputStream buffer = new BufferedOutputStream(file, BUFFER_SIZE);

            ObjectOutput output = new ObjectOutputStream(buffer);
            try {
                output.writeObject(data);
            } finally {
                output.close();
            }
        } catch (IOException ex) {
            throw new IOException("failed to serialize data");
        }
    }

    /**
     * Get the Temp data.
     *
     * @param type type of Temp data store
     * @param className name of the class
     * @return list of attribute info
     * @throws IOException when fails to read from the file
     * @throws ClassNotFoundException when class is missing
     * @throws FileNotFoundException when file is missing
     */
    public static List<String> getTempData(TempDataStoreType type, String className)
            throws IOException, FileNotFoundException, ClassNotFoundException {

        String fileName = "";
        if (type.equals(TempDataStoreType.ATTRIBUTE)) {
            fileName = ATTRIBUTE_FILE_NAME;
        } else if (type.equals(TempDataStoreType.GETTER_METHODS)) {
            fileName = GETTER_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreType.BUILDER_INTERFACE_METHODS)) {
            fileName = BUILDER_INTERFACE_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreType.BUILDER_METHODS)) {
            fileName = BUILDER_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreType.IMPL_METHODS)) {
            fileName = IMPL_METHOD_FILE_NAME;
        } else {
            fileName = IMPORT_FILE_NAME;
        }
        try {
            InputStream file = new FileInputStream(GEN_DIR + className + File.separator + fileName + FILE_EXTENSION);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            try {
                String data = (String) input.readObject();
                List<String> recoveredData = new ArrayList<>();
                recoveredData.add(data);
                return recoveredData;
            } finally {
                input.close();
                file.close();
            }
        } catch (FileNotFoundException ex) {
            throw new FileNotFoundException("No such file or directory.");
        } catch (ClassNotFoundException ex) {
            throw new ClassNotFoundException("failed to fetch the Temp data file.");
        }
    }
}
