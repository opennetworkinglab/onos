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

package org.onosproject.odtn.utils.tapi;

import java.util.UUID;
import org.onosproject.net.DeviceId;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Objects.equal;
import static java.util.Objects.hash;

public class TapiNodeRef {

    private final UUID topologyId;
    private final UUID nodeId;
    private DeviceId deviceId;

    TapiNodeRef(String topologyId, String nodeId) {
        this.topologyId = UUID.fromString(topologyId);
        this.nodeId = UUID.fromString(nodeId);
    }

    public static TapiNodeRef create(String topologyId, String nodeId) {
        return new TapiNodeRef(topologyId, nodeId);
    }

    public String getNodeId() {
        return nodeId.toString();
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public TapiNodeRef setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public String toString() {
        return toStringHelper(getClass())
//                .add("topologyId", topologyId)
                .add("nodeId", nodeId)
                .add("deviceId", deviceId)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TapiNodeRef)) {
            return false;
        }
        TapiNodeRef nodeRef = (TapiNodeRef) o;
        return equal(topologyId, nodeRef.topologyId) &&
                equal(nodeId, nodeRef.nodeId);
    }

    @Override
    public int hashCode() {
        return hash(topologyId, nodeId);
    }

}
