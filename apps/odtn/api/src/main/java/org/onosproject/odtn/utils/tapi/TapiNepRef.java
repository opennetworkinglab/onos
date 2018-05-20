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

import java.util.Optional;
import java.util.UUID;
import org.onosproject.net.ConnectPoint;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Objects.equal;
import static java.util.Objects.hash;

public class TapiNepRef {

    private final UUID topologyId;
    private final UUID nodeId;
    private final UUID nepId;
    private UUID sipId = null;
    private ConnectPoint cp = null;

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

    public ConnectPoint getConnectPoint() {
        return cp;
    }

    public TapiNepRef setSipId(String sipId) {
        this.sipId = UUID.fromString(sipId);
        return this;
    }

    public TapiNepRef setConnectPoint(ConnectPoint cp) {
        this.cp = cp;
        return this;
    }

    public String toString() {
        return toStringHelper(getClass())
//                .add("topologyId", topologyId)
                .add("nodeId", nodeId)
                .add("nepId", nepId)
                .add("sipId", sipId)
                .add("connectPoint", cp)
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
