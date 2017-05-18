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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkStore;
import org.onosproject.openstacknetworking.api.OpenstackNetworkStoreDelegate;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing OpenStack network,
 * subnet, and port.
 */
@Service
@Component(immediate = true)
public class OpenstackNetworkManager
        extends ListenerRegistry<OpenstackNetworkEvent, OpenstackNetworkListener>
        implements OpenstackNetworkAdminService, OpenstackNetworkService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_NETWORK  = "OpenStack network %s %s";
    private static final String MSG_SUBNET  = "OpenStack subnet %s %s";
    private static final String MSG_PORT = "OpenStack port %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_NETWORK  = "OpenStack network cannot be null";
    private static final String ERR_NULL_NETWORK_ID  = "OpenStack network ID cannot be null";
    private static final String ERR_NULL_NETWORK_NAME  = "OpenStack network name cannot be null";
    private static final String ERR_NULL_SUBNET = "OpenStack subnet cannot be null";
    private static final String ERR_NULL_SUBNET_ID = "OpenStack subnet ID cannot be null";
    private static final String ERR_NULL_SUBNET_NET_ID = "OpenStack subnet network ID cannot be null";
    private static final String ERR_NULL_SUBNET_CIDR = "OpenStack subnet CIDR cannot be null";
    private static final String ERR_NULL_PORT = "OpenStack port cannot be null";
    private static final String ERR_NULL_PORT_ID = "OpenStack port ID cannot be null";
    private static final String ERR_NULL_PORT_NET_ID = "OpenStack port network ID cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkStore osNetworkStore;

    private final OpenstackNetworkStoreDelegate delegate = new InternalNetworkStoreDelegate();

    @Activate
    protected void activate() {
        coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);
        osNetworkStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osNetworkStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createNetwork(Network osNet) {
        checkNotNull(osNet, ERR_NULL_NETWORK);
        checkArgument(!Strings.isNullOrEmpty(osNet.getId()), ERR_NULL_NETWORK_ID);

        osNetworkStore.createNetwork(osNet);
        log.info(String.format(MSG_NETWORK, osNet.getName(), MSG_CREATED));
    }

    @Override
    public void updateNetwork(Network osNet) {
        checkNotNull(osNet, ERR_NULL_NETWORK);
        checkArgument(!Strings.isNullOrEmpty(osNet.getId()), ERR_NULL_NETWORK_ID);

        osNetworkStore.updateNetwork(osNet);
        log.info(String.format(MSG_NETWORK, osNet.getId(), MSG_UPDATED));
    }

    @Override
    public void removeNetwork(String netId) {
        checkArgument(!Strings.isNullOrEmpty(netId), ERR_NULL_NETWORK_ID);
        synchronized (this) {
            if (isNetworkInUse(netId)) {
                final String error = String.format(MSG_NETWORK, netId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            Network osNet = osNetworkStore.removeNetwork(netId);
            if (osNet != null) {
                log.info(String.format(MSG_NETWORK, osNet.getName(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void createSubnet(Subnet osSubnet) {
        checkNotNull(osSubnet, ERR_NULL_SUBNET);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getId()), ERR_NULL_SUBNET_ID);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getNetworkId()), ERR_NULL_SUBNET_NET_ID);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getCidr()), ERR_NULL_SUBNET_CIDR);

        osNetworkStore.createSubnet(osSubnet);
        log.info(String.format(MSG_SUBNET, osSubnet.getCidr(), MSG_CREATED));
    }

    @Override
    public void updateSubnet(Subnet osSubnet) {
        checkNotNull(osSubnet, ERR_NULL_SUBNET);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getId()), ERR_NULL_SUBNET_ID);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getNetworkId()), ERR_NULL_SUBNET_NET_ID);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getCidr()), ERR_NULL_SUBNET_CIDR);

        osNetworkStore.updateSubnet(osSubnet);
        log.info(String.format(MSG_SUBNET, osSubnet.getCidr(), MSG_UPDATED));
    }

    @Override
    public void removeSubnet(String subnetId) {
        checkArgument(!Strings.isNullOrEmpty(subnetId), ERR_NULL_SUBNET_ID);
        synchronized (this) {
            if (isSubnetInUse(subnetId)) {
                final String error = String.format(MSG_SUBNET, subnetId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            Subnet osSubnet = osNetworkStore.removeSubnet(subnetId);
            if (osSubnet != null) {
                log.info(String.format(MSG_SUBNET, osSubnet.getCidr(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void createPort(Port osPort) {
        checkNotNull(osPort, ERR_NULL_PORT);
        checkArgument(!Strings.isNullOrEmpty(osPort.getId()), ERR_NULL_PORT_ID);
        checkArgument(!Strings.isNullOrEmpty(osPort.getNetworkId()), ERR_NULL_PORT_NET_ID);

        osNetworkStore.createPort(osPort);
        log.info(String.format(MSG_PORT, osPort.getId(), MSG_CREATED));
    }

    @Override
    public void updatePort(Port osPort) {
        checkNotNull(osPort, ERR_NULL_PORT);
        checkArgument(!Strings.isNullOrEmpty(osPort.getId()), ERR_NULL_PORT_ID);
        checkArgument(!Strings.isNullOrEmpty(osPort.getNetworkId()), ERR_NULL_PORT_NET_ID);

        osNetworkStore.updatePort(osPort);
        log.info(String.format(MSG_PORT, osPort.getId(), MSG_UPDATED));
    }

    @Override
    public void removePort(String portId) {
        checkArgument(!Strings.isNullOrEmpty(portId), ERR_NULL_PORT_ID);
        synchronized (this) {
            if (isPortInUse(portId)) {
                final String error = String.format(MSG_PORT, portId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            Port osPort = osNetworkStore.removePort(portId);
            if (osPort != null) {
                log.info(String.format(MSG_PORT, osPort.getId(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        osNetworkStore.clear();
    }

    @Override
    public Network network(String netId) {
        checkArgument(!Strings.isNullOrEmpty(netId), ERR_NULL_NETWORK_ID);
        return osNetworkStore.network(netId);
    }

    @Override
    public Set<Network> networks() {
        return osNetworkStore.networks();
    }

    @Override
    public Subnet subnet(String subnetId) {
        checkArgument(!Strings.isNullOrEmpty(subnetId), ERR_NULL_SUBNET_ID);
        return osNetworkStore.subnet(subnetId);
    }

    @Override
    public Set<Subnet> subnets() {
        return osNetworkStore.subnets();
    }

    @Override
    public Set<Subnet> subnets(String netId) {
        Set<Subnet> osSubnets = osNetworkStore.subnets().stream()
                .filter(subnet -> Objects.equals(subnet.getNetworkId(), netId))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osSubnets);
    }

    @Override
    public Port port(String portId) {
        checkArgument(!Strings.isNullOrEmpty(portId), ERR_NULL_PORT_ID);
        return osNetworkStore.port(portId);
    }

    @Override
    public Port port(org.onosproject.net.Port port) {
        String portName = port.annotations().value(PORT_NAME);
        if (Strings.isNullOrEmpty(portName)) {
            return null;
        }
        Optional<Port> osPort = osNetworkStore.ports()
                .stream()
                .filter(p -> p.getId().contains(portName.substring(3)))
                .findFirst();
        return osPort.isPresent() ? osPort.get() : null;
    }

    @Override
    public Set<Port> ports() {
        return osNetworkStore.ports();
    }

    @Override
    public Set<Port> ports(String netId) {
        Set<Port> osPorts = osNetworkStore.ports().stream()
                .filter(port -> Objects.equals(port.getNetworkId(), netId))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osPorts);
    }

    private boolean isNetworkInUse(String netId) {
        return !subnets(netId).isEmpty() && !ports(netId).isEmpty();
    }

    private boolean isSubnetInUse(String subnetId) {
        // TODO add something if needed
        return false;
    }

    private boolean isPortInUse(String portId) {
        // TODO add something if needed
        return false;
    }

    private class InternalNetworkStoreDelegate implements OpenstackNetworkStoreDelegate {

        @Override
        public void notify(OpenstackNetworkEvent event) {
            if (event != null) {
                log.trace("send oepnstack switching event {}", event);
                process(event);
            }
        }
    }
}
