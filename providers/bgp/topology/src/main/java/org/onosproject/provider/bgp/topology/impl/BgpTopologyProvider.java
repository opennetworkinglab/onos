/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.onosproject.provider.bgp.topology.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.bgp.controller.BgpDpid.uri;
import static org.onosproject.net.DeviceId.deviceId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.onlab.packet.ChassisId;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.bgp.controller.BGPController;
import org.onosproject.bgp.controller.BgpDpid;
import org.onosproject.bgp.controller.BgpLinkListener;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BGPNodeLSNlriVer4;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider which uses an BGP controller to detect network infrastructure topology.
 */
@Component(immediate = true)
public class BgpTopologyProvider extends AbstractProvider implements LinkProvider, DeviceProvider {

    public BgpTopologyProvider() {
        super(new ProviderId("bgp", "org.onosproject.provider.bgp"));
    }

    private static final Logger log = LoggerFactory.getLogger(BgpTopologyProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BGPController controller;

    private DeviceProviderService deviceProviderService;
    private LinkProviderService linkProviderService;

    private HashMap<String, List<PortDescription>> portMap = new HashMap<>();
    private InternalBgpProvider listener = new InternalBgpProvider();
    private static final String UNKNOWN = "unknown";

    @Activate
    public void activate() {
        linkProviderService = linkProviderRegistry.register(this);
        deviceProviderService = deviceProviderRegistry.register(this);
        controller.addListener(listener);
        controller.addLinkListener(listener);
    }

    @Deactivate
    public void deactivate() {
        linkProviderRegistry.unregister(this);
        linkProviderService = null;
        controller.removeListener(listener);
        controller.removeLinkListener(listener);
    }

    private List<PortDescription> buildPortDescriptions(String linkUri) {

        List<PortDescription> portList;

        if (portMap.containsKey(linkUri)) {
            portList = portMap.get(linkUri);
        } else {
            portList = new ArrayList<>();
        }

        // TODO: port description
        portList.add(new DefaultPortDescription(null, true));

        portMap.put(linkUri, portList);
        return portList;
    }

    /**
     * Build a link description from a bgp link.
     *
     * @param bgpLink bgp link
     * @return linkDescription link description
     */
    private LinkDescription buildLinkDescription(BgpLinkLsNlriVer4 bgpLink) {
        LinkDescription ld = null;
        checkNotNull(bgpLink);

        BgpDpid localNodeUri = new BgpDpid(bgpLink, BgpDpid.NODE_DESCRIPTOR_LOCAL);
        DeviceId srcDeviceID = deviceId(uri(localNodeUri.toString()));

        BgpDpid remoteNodeUri = new BgpDpid(bgpLink, BgpDpid.NODE_DESCRIPTOR_REMOTE);
        DeviceId dstDeviceID = deviceId(uri(remoteNodeUri.toString()));

        deviceProviderService.updatePorts(srcDeviceID, buildPortDescriptions(localNodeUri.toString()));

        deviceProviderService.updatePorts(dstDeviceID, buildPortDescriptions(remoteNodeUri.toString()));

        ConnectPoint src = new ConnectPoint(srcDeviceID, null);

        ConnectPoint dst = new ConnectPoint(dstDeviceID, null);

        ld = new DefaultLinkDescription(src, dst, Link.Type.INDIRECT);
        return ld;
    }

    /*
     * Implements device and link update.
     */
    private class InternalBgpProvider implements BgpNodeListener, BgpLinkListener {

        @Override
        public void addNode(BGPNodeLSNlriVer4 nodeNlri) {
            log.debug("Add node {}", nodeNlri.toString());

            if (deviceProviderService == null) {
                return;
            }
            BgpDpid nodeUri = new BgpDpid(nodeNlri);
            DeviceId deviceId = deviceId(uri(nodeUri.toString()));
            ChassisId cId = new ChassisId();

            DeviceDescription description = new DefaultDeviceDescription(uri(nodeUri.toString()), Device.Type.ROUTER,
                                                                         UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, cId);
            deviceProviderService.deviceConnected(deviceId, description);

        }

        @Override
        public void deleteNode(BGPNodeLSNlriVer4 nodeNlri) {
            log.debug("Delete node {}", nodeNlri.toString());

            if (deviceProviderService == null) {
                return;
            }

            BgpDpid nodeUri = new BgpDpid(nodeNlri);
            deviceProviderService.deviceDisconnected(deviceId(uri(nodeUri.toString())));
        }

        @Override
        public void addLink(BgpLinkLsNlriVer4 linkNlri) {
            log.debug("Add link {}", linkNlri.toString());

            if (linkProviderService == null) {
                return;
            }

            LinkDescription ld = buildLinkDescription(linkNlri);
            if (ld == null) {
                log.error("Invalid link info.");
                return;
            }

            linkProviderService.linkDetected(ld);
        }

        @Override
        public void deleteLink(BgpLinkLsNlriVer4 linkNlri) {
            log.debug("Delete link {}", linkNlri.toString());

            if (linkProviderService == null) {
                return;
            }

            LinkDescription ld = buildLinkDescription(linkNlri);
            if (ld == null) {
                log.error("Invalid link info.");
                return;
            }

            linkProviderService.linkVanished(ld);
        }
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return true;
    }
}
