/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.flow;

import org.onosproject.net.DeviceId;

/**
 * Interface for flow table statistics of a device.
 */
public interface TableStatisticsEntry {

    /**
     * Returns the device Id.
     *
     * @return device id
     */
    DeviceId  deviceId();

    /**
     * Returns the integer table id.
     *
     * @return integer table id
     */
    @Deprecated
    int  tableId();

    /**
     * Returns the table id.
     *
     * @return table id
     */
    TableId table();

    /**
     * Returns the number of active flow entries in this table.
     *
     * @return the number of active flow entries
     */
    long activeFlowEntries();

    /**
     * Returns the number of packets looked up in the table.
     *
     * @return the number of packets looked up in the table
     */
    long packetsLookedup();

    /**
     * Returns the number of packets that successfully matched in the table.
     *
     * @return the number of packets that successfully matched in the table
     */
    long packetsMatched();

    /**
     * Returns the maximum size of this table.
     *
     * @return the maximum size of this table
     */
    long maxSize();

    /**
     * To check whether packetLookedUp is present in this TableStatisticsEntry.
     *
     * @return true if packetLookedUp is present, otherwise false;
     */
    boolean hasPacketsLookedup();

    /**
     * To check whether maxSize is present in this TableStatisticsEntry.
     *
     * @return true if maxSize is present, otherwise false;
     */
    boolean hasMaxSize();

}