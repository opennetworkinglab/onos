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
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketContext;
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

    private static final int FLOW_RULE_PRIORITY = 40000;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

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
        pushRules();

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

    /**
     * Pushes flow rules to all devices.
     */
    private void pushRules() {
        for (Device device : deviceService.getDevices()) {
            pushRules(device);
        }
    }

    /**
     * Pushes flow rules to the device to receive control packets that need
     * to be processed.
     *
     * @param device the device to push the rules to
     */
    private synchronized void pushRules(Device device) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

        // Get all ARP packets
        sbuilder.matchEthType(Ethernet.TYPE_ARP);
        tbuilder.setOutput(PortNumber.CONTROLLER);
        FlowRule flowArp =
            new DefaultFlowRule(device.id(),
                                sbuilder.build(), tbuilder.build(),
                                FLOW_RULE_PRIORITY, appId, 0, true);

        flowRuleService.applyFlowRules(flowArp);
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

            // Potentially a new or moved host
            if (eth.getEtherType() == Ethernet.TYPE_ARP) {
                ARP arp = (ARP) eth.getPayload();
                IpAddress ip =
                        IpAddress.valueOf(IpAddress.Version.INET,
                                          arp.getSenderProtocolAddress());
                HostDescription hdescr =
                        new DefaultHostDescription(eth.getSourceMAC(), vlan, hloc, ip);
                providerService.hostDetected(hid, hdescr);

            } else if (eth.getEtherType() == Ethernet.TYPE_IPV4) {
                //Do not learn new ip from ip packet.
                HostDescription hdescr =
                        new DefaultHostDescription(eth.getSourceMAC(), vlan, hloc);
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
                pushRules(device);
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
