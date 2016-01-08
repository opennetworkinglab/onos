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
package org.onosproject.net.newresource.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.behaviour.MplsQuery;
import org.onosproject.net.behaviour.TributarySlotQuery;
import org.onosproject.net.behaviour.VlanQuery;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.newresource.ResourceAdminService;
import org.onosproject.net.newresource.ResourcePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of DeviceListener registering devices as resources.
 */
final class ResourceDeviceListener implements DeviceListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceDeviceListener.class);

    private final ResourceAdminService adminService;
    private final DeviceService deviceService;
    private final DriverService driverService;
    private final ExecutorService executor;

    /**
     * Creates an instance with the specified ResourceAdminService and ExecutorService.
     *
     * @param adminService instance invoked to register resources
     * @param deviceService {@link DeviceService} to be used.
     * @param executor executor used for processing resource registration
     */
    ResourceDeviceListener(ResourceAdminService adminService, DeviceService deviceService, DriverService driverService,
                           ExecutorService executor) {
        this.adminService = checkNotNull(adminService);
        this.deviceService = checkNotNull(deviceService);
        this.driverService = checkNotNull(driverService);
        this.executor = checkNotNull(executor);
    }

    @Override
    public void event(DeviceEvent event) {
        Device device = event.subject();
        switch (event.type()) {
            case DEVICE_ADDED:
                registerDeviceResource(device);
                break;
            case DEVICE_REMOVED:
                unregisterDeviceResource(device);
                break;
            case DEVICE_AVAILABILITY_CHANGED:
                if (deviceService.isAvailable(device.id())) {
                    registerDeviceResource(device);
                    // TODO: do we need to walk the ports?
                } else {
                    unregisterDeviceResource(device);
                }
                break;
            case PORT_ADDED:
            case PORT_UPDATED:
                if (event.port().isEnabled()) {
                    registerPortResource(device, event.port());
                } else {
                    unregisterPortResource(device, event.port());
                }
                break;
            case PORT_REMOVED:
                unregisterPortResource(device, event.port());
                break;
            default:
                break;
        }
    }

    private void registerDeviceResource(Device device) {
        executor.submit(() -> adminService.registerResources(ResourcePath.discrete(device.id())));
    }

    private void unregisterDeviceResource(Device device) {
        executor.submit(() -> adminService.unregisterResources(ResourcePath.discrete(device.id())));
    }

    private void registerPortResource(Device device, Port port) {
        ResourcePath portPath = ResourcePath.discrete(device.id(), port.number());
        executor.submit(() -> {
            adminService.registerResources(portPath);

            // for VLAN IDs
            Set<VlanId> vlans = queryVlanIds(device.id(), port.number());
            if (!vlans.isEmpty()) {
                adminService.registerResources(vlans.stream()
                                               .map(portPath::child)
                                               .collect(Collectors.toList()));
            }

            // for MPLS labels
            Set<MplsLabel> mplsLabels = queryMplsLabels(device.id(), port.number());
            if (!mplsLabels.isEmpty()) {
                adminService.registerResources(mplsLabels.stream()
                                               .map(portPath::child)
                                               .collect(Collectors.toList()));
            }

            // for Lambdas
            Set<OchSignal> lambdas = queryLambdas(device.id(), port.number());
            if (!lambdas.isEmpty()) {
                adminService.registerResources(lambdas.stream()
                                               .map(portPath::child)
                                               .collect(Collectors.toList()));
            }

            // for Tributary slots
            Set<TributarySlot> tSlots = queryTributarySlots(device.id(), port.number());
            if (!tSlots.isEmpty()) {
                adminService.registerResources(tSlots.stream()
                                               .map(portPath::child)
                                               .collect(Collectors.toList()));
            }
        });
    }

    private void unregisterPortResource(Device device, Port port) {
        ResourcePath resource = ResourcePath.discrete(device.id(), port.number());
        executor.submit(() -> adminService.unregisterResources(resource));
    }

    private Set<OchSignal> queryLambdas(DeviceId did, PortNumber port) {
        try {
            DriverHandler handler = driverService.createHandler(did);
            if (handler == null || !handler.hasBehaviour(LambdaQuery.class)) {
                return Collections.emptySet();
            }
            LambdaQuery query = handler.behaviour(LambdaQuery.class);
            if (query != null) {
                return query.queryLambdas(port).stream()
                        .flatMap(x -> OchSignal.toFlexGrid(x).stream())
                        .collect(Collectors.toSet());
            } else {
                return Collections.emptySet();
            }
        } catch (ItemNotFoundException e) {
            return Collections.emptySet();
        }
    }

    private Set<VlanId> queryVlanIds(DeviceId device, PortNumber port) {
        try {
            DriverHandler handler = driverService.createHandler(device);
            if (handler == null || !handler.hasBehaviour(VlanQuery.class)) {
                return ImmutableSet.of();
            }

            VlanQuery query = handler.behaviour(VlanQuery.class);
            if (query == null) {
                return ImmutableSet.of();
            }
            return query.queryVlanIds(port);
        } catch (ItemNotFoundException e) {
            return ImmutableSet.of();
        }
    }

    private Set<MplsLabel> queryMplsLabels(DeviceId device, PortNumber port) {
        try {
            DriverHandler handler = driverService.createHandler(device);
            if (handler == null || !handler.hasBehaviour(MplsQuery.class)) {
                return ImmutableSet.of();
            }

            MplsQuery query = handler.behaviour(MplsQuery.class);
            if (query == null) {
                return ImmutableSet.of();
            }
            return query.queryMplsLabels(port);
        } catch (ItemNotFoundException e) {
            return ImmutableSet.of();
        }
    }

    private Set<TributarySlot> queryTributarySlots(DeviceId device, PortNumber port) {
        try {
            DriverHandler handler = driverService.createHandler(device);
            if (handler == null || !handler.hasBehaviour(TributarySlotQuery.class)) {
                return Collections.emptySet();
            }
            TributarySlotQuery query = handler.behaviour(TributarySlotQuery.class);
            if (query != null) {
                return query.queryTributarySlots(port);
            } else {
                return Collections.emptySet();
            }
        } catch (ItemNotFoundException e) {
            return Collections.emptySet();
        }
    }
}
