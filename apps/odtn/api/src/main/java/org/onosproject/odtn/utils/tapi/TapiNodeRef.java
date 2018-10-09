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
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.hash;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.DEVICE_ID;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * TAPI Node reference class.
 *
 * TAPI reference class should be used in ODTN ServiceApplication
 * in order to make independent ServiceApplication implementation from DCS.
 */
public final class TapiNodeRef {

    protected final Logger log = getLogger(getClass());

    private final UUID topologyId;
    private final UUID nodeId;
    private DeviceId deviceId;

    private TapiNodeRef(String topologyId, String nodeId) {
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

    /**
     * Check if this Node matches input filter condition.
     *
     * @param key Filter key
     * @param value Filter value
     * @return If match or not
     */
    public boolean is(String key, String value) {
        checkNotNull(value);
        switch (key) {
            case DEVICE_ID:
                if (deviceId == null) {
                    return false;
                }
                return value.equals(deviceId.toString());
            default:
                log.warn("Unknown key: {}", key);
                return true;
        }
    }

    public String toString() {
        return toStringHelper(getClass())
                .add("topologyId", topologyId)
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
