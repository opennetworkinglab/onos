/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.pim.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

/**
 * Protocol Independent Multicast (PIM) Emulation.  This component is responsible
 * for reference the services this PIM module is going to need, then initializing
 * the corresponding utility classes.
 */
@Component(immediate = true)
public class PIMComponent {
    private final Logger log = getLogger(getClass());

    // Register to receive PIM packets, used to send packets as well
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    // Get the appId
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    // Get the network configuration updates
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    // Access defined network (IP) interfaces
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    private static ApplicationId appId;

    private PIMInterfaces pimInterfaces;
    private PIMPacketHandler pimPacketHandler;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.pim");

        // Initialize the Packet Handler class
        pimPacketHandler = PIMPacketHandler.getInstance();
        pimPacketHandler.initialize(packetService, appId);

        // Initialize the Interface class
        pimInterfaces = PIMInterfaces.getInstance();
        pimInterfaces.initialize(configService, interfaceService);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        PIMPacketHandler.getInstance().stop();
        log.info("Stopped");
    }
}
