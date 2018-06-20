/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.segmentrouting.xconnect.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Xconnect key.
 */
public class XconnectKey {
    private DeviceId deviceId;
    private VlanId vlanId;

    /**
     * Constructs new XconnectKey with given device ID and VLAN ID.
     *
     * @param deviceId device ID
     * @param vlanId vlan ID
     */
    public XconnectKey(DeviceId deviceId, VlanId vlanId) {
        this.deviceId = deviceId;
        this.vlanId = vlanId;
    }

    /**
     * Gets device ID.
     *
     * @return device ID of the Xconnect key
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Gets VLAN ID.
     *
     * @return VLAN ID of the Xconnect key
     */
    public VlanId vlanId() {
        return vlanId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof XconnectKey)) {
            return false;
        }
        final XconnectKey other = (XconnectKey) obj;
        return Objects.equals(this.deviceId, other.deviceId) &&
                Objects.equals(this.vlanId, other.vlanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, vlanId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("deviceId", deviceId)
                .add("vlanId", vlanId)
                .toString();
    }
}
