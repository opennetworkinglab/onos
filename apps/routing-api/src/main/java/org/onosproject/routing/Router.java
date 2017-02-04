/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.routing;

import com.google.common.collect.Sets;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the configuration and provisioning of a single-device router.
 * It maintains which interfaces are part of the router when the configuration
 * changes, and handles the provisioning/unprovisioning of interfaces when they
 * are added/removed.
 */
public class Router {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Consumer<InterfaceProvisionRequest> provisioner;
    private final Consumer<InterfaceProvisionRequest> unprovisioner;

    private RouterInfo info;

    private Set<Interface> provisioned = new HashSet<>();

    private InterfaceService interfaceService;
    private InterfaceListener listener = new InternalInterfaceListener();

    private AsyncDeviceFetcher asyncDeviceFetcher;

    private volatile boolean deviceAvailable = false;

    /**
     * Creates a new router interface manager.
     *
     * @param info router configuration information
     * @param interfaceService interface service
     * @param deviceService device service
     * @param provisioner consumer that will provision new interfaces
     * @param unprovisioner consumer that will unprovision old interfaces
     */
    public Router(RouterInfo info,
                  InterfaceService interfaceService,
                  DeviceService deviceService,
                  Consumer<InterfaceProvisionRequest> provisioner,
                  Consumer<InterfaceProvisionRequest> unprovisioner) {
        this.info = checkNotNull(info);
        this.provisioner = checkNotNull(provisioner);
        this.unprovisioner = checkNotNull(unprovisioner);
        this.interfaceService = checkNotNull(interfaceService);

        this.asyncDeviceFetcher = AsyncDeviceFetcher.create(deviceService);
        asyncDeviceFetcher.getDevice(info.deviceId())
                .thenAccept(deviceId1 -> {
                    deviceAvailable = true;
                    provision();
                }).whenComplete((v, t) -> {
                    if (t != null) {
                        log.error("Error provisioning: ", t);
                    }
                });

        interfaceService.addListener(listener);
    }

    /**
     * Cleans up the router and unprovisions all interfaces.
     */
    public void cleanup() {
        interfaceService.removeListener(listener);

        unprovision();
    }

    /**
     * Retrieves the router configuration information.
     *
     * @return router configuration information
     */
    public RouterInfo info() {
        return info;
    }

    /**
     * Changes the router configuration.
     *
     * @param newConfig new configuration
     */
    public void changeConfiguration(RouterInfo newConfig) {
        Set<String> oldConfiguredInterfaces = info.interfaces();
        info = newConfig;
        Set<String> newConfiguredInterfaces = info.interfaces();

        if (newConfiguredInterfaces.isEmpty() && !oldConfiguredInterfaces.isEmpty()) {
            // Reverted to using all interfaces. Provision interfaces that
            // weren't previously in the configured list
            getInterfacesForDevice(info.deviceId())
                    .filter(intf -> !oldConfiguredInterfaces.contains(intf.name()))
                    .forEach(this::provision);
        } else if (!newConfiguredInterfaces.isEmpty() && oldConfiguredInterfaces.isEmpty()) {
            // Began using an interface list. Unprovision interfaces that
            // are not in the new interface list.
            getInterfacesForDevice(info.deviceId())
                    .filter(intf -> !newConfiguredInterfaces.contains(intf.name()))
                    .forEach(this::unprovision);
        } else {
            // The existing interface list was changed.
            Set<String> toUnprovision = Sets.difference(oldConfiguredInterfaces, newConfiguredInterfaces);
            Set<String> toProvision = Sets.difference(newConfiguredInterfaces, oldConfiguredInterfaces);

            toUnprovision.forEach(name ->
                    getInterfacesForDevice(info.deviceId())
                            .filter(intf -> intf.name().equals(name))
                            .findFirst()
                            .ifPresent(this::unprovision)
            );

            toProvision.forEach(name ->
                    getInterfacesForDevice(info.deviceId())
                            .filter(intf -> intf.name().equals(name))
                            .findFirst()
                            .ifPresent(this::provision)
            );
        }
    }

    private void provision() {
        getInterfacesForDevice(info.deviceId())
                .filter(this::shouldProvision)
                .forEach(this::provision);
    }

    private void unprovision() {
        getInterfacesForDevice(info.deviceId())
                .filter(this::shouldProvision)
                .forEach(this::unprovision);
    }

    private void provision(Interface intf) {
        if (!provisioned.contains(intf) && shouldProvision(intf)) {
            log.info("Provisioning interface {}", intf);
            provisioner.accept(InterfaceProvisionRequest.of(info, intf));
            provisioned.add(intf);
        }
    }

    private void unprovision(Interface intf) {
        if (provisioned.contains(intf)) {
            log.info("Unprovisioning interface {}", intf);
            unprovisioner.accept(InterfaceProvisionRequest.of(info, intf));
            provisioned.remove(intf);
        }
    }

    private boolean shouldProvision(Interface intf) {
        return deviceAvailable &&
                (info.interfaces().isEmpty() || info.interfaces().contains(intf.name()));
    }

    private Stream<Interface> getInterfacesForDevice(DeviceId deviceId) {
        return interfaceService.getInterfaces().stream()
                .filter(intf -> intf.connectPoint().deviceId().equals(deviceId));
    }

    private class InternalInterfaceListener implements InterfaceListener {
        @Override
        public void event(InterfaceEvent event) {
            Interface intf = event.subject();
            switch (event.type()) {
            case INTERFACE_ADDED:
                provision(intf);
                break;
            case INTERFACE_UPDATED:
                // TODO
                break;
            case INTERFACE_REMOVED:
                unprovision(intf);
                break;
            default:
                break;
            }
        }
    }
}
