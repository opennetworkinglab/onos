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
package org.onosproject.net.resource.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.behaviour.MplsQuery;
import org.onosproject.net.behaviour.TributarySlotQuery;
import org.onosproject.net.behaviour.VlanQuery;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.ResourceAdminService;
import org.onosproject.net.config.basics.BandwidthCapacity;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceQueryService;
import org.onosproject.net.resource.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of DeviceListener registering devices as resources.
 */
final class ResourceDeviceListener implements DeviceListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceDeviceListener.class);

    private final ResourceAdminService adminService;
    private final ResourceQueryService resourceService;
    private final DeviceService deviceService;
    private final MastershipService mastershipService;
    private final DriverService driverService;
    private final NetworkConfigService netcfgService;
    private final ExecutorService executor;


    /**
     * Creates an instance with the specified ResourceAdminService and ExecutorService.
     *
     * @param adminService instance invoked to register resources
     * @param resourceService {@link ResourceQueryService} to be used
     * @param deviceService {@link DeviceService} to be used
     * @param mastershipService {@link MastershipService} to be used
     * @param driverService {@link DriverService} to be used
     * @param netcfgService {@link NetworkConfigService} to be used.
     * @param executor executor used for processing resource registration
     */
    ResourceDeviceListener(ResourceAdminService adminService, ResourceQueryService resourceService,
                           DeviceService deviceService, MastershipService mastershipService,
                           DriverService driverService, NetworkConfigService netcfgService,
                           ExecutorService executor) {
        this.adminService = checkNotNull(adminService);
        this.resourceService = checkNotNull(resourceService);
        this.deviceService = checkNotNull(deviceService);
        this.mastershipService = checkNotNull(mastershipService);
        this.driverService = checkNotNull(driverService);
        this.netcfgService = checkNotNull(netcfgService);
        this.executor = checkNotNull(executor);
    }

    @Override
    public void event(DeviceEvent event) {
        executor.execute(() -> {
            Device device = event.subject();
            // registration happens only when the caller is the master of the device
            if (!mastershipService.isLocalMaster(device.id())) {
                return;
            }

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
        });
    }

    private void registerDeviceResource(Device device) {
        boolean success = adminService.register(Resources.discrete(device.id()).resource());
        if (!success) {
            log.error("Failed to register Device: {}", device.id());
        }
    }

    private void unregisterDeviceResource(Device device) {
        DiscreteResource devResource = Resources.discrete(device.id()).resource();
        List<Resource> allResources = getDescendantResources(devResource);
        adminService.unregister(Lists.transform(allResources, Resource::id));
    }

    private void registerPortResource(Device device, Port port) {
        Resource portPath = Resources.discrete(device.id(), port.number()).resource();
        if (!adminService.register(portPath)) {
            log.error("Failed to register Port: {}", portPath.id());
        }

        queryBandwidth(device.id(), port.number())
            .map(bw -> portPath.child(Bandwidth.class, bw.bps()))
            .map(adminService::register)
            .ifPresent(success -> {
                if (!success) {
                    log.error("Failed to register Bandwidth for {}", portPath.id());
                }
            });

        // for VLAN IDs
        Set<VlanId> vlans = queryVlanIds(device.id(), port.number());
        if (!vlans.isEmpty()) {
            boolean success = adminService.register(vlans.stream()
                .map(portPath::child)
                .collect(Collectors.toList()));
            if (!success) {
                log.error("Failed to register VLAN IDs for {}", portPath.id());
            }
        }

        // for MPLS labels
        Set<MplsLabel> mplsLabels = queryMplsLabels(device.id(), port.number());
        if (!mplsLabels.isEmpty()) {
            boolean success = adminService.register(mplsLabels.stream()
                .map(portPath::child)
                .collect(Collectors.toList()));
            if (!success) {
                log.error("Failed to register MPLS Labels for {}", portPath.id());
            }
        }

        // for Lambdas
        Set<OchSignal> lambdas = queryLambdas(device.id(), port.number());
        if (!lambdas.isEmpty()) {
            boolean success = adminService.register(lambdas.stream()
                .map(portPath::child)
                .collect(Collectors.toList()));
            if (!success) {
                log.error("Failed to register lambdas for {}", portPath.id());
            }
        }

        // for Tributary slots
        Set<TributarySlot> tSlots = queryTributarySlots(device.id(), port.number());
        if (!tSlots.isEmpty()) {
            boolean success = adminService.register(tSlots.stream()
                .map(portPath::child)
                .collect(Collectors.toList()));
            if (!success) {
                log.error("Failed to register tributary slots for {}", portPath.id());
            }
        }
    }

    private void unregisterPortResource(Device device, Port port) {
        DiscreteResource portResource = Resources.discrete(device.id(), port.number()).resource();
        List<Resource> allResources = getDescendantResources(portResource);
        adminService.unregister(Lists.transform(allResources, Resource::id));
    }

    // Returns list of all descendant resources of given resource, including itself.
    private List<Resource> getDescendantResources(DiscreteResource parent) {
        LinkedList<Resource> allResources = new LinkedList<>();
        allResources.add(parent);

        Set<Resource> nextResources = resourceService.getRegisteredResources(parent.id());
        while (!nextResources.isEmpty()) {
            Set<Resource> currentResources = nextResources;
            // resource list should be ordered from leaf to root
            allResources.addAll(0, currentResources);

            nextResources = currentResources.stream()
                    .filter(r -> r instanceof DiscreteResource)
                    .map(r -> (DiscreteResource) r)
                    .flatMap(r -> resourceService.getRegisteredResources(r.id()).stream())
                    .collect(Collectors.toSet());
        }

        return allResources;
    }

    /**
     * Query bandwidth capacity on a port.
     *
     * @param did {@link DeviceId}
     * @param number {@link PortNumber}
     * @return bandwidth capacity
     */
    private Optional<Bandwidth> queryBandwidth(DeviceId did, PortNumber number) {
        // Check and use netcfg first.
        ConnectPoint cp = new ConnectPoint(did, number);
        BandwidthCapacity config = netcfgService.getConfig(cp, BandwidthCapacity.class);
        if (config != null) {
            log.trace("Registering configured bandwidth {} for {}/{}", config.capacity(), did, number);
            return Optional.of(config.capacity());
        }

        // populate bandwidth value, assuming portSpeed == bandwidth
        Port port = deviceService.getPort(did, number);
        if (port != null) {
            return Optional.of(Bandwidth.mbps(port.portSpeed()));
        }
        return Optional.empty();
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
                        .flatMap(ResourceDeviceListener::toResourceGrid)
                        .collect(ImmutableSet.toImmutableSet());
            } else {
                return Collections.emptySet();
            }
        } catch (ItemNotFoundException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Convert {@link OchSignal} into gridtype used to track Resource.
     *
     * @param ochSignal {@link OchSignal}
     * @return {@code ochSignal} mapped to Stream of flex grid slots with 6.25 GHz spacing
     *         and 12.5 GHz slot width.
     */
    private static Stream<OchSignal> toResourceGrid(OchSignal ochSignal) {
        if (ochSignal.gridType() != GridType.FLEX) {
            return OchSignal.toFlexGrid(ochSignal).stream();
        }
        if (ochSignal.gridType() == GridType.FLEX &&
            ochSignal.channelSpacing() == ChannelSpacing.CHL_6P25GHZ &&
            ochSignal.slotGranularity() == 1) {
                // input was already flex grid slots with 6.25 GHz spacing and 12.5 GHz slot width.
                return Stream.of(ochSignal);
        }
        // FIXME handle FLEX but not 6.25 GHz spacing or 12.5 GHz slot width case.
        log.error("Converting {} to resource tracking grid not supported yet.", ochSignal);
        return Stream.<OchSignal>builder().build();
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
