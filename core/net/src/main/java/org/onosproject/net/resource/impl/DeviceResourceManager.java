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
package org.onosproject.net.resource.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.Port;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.device.DeviceResourceService;
import org.onosproject.net.resource.device.DeviceResourceStore;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides basic implementation of device resources allocation.
 */
@Component(immediate = true)
@Service
public class DeviceResourceManager implements DeviceResourceService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceResourceStore store;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean requestPorts(Set<Port> ports, Intent intent) {
        checkNotNull(intent);

        return store.allocatePorts(ports, intent.id());
    }

    @Override
    public Set<Port> getAllocations(IntentId intentId) {
        return store.getAllocations(intentId);
    }

    @Override
    public IntentId getAllocations(Port port) {
        return store.getAllocations(port);
    }

    @Override
    public void releaseMapping(IntentId intentId) {
        store.releaseMapping(intentId);
    }

    @Override
    public boolean requestMapping(IntentId keyIntentId, IntentId valIntentId) {
        return store.allocateMapping(keyIntentId, valIntentId);
    }

    @Override
    public Set<IntentId> getMapping(IntentId intentId) {
        return store.getMapping(intentId);
    }

    @Override
    public void releasePorts(IntentId intentId) {
        store.releasePorts(intentId);
    }

    private Port getTypedPort(Set<Port> ports, Port.Type type) {
        for (Port port : ports) {
            if (port.type() == type) {
                return port;
            }
        }

        return null;
    }
}
