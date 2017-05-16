/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.distributedprimitives;

import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Simple application to test distributed primitives.
 */
@Component(immediate = true)
@Service(value = DistributedPrimitivesTest.class)
public class DistributedPrimitivesTest {

    private final Logger log = getLogger(getClass());

    private static final String APP_NAME = "org.onosproject.distributedprimitives";
    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final Map<String, EventuallyConsistentMap<String, String>> maps = Maps.newConcurrentMap();

    @Activate
    protected void activate() {
        log.info("Distributed-Primitives-test app started");
        appId = coreService.registerApplication(APP_NAME);
    }

    @Deactivate
    protected void deactivate() {
        log.info("Distributed-Primitives-test app Stopped");
    }

    /**
     * Returns an eventually consistent test map by name.
     *
     * @param name the test map name
     * @return the test map
     */
    public EventuallyConsistentMap<String, String> getEcMap(String name) {
        return maps.computeIfAbsent(name, n -> storageService.<String, String>eventuallyConsistentMapBuilder()
                .withName(name)
                .withSerializer(KryoNamespaces.API)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build());
    }
}
