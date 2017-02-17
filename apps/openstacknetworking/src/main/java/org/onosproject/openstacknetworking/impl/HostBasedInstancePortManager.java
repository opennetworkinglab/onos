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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
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
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.slf4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;

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

    private final HostListener hostListener = new InternalHostListener();

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

    private boolean isValidHost(Host host) {
        return !host.ipAddresses().isEmpty() &&
                host.annotations().value(ANNOTATION_NETWORK_ID) != null &&
                host.annotations().value(ANNOTATION_PORT_ID) != null;
    }

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
            InstancePortEvent instPortEvent;
            switch (event.type()) {
                case HOST_UPDATED:
                    instPortEvent = new InstancePortEvent(
                            OPENSTACK_INSTANCE_PORT_UPDATED,
                            instPort);
                    log.debug("Instance port is updated: {}", instPort);
                    process(instPortEvent);
                    break;
                case HOST_ADDED:
                    instPortEvent = new InstancePortEvent(
                            OPENSTACK_INSTANCE_PORT_DETECTED,
                            instPort);
                    log.debug("Instance port is detected: {}", instPort);
                    process(instPortEvent);
                    break;
                case HOST_REMOVED:
                    instPortEvent = new InstancePortEvent(
                            OPENSTACK_INSTANCE_PORT_VANISHED,
                            instPort);
                    log.debug("Instance port is disabled: {}", instPort);
                    process(instPortEvent);
                    break;
                default:
                    break;
            }
        }
    }
}
