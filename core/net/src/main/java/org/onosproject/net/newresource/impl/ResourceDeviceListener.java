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

import com.google.common.collect.Lists;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.DefaultOchSignalComparator;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.OchPort;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.behaviour.MplsQuery;
import org.onosproject.net.behaviour.VlanQuery;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.newresource.ResourceAdminService;
import org.onosproject.net.newresource.ResourcePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of DeviceListener registering devices as resources.
 */
final class ResourceDeviceListener implements DeviceListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceDeviceListener.class);

    private static final int MAX_VLAN_ID = VlanId.MAX_VLAN;
    private static final List<VlanId> ENTIRE_VLAN_IDS = getEntireVlans();

    private static final int MAX_MPLS_LABEL = 1048576;
    private static final List<MplsLabel> ENTIRE_MPLS_LABELS = getEntireMplsLabels();

    private static final int TOTAL_ODU2_TRIBUTARY_SLOTS = 8;
    private static final int TOTAL_ODU4_TRIBUTARY_SLOTS = 80;
    private static final List<TributarySlot> ENTIRE_ODU2_TRIBUTARY_SLOTS = getEntireOdu2TributarySlots();
    private static final List<TributarySlot> ENTIRE_ODU4_TRIBUTARY_SLOTS = getEntireOdu4TributarySlots();

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
            if (isVlanEnabled(device.id(), port.number())) {
                adminService.registerResources(Lists.transform(ENTIRE_VLAN_IDS, portPath::child));
            }

            // for MPLS labels
            if (isMplsEnabled(device.id(), port.number())) {
                adminService.registerResources(Lists.transform(ENTIRE_MPLS_LABELS, portPath::child));
            }

            // for Lambdas
            SortedSet<OchSignal> lambdas = queryLambdas(device.id(), port.number());
            if (!lambdas.isEmpty()) {
                adminService.registerResources(lambdas.stream()
                                               .map(portPath::child)
                                               .collect(Collectors.toList()));
            }

            // for Tributary slots
            // TODO: need to define Behaviour to make a query about OCh port
            switch (port.type()) {
                case OCH:
                    // register ODU TributarySlots against the OCH port
                    registerTributarySlotsResources(((OchPort) port).signalType(), portPath);
                    break;
                default:
                    break;
            }
        });
    }

    private void registerTributarySlotsResources(OduSignalType oduSignalType, ResourcePath portPath) {
        switch (oduSignalType) {
            case ODU2:
                adminService.registerResources(Lists.transform(ENTIRE_ODU2_TRIBUTARY_SLOTS, portPath::child));
                break;
            case ODU4:
                adminService.registerResources(Lists.transform(ENTIRE_ODU4_TRIBUTARY_SLOTS, portPath::child));
                break;
            default:
                break;
        }
    }

    private void unregisterPortResource(Device device, Port port) {
        ResourcePath resource = ResourcePath.discrete(device.id(), port.number());
        executor.submit(() -> adminService.unregisterResources(resource));
    }

    private SortedSet<OchSignal> queryLambdas(DeviceId did, PortNumber port) {
        try {
            // DriverHandler does not provide a way to check if a
            // behaviour is supported.
            Driver driver = driverService.getDriver(did);
            if (driver == null || !driver.hasBehaviour(LambdaQuery.class)) {
                return Collections.emptySortedSet();
            }
            DriverHandler handler = driverService.createHandler(did);
            if (handler == null) {
                return Collections.emptySortedSet();
            }
            LambdaQuery query = handler.behaviour(LambdaQuery.class);
            if (query != null) {
                Supplier<SortedSet<OchSignal>> supplier = () -> new TreeSet<>(new DefaultOchSignalComparator());
                return query.queryLambdas(port).stream()
                        .flatMap(x -> OchSignal.toFlexGrid(x).stream())
                        .collect(Collectors.toCollection(supplier));
            } else {
                return Collections.emptySortedSet();
            }
        } catch (ItemNotFoundException e) {
            return Collections.emptySortedSet();
        }
    }

    private boolean isVlanEnabled(DeviceId device, PortNumber port) {
        try {
            // DriverHandler does not provide a way to check if a
            // behaviour is supported.
            Driver driver = driverService.getDriver(device);
            if (driver == null || !driver.hasBehaviour(VlanQuery.class)) {
                // device does not support this
                return false;
            }

            DriverHandler handler = driverService.createHandler(device);
            if (handler == null) {
                return false;
            }

            VlanQuery query = handler.behaviour(VlanQuery.class);
            return query != null && query.isEnabled(port);
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    private boolean isMplsEnabled(DeviceId device, PortNumber port) {
        try {
            // DriverHandler does not provide a way to check if a
            // behaviour is supported.
            Driver driver = driverService.getDriver(device);
            if (driver == null || !driver.hasBehaviour(MplsQuery.class)) {
                // device does not support this
                return false;
            }
            DriverHandler handler = driverService.createHandler(device);
            if (handler == null) {
                return false;
            }

            MplsQuery query = handler.behaviour(MplsQuery.class);
            return query != null && query.isEnabled(port);
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    private static List<VlanId> getEntireVlans() {
        return IntStream.range(0, MAX_VLAN_ID)
                .mapToObj(x -> VlanId.vlanId((short) x))
                .collect(Collectors.toList());
    }

    private static List<MplsLabel> getEntireMplsLabels() {
        // potentially many objects are created
        return IntStream.range(0, MAX_MPLS_LABEL)
                .mapToObj(MplsLabel::mplsLabel)
                .collect(Collectors.toList());
    }

    private static List<TributarySlot> getEntireOdu2TributarySlots() {
        return IntStream.rangeClosed(1, TOTAL_ODU2_TRIBUTARY_SLOTS)
                .mapToObj(TributarySlot::of)
                .collect(Collectors.toList());
    }
    private static List<TributarySlot> getEntireOdu4TributarySlots() {
        return IntStream.rangeClosed(1, TOTAL_ODU4_TRIBUTARY_SLOTS)
                .mapToObj(TributarySlot::of)
                .collect(Collectors.toList());
    }
}
