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
import org.onosproject.net.driver.HandlerBehaviour;

import java.io.IOException;
import java.util.Collections;
import java.util.Collection;

/**
 * Means to configure bandwidth profiles on devices.
 */
@Beta
public interface BandwidthProfileConfigBehaviour extends HandlerBehaviour {
    /**
     * Adds a new bandwidth profile on the device.
     * If a profile with the same name already exists on the device, the profile
     * is not added.
     *
     * @param bwProfile the bandwidth profile to add
     * @return true, if the profile was added successfully; false otherwise
     */
    default boolean addBandwidthProfile(BandwidthProfile bwProfile) {
        return addBandwidthProfile(Collections.singletonList(bwProfile));
    }

    /**
     * Adds new bandwidth profiles on the device.
     * If profiles with the same names already exist on the device, the
     * conflicting profiles are not added.
     *
     * @param bwProfiles the bandwidth profiles to add
     * @return true, if any of the profiles were added successfully;
     * false otherwise
     */
    boolean addBandwidthProfile(Collection<BandwidthProfile> bwProfiles);

    /**
     * Removes an existing bandwidth profile from a device.
     * Returns false if the profile does not exist on the device.
     *
     * @param profileName the name of the profile to remove from the device
     * @return true, if the profile was removed successfully; false otherwise
     */
    default boolean removeBandwidthProfile(String profileName) {
        return removeBandwidthProfile(Collections.singletonList(profileName));
    }

    /**
     * Removes existing bandwidth profiles from a device.
     * Returns false if none of the profiles exist on the device.
     *
     * @param profileNames the names of the profiles to remove from the device
     * @return true, if any of the profiles were removed successfully;
     * false otherwise
     */
    boolean removeBandwidthProfile(Collection<String> profileNames);

    /**
     * Removes all existing bandwidth profiles from a device.
     * Returns true if no profiles exist on the device.
     *
     * @return true, if all profiles were removed successfully; false otherwise
     */
    boolean removeAllBandwidthProfiles();

    /**
     * Updates an already configured bandwidth profile on the device.
     * Returns false if the profile does not exist on the device.
     *
     * @param bwProfile the updated bandwidth profile
     * @return true, if the profile was updated successfully; false otherwise
     */
    default boolean updateBandwidthProfile(BandwidthProfile bwProfile) {
        return updateBandwidthProfile(Collections.singletonList(bwProfile));
    }

    /**
     * Updates already configured bandwidth profiles on the device.
     * Returns false if none of the profiles exist on the device.
     *
     * @param bwProfiles the updated bandwidth profile
     * @return true, if any of the profiles were updated successfully;
     * false otherwise
     */
    boolean updateBandwidthProfile(
            Collection<BandwidthProfile> bwProfiles);

    /**
     * Obtains an already configured bandwidth profile from the device.
     *
     * @param profileName the name of the profile to obtain from the device
     * @return the bandwidth profile; null if the profile does not exist
     * @throws IOException if profile could not be obtained due to
     * communication issues with the device
     */
    BandwidthProfile getBandwidthProfile(String profileName) throws IOException;

    /**
     * Obtains all already configured bandwidth profiles from the device.
     *
     * @return the bandwidth profiles; empty collection if no profiles exist
     * @throws IOException if profiles could not be obtained due to
     * communication issues with the device
     */
    Collection<BandwidthProfile> getAllBandwidthProfiles() throws IOException;
}
