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
 *
 */

package org.onosproject.drivers.bmv2.mirror;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.util.KryoNamespace;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2Entity;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2Handle;
import org.onosproject.net.DeviceId;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of a distributed BMv2 mirror, backed by an
 * {@link EventuallyConsistentMap}.
 *
 * @param <H> Handle class
 * @param <E> Entry class
 */
@Component(immediate = true)
public abstract class AbstractDistributedBmv2Mirror
        <H extends Bmv2Handle, E extends Bmv2Entity>
        implements Bmv2Mirror<H, E> {


    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private StorageService storageService;

    private EventuallyConsistentMap<H, E> mirrorMap;

    @Activate
    public void activate() {
        mirrorMap = storageService
                .<H, E>eventuallyConsistentMapBuilder()
                .withName(mapName())
                .withSerializer(storeSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Started");
    }

    abstract String mapName();

    abstract KryoNamespace storeSerializer();

    @Deactivate
    public void deactivate() {
        mirrorMap.destroy();
        mirrorMap = null;
        log.info("Stopped");
    }

    @Override
    public Collection<E> getAll(DeviceId deviceId) {
        checkNotNull(deviceId);
        return mirrorMap.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public E get(H handle) {
        checkNotNull(handle);
        return mirrorMap.get(handle);
    }

    @Override
    public void put(H handle, E entry) {
        checkNotNull(handle);
        checkNotNull(entry);
        mirrorMap.put(handle, entry);
    }

    @Override
    public void remove(H handle) {
        checkNotNull(handle);
        mirrorMap.remove(handle);
    }
}