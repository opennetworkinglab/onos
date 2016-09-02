/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.roadm;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashMap;
import java.util.Map;

/**
 * Manages the port target powers for ROADM devices.
 */
@Component(immediate = true)
@Service
public class DistributedRoadmStore implements RoadmStore {
    private static Logger log = LoggerFactory.getLogger(DistributedRoadmStore.class);

    private ConsistentMap<DeviceId, Map<PortNumber, Long>> distPowerMap;
    private Map<DeviceId, Map<PortNumber, Long>> powerMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Activate
    public void activate() {
        distPowerMap = storageService.<DeviceId, Map<PortNumber, Long>>consistentMapBuilder()
                .withName("onos-roadm-distributed-store")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build();
        powerMap = distPowerMap.asJavaMap();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    // Add a map to the store for a device if not already added.
    // Powers still need to be initialized with calls to setTargetPower().
    @Override
    public void addDevice(DeviceId deviceId) {
        powerMap.putIfAbsent(deviceId, new HashMap<>());
        log.info("Initializing {}", deviceId);
    }

    // Returns true if Map for device exists in ConsistentMap
    @Override
    public boolean deviceAvailable(DeviceId deviceId) {
        return powerMap.get(deviceId) != null;
    }


    @Override
    public void setTargetPower(DeviceId deviceId, PortNumber portNumber, long targetPower) {
        Map<PortNumber, Long> portMap = powerMap.get(deviceId);
        if (portMap != null) {
            portMap.put(portNumber, targetPower);
            powerMap.put(deviceId, portMap);
        } else {
            log.info("Device {} not found in store", deviceId);
        }
    }

    @Override
    public Long getTargetPower(DeviceId deviceId, PortNumber portNumber) {
        Map<PortNumber, Long> portMap = powerMap.get(deviceId);
        if (portMap != null) {
            return portMap.get(portNumber);
        }
        return null;
    }
}
