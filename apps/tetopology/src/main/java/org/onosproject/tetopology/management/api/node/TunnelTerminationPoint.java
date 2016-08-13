/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.node;

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of a tunnel termination point.
 */
public class TunnelTerminationPoint {
    //See org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te
    //.topology.rev20160708.ietftetopology
    //.augmentednwnode.te.DefaultTunnelTerminationPoint
    //org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology
    //.rev20160708.ietftetopology
    //.augmentednwnode.te.tunnelterminationpoint.DefaultConfig
    private byte[] tunnelTpId;
    private List<InterfaceSwitchingCapability> switchingCapabilities;
    private long interLayerLockId;
    private List<TerminationCapability> terminationCapability;

    /**
     * Create an instance of TunnelTerminationPoint.
     *
     * @param tunnelTpId tunnel termination point id
     */
    public TunnelTerminationPoint(byte[] tunnelTpId) {
        this.tunnelTpId = tunnelTpId;
    }

    /**
     * Sets the switching capabilities.
     *
     * @param swcaps the switching capabilities to set
     */
    public void setSwitchingCapabilities(List<InterfaceSwitchingCapability> swcaps) {
        this.switchingCapabilities = swcaps;
    }

    /**
     * Sets the interLayerLockId.
     *
     * @param id the interLayerLockId to set
     */
    public void setInterLayerLockId(long id) {
        this.interLayerLockId = id;
    }

    /**
     * Sets the termination capability.
     *
     * @param terminationCapability the terminationCapability to set
     */
    public void setTerminationCapabilities(List<TerminationCapability> terminationCapability) {
        this.terminationCapability = terminationCapability;
    }

    /**
     * Returns the tunnelTpId.
     *
     * @return tunnel termination point id
     */
    public byte[] getTunnelTpId() {
        return tunnelTpId;
    }

    /**
     * Returns the switching capabilities.
     *
     * @return switching capabilities
     */
    public List<InterfaceSwitchingCapability> getSwitchingCapabilities() {
        return switchingCapabilities;
    }

    /**
     * Returns the interLayerLockId.
     *
     * @return inter layer lock identifier
    */
    public long getInterLayerLockId() {
        return interLayerLockId;
    }

    /**
     * Returns the termination capability list.
     *
     * @return termination capabilities
     */
    public List<TerminationCapability> getTerminationCapabilities() {
        return terminationCapability;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tunnelTpId, switchingCapabilities, interLayerLockId, terminationCapability);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TunnelTerminationPoint) {
            TunnelTerminationPoint that = (TunnelTerminationPoint) object;
            return Objects.equal(this.tunnelTpId, that.tunnelTpId) &&
                    Objects.equal(this.switchingCapabilities, that.switchingCapabilities) &&
                    Objects.equal(this.terminationCapability, that.terminationCapability) &&
                    Objects.equal(this.interLayerLockId, that.interLayerLockId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tunnelTpId", tunnelTpId)
                .add("switchingCapabilities", switchingCapabilities)
                .add("interLayerLockId", interLayerLockId)
                .add("terminationCapability", terminationCapability)
                .toString();
    }

}
