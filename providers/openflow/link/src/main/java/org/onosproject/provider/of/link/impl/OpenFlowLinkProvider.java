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
package org.onosproject.provider.of.link.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.DeviceId;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.PacketListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.slf4j.Logger;


/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure links.
 */
@Component(immediate = true)
@Deprecated
public class OpenFlowLinkProvider extends AbstractProvider implements LinkProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    private LinkProviderService providerService;

    private final boolean useBDDP = true;

    private final InternalLinkProvider listener = new InternalLinkProvider();

    protected final Map<Dpid, LinkDiscovery> discoverers = new ConcurrentHashMap<>();

    /**
     * Creates an OpenFlow link provider.
     */
    public OpenFlowLinkProvider() {
        super(new ProviderId("of", "org.onosproject.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        controller.addPacketListener(0, listener);
        for (OpenFlowSwitch sw : controller.getSwitches()) {
            listener.switchAdded(new Dpid(sw.getId()));
        }
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        for (LinkDiscovery ld : discoverers.values()) {
            ld.stop();
        }
        providerRegistry.unregister(this);
        controller.removeListener(listener);
        controller.removePacketListener(listener);
        providerService = null;

        log.info("Stopped");
    }


    private class InternalLinkProvider implements PacketListener, OpenFlowSwitchListener {


        @Override
        public void handlePacket(OpenFlowPacketContext pktCtx) {
            LinkDiscovery ld = discoverers.get(pktCtx.dpid());
            if (ld == null) {
                return;
            }
            if (ld.handleLLDP(pktCtx.unparsed(), pktCtx.inPort())) {
                pktCtx.block();
            }

        }

        @Override
        public void switchAdded(Dpid dpid) {
            discoverers.put(dpid, new LinkDiscovery(controller.getSwitch(dpid),
                    controller, providerService, useBDDP));

        }

        @Override
        public void switchRemoved(Dpid dpid) {
            LinkDiscovery ld = discoverers.remove(dpid);
            if (ld != null) {
                ld.removeAllPorts();
            }
            providerService.linksVanished(
                    DeviceId.deviceId("of:" + Long.toHexString(dpid.value())));
        }


        @Override
        public void switchChanged(Dpid dpid) {
            //might not need to do anything since DeviceManager is notified
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            LinkDiscovery ld = discoverers.get(dpid);
            if (ld == null) {
                return;
            }
            final OFPortDesc port = status.getDesc();
            final boolean enabled = !port.getState().contains(OFPortState.LINK_DOWN) &&
                    !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            if (enabled) {
                ld.addPort(port);
            } else {
                /*
                 * remove port calls linkVanished
                 */
                ld.removePort(port);
            }

        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested,
                RoleState response) {
            // do nothing for this.
        }

    }

}
