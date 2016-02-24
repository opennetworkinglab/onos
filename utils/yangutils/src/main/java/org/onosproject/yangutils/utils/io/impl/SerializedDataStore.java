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
 * Provides storage for serialized data while traversing data model tree for code generation.
 */
public final class SerializedDataStore {

    /**
     * Data Store types.
     */
    public static enum SerializedDataStoreType {

        /**
         * Methods.
         */
        INTERFACE_METHODS,

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
     * File name string for serialized files of methods.
     */
    private static final String INTERFACE_METHOD_FILE_NAME = "SerializedInterfaceMethodDataStore";

    /**
     * File name string for serialized files of methods.
     */
    private static final String BUILDER_METHOD_FILE_NAME = "SerializedBuilderMethodDataStore";

    /**
     * File name string for serialized files of methods.
     */
    private static final String BUILDER_INTERFACE_METHOD_FILE_NAME = "SerializedBuilderInterfaceMethodDataStore";

    /**
     * File name string for serialized files of methods.
     */
    private static final String IMPL_METHOD_FILE_NAME = "SerializedImplMethodDataStore";

    /**
     * File name string for serialized files of attributes.
     */
    private static final String ATTRIBUTE_FILE_NAME = "SerializedAttributeDataStore";

    /**
     * File name string for serialized files of imports.
     */
    private static final String IMPORT_FILE_NAME = "SerializedImportDataStore";

    /**
     * File extension of serialized files.
     */
    private static final String SERIALIZE_FILE_EXTENSION = ".ser";

    /**
     * Directory for generating Serialized files.
     */
    private static final String GEN_DIR = "target/";

    /**
     * Buffer size.
     */
    private static final int BUFFER_SIZE = 8 * 1024;

    /**
     * Default constructor.
     */
    private SerializedDataStore() {
    }

    /**
     * Writes specific info to a serialized file.
     *
     * @param data data to be stored
     * @param type type of serialized data store
     * @throws IOException when fails to create a serialized data file.
     */
    public static void setSerializeData(String data, SerializedDataStoreType type) throws IOException {

        String fileName = "";
        if (type.equals(SerializedDataStoreType.ATTRIBUTE)) {
            fileName = ATTRIBUTE_FILE_NAME;
        } else if (type.equals(SerializedDataStoreType.INTERFACE_METHODS)) {
            fileName = INTERFACE_METHOD_FILE_NAME;
        } else if (type.equals(SerializedDataStoreType.BUILDER_INTERFACE_METHODS)) {
            fileName = BUILDER_INTERFACE_METHOD_FILE_NAME;
        } else if (type.equals(SerializedDataStoreType.BUILDER_METHODS)) {
            fileName = BUILDER_METHOD_FILE_NAME;
        } else if (type.equals(SerializedDataStoreType.IMPL_METHODS)) {
            fileName = IMPL_METHOD_FILE_NAME;
        } else {
            fileName = IMPORT_FILE_NAME;
        }

        try {
            OutputStream file = new FileOutputStream(GEN_DIR + fileName + SERIALIZE_FILE_EXTENSION);
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
     * Get the serialized data.
     *
     * @param type type of serialized data store
     * @return list of attribute info.
     * @throws IOException when fails to read from the file.
     * @throws ClassNotFoundException when class is missing.
     * @throws FileNotFoundException when file is missing.
     */
    public static List<String> getSerializeData(SerializedDataStoreType type)
            throws IOException, FileNotFoundException, ClassNotFoundException {

        String fileName = "";
        if (type.equals(SerializedDataStoreType.ATTRIBUTE)) {
            fileName = ATTRIBUTE_FILE_NAME;
        } else if (type.equals(SerializedDataStoreType.INTERFACE_METHODS)) {
            fileName = INTERFACE_METHOD_FILE_NAME;
        } else if (type.equals(SerializedDataStoreType.BUILDER_INTERFACE_METHODS)) {
            fileName = BUILDER_INTERFACE_METHOD_FILE_NAME;
        } else if (type.equals(SerializedDataStoreType.BUILDER_METHODS)) {
            fileName = BUILDER_METHOD_FILE_NAME;
        } else if (type.equals(SerializedDataStoreType.IMPL_METHODS)) {
            fileName = IMPL_METHOD_FILE_NAME;
        } else {
            fileName = IMPORT_FILE_NAME;
        }
        try {
            InputStream file = new FileInputStream(GEN_DIR + fileName + SERIALIZE_FILE_EXTENSION);
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
            throw new ClassNotFoundException("failed to fetch the serialized data file.");
        }
    }
}
