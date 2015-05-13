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
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.resource.DeviceResourceService;
import org.onosproject.net.resource.DeviceResourceStore;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
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
    public Set<Port> requestPorts(Intent intent) {
        checkNotNull(intent);
        if (intent instanceof OpticalConnectivityIntent) {
            OpticalConnectivityIntent opticalIntent = (OpticalConnectivityIntent) intent;
            Set<Port> srcPorts = store.getFreePorts(opticalIntent.getSrc().deviceId());
            Set<Port> dstPorts = store.getFreePorts(opticalIntent.getDst().deviceId());

            Port srcPort = getTypedPort(srcPorts, Port.Type.OCH);
            Port dstPort = getTypedPort(dstPorts, Port.Type.OCH);

            if (srcPort == null || dstPort == null) {
                return null;
            }

            Set<Port> allocPorts = new HashSet(Arrays.asList(srcPort, dstPort));

            store.allocatePorts(allocPorts, intent.id());

            return allocPorts;
        }

        return null;
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
