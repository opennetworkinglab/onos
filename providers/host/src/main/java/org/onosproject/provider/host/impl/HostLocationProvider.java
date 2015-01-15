/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.provider.host.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Dictionary;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * end-station hosts.
 */
@Component(immediate = true)
public class HostLocationProvider extends AbstractProvider implements HostProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService pktService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private HostProviderService providerService;

    private final InternalHostProvider processor = new InternalHostProvider();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    private ApplicationId appId;

    @Property(name = "hostRemovalEnabled", boolValue = true,
            label = "Enable host removal on port/device down events")
    private boolean hostRemovalEnabled = true;


    /**
     * Creates an OpenFlow host provider.
     */
    public HostLocationProvider() {
        super(new ProviderId("of", "org.onosproject.provider.host"));
    }

    @Activate
    public void activate(ComponentContext context) {
        appId =
            coreService.registerApplication("org.onosproject.provider.host");

        modified(context);
        providerService = providerRegistry.register(this);
        pktService.addProcessor(processor, 1);
        deviceService.addListener(deviceListener);

        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_ARP);
        pktService.requestPackets(selectorBuilder.build(),
                                  PacketPriority.CONTROL, appId);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        pktService.removeProcessor(processor);
        deviceService.removeListener(deviceListener);
        providerService = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary properties = context.getProperties();
        try {
            String flag = (String) properties.get("hostRemovalEnabled");
            if (flag != null) {
                hostRemovalEnabled = flag.equals("true");
            }
        } catch (Exception e) {
            hostRemovalEnabled = true;
        }
        log.info("Host removal is {}", hostRemovalEnabled ? "enabled" : "disabled");
    }

    @Override
    public void triggerProbe(Host host) {
        log.info("Triggering probe on device {}", host);
    }

    private class InternalHostProvider implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context == null) {
                return;
            }
            Ethernet eth = context.inPacket().parsed();

            if (eth == null) {
                return;
            }

            VlanId vlan = VlanId.vlanId(eth.getVlanID());
            ConnectPoint heardOn = context.inPacket().receivedFrom();

            // If this is not an edge port, bail out.
            Topology topology = topologyService.currentTopology();
            if (topologyService.isInfrastructure(topology, heardOn)) {
                return;
            }

            HostLocation hloc = new HostLocation(heardOn, System.currentTimeMillis());

            HostId hid = HostId.hostId(eth.getSourceMAC(), vlan);

            // ARP: possible new hosts, update both location and IP
            if (eth.getEtherType() == Ethernet.TYPE_ARP) {
                ARP arp = (ARP) eth.getPayload();
                IpAddress ip = IpAddress.valueOf(IpAddress.Version.INET, arp.getSenderProtocolAddress());
                HostDescription hdescr =
                        new DefaultHostDescription(eth.getSourceMAC(), vlan, hloc, ip);
                providerService.hostDetected(hid, hdescr);

            // IPv4: update location only
            } else if (eth.getEtherType() == Ethernet.TYPE_IPV4) {
                HostDescription hdescr =
                        new DefaultHostDescription(eth.getSourceMAC(), vlan, hloc);
                providerService.hostDetected(hid, hdescr);

            // NeighborAdvertisement and NeighborSolicitation: possible new hosts, update both location and IP
            // IPv6: update location only
            } else if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
                IpAddress ip = null;
                IPv6 ipv6 = (IPv6) eth.getPayload();

                IPacket iPkt = ipv6;
                while (iPkt != null) {
                    if (iPkt instanceof NeighborAdvertisement || iPkt instanceof NeighborSolicitation) {
                        IpAddress sourceAddress =
                                IpAddress.valueOf(IpAddress.Version.INET6, ipv6.getSourceAddress());
                        // Ignore DAD packets, in which source address is all zeros.
                        if (!sourceAddress.isZero()) {
                            ip = sourceAddress;
                            break;
                        }
                    }
                    iPkt = iPkt.getPayload();
                }
                HostDescription hdescr = (ip == null) ?
                        new DefaultHostDescription(eth.getSourceMAC(), vlan, hloc) :
                        new DefaultHostDescription(eth.getSourceMAC(), vlan, hloc, ip);
                providerService.hostDetected(hid, hdescr);
            }
        }
    }

    // Auxiliary listener to device events.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            switch (event.type()) {
            case DEVICE_ADDED:
                break;
            case DEVICE_AVAILABILITY_CHANGED:
                if (hostRemovalEnabled &&
                    !deviceService.isAvailable(device.id())) {
                    removeHosts(hostService.getConnectedHosts(device.id()));
                }
                break;
            case DEVICE_SUSPENDED:
            case DEVICE_UPDATED:
                // Nothing to do?
                break;
            case DEVICE_REMOVED:
                if (hostRemovalEnabled) {
                    removeHosts(hostService.getConnectedHosts(device.id()));
                }
                break;
            case PORT_ADDED:
                break;
            case PORT_UPDATED:
                if (hostRemovalEnabled) {
                    ConnectPoint point =
                        new ConnectPoint(device.id(), event.port().number());
                    removeHosts(hostService.getConnectedHosts(point));
                }
                break;
            case PORT_REMOVED:
                // Nothing to do?
                break;
            default:
                break;
            }
        }
    }

    // Signals host vanish for all specified hosts.
    private void removeHosts(Set<Host> hosts) {
        for (Host host : hosts) {
            providerService.hostVanished(host.id());
        }
    }

}
