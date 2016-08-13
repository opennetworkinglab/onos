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

import org.onosproject.tetopology.management.api.KeyId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * TE termination point representation.
 */
public class TeTerminationPoint {
    private KeyId teTpId;
    private List<InterfaceSwitchingCapability> capabilities;
    private long interLayerLockId;

    /**
     * Creates an instance of TeTerminationPoint.
     *
     * @param teTpId TE termination point identifier
     * @param capabilities capability descriptor for termination point
     * @param interLayerLockId inter-layer lock identifier
     */
    public TeTerminationPoint(KeyId teTpId,
      List<InterfaceSwitchingCapability> capabilities, long interLayerLockId) {
        this.teTpId = teTpId;
        this.capabilities = capabilities;
        this.interLayerLockId = interLayerLockId;
    }

    /**
     * Creates an instance of TeTerminationPoint with teTpId only.
     *
     * @param teTpId TE termination point id
     */
    public TeTerminationPoint(KeyId teTpId) {
        this.teTpId = teTpId;
    }

    /**
     * Returns the TE termination point id.
     *
     * @return value of teTpId
     */
    public KeyId teTpId() {
        return teTpId;
    }

    /**
     * Returns the interface switching capabilities.
     *
     * @return interface switching capabilities
     */
    public List<InterfaceSwitchingCapability> interfaceSwitchingCapabilities() {
        return capabilities;
    }

    /**
     * Returns the interLayerLockId.
     *
     * @return interlayer lock id
     */
    public long getInterLayerLockId() {
        return interLayerLockId;
    }

    /**
     * Sets the te tp Id.
     *
     * @param teTpId the teTpId to set
     */
    public void setTeTpId(KeyId teTpId) {
        this.teTpId = teTpId;
    }

    /**
     * Sets the interface switching capabilities.
     *
     * @param capabilities the capabilities to set
     */
    public void setInterfaceSwitchingCapabilities(List<InterfaceSwitchingCapability> capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Sets the inter layer lockId.
     *
     * @param interLayerLockId the interLayerLockId to set
     */
    public void setInterLayerLockId(long interLayerLockId) {
        this.interLayerLockId = interLayerLockId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(teTpId, capabilities, interLayerLockId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeTerminationPoint) {
            TeTerminationPoint that = (TeTerminationPoint) object;
            return Objects.equal(this.teTpId, that.teTpId) &&
                    Objects.equal(this.capabilities, that.capabilities) &&
                    Objects.equal(this.interLayerLockId, that.interLayerLockId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("teTpId", teTpId)
                .add("capabilities", capabilities)
                .add("interLayerLockId", interLayerLockId)
                .toString();
    }

}
