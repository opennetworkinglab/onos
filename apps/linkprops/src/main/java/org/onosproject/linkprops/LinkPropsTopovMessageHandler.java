/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.linkprops;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.Bandwidth;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.TopoJson;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.resource.ResourceQueryService;
import org.onosproject.net.statistic.PortStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * ONOS UI Topology overlay for LinkProps. Creates the highlights for each link
 * depending on the selected mode (rate,byte,band) and adds labels to egress
 * links of the device being hovered over.
 */
public class LinkPropsTopovMessageHandler extends UiMessageHandler {

    private static final String LP_TOPOV_DISPLAY_START = "linkPropsTopovDisplayStart";
    private static final String LP_TOPOV_DISPLAY_UPDATE = "linkPropsTopovDisplayUpdate";
    private static final String LP_TOPOV_DISPLAY_STOP = "linkPropsTopovDisplayStop";

    private static final String ID = "id";
    private static final String MODE = "mode";

    private enum Mode { IDLE, RATE, BYTE, BAND }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceService deviceService;
    private HostService hostService;
    private LinkService linkService;
    private PortStatisticsService portStatisticsService;
    private ResourceQueryService resourceQueryService;

    private Mode currentMode = Mode.IDLE;
    private Element elementOfNote;


    // ===============-=-=-=-=-=-======================-=-=-=-=-=-=-================================


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        deviceService = directory.get(DeviceService.class);
        hostService = directory.get(HostService.class);
        linkService = directory.get(LinkService.class);
        portStatisticsService = directory.get(PortStatisticsService.class);
        resourceQueryService = directory.get(ResourceQueryService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DisplayStartHandler(),
                new DisplayUpdateHandler(),
                new DisplayStopHandler()
        );
    }

    // === -------------------------
    // === Handler classes

    private final class DisplayStartHandler extends RequestHandler {
        public DisplayStartHandler() {
            super(LP_TOPOV_DISPLAY_START);
        }

        @Override
        public void process(ObjectNode payload) {
            String mode = string(payload, MODE);

            log.debug("Start Display: mode [{}]", mode);
            clearState();
            clearForMode();

            switch (mode) {
                case "rate":
                    currentMode = Mode.RATE;
                    sendRateData();
                    break;

                case "byte":
                    currentMode = Mode.BYTE;
                    sendByteData();
                    break;

                case "band":
                    currentMode = Mode.BAND;
                    sendBandwidth();
                    break;

                default:
                    currentMode = Mode.IDLE;
                    break;
            }
        }
    }

    private final class DisplayUpdateHandler extends RequestHandler {
        public DisplayUpdateHandler() {
            super(LP_TOPOV_DISPLAY_UPDATE);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            log.debug("Update Display: id [{}]", id);
            if (!Strings.isNullOrEmpty(id)) {
                updateForMode(id);
            } else {
                clearForMode();
            }
        }
    }

    private final class DisplayStopHandler extends RequestHandler {
        public DisplayStopHandler() {
            super(LP_TOPOV_DISPLAY_STOP);
        }

        @Override
        public void process(ObjectNode payload) {
            log.debug("Stop Display");
            clearState();
            clearForMode();
        }
    }

    // === ------------

    private void clearState() {
        currentMode = Mode.IDLE;
        elementOfNote = null;
    }

    private void updateForMode(String id) {
        log.debug("host service: {}", hostService);
        log.debug("device service: {}", deviceService);

        try {
            HostId hid = HostId.hostId(id);
            log.debug("host id {}", hid);
            elementOfNote = hostService.getHost(hid);
            log.debug("host element {}", elementOfNote);

        } catch (Exception e) {
            try {
                DeviceId did = DeviceId.deviceId(id);
                log.debug("device id {}", did);
                elementOfNote = deviceService.getDevice(did);
                log.debug("device element {}", elementOfNote);

            } catch (Exception e2) {
                log.debug("Unable to process ID [{}]", id);
                elementOfNote = null;
            }
        }

        switch (currentMode) {
            case RATE:
                sendRateData();
                break;

            case BYTE:
                sendByteData();
                break;

            case BAND:
                sendBandwidth();
                break;

            default:
                break;
        }

    }

    private void clearForMode() {
        sendHighlights(new Highlights());
    }

    private void sendHighlights(Highlights highlights) {
        sendMessage(TopoJson.highlightsMessage(highlights));
    }


    private void sendRateData() {
        if (elementOfNote != null && elementOfNote instanceof Device) {
            DeviceId devId = (DeviceId) elementOfNote.id();
            Set<Link> links = linkService.getDeviceEgressLinks(devId);
            Highlights highlights = getLinkSpeed(links, devId);
            sendHighlights(highlights);
        }
    }

    private void sendBandwidth() {
        if (elementOfNote != null && elementOfNote instanceof Device) {
            DeviceId devId = (DeviceId) elementOfNote.id();
            Set<Link> links = linkService.getDeviceEgressLinks(devId);
            Highlights highlights = getBandwidth(links, devId);
            sendHighlights(highlights);
        }
    }

    /**
     * Gets the links connected to the highlighted device.
     * Creates a ContinuousResource object for each link
     * and gets the bandwidth of the link from the query
     * and sets the label of the link as the bandwidth value.
     */
    private Highlights getBandwidth(Set<Link> links, DeviceId devId) {
        LpLinkMap linkMap = new LpLinkMap();
        Highlights highlights = new Highlights();
        if (links != null) {
            log.debug("Processing {} links", links.size());
            links.forEach(linkMap::add);

            PortNumber portnum = PortNumber.portNumber((int) links.iterator().next().src().port().toLong());

            for (LpLink dlink : linkMap.biLinks()) {
                DiscreteResourceId parent = Resources.discrete(devId, portnum).id();
                ContinuousResource continuousResource =
                        (ContinuousResource) resourceQueryService.getAvailableResources(parent,
                                Bandwidth.class).iterator().next();
                double availBandwidth = continuousResource.value();

                dlink.makeImportant().setLabel(Double.toString(availBandwidth) + " bytes/s");
                highlights.add(dlink.highlight(null));
            }
        } else {
            log.debug("No egress links found for device {}", devId);
        }
        return highlights;
    }

    /**
     * Gets the links connected to the highlighted device.
     * Uses PortStatisticsService to get a load object of
     * the links and find the rate of data flow out of the
     * device via that link (src) and into the device via
     * that link (dst), because dlink.two() gives a
     * NullPointerException. Creates a label which displays
     * the src and dst rates for the link.
     */
    private Highlights getLinkSpeed(Set<Link> links, DeviceId devId) {
        LpLinkMap linkMap = new LpLinkMap();
        if (links != null) {
            log.debug("Processing {} links", links.size());
            links.forEach(linkMap::add);
        } else {
            log.debug("No egress links found for device {}", devId);
        }

        Highlights highlights = new Highlights();

        for (LpLink dlink : linkMap.biLinks()) {
            String rate = "Out: " + getSpeedString((portStatisticsService.load(dlink.one().src()).rate())) + " | In: "
                    + getSpeedString((portStatisticsService.load(dlink.one().dst()).rate()));
            dlink.makeImportant().setLabel(rate);
            highlights.add(dlink.highlight(null));
        }
        return highlights;
    }

    private String getSpeedString(Long speed) {
        if (speed > 1_000_000_000) {
            return Long.toString(speed / 1_000_000_000) + "Gb/s";
        } else if (speed > 1_000_000) {
            return Long.toString(speed / 1_000_000) + "Mb/s";
        } else if (speed > 1_000) {
            return Long.toString(speed / 1_000) + "kb/s";
        } else {
            return Long.toString(speed) + "bytes/s";
        }
    }

    private void sendByteData() {
        if (elementOfNote != null && elementOfNote instanceof Device) {
            DeviceId devId = (DeviceId) elementOfNote.id();
            Set<Link> links = linkService.getDeviceEgressLinks(devId);
            Highlights highlights = getTotalBytes(links, devId);
            sendHighlights(highlights);
        }
    }

    /**
     * Gets the links connected to the highlighted device.
     * Uses PortStatisticsService to get a load object of
     * the links and find the total number of bytes sent out
     * of the device via that link (src) and into the device
     * via that link (dst), because dlink.two() gives a
     * NullPointerException. Creates a label which displays
     * the src and dst total bytes for the link.
     */
    private Highlights getTotalBytes(Set<Link> links, DeviceId devId) {
        LpLinkMap linkMap = new LpLinkMap();
        if (links != null) {
            log.debug("Processing {} links", links.size());
            links.forEach(linkMap::add);
        } else {
            log.debug("No egress links found for device {}", devId);
        }

        Highlights highlights = new Highlights();

        for (LpLink dlink : linkMap.biLinks()) {
            String bytes = "Out: " + getBytesString(portStatisticsService.load(dlink.one().src()).latest()) + " | In: "
                    + getBytesString(portStatisticsService.load(dlink.one().dst()).latest());
            dlink.makeImportant().setLabel(bytes);
            highlights.add(dlink.highlight(null));
        }
        return highlights;
    }

    private String getBytesString(Long bytes) {
        final long tb = (long) 1_000_000_000_000.0;
        if (bytes > tb) {
            return Long.toString(bytes / tb) + "Tb";
        } else if (bytes > 1_000_000_000) {
            return Long.toString(bytes / 1_000_000_000) + "Gb";
        } else if (bytes > 1_000_000) {
            return Long.toString(bytes / 1_000_000) + "Mb";
        } else if (bytes > 1_000) {
            return Long.toString(bytes / 1_000) + "kb";
        } else {
            return Long.toString(bytes) + "bytes";
        }
    }

}
