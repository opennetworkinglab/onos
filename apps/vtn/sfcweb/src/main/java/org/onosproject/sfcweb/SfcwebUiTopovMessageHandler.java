/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.sfcweb;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import jersey.repackaged.com.google.common.collect.Lists;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.NodeBadge;
import org.onosproject.ui.topo.TopoJson;
import org.onosproject.vtnrsc.FiveTuple;
import org.onosproject.vtnrsc.LoadBalanceId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TimerTask;

import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;

/**
 * SFC web gui topology-overlay message handler.
 */
public class SfcwebUiTopovMessageHandler extends UiMessageHandler {

    private static final String SAMPLE_TOPOV_DISPLAY_START = "sfcwebTopovDisplayStart";
    private static final String SAMPLE_TOPOV_DISPLAY_SFC = "showSfcInfo";
    private static final String SAMPLE_TOPOV_DISPLAY_STOP = "sfcTopovClear";
    private static final String CONFIG_SFP_MSG = "configSfpMessage";
    private static final String SAMPLE_TOPOV_SHOW_SFC_PATH = "showSfcPath";
    private static final String ID = "id";
    private static final String MODE = "mode";
    private static final String CLASSIFIER = "CLS";
    private static final String FORWARDER = "SFF";

    private static final Link[] EMPTY_LINK_SET = new Link[0];

    private enum Mode {
        IDLE, MOUSE, LINK
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private HostService hostService;
    private LinkService linkService;
    private TimerTask demoTask = null;
    private Mode currentMode = Mode.IDLE;
    private Element elementOfNote;
    private Link[] linkSet = EMPTY_LINK_SET;

    protected PortPairService portPairService;
    protected VtnRscService vtnRscService;
    protected VirtualPortService virtualPortService;
    protected PortChainService portChainService;
    protected PortPairGroupService portPairGroupService;

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        hostService = directory.get(HostService.class);
        linkService = directory.get(LinkService.class);
        portChainService = directory.get(PortChainService.class);
        portPairService = directory.get(PortPairService.class);
        portPairGroupService = directory.get(PortPairGroupService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DisplayStartHandler(),
                new DisplayStopHandler(),
                new ConfigSfpMsg()
        );
    }

    /**
     * Handler classes.
     */
    private final class DisplayStartHandler extends RequestHandler {
        public DisplayStartHandler() {
            super(SAMPLE_TOPOV_DISPLAY_START);
        }

        @Override
        public void process(ObjectNode payload) {
            String mode = string(payload, MODE);
            PortChainService pcs = get(PortChainService.class);
            Iterable<PortChain> portChains = pcs.getPortChains();
            ObjectNode result = objectNode();

            ArrayNode arrayNode = arrayNode();

            for (final PortChain portChain : portChains) {
                arrayNode.add(portChain.portChainId().value().toString());
            }
            result.putArray("a").addAll(arrayNode);

            sendMessage(SAMPLE_TOPOV_DISPLAY_SFC, result);
        }
    }

    private final class DisplayStopHandler extends RequestHandler {
        public DisplayStopHandler() {
            super(SAMPLE_TOPOV_DISPLAY_STOP);
        }

        @Override
        public void process(ObjectNode payload) {
            log.debug("Stop Display");
            clearState();
            clearForMode();
            cancelTask();
        }
    }

