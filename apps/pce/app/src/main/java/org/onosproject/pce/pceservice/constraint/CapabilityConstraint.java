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
package org.onosproject.pce.pceservice.constraint;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.intent.constraint.BooleanConstraint;
import org.onosproject.pcep.api.DeviceCapability;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Constraint that evaluates whether devices satisfies capability.
 */
public final class CapabilityConstraint extends BooleanConstraint {

    private final CapabilityType capabilityType;
    public static final String LSRID = "lsrId";
    public static final String TRUE = "true";

    /**
     * Represents about capability type.
     */
    public enum CapabilityType {
        /**
         * Signifies that path is created via signaling mode.
         */
        WITH_SIGNALLING(0),

        /**
         * Signifies that path is created via SR mode.
         */
        SR_WITHOUT_SIGNALLING(1),

        /**
         * Signifies that path is created via without signaling and without SR mode.
         */
        WITHOUT_SIGNALLING_AND_WITHOUT_SR(2);

        int value;

        /**
         * Assign val with the value as the capability type.
         *
         * @param val capability type
         */
        CapabilityType(int val) {
            value = val;
        }

        /**
         * Returns value of capability type.
         *
         * @return capability type
         */
        public byte type() {
            return (byte) value;
        }
    }

    // Constructor for serialization
    private CapabilityConstraint() {
        capabilityType = null;
    }

    /**
     * Creates a new capability constraint.
     *
     * @param capabilityType type of capability device supports
     */
    public CapabilityConstraint(CapabilityType capabilityType) {
        this.capabilityType = capabilityType;
    }

    /**
     * Creates a new capability constraint.
     *
     * @param capabilityType type of capability device supports
     * @return instance of CapabilityConstraint for specified capability type
     */
    public static CapabilityConstraint of(CapabilityType capabilityType) {
        return new CapabilityConstraint(capabilityType);
    }

    /**
     * Obtains type of capability.
     *
     * @return type of capability
     */
    public CapabilityType capabilityType() {
        return capabilityType;
    }

    /**
     * Validates the link based on capability constraint.
     *
     * @param link to validate source and destination based on capability constraint
     * @param deviceService instance of DeviceService
     * @param netCfgService instance of NetworkConfigService
     * @return true if link satisfies capability constraint otherwise false
     */
    public boolean isValidLink(Link link, DeviceService deviceService, NetworkConfigService netCfgService) {
        if (deviceService == null || netCfgService == null) {
            return false;
        }

        Device srcDevice = deviceService.getDevice(link.src().deviceId());
        Device dstDevice = deviceService.getDevice(link.dst().deviceId());

        //TODO: Usage of annotations are for transient solution. In future will be replaced with the
        // network config service / Projection model.
        // L3 device
        if (srcDevice == null || dstDevice == null) {
            return false;
        }

        String srcLsrId = srcDevice.annotations().value(LSRID);
        String dstLsrId = dstDevice.annotations().value(LSRID);

        DeviceCapability srcDeviceConfig = netCfgService.getConfig(DeviceId.deviceId(srcLsrId),
                                                                       DeviceCapability.class);
        DeviceCapability dstDeviceConfig = netCfgService.getConfig(DeviceId.deviceId(dstLsrId),
                                                                       DeviceCapability.class);

        switch (capabilityType) {
        case WITH_SIGNALLING:
            return true;
        case WITHOUT_SIGNALLING_AND_WITHOUT_SR:
            return srcDeviceConfig != null && dstDeviceConfig != null
                    && srcDeviceConfig.localLabelCap() && dstDeviceConfig.localLabelCap();
        case SR_WITHOUT_SIGNALLING:
            return srcDeviceConfig != null && dstDeviceConfig != null && srcDeviceConfig.srCap()
                    && dstDeviceConfig.srCap() && srcDeviceConfig.labelStackCap() && dstDeviceConfig.labelStackCap();
        default:
            return false;
        }
    }

    @Override
    public boolean isValid(Link link, ResourceContext context) {
        return false;
        //Do nothing instead using isValidLink needs device service to validate link
    }

    @Override
    public int hashCode() {
        return Objects.hash(capabilityType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof CapabilityConstraint) {
            CapabilityConstraint other = (CapabilityConstraint) obj;
            return Objects.equals(this.capabilityType, other.capabilityType);
        }

        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("capabilityType", capabilityType)
                .toString();
    }
}