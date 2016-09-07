/*
 * Copyright 2015-present Open Networking Laboratory
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
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of table statistics entry interface.
 */
public final class DefaultTableStatisticsEntry implements TableStatisticsEntry {

    private final DeviceId deviceId;
    private final int tableId;
    private final long activeFlowEntries;
    private final long packetsLookedupCount;
    private final long packetsMatchedCount;

    /**
     * Default table statistics constructor.
     *
     * @param deviceId device identifier
     * @param tableId table identifier
     * @param activeFlowEntries number of active flow entries in the table
     * @param packetsLookedupCount number of packets looked up in table
     * @param packetsMatchedCount number of packets that hit table
     */
    public DefaultTableStatisticsEntry(DeviceId deviceId,
                                  int  tableId,
                                  long activeFlowEntries,
                                  long packetsLookedupCount,
                                  long packetsMatchedCount) {
        this.deviceId = checkNotNull(deviceId);
        this.tableId = tableId;
        this.activeFlowEntries = activeFlowEntries;
        this.packetsLookedupCount = packetsLookedupCount;
        this.packetsMatchedCount = packetsMatchedCount;
    }

    @Override
    public String toString() {
        return "device: " + deviceId + ", " +
                "tableId: " + this.tableId + ", " +
                "activeEntries: " + this.activeFlowEntries + ", " +
                "packetsLookedUp: " + this.packetsLookedupCount + ", " +
                "packetsMatched: " + this.packetsMatchedCount;
    }

    @Override
    public int tableId() {
        return tableId;
    }

    @Override
    public long activeFlowEntries() {
        return activeFlowEntries;
    }

    @Override
    public long packetsLookedup() {
        return packetsLookedupCount;
    }

    @Override
    public long packetsMatched() {
        return packetsMatchedCount;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }
}
