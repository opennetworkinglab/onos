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
package org.onosproject.artemis.impl;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper class that handles BGP Update messages.
 */
public final class DataHandler {
    private static final File DATA_FILE = new File("data.json");
    private static final File HIJACKS_FILE = new File("hijack.json");

    private final AtomicReference<ArrayList<JSONObject>> data = new AtomicReference<ArrayList<JSONObject>>();

    private static DataHandler instance = new DataHandler();

    private DataHandler() {
        data.set(new ArrayList<>());
    }

    /**
     * Singleton for data handler class.
     *
     * @return instance of class
     */
    public static synchronized DataHandler getInstance() {
        if (instance == null) {
            instance = new DataHandler();
        }
        return instance;
    }

    /**
     * Atomic append a BGP update message to a list.
     *
     * @param obj BGP update message
     */
    public synchronized void appendData(JSONObject obj) {
        data.get().add(obj);
    }

    /**
     * Atomic read and clear a list of BGP updates.
     *
     * @return list of messages that received in 'threshold' period
     */
    synchronized ArrayList<JSONObject> getData() {
        ArrayList<JSONObject> tmp = (ArrayList<JSONObject>) data.get().clone();
        data.get().clear();
        return tmp;
    }

    /**
     * A serializer to write incoming BGP updates and hijack attempts to json files.
     */
    public static class Serializer {
        private static RandomAccessFile fwData, fwHijack;
        private static long lengthData, lengthHijack;

        static {
            try {
                if (DATA_FILE.exists()) {
                    fwData = new RandomAccessFile(DATA_FILE, "rw");
                } else {
                    fwData = new RandomAccessFile(DATA_FILE, "rw");
                    fwData.writeBytes("[\n]");
                }
                lengthData = fwData.length() - 1;

                if (HIJACKS_FILE.exists()) {
                    fwHijack = new RandomAccessFile(HIJACKS_FILE, "rw");
                } else {
                    fwHijack = new RandomAccessFile(HIJACKS_FILE, "rw");
                    fwHijack.writeBytes("[\n]");
                }
                lengthHijack = fwHijack.length() - 1;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Writes BGP update to json file.
         *
         * @param data BGP update
         */
        public static synchronized void writeData(Object data) {
            try {
                String entry = data.toString() + ",\n]";
                fwData.seek(lengthData);
                fwData.writeBytes(entry);
                lengthData += entry.length() - 1;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Writes detected BGP hijack to json file.
         *
         * @param data BGP update of hijack
         */
        static synchronized void writeHijack(Object data) {
            try {
                String entry = data.toString() + ",\n]";
                fwHijack.seek(lengthHijack);
                fwHijack.writeBytes(entry);
                lengthHijack += entry.length() - 1;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
