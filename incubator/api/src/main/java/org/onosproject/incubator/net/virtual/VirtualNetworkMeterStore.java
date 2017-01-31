/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual;

import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterEvent;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterFeaturesKey;
import org.onosproject.net.meter.MeterKey;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterStoreDelegate;
import org.onosproject.net.meter.MeterStoreResult;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface VirtualNetworkMeterStore
        extends VirtualStore<MeterEvent, MeterStoreDelegate> {

    /**
     * Adds a meter to the store.
     *
     * @param networkId a virtual network identifier
     * @param meter a meter
     * @return a future indicating the result of the store operation
     */
    CompletableFuture<MeterStoreResult> storeMeter(NetworkId networkId, Meter meter);

    /**
     * Deletes a meter from the store.
     *
     * @param networkId a virtual network identifier
     * @param meter a meter
     * @return a future indicating the result of the store operation
     */
    CompletableFuture<MeterStoreResult> deleteMeter(NetworkId networkId, Meter meter);


    /**
     * Adds the meter features to the store.
     *
     * @param networkId a virtual network identifier
     * @param meterfeatures the meter features
     * @return the result of the store operation
     */
    MeterStoreResult storeMeterFeatures(NetworkId networkId, MeterFeatures meterfeatures);

    /**
     * Deletes the meter features from the store.
     *
     * @param networkId a virtual network identifier
     * @param deviceId the device id
     * @return a future indicating the result of the store operation
     */
    MeterStoreResult deleteMeterFeatures(NetworkId networkId, DeviceId deviceId);


    /**
     * Updates a meter whose meter id is the same as the passed meter.
     *
     * @param networkId a virtual network identifier
     * @param meter a new meter
     * @return a future indicating the result of the store operation
     */
    CompletableFuture<MeterStoreResult> updateMeter(NetworkId networkId, Meter meter);

    /**
     * Updates a given meter's state with the provided state.
     *
     * @param networkId a virtual network identifier
     * @param meter a meter
     */
    void updateMeterState(NetworkId networkId, Meter meter);

    /**
     * Obtains a meter matching the given meter key.
     *
     * @param networkId a virtual network identifier
     * @param key a meter key
     * @return a meter
     */
    Meter getMeter(NetworkId networkId, MeterKey key);

    /**
     * Returns all meters stored in the store.
     *
     * @param networkId a virtual network identifier
     * @return a collection of meters
     */
    Collection<Meter> getAllMeters(NetworkId networkId);

    /**
     * Update the store by deleting the failed meter.
     * Notifies the delegate that the meter failed to allow it
     * to nofity the app.
     *
     * @param networkId a virtual network identifier
     * @param op a failed meter operation
     * @param reason a failure reason
     */
    void failedMeter(NetworkId networkId, MeterOperation op, MeterFailReason reason);

    /**
     * Delete this meter immediately.
     *
     * @param networkId a virtual network identifier
     * @param m a meter
     */
    void deleteMeterNow(NetworkId networkId, Meter m);

    /**
     * Retrieve maximum meters available for the device.
     *
     * @param networkId a virtual network identifier
     * @param key the meter features key
     * @return the maximum number of meters supported by the device
     */
    long getMaxMeters(NetworkId networkId, MeterFeaturesKey key);
}
