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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages which interfaces are part of the router when the configuration is
 * updated, and handles the provisioning/unprovisioning of interfaces when they
 * are added/removed.
 */
public class RouterInterfaceManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Consumer<Interface> provisioner;
    private final Consumer<Interface> unprovisioner;

    private Set<String> configuredInterfaces = Collections.emptySet();
    private Set<Interface> provisioned = new HashSet<>();

    private InterfaceService interfaceService;
    private InterfaceListener listener = new InternalInterfaceListener();

    private final DeviceId routerDeviceId;

    /**
     * Creates a new router interface manager.
     *
     * @param deviceId router device ID
     * @param configuredInterfaces names of interfaces configured for this router
     * @param interfaceService interface service
     * @param provisioner consumer that will provision new interfaces
     * @param unprovisioner consumer that will unprovision old interfaces
     */
    public RouterInterfaceManager(DeviceId deviceId,
                                  Set<String> configuredInterfaces,
                                  InterfaceService interfaceService,
                                  Consumer<Interface> provisioner,
                                  Consumer<Interface> unprovisioner) {
        this.routerDeviceId = checkNotNull(deviceId);
        this.provisioner = checkNotNull(provisioner);
        this.unprovisioner = checkNotNull(unprovisioner);
        this.interfaceService = checkNotNull(interfaceService);
        this.configuredInterfaces = checkNotNull(configuredInterfaces);

        provision();

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
     * Retrieves the set of configured interface names.
     *
     * @return interface names
     */
    public Set<String> configuredInterfaces() {
        return configuredInterfaces;
    }

    /**
     * Changes the set of interfaces configured on the router.
     *
     * @param newConfiguredInterfaces new set of router interfaces
     */
    public void changeConfiguredInterfaces(Set<String> newConfiguredInterfaces) {
        Set<String> oldConfiguredInterfaces = configuredInterfaces;
        configuredInterfaces = ImmutableSet.copyOf(newConfiguredInterfaces);

        if (newConfiguredInterfaces.isEmpty() && !oldConfiguredInterfaces.isEmpty()) {
            // Reverted to using all interfaces. Provision interfaces that
            // weren't previously in the configured list
            getInterfacesForDevice(routerDeviceId)
                    .filter(intf -> !oldConfiguredInterfaces.contains(intf.name()))
                    .forEach(this::provision);
        } else if (!newConfiguredInterfaces.isEmpty() && oldConfiguredInterfaces.isEmpty()) {
            // Began using an interface list. Unprovision interfaces that
            // are not in the new interface list.
            getInterfacesForDevice(routerDeviceId)
                    .filter(intf -> !newConfiguredInterfaces.contains(intf.name()))
                    .forEach(this::unprovision);
        } else {
            // The existing interface list was changed.
            Set<String> toUnprovision = Sets.difference(oldConfiguredInterfaces, newConfiguredInterfaces);
            Set<String> toProvision = Sets.difference(newConfiguredInterfaces, oldConfiguredInterfaces);

            toUnprovision.forEach(name ->
                    getInterfacesForDevice(routerDeviceId)
                            .filter(intf -> intf.name().equals(name))
                            .findFirst()
                            .ifPresent(this::unprovision)
            );

            toProvision.forEach(name ->
                    getInterfacesForDevice(routerDeviceId)
                            .filter(intf -> intf.name().equals(name))
                            .findFirst()
                            .ifPresent(this::provision)
            );
        }

        configuredInterfaces = newConfiguredInterfaces;
    }

    private void provision() {
        getInterfacesForDevice(routerDeviceId)
                .filter(this::shouldUse)
                .forEach(this::provision);
    }

    private void unprovision() {
        getInterfacesForDevice(routerDeviceId)
                .filter(this::shouldUse)
                .forEach(this::unprovision);
    }

    private void provision(Interface intf) {
        if (!provisioned.contains(intf) && shouldUse(intf)) {
            log.info("Provisioning interface {}", intf);
            provisioner.accept(intf);
            provisioned.add(intf);
        }
    }

    private void unprovision(Interface intf) {
        if (provisioned.contains(intf)) {
            log.info("Unprovisioning interface {}", intf);
            unprovisioner.accept(intf);
            provisioned.remove(intf);
        }
    }

    private boolean shouldUse(Interface intf) {
        return configuredInterfaces.isEmpty() || configuredInterfaces.contains(intf.name());
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
