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
import com.google.common.base.MoreObjects.ToStringHelper;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of table statistics entry interface.
 */
public final class DefaultTableStatisticsEntry implements TableStatisticsEntry {

    private final DeviceId deviceId;
    private final TableId tableId;
    private final long activeFlowEntries;
    private final long packetsLookedupCount;
    private final long packetsMatchedCount;
    private final long maxSize;
    private static final Long NOT_PRESENT = (long) -1;

    /**
     * Default table statistics constructor.
     *
     * @param deviceId device identifier
     * @param tableId table identifier
     * @param activeFlowEntries number of active flow entries in the table
     * @param packetsLookedupCount number of packets looked up in table
     * @param packetsMatchedCount number of packets that hit table
     * @param maxSize maximum size of this table
     */
    private DefaultTableStatisticsEntry(DeviceId deviceId,
                                       TableId  tableId,
                                       long activeFlowEntries,
                                       long packetsLookedupCount,
                                       long packetsMatchedCount,
                                       long maxSize) {
        this.deviceId = checkNotNull(deviceId);
        this.tableId = tableId;
        this.activeFlowEntries = activeFlowEntries;
        this.packetsLookedupCount = packetsLookedupCount;
        this.packetsMatchedCount = packetsMatchedCount;
        this.maxSize = maxSize;
    }

    @Override
    public String toString() {
        ToStringHelper toStringHelper = toStringHelper(this);
        toStringHelper
            .omitNullValues()
            .add("Device ID", deviceId)
            .add("Table ID", tableId)
            .add("Active entries", activeFlowEntries);
        if (hasPacketsLookedup()) {
            toStringHelper.add("Packets looked-up", packetsLookedupCount);
        }
        toStringHelper.add("Packets matched", packetsMatchedCount);
        if (hasMaxSize()) {
            toStringHelper.add("Max size", maxSize);
        }

        return toStringHelper.toString();
    }

    @Override
    public int tableId() {
        return tableId.type() == TableId.Type.INDEX ? ((IndexTableId) tableId).id() : tableId.hashCode();
    }

    @Override
    public TableId table() {
        //TODO: this is a temporary method, should implement tableId() like this method.
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

    @Override
    public long maxSize() {
        return maxSize;
    }

    @Override
    public boolean hasPacketsLookedup() {
        return packetsLookedupCount == NOT_PRESENT ? false : true;
    }

    @Override
    public boolean hasMaxSize() {
        return maxSize == NOT_PRESENT ? false : true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DeviceId deviceId;
        private TableId tableId;
        private Long activeFlowEntries;
        private Long packetsMatchedCount;
        private Long packetsLookedUpCount = NOT_PRESENT;
        private Long maxSize = NOT_PRESENT;

        public Builder withDeviceId(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder withTableId(TableId tableId) {
            this.tableId = tableId;
            return this;
        }

        public Builder withActiveFlowEntries(long activeFlowEntries) {
            this.activeFlowEntries = activeFlowEntries;
            return this;
        }

        public Builder withPacketsLookedUpCount(long packetsLookedUpCount) {
            this.packetsLookedUpCount = packetsLookedUpCount;
            return this;
        }

        public Builder withPacketsMatchedCount(long packetsMatchedCount) {
            this.packetsMatchedCount = packetsMatchedCount;
            return this;
        }

        public Builder withMaxSize(long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public TableStatisticsEntry build() {
            checkNotNull(deviceId, "DeviceId cannot be null");
            checkNotNull(tableId, "TableId cannot be null");
            checkNotNull(activeFlowEntries, "ActiveFlowEntries cannot be null");
            checkNotNull(packetsMatchedCount, "PacketsMatchedCount cannot be null");

            return new DefaultTableStatisticsEntry(deviceId, tableId, activeFlowEntries, packetsLookedUpCount,
                    packetsMatchedCount, maxSize);
        }
    }

}
