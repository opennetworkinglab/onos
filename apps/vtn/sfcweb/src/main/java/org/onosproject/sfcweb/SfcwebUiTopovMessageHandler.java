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

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;

import org.onlab.packet.MacAddress;
import org.onosproject.net.Device;
import org.onosproject.net.Element;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.NodeBadge;
import org.onosproject.ui.topo.TopoJson;
import org.onosproject.ui.topo.DeviceHighlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairGroup;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ListIterator;

/**
 * SFC web gui topology-overlay message handler.
 */
public class SfcwebUiTopovMessageHandler extends UiMessageHandler {

    private static final String SAMPLE_TOPOV_DISPLAY_START = "sfcwebTopovDisplayStart";
    private static final String SAMPLE_TOPOV_DISPLAY_SFC = "showSfcInfo";
    private static final String SAMPLE_TOPOV_DISPLAY_STOP = "sfcTopovClear";
    private static final String CONFIG_SFP_MSG = "configSfpMessage";

    private static final String ID = "id";
    private static final String MODE = "mode";
    private static final String SFC_ID   = "SFC";

    private static final long UPDATE_PERIOD_MS = 1000;

    private static final Link[] EMPTY_LINK_SET = new Link[0];

    private enum Mode { IDLE, MOUSE, LINK }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceService deviceService;
    private HostService hostService;
    private LinkService linkService;

    private final Timer timer = new Timer("sfcweb-overlay");
    private TimerTask demoTask = null;
    private Mode currentMode = Mode.IDLE;
    private Element elementOfNote;
    private Link[] linkSet = EMPTY_LINK_SET;
    private int linkIndex;

    private long someNumber = 1;
    private long someIncrement = 1;
    protected PortPairService portPairService;
    protected VtnRscService vtnRscService;
    protected VirtualPortService virtualPortService;
    protected PortChainService portChainService;
    protected PortPairGroupService portPairGroupService;

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        deviceService = directory.get(DeviceService.class);
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
        public void process(long sid, ObjectNode payload) {
            String mode = string(payload, MODE);
            PortChainService pcs = get(PortChainService.class);
            Iterable<PortChain> portChains = pcs.getPortChains();
            ObjectNode result = objectNode();

            ArrayNode arrayNode = arrayNode();

            for (final PortChain portChain : portChains) {
                arrayNode.add(portChain.portChainId().value().toString());
            }
            result.putArray("a").addAll(arrayNode);

            sendMessage(SAMPLE_TOPOV_DISPLAY_SFC, sid, result);
        }
    }

    private final class DisplayStopHandler extends RequestHandler {
        public DisplayStopHandler() {
            super(SAMPLE_TOPOV_DISPLAY_STOP);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
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
        public void process(long sid, ObjectNode payload) {
            String id = string(payload, ID);
            ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
            vtnRscService = serviceDirectory.get(VtnRscService.class);
            virtualPortService = serviceDirectory.get(VirtualPortService.class);

            Highlights highlights = new Highlights();

            PortChainId portChainId = PortChainId.of(id);
            boolean portChainIdExist = portChainService.exists(portChainId);
            if (!portChainIdExist) {
                log.info("portchain id doesn't exist");
                return;
            }

            PortChain portChain = portChainService.getPortChain(portChainId);

            List<PortPairGroupId> llPortPairGroupIdList = portChain.portPairGroups();
            ListIterator<PortPairGroupId> portPairGroupIdListIterator = llPortPairGroupIdList.listIterator();
            while (portPairGroupIdListIterator.hasNext()) {
                PortPairGroupId portPairGroupId = portPairGroupIdListIterator.next();
                PortPairGroup portPairGroup = portPairGroupService.getPortPairGroup(portPairGroupId);
                List<PortPairId> llPortPairIdList = portPairGroup.portPairs();
                ListIterator<PortPairId> portPairListIterator = llPortPairIdList.listIterator();

                while (portPairListIterator.hasNext()) {
                    PortPairId portPairId = portPairListIterator.next();
                    PortPair portPair = portPairService.getPortPair(portPairId);
                    DeviceId deviceId = vtnRscService.getSfToSffMaping(VirtualPortId.portId(portPair.egress()));
                    Device device = deviceService.getDevice(deviceId);
                    DeviceHighlight dh = new DeviceHighlight(device.id().toString());
                    dh.setBadge(NodeBadge.text(SFC_ID));

                    MacAddress dstMacAddress = virtualPortService.getPort(VirtualPortId
                                                        .portId(portPair.egress())).macAddress();
                    Host host = hostService.getHost(HostId.hostId(dstMacAddress));
                    HostHighlight hhDst = new HostHighlight(host.id().toString());
                    hhDst.setBadge(NodeBadge.text(SFC_ID));

                    MacAddress srcMacAddress = virtualPortService.getPort(VirtualPortId
                                                        .portId(portPair.ingress())).macAddress();
                    Host hostSrc = hostService.getHost(HostId.hostId(srcMacAddress));
                    HostHighlight hhSrc = new HostHighlight(hostSrc.id().toString());
                    hhSrc.setBadge(NodeBadge.text(SFC_ID));

                    highlights.add(dh);
                    highlights.add(hhSrc);
                    highlights.add(hhDst);
                }
            }

            sendHighlights(highlights);
        }
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
