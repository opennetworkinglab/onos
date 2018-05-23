/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.Tools;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortEvent.Type;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_ENDED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_STARTED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_PORT_DETECTED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_PORT_UPDATED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_PORT_VANISHED;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_NETWORK_ID;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_PORT_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing host based instance ports.
 * It also provides instance port events for the hosts mapped to OpenStack VM interface.
 */
@Service
@Component(immediate = true)
public class HostBasedInstancePortManager
        extends ListenerRegistry<InstancePortEvent, InstancePortListener>
        implements InstancePortService {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private final InternalHostListener hostListener = new InternalHostListener();

    @Activate
    protected void activate() {
        hostService.addListener(hostListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostService.removeListener(hostListener);
        log.info("Stopped");
    }

    @Override
    public InstancePort instancePort(MacAddress macAddress) {
        Host host = hostService.getHost(HostId.hostId(macAddress));
        if (host == null || !isValidHost(host)) {
            return null;
        }
        return HostBasedInstancePort.of(host);
    }

    @Override
    public InstancePort instancePort(IpAddress ipAddress, String osNetId) {
        return Tools.stream(hostService.getHosts()).filter(this::isValidHost)
                .map(HostBasedInstancePort::of)
                .filter(instPort -> instPort.networkId().equals(osNetId))
                .filter(instPort -> instPort.ipAddress().equals(ipAddress))
                .findAny().orElse(null);
    }

    @Override
    public InstancePort instancePort(String osPortId) {
        return Tools.stream(hostService.getHosts()).filter(this::isValidHost)
                .map(HostBasedInstancePort::of)
                .filter(instPort -> instPort.portId().equals(osPortId))
                .findAny().orElse(null);
    }

    @Override
    public Set<InstancePort> instancePorts() {
        Set<InstancePort> instPors = Tools.stream(hostService.getHosts())
                .filter(this::isValidHost)
                .map(HostBasedInstancePort::of)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(instPors);
    }

    @Override
    public Set<InstancePort> instancePorts(String osNetId) {
        Set<InstancePort> instPors = Tools.stream(hostService.getHosts())
                .filter(this::isValidHost)
                .map(HostBasedInstancePort::of)
                .filter(instPort -> instPort.networkId().equals(osNetId))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(instPors);
    }

    @Override
    public void migrationPortAdded(InstancePort port) {
        hostListener.processEvent(OPENSTACK_INSTANCE_MIGRATION_STARTED, port);
    }

    @Override
    public void migrationPortRemoved(InstancePort port) {
        hostListener.processEvent(OPENSTACK_INSTANCE_MIGRATION_ENDED, port);
    }

    private boolean isValidHost(Host host) {
        return !host.ipAddresses().isEmpty() &&
                host.annotations().value(ANNOTATION_NETWORK_ID) != null &&
                host.annotations().value(ANNOTATION_PORT_ID) != null;
    }

    /**
     * An internal listener that listens host event generated by HostLocationTracker
     * in DistributedHostStore. The role of this listener is to convert host event
     * to instance port event and post to the subscribers that have interested on
     * this type of event.
     */
    private class InternalHostListener implements HostListener {

        @Override
        public boolean isRelevant(HostEvent event) {
            Host host = event.subject();
            if (!isValidHost(host)) {
                log.debug("Invalid host detected, ignore it {}", host);
                return false;
            }
            return true;
        }

        @Override
        public void event(HostEvent event) {
            InstancePort instPort = HostBasedInstancePort.of(event.subject());
            switch (event.type()) {
                case HOST_UPDATED:
                    processEvent(OPENSTACK_INSTANCE_PORT_UPDATED, instPort);
                    break;
                case HOST_ADDED:
                    processEvent(OPENSTACK_INSTANCE_PORT_DETECTED, instPort);
                    break;
                case HOST_REMOVED:
                    processEvent(OPENSTACK_INSTANCE_PORT_VANISHED, instPort);
                    break;
                default:
                    break;
            }
        }

        private void processEvent(Type type, InstancePort port) {
            Map<Type, String> eventMap = Maps.newConcurrentMap();
            eventMap.put(OPENSTACK_INSTANCE_PORT_UPDATED, "updated");
            eventMap.put(OPENSTACK_INSTANCE_PORT_DETECTED, "detected");
            eventMap.put(OPENSTACK_INSTANCE_PORT_VANISHED, "disabled");
            eventMap.put(OPENSTACK_INSTANCE_MIGRATION_STARTED, "detected");
            eventMap.put(OPENSTACK_INSTANCE_MIGRATION_ENDED, "disabled");

            InstancePortEvent instPortEvent = new InstancePortEvent(type, port);
            log.debug("Instance port is {}: {}", eventMap.get(type), port);
            process(instPortEvent);
        }
    }
}
