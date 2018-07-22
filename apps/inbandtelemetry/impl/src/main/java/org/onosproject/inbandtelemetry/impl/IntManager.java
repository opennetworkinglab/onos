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
package org.onosproject.inbandtelemetry.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.inbandtelemetry.api.IntConfig;
import org.onosproject.inbandtelemetry.api.IntIntent;
import org.onosproject.inbandtelemetry.api.IntIntentId;
import org.onosproject.inbandtelemetry.api.IntObjective;
import org.onosproject.inbandtelemetry.api.IntProgrammable;
import org.onosproject.inbandtelemetry.api.IntService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicIdGenerator;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of IntService, for controlling INT-capable pipelines.
 */
@Component(immediate = true)
@Service
public class IntManager implements IntService {
    private final String appName = "org.onosproject.inbandtelemetry";
    private ApplicationId appId;
    private final Logger log = getLogger(getClass());
    private ConsistentMap<IntIntentId, IntIntent> intentConsistentMap;
    private ConsistentMap<DeviceId, IntDeviceRole> deviceRoleConsistentMap;
    private IntConfig cfg;
    private AtomicIdGenerator intentIds;

    private InternalHostListener hostListener = new InternalHostListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(appName);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(IntIntent.class)
                .register(IntIntentId.class)
                .register(IntDeviceRole.class)
                .register(IntIntent.IntHeaderType.class)
                .register(IntIntent.IntMetadataType.class)
                .register(IntIntent.IntReportType.class)
                .register(IntIntent.TelemetryMode.class);

        intentConsistentMap = storageService.<IntIntentId, IntIntent>consistentMapBuilder()
                .withSerializer(Serializer.using(serializer.build()))
                .withName("int-intents")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();

        deviceRoleConsistentMap = storageService.<DeviceId, IntDeviceRole>consistentMapBuilder()
                .withSerializer(Serializer.using(serializer.build()))
                .withName("int-device-roles")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();

        // Assign IntDeviceRole to each device
        deviceService.getAvailableDevices().forEach(device ->
                deviceRoleConsistentMap.put(device.id(),
                                            hostService.getConnectedHosts(device.id()).isEmpty() ?
                                                    IntDeviceRole.TRANSIT :
                                                    IntDeviceRole.SOURCE_SINK)
        );
        hostService.addListener(hostListener);
        intentIds = storageService.getAtomicIdGenerator("int-intent-id-generator");
        startInt();
        log.info("Started", appId.id());
    }

    @Deactivate
    public void deactivate() {
        hostService.removeListener(hostListener);
        log.info("Deactivated");
    }

    @Override
    public void startInt() {
        deviceService.getAvailableDevices().forEach(device -> {
            if (device.is(IntProgrammable.class)) {
                IntProgrammable intDevice = device.as(IntProgrammable.class);
                intDevice.init();
            }
        });
    }

    @Override
    public void startInt(Set<DeviceId> deviceIds) {
        deviceIds.forEach(deviceId -> {
            Device device = deviceService.getDevice(deviceId);
            if (device.is(IntProgrammable.class) &&
                    getIntRole(deviceId) == IntDeviceRole.TRANSIT) {
                IntProgrammable intDevice = device.as(IntProgrammable.class);
                intDevice.init();
            }
        });
    }

    @Override
    public void stopInt() {
        flowRuleService.removeFlowRulesById(appId);
    }

    @Override
    public void stopInt(Set<DeviceId> deviceIds) {

    }

    @Override
    public void setConfig(IntConfig cfg) {
        this.cfg = cfg;
        deviceService.getAvailableDevices().forEach(device -> {
            if (device.is(IntProgrammable.class)) {
                IntProgrammable intDevice = device.as(IntProgrammable.class);
                intDevice.setupIntConfig(cfg);
            }
        });
    }

    @Override
    public IntConfig getConfig() {
        return cfg;
    }

    @Override
    public IntIntentId installIntIntent(IntIntent intent) {
        Integer intentId = (int) intentIds.nextId();
        IntIntentId intIntentId = IntIntentId.valueOf(intentId);
        intentConsistentMap.put(intIntentId, intent);

        // Convert IntIntent into an IntObjective
        IntObjective obj = new IntObjective.Builder()
                .withSelector(intent.selector())
                .withMetadataTypes(intent.metadataTypes())
                .withHeaderType(intent.headerType())
                .build();

        // Install IntObjective on each INT source device
        deviceService.getAvailableDevices().forEach(device -> {
            if (device.is(IntProgrammable.class)
                    && deviceRoleConsistentMap.get(device.id()).value() == IntDeviceRole.SOURCE_SINK) {
                IntProgrammable intDevice = device.as(IntProgrammable.class);
                intDevice.addIntObjective(obj);
            }
        });
        return intIntentId;
    }

    @Override
    public void removeIntIntent(IntIntentId intentId) {
        IntIntent intent = intentConsistentMap.remove(intentId).value();

        // Convert IntIntent into an IntObjective
        IntObjective obj = new IntObjective.Builder()
                .withSelector(intent.selector())
                .withMetadataTypes(intent.metadataTypes())
                .withHeaderType(intent.headerType())
                .build();

        // Remove IntObjective on each INT source device
        deviceService.getAvailableDevices().forEach(device -> {
            if (device.is(IntProgrammable.class)
                    && deviceRoleConsistentMap.get(device.id()).value() == IntDeviceRole.SOURCE_SINK) {
                IntProgrammable intDevice = device.as(IntProgrammable.class);
                intDevice.removeIntObjective(obj);
            }
        });
    }

    @Override
    public IntIntent getIntIntent(IntIntentId intentId) {
        return Optional.ofNullable(intentConsistentMap.get(intentId).value()).orElse(null);
    }

    @Override
    public Map<IntIntentId, IntIntent> getIntIntents() {
        return intentConsistentMap.asJavaMap();
    }

    private IntDeviceRole getIntRole(DeviceId deviceId) {
        return deviceRoleConsistentMap.get(deviceId).value();
    }

    private void setIntRole(DeviceId deviceId, IntDeviceRole role) {
        deviceRoleConsistentMap.put(deviceId, role);
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            DeviceId deviceId = event.subject().location().deviceId();
            if (!deviceService.getDevice(deviceId).is(IntProgrammable.class)) {
                return;
            }
            switch (event.type()) {
                case HOST_ADDED:
                    // When a host is attached to the switch, we can configure it
                    // to work as SOURCE_SINK switch.
                    if (deviceRoleConsistentMap.getOrDefault(deviceId, IntDeviceRole.TRANSIT).value()
                            != IntDeviceRole.SOURCE_SINK) {
                        setIntRole(deviceId, IntDeviceRole.SOURCE_SINK);
                    }
                    break;
                default:
                    break;
            }
        }
    }

}
