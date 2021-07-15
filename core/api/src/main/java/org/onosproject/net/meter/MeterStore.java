/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.meter;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Entity that stores and distributed meter objects.
 */
public interface MeterStore extends Store<MeterEvent, MeterStoreDelegate> {

    /**
     * Adds a meter to the store or updates a meter in the store.
     *
     * @param meter a meter
     * @return a future indicating the result of the store operation
     */
    CompletableFuture<MeterStoreResult> addOrUpdateMeter(Meter meter);

    /**
     * Adds a meter to the store.
     *
     * @param meter a meter
     * @return a future indicating the result of the store operation
     * @deprecated in onos-2.5 replaced by {@link #addOrUpdateMeter(Meter)}
     */
    @Deprecated
    CompletableFuture<MeterStoreResult> storeMeter(Meter meter);

    /**
     * Deletes a meter from the store.
     *
     * @param meter a meter
     * @return a future indicating the result of the store operation
     */
    CompletableFuture<MeterStoreResult> deleteMeter(Meter meter);

    /**
     * Adds the meter features to the store.
     *
     * @param meterfeatures the meter features
     * @return the result of the store operation
     */
    MeterStoreResult storeMeterFeatures(MeterFeatures meterfeatures);

    /**
     * Adds a collection of meter features to the store.
     *
     * @param meterfeatures the collection of meter features
     * @return the result of the store operation
     */
    MeterStoreResult storeMeterFeatures(Collection<MeterFeatures> meterfeatures);

    /**
     * Deletes the meter features from the store.
     *
     * @param deviceId the device id
     * @return a future indicating the result of the store operation
     */
    MeterStoreResult deleteMeterFeatures(DeviceId deviceId);

    /**
     * Deletes a collection of meter features from the store.
     *
     * @param meterfeatures a collection of meter features
     * @return a future indicating the result of the store operation
     */
    MeterStoreResult deleteMeterFeatures(Collection<MeterFeatures> meterfeatures);

    /**
     * Updates a meter whose meter id is the same as the passed meter.
     *
     * @param meter a new meter
     * @return a future indicating the result of the store operation
     * @deprecated in onos-2.5 replaced by {@link #addOrUpdateMeter(Meter)}
     */
    @Deprecated
    CompletableFuture<MeterStoreResult> updateMeter(Meter meter);

    /**
     * Updates a given meter's state with the provided state.
     *
     * @param meter a meter
     * @return the updated meter
     */
    Meter updateMeterState(Meter meter);

    /**
     * Obtains a meter matching the given meter key.
     *
     * @param key a meter key
     * @return a meter
     */
    Meter getMeter(MeterKey key);

    /**
     * Returns all meters stored in the store.
     *
     * @return an immutable copy of all meters
     */
    Collection<Meter> getAllMeters();

    /**
     * Returns all meters stored in the store for a
     * precise device.
     *
     * @param deviceId the device to get the meter list from
     * @return an immutable copy of the meters stored for a given device
     */
    Collection<Meter> getAllMeters(DeviceId deviceId);

    /**
     * Update the store by deleting the failed meter.
     * Notifies the delegate that the meter failed to allow it
     * to nofity the app.
     *
     * @param op     a failed meter operation
     * @param reason a failure reason
     */
    void failedMeter(MeterOperation op, MeterFailReason reason);

    /**
     * Delete this meter immediately.
     *
     * @param m a meter
     * @deprecated in onos-2.5 renamed {@link #purgeMeter(Meter)}
     */
    @Deprecated
    void deleteMeterNow(Meter m);

    /**
     * Delete this meter immediately.
     *
     * @param m a meter
     */
    void purgeMeter(Meter m);

    /**
     * Retrieve maximum meters available for the device.
     *
     * @param key the meter features key
     * @return the maximum number of meters supported by the device
     * @deprecated in onos-2.5, Max meters is replaced by start and end index
     */
    @Deprecated
    long getMaxMeters(MeterFeaturesKey key);

    /**
     * Allocates the first available MeterId.
     *
     * @param deviceId the device id
     * @return the meter Id or null if it was not possible
     * to allocate a meter id
     * @deprecated in onos-2.5 replaced by {@link #allocateMeterId(DeviceId, MeterScope)}
     */
    @Deprecated
    MeterId allocateMeterId(DeviceId deviceId);

    /**
     * Allocates the first available MeterId.
     *
     * @param deviceId the device id
     * @param meterScope the meter scope
     * @return the meter Id or null if it was not possible
     * to allocate a meter id
     */
    MeterCellId allocateMeterId(DeviceId deviceId, MeterScope meterScope);

    /**
     * Frees the given meter id.
     *
     * @param deviceId the device id
     * @param meterId  the id to be freed
     * @deprecated in onos-2.5, freeing an ID is closely related to removal of a meter
     * so, this function is no longer exposed on interface
     */
    @Deprecated
    void freeMeterId(DeviceId deviceId, MeterId meterId);

    /**
     * Removes all meters of given device from store.
     * This API is typically used when the device is offline.
     *
     * @param deviceId the device id
     */
    void purgeMeter(DeviceId deviceId);

    /**
     * Removes all meters of given device and for the given application from store.
     * This API is typically used when the device is offline.
     *
     * @param deviceId the device id
     * @param appId the application id
     */
    void purgeMeters(DeviceId deviceId, ApplicationId appId);

}
