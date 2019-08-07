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
package org.onosproject.roadm;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the port target powers for ROADM devices.
 */
@Component(immediate = true, service = RoadmStore.class)
public class DistributedRoadmStore implements RoadmStore {
    private static Logger log = LoggerFactory.getLogger(DistributedRoadmStore.class);

    private ConsistentMap<DeviceId, Map<PortNumber, Double>> distPowerMap;
    private Map<DeviceId, Map<PortNumber, Double>> powerMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Activate
    public void activate() {
        distPowerMap = storageService.<DeviceId, Map<PortNumber, Double>>consistentMapBuilder()
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
    public void setTargetPower(DeviceId deviceId, PortNumber portNumber, double targetPower) {
        Map<PortNumber, Double> portMap = powerMap.get(deviceId);
        if (portMap != null) {
            portMap.put(portNumber, targetPower);
            powerMap.put(deviceId, portMap);
        } else {
            log.info("Device {} not found in store", deviceId);
        }
    }

    @Override
    public Double getTargetPower(DeviceId deviceId, PortNumber portNumber) {
        Map<PortNumber, Double> portMap = powerMap.get(deviceId);
        if (portMap != null) {
            return portMap.get(portNumber);
        }
        return null;
    }

    @Override
    public void removeTargetPower(DeviceId deviceId, PortNumber portNumber) {
        if (powerMap.get(deviceId) != null) {
            powerMap.get(deviceId).remove(portNumber);
        }
    }
}
