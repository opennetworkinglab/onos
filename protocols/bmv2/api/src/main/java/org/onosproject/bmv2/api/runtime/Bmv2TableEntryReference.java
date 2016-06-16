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

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;

/**
 * A reference to a table entry installed on a BMv2 device.
 */
@Beta
public final class Bmv2TableEntryReference {


    private final DeviceId deviceId;
    private final String tableName;
    private final Bmv2MatchKey matchKey;

    /**
     * Creates a new table entry reference.
     *
     * @param deviceId a device ID
     * @param tableName a table name
     * @param matchKey a match key
     */
    public Bmv2TableEntryReference(DeviceId deviceId, String tableName, Bmv2MatchKey matchKey) {
        this.deviceId = deviceId;
        this.tableName = tableName;
        this.matchKey = matchKey;
    }

    /**
     * Returns the device ID of this table entry reference.
     *
     * @return a device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the name of the table of this table entry reference.
     *
     * @return a table name
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns the match key of this table entry reference.
     *
     * @return a match key
     */
    public Bmv2MatchKey matchKey() {
        return matchKey;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId, tableName, matchKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2TableEntryReference other = (Bmv2TableEntryReference) obj;
        return Objects.equal(this.deviceId, other.deviceId)
                && Objects.equal(this.tableName, other.tableName)
                && Objects.equal(this.matchKey, other.matchKey);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId)
                .add("tableName", tableName)
                .add("matchKey", matchKey)
                .toString();
    }
}
