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

package org.onosproject.bmv2.api.runtime;

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.ImmutableByteSequence;

import java.util.Collection;
import java.util.List;

/**
 * RPC client to control a BMv2 device.
 */
public interface Bmv2Client {
    /**
     * Adds a new table entry.
     *
     * @param entry a table entry value
     * @return table-specific entry ID
     * @throws Bmv2RuntimeException if any error occurs
     */
    long addTableEntry(Bmv2TableEntry entry) throws Bmv2RuntimeException;

    /**
     * Modifies a currently installed entry by updating its action.
     *
     * @param tableName string value of table name
     * @param entryId   long value of entry ID
     * @param action    an action value
     * @throws Bmv2RuntimeException if any error occurs
     */
    void modifyTableEntry(String tableName,
                          long entryId, Bmv2Action action)
            throws Bmv2RuntimeException;

    /**
     * Deletes currently installed entry.
     *
     * @param tableName string value of table name
     * @param entryId   long value of entry ID
     * @throws Bmv2RuntimeException if any error occurs
     */
    void deleteTableEntry(String tableName,
                          long entryId) throws Bmv2RuntimeException;

    /**
     * Sets table default action.
     *
     * @param tableName string value of table name
     * @param action    an action value
     * @throws Bmv2RuntimeException if any error occurs
     */
    void setTableDefaultAction(String tableName, Bmv2Action action)
            throws Bmv2RuntimeException;

    /**
     * Returns information of the ports currently configured in the switch.
     *
     * @return collection of port information
     * @throws Bmv2RuntimeException if any error occurs
     */
    Collection<Bmv2PortInfo> getPortsInfo() throws Bmv2RuntimeException;

    /**
     * Return a string representation of a table content.
     *
     * @param tableName string value of table name
     * @return table string dump
     * @throws Bmv2RuntimeException if any error occurs
     */
    String dumpTable(String tableName) throws Bmv2RuntimeException;

    /**
     * Returns a list of ids for the entries installed in the given table.
     *
     * @param tableName string value of table name
     * @return a list of entry ids
     * @throws Bmv2RuntimeException if any error occurs
     */
    List<Long> getInstalledEntryIds(String tableName) throws Bmv2RuntimeException;

    /**
     * Removes all entries installed in the given table.
     *
     * @param tableName string value of table name
     * @return the number of entries removed
     * @throws Bmv2RuntimeException if any error occurs
     */
    int cleanupTable(String tableName) throws Bmv2RuntimeException;

    /**
     * Requests the device to transmit a given byte sequence over the given port.
     *
     * @param portNumber a port number
     * @param packet a byte sequence
     * @throws Bmv2RuntimeException
     */
    void transmitPacket(int portNumber, ImmutableByteSequence packet) throws Bmv2RuntimeException;

    /**
     * Reset the state of the switch (e.g. delete all entries, etc.).
     *
     * @throws Bmv2RuntimeException if any error occurs
     */
    void resetState() throws Bmv2RuntimeException;

    /**
     * Returns the JSON-formatted model configuration currently used to process packets.
     *
     * @return a JSON-formatted string value
     * @throws Bmv2RuntimeException if any error occurs
     */
    String dumpJsonConfig() throws Bmv2RuntimeException;

    /**
     * Returns the md5 hash of the JSON-formatted model configuration currently used to process packets.
     *
     * @return a string value
     * @throws Bmv2RuntimeException if any error occurs
     */
    String getJsonConfigMd5() throws Bmv2RuntimeException;

    /**
     * Returns the counter values for a given table and entry.
     *
     * @param tableName a table name
     * @param entryId an entry id
     * @return a pair of long values, where the left value is the number of bytes and the right the number of packets
     * @throws Bmv2RuntimeException if any error occurs
     */
    Pair<Long, Long> readTableEntryCounter(String tableName, long entryId) throws Bmv2RuntimeException;
}
