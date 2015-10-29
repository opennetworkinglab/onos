/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.openstackrouting;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;

@Service
@Component(immediate = true)
/**
 * Populates flow rules about L3 functionality for VMs in Openstack.
 */
public class OpenstackRoutingManager implements OpenstackRoutingService {
    private static Logger log = LoggerFactory
            .getLogger(OpenstackRoutingManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    private ApplicationId appId;
    private OpenstackIcmpHandler icmpHandler;
    private OpenstackPnatHandler natHandler;
    private OpenstackFloatingIPHandler floatingIPHandler;
    private OpenstackRoutingRulePopulator openstackRoutingRulePopulator;

    private InternalPacketProcessor internalPacketProcessor = new InternalPacketProcessor();
    private ExecutorService l3EventExcutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "L3-event"));
    private ExecutorService icmpEventExcutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "icmp-event"));

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.openstackrouting");
        packetService.addProcessor(internalPacketProcessor, PacketProcessor.director(1));

        log.info("onos-openstackrouting started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(internalPacketProcessor);
        log.info("onos-openstackrouting stopped");
    }


    @Override
    public void createFloatingIP(OpenstackFloatingIP openstackFloatingIP) {

    }

    @Override
    public void updateFloatingIP(OpenstackFloatingIP openstackFloatingIP) {

    }

    @Override
    public void deleteFloatingIP(String id) {

    }

    @Override
    public void createRouter(OpenstackRouter openstackRouter) {

    }

    @Override
    public void updateRouter(OpenstackRouter openstackRouter) {

    }

    @Override
    public void deleteRouter(String id) {

    }

    @Override
    public void createRouterInterface(OpenstackRouterInterface openstackRouterInterface) {

    }

    @Override
    public void updateRouterInterface(OpenstackRouterInterface openstackRouterInterface) {

    }

    @Override
    public void deleteRouterInterface(String id) {

    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();

            if (ethernet != null && ethernet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 iPacket = (IPv4) ethernet.getPayload();
                switch (iPacket.getProtocol()) {
                    case IPv4.PROTOCOL_ICMP:
                        icmpEventExcutorService.execute(new OpenstackIcmpHandler(context));
                        break;
                    default:
                        l3EventExcutorService.execute(new OpenstackPnatHandler(context));
                        break;
                }

            }


        }
    }

}
