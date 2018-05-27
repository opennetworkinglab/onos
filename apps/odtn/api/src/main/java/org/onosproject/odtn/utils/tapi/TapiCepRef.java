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

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * TAPI Cep reference class.
 * <p>
 * TAPI reference class should be used in ODTN ServiceApplication
 * in order to make independent ServiceApplication implementation from DCS.
 */
public final class TapiCepRef {

    protected final Logger log = getLogger(getClass());

    private final UUID topologyId;
    private final UUID nodeId;
    private final UUID nepId;
    private final UUID cepId;

    private TapiCepRef(String topologyId, String nodeId, String nepId, String cepId) {
        this.topologyId = UUID.fromString(topologyId);
        this.nodeId = UUID.fromString(nodeId);
        this.nepId = UUID.fromString(nepId);
        this.cepId = UUID.fromString(cepId);
    }

    public static TapiCepRef create(String topologyId, String nodeId, String nepId, String cepId) {
        return new TapiCepRef(topologyId, nodeId, nepId, cepId);
    }

    public static TapiCepRef create(TapiNepRef nepRef, String cepId) {
        return new TapiCepRef(nepRef.getTopologyId(), nepRef.getNodeId(), nepRef.getNepId(), cepId);
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

    public String getCepId() {
        return cepId.toString();
    }

    public TapiNepRef getNepRef() {
        return new TapiNepRef(topologyId.toString(), nodeId.toString(), nepId.toString());
    }

    public String toString() {
        return toStringHelper(getClass())
                .add("topologyId", topologyId)
                .add("nodeId", nodeId)
                .add("nepId", nepId)
                .add("cepId", cepId)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TapiCepRef)) {
            return false;
        }
        TapiCepRef that = (TapiCepRef) o;
        return Objects.equals(topologyId, that.topologyId) &&
                Objects.equals(nodeId, that.nodeId) &&
                Objects.equals(nepId, that.nepId) &&
                Objects.equals(cepId, that.cepId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topologyId, nodeId, nepId, cepId);
    }
}
