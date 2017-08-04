/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onlab.packet.VlanId;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Arrays;
import java.util.Collection;

/**
 * Means to configure VLANs on legacy L2 switch devices.
 */
@Beta
public interface L2SwitchVlanConfigBehaviour extends HandlerBehaviour {
    /**
     * Provides the VLANs configured on a device.
     *
     * @return the configured VLANs on the device
     */
    Collection<VlanId> getVlans();

    /**
     * Adds a VLAN on a device.
     * Default enabled/disabled status of VLAN after depends on device.
     *
     * @param vlanId the VLAN to add
     * @return true, if the VLAN was added successfully; false otherwise
     */
    default boolean addVlan(VlanId vlanId) {
        return addVlan(Arrays.asList(vlanId));
    }

    /**
     * Adds VLANs on a device.
     * Default enabled/disabled status of VLAN after depends on device.
     *
     * @param vlanIds the VLANs to add
     * @return true, if the VLANs were added successfully; false otherwise
     */
    boolean addVlan(Collection<VlanId> vlanIds);

    /**
     * Removes a VLAN from a device.
     *
     * @param vlanId the VLAN to remove
     * @return true, if the VLAN was removed successfully; false otherwise
     */
    default boolean removeVlan(VlanId vlanId) {
        return removeVlan(Arrays.asList(vlanId));
    }

    /**
     * Removes VLANs from a device.
     *
     * @param vlanIds the VLANs to remove
     * @return true, if the VLANs were removed successfully; false otherwise
     */
    boolean removeVlan(Collection<VlanId> vlanIds);

    /**
     * Obtains the status of a VLAN on a device.
     *
     * @param vlanId the VLAN to check
     * @return true, if the VLAN is configured and enabled; false otherwise
     */
    boolean isEnabled(VlanId vlanId);

    /**
     * Enables a VLAN on a device.
     *
     * @param vlanId the VLAN to enable
     * @return true, if the VLAN was enabled successfully; false otherwise
     */
    default boolean enableVlan(VlanId vlanId) {
        return enableVlan(Arrays.asList(vlanId));
    }

    /**
     * Enables VLANs on a device.
     *
     * @param vlanIds the VLANs to enable
     * @return true, if the VLANs were enabled successfully; false otherwise
     */
    boolean enableVlan(Collection<VlanId> vlanIds);

    /**
     * Disables a VLAN on a device.
     *
     * @param vlanId the VLAN to disable
     * @return true, if the VLAN was disabled successfully; false otherwise
     */
    default boolean disableVlan(VlanId vlanId) {
        return disableVlan(Arrays.asList(vlanId));
    }

    /**
     * Disables VLANs on a device.
     *
     * @param vlanIds VLANs to disable
     * @return true, if the VLANs were disabled successfully; false otherwise
     */
    boolean disableVlan(Collection<VlanId> vlanIds);
}
