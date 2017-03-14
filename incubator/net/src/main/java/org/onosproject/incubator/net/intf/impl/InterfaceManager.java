/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.incubator.net.intf.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.incubator.net.config.basics.InterfaceConfig;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceAdminService;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

/**
 * Manages the inventory of interfaces in the system.
 */
@Service
@Component(immediate = true)
public class InterfaceManager extends ListenerRegistry<InterfaceEvent, InterfaceListener>
        implements InterfaceService, InterfaceAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final Class<ConnectPoint> SUBJECT_CLASS = ConnectPoint.class;
    private static final Class<InterfaceConfig> CONFIG_CLASS = InterfaceConfig.class;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

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
                    InterfaceConfig config =
                            configService.getConfig((ConnectPoint) event.subject(), InterfaceConfig.class);
                    updateInterfaces(config);
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
