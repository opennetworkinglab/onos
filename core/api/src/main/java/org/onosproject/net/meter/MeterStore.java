/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onosproject.store.Store;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Entity that stores and distributed meter objects.
 */
public interface MeterStore extends Store<MeterEvent, MeterStoreDelegate> {

    /**
     * Adds a meter to the store.
     *
     * @param meter a meter
     * @return a future indicating the result of the store operation
     */
    CompletableFuture<MeterStoreResult> storeMeter(Meter meter);

    /**
     * Deletes a meter from the store.
     *
     * @param meter a meter
     * @return a future indicating the result of the store operation
     */
    CompletableFuture<MeterStoreResult> deleteMeter(Meter meter);

    /**
     * Updates a meter whose meter id is the same as the passed meter.
     *
     * @param meter a new meter
     * @return a future indicating the result of the store operation
     */
    CompletableFuture<MeterStoreResult> updateMeter(Meter meter);

    /**
     * Updates a given meter's state with the provided state.
     *
     * @param meter a meter
     */
    void updateMeterState(Meter meter);

    /**
     * Obtains a meter matching the given meter id.
     *
     * @param meterId a meter id
     * @return a meter
     */
    Meter getMeter(MeterId meterId);

    /**
     * Returns all meters stored in the store.
     *
     * @return a collection of meters
     */
    Collection<Meter> getAllMeters();

    /**
     * Update the store by deleting the failed meter.
     * Notifies the delegate that the meter failed to allow it
     * to nofity the app.
     *
     * @param op a failed meter operation
     * @param reason a failure reason
     */
    void failedMeter(MeterOperation op, MeterFailReason reason);

    /**
     * Delete this meter immediately.
     * @param m a meter
     */
    void deleteMeterNow(Meter m);

}