    private final class ConfigSfpMsg extends RequestHandler {
        public ConfigSfpMsg() {
            super(CONFIG_SFP_MSG);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
            vtnRscService = serviceDirectory.get(VtnRscService.class);
            virtualPortService = serviceDirectory.get(VirtualPortService.class);

            List<String> sfcPathList = Lists.newArrayList();

            Highlights highlights = new Highlights();
            SfcLinkMap linkMap = new SfcLinkMap();

            PortChainId portChainId = PortChainId.of(id);
            boolean portChainIdExist = portChainService.exists(portChainId);
            if (!portChainIdExist) {
                log.info("portchain id doesn't exist");
                return;
            }

            PortChain portChain = portChainService.getPortChain(portChainId);

            Set<FiveTuple> fiveTupleSet = portChain.getLoadBalanceIdMapKeys();
            for (FiveTuple fiveTuple : fiveTupleSet) {
                List<PortPairId> path = portChain.getLoadBalancePath(fiveTuple);
                LoadBalanceId lbId = portChain.getLoadBalanceId(fiveTuple);
                ListIterator<PortPairId> pathIterator = path.listIterator();

                // Add source
                Host srcHost = hostService.getHost(HostId.hostId(fiveTuple.macSrc()));

                HostHighlight hSrc = new HostHighlight(srcHost.id().toString());
                hSrc.setBadge(NodeBadge.text("SRC"));
                String sfcPath = "SRC -> ";
                highlights.add(hSrc);

                DeviceId previousDeviceId = null;
                while (pathIterator.hasNext()) {

                    PortPairId portPairId = pathIterator.next();
                    PortPair portPair = portPairService.getPortPair(portPairId);
                    DeviceId deviceId = vtnRscService.getSfToSffMaping(VirtualPortId.portId(portPair.egress()));
                    VirtualPort vPort = virtualPortService.getPort(VirtualPortId.portId(portPair.egress()));
                    MacAddress dstMacAddress = vPort.macAddress();
                    Host host = hostService.getHost(HostId.hostId(dstMacAddress));

                    addEdgeLinks(linkMap, host);
                    log.info("before check");
                    if (previousDeviceId != null) {
                        log.info("pdid not null");
                        if (!deviceId.equals(previousDeviceId)) {
                            // Highlight the link between devices.

                            Link link = getLinkBetweenDevices(deviceId, previousDeviceId);
                            if (link != null) {
                                linkMap.add(link);
                            }
                        }
                    }

                    DeviceHighlight dh = new DeviceHighlight(deviceId.toString());
                    if (portChain.getSfcClassifiers(lbId).contains(deviceId)) {
                        dh.setBadge(NodeBadge.text(CLASSIFIER));
                    } else {
                        dh.setBadge(NodeBadge.text(FORWARDER));
                    }

                    highlights.add(dh);

                    HostHighlight hhDst = new HostHighlight(host.id().toString());
                    hhDst.setBadge(NodeBadge.text(portPair.name()));
                    sfcPath = sfcPath + portPair.name() + "(" + vPort.fixedIps().iterator().next().ip() + ") -> ";

                    if (!portPair.ingress().equals(portPair.egress())) {
                        MacAddress srcMacAddress = virtualPortService.getPort(VirtualPortId
                                .portId(portPair.ingress()))
                                .macAddress();
                        Host hostSrc = hostService.getHost(HostId.hostId(srcMacAddress));
                        HostHighlight hhSrc = new HostHighlight(hostSrc.id().toString());
                        hhSrc.setBadge(NodeBadge.text(portPair.name()));
                        highlights.add(hhSrc);
                    }
                    highlights.add(hhDst);
                    previousDeviceId = deviceId;
                }

                // Add destination
                Host dstHost = hostService.getHost(HostId.hostId(fiveTuple.macDst()));

                HostHighlight hDst = new HostHighlight(dstHost.id().toString());
                hDst.setBadge(NodeBadge.text("DST"));
                sfcPath = sfcPath + "DST";
                highlights.add(hDst);
                sfcPathList.add(sfcPath);
            }

            for (SfcLink sfcLink : linkMap.biLinks()) {
                highlights.add(sfcLink.highlight(null));
            }
            sendHighlights(highlights);

            ObjectNode result = objectNode();
            ArrayNode arrayNode = arrayNode();
            for (String path : sfcPathList) {
                arrayNode.add(path);
            }
            result.putArray("sfcPathList").addAll(arrayNode);

            sendMessage(SAMPLE_TOPOV_SHOW_SFC_PATH, result);
        }
    }

    private Link getLinkBetweenDevices(DeviceId deviceId, DeviceId previousDeviceId) {
        Set<Link> deviceLinks = linkService.getDeviceEgressLinks(deviceId);
        Set<Link> previousDeviceLinks = linkService.getDeviceIngressLinks(previousDeviceId);
        for (Link link : deviceLinks) {
            previousDeviceLinks.contains(link);
            return link;
        }
        return null;
    }

    private void addEdgeLinks(SfcLinkMap linkMap, Host host1) {
        linkMap.add(createEdgeLink(host1, true));
        linkMap.add(createEdgeLink(host1, false));
    }

    private synchronized void cancelTask() {
        if (demoTask != null) {
            demoTask.cancel();
            demoTask = null;
        }
    }

    private void clearState() {
        currentMode = Mode.IDLE;
        elementOfNote = null;
        linkSet = EMPTY_LINK_SET;
    }

    private void clearForMode() {
        sendHighlights(new Highlights());
    }

    private void sendHighlights(Highlights highlights) {
        sendMessage(TopoJson.highlightsMessage(highlights));
    }

}
