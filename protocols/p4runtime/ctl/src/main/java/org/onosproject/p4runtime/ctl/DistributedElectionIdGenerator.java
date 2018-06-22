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

package org.onosproject.p4runtime.ctl;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounterMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed implementation of a generator of P4Runtime election IDs.
 */
class DistributedElectionIdGenerator {

    private final Logger log = getLogger(this.getClass());

    private AtomicCounterMap<DeviceId> electionIds;

    /**
     * Creates a new election ID generator using the given storage service.
     *
     * @param storageService storage service
     */
    DistributedElectionIdGenerator(StorageService storageService) {
        KryoNamespace serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .build();
        this.electionIds = storageService.<DeviceId>atomicCounterMapBuilder()
                .withName("p4runtime-election-ids")
                .withSerializer(Serializer.using(serializer))
                .build();
    }

    /**
     * Returns an election ID for the given device ID. The first election ID for
     * a given device ID is always 1.
     *
     * @param deviceId device ID
     * @return new election ID
     */
    BigInteger generate(DeviceId deviceId) {
        if (electionIds == null) {
            return null;
        }
        // Default value is 0 for AtomicCounterMap.
        return BigInteger.valueOf(electionIds.incrementAndGet(deviceId));
    }

    /**
     * Destroy the backing distributed primitive of this generator.
     */
    void destroy() {
        try {
            electionIds.destroy().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Exception while destroying distributed counter map", e);
        } finally {
            electionIds = null;
        }
    }
}
