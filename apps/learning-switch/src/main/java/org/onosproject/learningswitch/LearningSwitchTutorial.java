/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.learningswitch;

import com.google.common.collect.Maps;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Tutorial class used to help build a basic onos learning switch application.
 * Edit your code in the activate, deactivate, and actLikeSwitch methods.
 */
@Component(immediate = true)
public class LearningSwitchTutorial {
    // Instantiates the relevant services.

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Defining macTables as a concurrent map allows multiple threads and packets to
     * use the map without an issue.
     */
    protected Map<DeviceId, Map<MacAddress, PortNumber>> macTables = Maps.newConcurrentMap();
    private ApplicationId appId;
    private PacketProcessor processor;

    /**
     * Create a variable of the SwitchPacketProcessor class using the PacketProcessor defined above.
     * Activates the app.
     *
     * Create code to add a processor
     */
    @Activate
    protected void activate() {
        log.info("Started");
        appId = coreService.getAppId("org.onosproject.learningswitch"); //equal to the name shown in pom.xml file

        //Create and processor and add it using packetService

        /*
         * Restricts packet types to IPV4 and ARP by only requesting those types
         */
        packetService.requestPackets(DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4).build(), PacketPriority.REACTIVE, appId, Optional.empty());
        packetService.requestPackets(DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP).build(), PacketPriority.REACTIVE, appId, Optional.empty());
    }

    /**
     * Deactivates the processor by removing it.
     *
     * Create code to remove the processor.
     */
    @Deactivate
    protected void deactivate() {
        log.info("Stopped");

        //Remove the processor
    }

    /**
     * This class contains pseudo code that you must replace with your own code in actLikeSwitch.  Your job is to
     * send the packet out the port previously learned for the destination MAC.  If it does not exist,
     * flood the packet out (to all ports).
     */
    private class SwitchPacketProcessor implements PacketProcessor {
        /**
         * Learns the source port associated with the packet's DeviceId if it has not already been learned.
         * Calls actLikeSwitch to process and send the packet.
         * @param pc PacketContext object containing packet info
         */
        @Override
        public void process(PacketContext pc) {
            log.info(pc.toString());
            /*
             * Puts the packet's source's device Id into the map macTables if it has not previously been added.
             * (learns the output port)
             */
            ConnectPoint cp = pc.inPacket().receivedFrom();
            macTables.putIfAbsent(cp.deviceId(), Maps.newConcurrentMap());



            // This method simply floods all ports with the packet.
            actLikeHub(pc);

            /*
             * This is the call to the actLikeSwitch method you will be creating. When
             * you are ready to test it, uncomment the line below, and comment out the
             * actLikeHub call above.
             *
             * NOTE: The perk of an actLikeSwitch method over actLikeHub is speed.
             * FlowRule allows much faster processing.
             */
            //actLikeSwitch(pc);
        }

        /**
         * Example method. Floods packet out of all switch ports.
         *
         * @param pc the PacketContext object passed through from activate method
         */
        public void actLikeHub(PacketContext pc) {
            pc.treatmentBuilder().setOutput(PortNumber.FLOOD);
            pc.send();
        }

        /**
         * Ensures packet is of required type. Obtain the port number associated with the packet's source ID.
         * If this port has previously been learned (in the process method) build a flow using the packet's
         * out port, treatment, destination, and other properties.  Send the flow to the learned out port.
         * Otherwise, flood packet to all ports if out port has not been learned.
         *
         * @param pc the PacketContext object passed through from activate() method
         */
        public void actLikeSwitch(PacketContext pc) {

            /*
             * Ensures the type of packet being processed is only of type IPV4 or ARP (not LLDP or BDDP).
             * If it is not, return and do nothing with the packet. actLikeSwitch can only process
             * IPV4 and ARP packets.
             */
            Short type = pc.inPacket().parsed().getEtherType();
            if (type != Ethernet.TYPE_IPV4 &&  type != Ethernet.TYPE_ARP) {
                return;
            }

            /*
             * Learn the destination, source, and output port of the packet using a ConnectPoint and the
             * associated macTable.  If there is a known port associated with the packet's destination MAC Address,
             * the output port will not be null.
             */
            //find the packets connect point
            //save the macTables port value for the deviceID
            //save the outPort as a variable
            //PortNumber outPort = ...

            /*
             * If port is known, set output port to the packet's learned output port and construct a
             * FlowRule using a source, destination, treatment and other properties. Send the FlowRule
             * to the designated output port.
             */
            //if outPort isn't null
                //construct FlowRule
                //FlowRule fr = ...
                //send the packet

            /*
             * else, the output port has not been learned yet.  Flood the packet to all ports using
             * the actLikeHub method
             */
            //else
            //  call actLikeHub method
        }
    }
}
