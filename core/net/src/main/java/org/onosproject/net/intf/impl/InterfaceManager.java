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

package org.onosproject.net.intf.impl;


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.BasicNetworkConfigService;
import org.onosproject.net.config.ConfigException;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.InterfaceConfig;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceAdminService;
import org.onosproject.net.intf.InterfaceEvent;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

/**
 * Manages the inventory of interfaces in the system.
 */
@Component(immediate = true, service = { InterfaceService.class, InterfaceAdminService.class })
public class InterfaceManager extends ListenerRegistry<InterfaceEvent, InterfaceListener>
        implements InterfaceService, InterfaceAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final Class<ConnectPoint> SUBJECT_CLASS = ConnectPoint.class;
    private static final Class<InterfaceConfig> CONFIG_CLASS = InterfaceConfig.class;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService configService;

    //Dependency to ensure subject factories are properly initialized
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected BasicNetworkConfigService basicNetworkConfigService;

    private final InternalConfigListener listener = new InternalConfigListener();

    private final Map<ConnectPoint, Set<Interface>> interfaces = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        configService.addListener(listener);

        // TODO address concurrency issues here
        for (ConnectPoint subject : configService.getSubjects(SUBJECT_CLASS, CONFIG_CLASS)) {
            InterfaceConfig config = configService.getConfig(subject, CONFIG_CLASS);

            if (config != null) {
                updateInterfaces(config);
            }
        }

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        configService.removeListener(listener);

        log.info("Stopped");
    }

    @Override
    public Set<Interface> getInterfaces() {
        return interfaces.values()
                .stream()
                .flatMap(set -> set.stream())
                .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
    }

    @Override
    public Interface getInterfaceByName(ConnectPoint connectPoint, String name) {
        Optional<Interface> intf =
                interfaces.getOrDefault(connectPoint, Collections.emptySet())
                .stream()
                .filter(i -> i.name().equals(name))
                .findAny();

        return intf.orElse(null);
    }

    @Override
    public Set<Interface> getInterfacesByPort(ConnectPoint port) {
        Set<Interface> intfs = interfaces.get(port);
        if (intfs == null) {
            return Collections.emptySet();
        }
        return ImmutableSet.copyOf(intfs);
    }

    @Override
    public Set<Interface> getInterfacesByIp(IpAddress ip) {
        return interfaces.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(intf -> intf.ipAddressesList()
                        .stream()
                        .anyMatch(ia -> ia.ipAddress().equals(ip)))
                .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
    }

    @Override
    public Interface getMatchingInterface(IpAddress ip) {
        return getMatchingInterfacesStream(ip).findFirst().orElse(null);
    }

    @Override
    public Set<Interface> getMatchingInterfaces(IpAddress ip) {
        return getMatchingInterfacesStream(ip).collect(toSet());
    }

    private Stream<Interface> getMatchingInterfacesStream(IpAddress ip) {
        return interfaces.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(intf -> intf.ipAddressesList()
                        .stream()
                        .anyMatch(intfIp -> intfIp.subnetAddress().contains(ip)));
    }

    @Override
    public Set<Interface> getInterfacesByVlan(VlanId vlan) {
        return interfaces.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(intf -> intf.vlan().equals(vlan))
                .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
    }

    /**
     * Returns untagged VLAN configured on given connect point.
     * <p>
     * Only returns the first match if there are multiple untagged VLAN configured
     * on the connect point.
     *
     * @param connectPoint connect point
     * @return untagged VLAN or null if not configured
     */
    @Override
    public VlanId getUntaggedVlanId(ConnectPoint connectPoint) {
        return getInterfacesByPort(connectPoint).stream()
                .filter(intf -> !intf.vlanUntagged().equals(VlanId.NONE))
                .map(Interface::vlanUntagged)
                .findFirst().orElse(null);
    }

    /**
     * Returns tagged VLAN configured on given connect point.
     * <p>
     * Returns all matches if there are multiple tagged VLAN configured
     * on the connect point.
     *
     * @param connectPoint connect point
     * @return tagged VLAN or empty set if not configured
     */
    @Override
    public Set<VlanId> getTaggedVlanId(ConnectPoint connectPoint) {
        Set<Interface> interfaces = getInterfacesByPort(connectPoint);
        return interfaces.stream()
                .map(Interface::vlanTagged)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Returns native VLAN configured on given connect point.
     * <p>
     * Only returns the first match if there are multiple native VLAN configured
     * on the connect point.
     *
     * @param connectPoint connect point
     * @return native VLAN or null if not configured
     */
    @Override
    public VlanId getNativeVlanId(ConnectPoint connectPoint) {
        Set<Interface> interfaces = getInterfacesByPort(connectPoint);
        return interfaces.stream()
                .filter(intf -> !intf.vlanNative().equals(VlanId.NONE))
                .map(Interface::vlanNative)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean isConfigured(ConnectPoint connectPoint) {
        Set<Interface> intfs = interfaces.get(connectPoint);
        if (intfs == null) {
            return false;
        }
        for (Interface intf : intfs) {
            if (!intf.ipAddressesList().isEmpty() || intf.vlan() != VlanId.NONE
                    || intf.vlanNative() != VlanId.NONE
                    || intf.vlanUntagged() != VlanId.NONE
                    || !intf.vlanTagged().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean inUse(VlanId vlanId) {
        for (Set<Interface> intfs : interfaces.values()) {
            for (Interface intf : intfs) {
                if (intf.vlan().equals(vlanId)
                        || intf.vlanNative().equals(vlanId)
                        || intf.vlanUntagged().equals(vlanId)
                        || intf.vlanTagged().contains(vlanId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateInterfaces(InterfaceConfig intfConfig) {
        try {
            Set<Interface> old = interfaces.put(intfConfig.subject(),
                    Sets.newHashSet(intfConfig.getInterfaces()));

            if (old == null) {
                old = Collections.emptySet();
            }

            for (Interface intf : intfConfig.getInterfaces()) {
                if (intf.name().equals(Interface.NO_INTERFACE_NAME)) {
                    process(new InterfaceEvent(InterfaceEvent.Type.INTERFACE_ADDED, intf));
                } else {
                    Optional<Interface> oldIntf = findInterface(intf, old);
                    if (oldIntf.isPresent()) {
                        old.remove(oldIntf.get());
                        if (!oldIntf.get().equals(intf)) {
                            process(new InterfaceEvent(InterfaceEvent.Type.INTERFACE_UPDATED, intf, oldIntf.get()));
                        }
                    } else {
                        process(new InterfaceEvent(InterfaceEvent.Type.INTERFACE_ADDED, intf));
                    }
                }
            }

            for (Interface intf : old) {
                if (!intf.name().equals(Interface.NO_INTERFACE_NAME)) {
                    process(new InterfaceEvent(InterfaceEvent.Type.INTERFACE_REMOVED, intf));
                }
            }
        } catch (ConfigException e) {
            log.error("Error in interface config", e);
        }
    }

    private Optional<Interface> findInterface(Interface intf, Set<Interface> set) {
        return set.stream().filter(i -> i.name().equals(intf.name())).findAny();
    }

    private void removeInterfaces(ConnectPoint port) {
        Set<Interface> old = interfaces.remove(port);

        old.stream()
                .filter(i -> !i.name().equals(Interface.NO_INTERFACE_NAME))
                .forEach(i -> process(new InterfaceEvent(InterfaceEvent.Type.INTERFACE_REMOVED, i)));
    }

    @Override
    public void add(Interface intf) {
        InterfaceConfig config =
                configService.addConfig(intf.connectPoint(), CONFIG_CLASS);

        config.addInterface(intf);

        configService.applyConfig(intf.connectPoint(), CONFIG_CLASS, config.node());
    }

    @Override
    public boolean remove(ConnectPoint connectPoint, String name) {
        InterfaceConfig config = configService.addConfig(connectPoint, CONFIG_CLASS);
        config.removeInterface(name);

        try {
            if (config.getInterfaces().isEmpty()) {
                configService.removeConfig(connectPoint, CONFIG_CLASS);
            } else {
                configService.applyConfig(connectPoint, CONFIG_CLASS, config.node());
            }
        } catch (ConfigException e) {
            log.error("Error reading interfaces JSON", e);
            return false;
        }

        return true;
    }

    /**
     * Listener for network config events.
     */
    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == CONFIG_CLASS) {
                switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    event.config().ifPresent(config -> updateInterfaces((InterfaceConfig) config));
                    break;
                case CONFIG_REMOVED:
                    removeInterfaces((ConnectPoint) event.subject());
                    break;
                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED:
                default:
                    break;
                }
            }
        }
    }
}
