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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.onosproject.net.ConnectPoint;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.hash;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.DEVICE_ID;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.ODTN_PORT_TYPE;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.CONNECTION_ID;

import org.onosproject.net.DeviceId;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * TAPI Nep reference class.
 *
 * TAPI reference class should be used in ODTN ServiceApplication
 * in order to make independent ServiceApplication implementation from DCS.
 */
public final class TapiNepRef {

    protected final Logger log = getLogger(getClass());

    private final UUID topologyId;
    private final UUID nodeId;
    private final UUID nepId;

    // Annotations to be used for reference of related TAPI objects.
    private UUID sipId = null;
    private List<UUID> cepIds = Collections.emptyList();

    // Annotations to be used for OpenConfig configuration.
    private ConnectPoint cp = null;
    private OdtnDeviceDescriptionDiscovery.OdtnPortType portType = null;
    private String connectionId = null;

    TapiNepRef(String topologyId, String nodeId, String nepId) {
        this.topologyId = UUID.fromString(topologyId);
        this.nodeId = UUID.fromString(nodeId);
        this.nepId = UUID.fromString(nepId);
    }

    public static TapiNepRef create(String topologyId, String nodeId, String nepId) {
        return new TapiNepRef(topologyId, nodeId, nepId);
    }

    public String getTopologyId() {
        return topologyId.toString();
    }

    public String getNodeId() {
        return nodeId.toString();
    }

    public String getNepId() {
        return nepId.toString();
    }

    public String getSipId() {
        return Optional.ofNullable(sipId)
                .map(UUID::toString)
                .orElse(null);
    }

    public List<String> getCepIds() {
        return cepIds.stream().map(UUID::toString).collect(Collectors.toList());
    }

    public OdtnDeviceDescriptionDiscovery.OdtnPortType getPortType() {
        return portType;
    }

    public ConnectPoint getConnectPoint() {
        return cp;
    }

    public String getConnectionId() {
        return connectionId;
    }


    public TapiNepRef setSipId(String sipId) {
        this.sipId = UUID.fromString(sipId);
        return this;
    }

    public TapiNepRef setCepIds(List<String> cepIds) {
        this.cepIds = cepIds.stream().map(UUID::fromString).collect(Collectors.toList());
        return this;
    }

    public TapiNepRef setPortType(String portType) {
        this.portType = Optional.ofNullable(portType)
                .map(OdtnDeviceDescriptionDiscovery.OdtnPortType::fromValue)
                .orElse(null);
        return this;
    }

    public TapiNepRef setConnectPoint(ConnectPoint cp) {
        this.cp = cp;
        return this;
    }

    public TapiNepRef setConnectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    /**
     * Check if this Nep matches input filter condition.
     *
     * @param key Filter key
     * @param value Filter value
     * @return If match or not
     */
    public boolean is(String key, String value) {
        checkNotNull(key);
        checkNotNull(value);
        switch (key) {
            case DEVICE_ID:
                return value.equals(
                        Optional.ofNullable(cp)
                                .map(ConnectPoint::deviceId)
                                .map(DeviceId::toString)
                                .orElse(null));
            case ODTN_PORT_TYPE:
                return value.equals(
                        Optional.ofNullable(portType)
                                .map(OdtnDeviceDescriptionDiscovery.OdtnPortType::value)
                                .orElse(null));
            case CONNECTION_ID:
                return value.equals(connectionId);
            default:
                log.warn("Unknown key: {}", key);
                return true;
        }
    }

    public String toString() {
        return toStringHelper(getClass())
                .add("topologyId", topologyId)
                .add("nodeId", nodeId)
                .add("nepId", nepId)
                .add("sipId", sipId)
                .add("connectPoint", cp)
                .add("portType", portType)
                .add("connectionId", connectionId)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TapiNepRef)) {
            return false;
        }
        TapiNepRef that = (TapiNepRef) o;
        return equal(topologyId, that.topologyId) &&
                equal(nodeId, that.nodeId) &&
                equal(nepId, that.nepId);
    }

    @Override
    public int hashCode() {
        return hash(topologyId, nodeId, nepId);
    }

}
