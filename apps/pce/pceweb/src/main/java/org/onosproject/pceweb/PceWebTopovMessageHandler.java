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

package org.onosproject.pceweb;

import com.fasterxml.jackson.databind.node.ObjectNode;


import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.NodeBadge;
import org.onosproject.ui.topo.TopoJson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * ONOS UI PCE WEB Topology-Overlay message handler.
 */
public class PceWebTopovMessageHandler extends UiMessageHandler {

    private static final String PCEWEB_CLEAR = "pceTopovClear";
    private static final String PCEWEB_SET_SRC = "pceTopovSetSrc";
    private static final String PCEWEB_SET_DST = "pceTopovSetDst";
    private static final String PCEWEB_SET_PATH = "pceTopovSetMode";

    private static final String ID = "id";
    private static final String MODE = "mode";
    private static final String TYPE = "type";
    private static final String SWITCH = "switch";
    private static final String ENDSTATION = "endstation";
    public static final String DST = "Dst";
    public static final String SRC = "Src";
    // Delay for showHighlights event processing on GUI client side to
    // account for addLink animation.
    public static final int DELAY_MS = 1100;

    private static final String CLASS = "class";
    private static final String UNKNOWN = "unknown";
    private static final String DEVICE = "device";

    private Set<Link> allPathLinks;
    private boolean listenersRemoved;
    private LinkWeight linkData;
    private int highlightDelay;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private PathService pathService;

    private ElementId src, dst;
    private String srcType, dstType;
    private List<Path> paths;
    private int pathIndex;

    protected TopologyService topologyService;
    protected DeviceService deviceService;


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {

        super.init(connection, directory);
        //TODO: Need add listeners.
        //topologyService = directory.get(TopologyService.class);
        //addListeners();
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new ClearHandler(),
                new SetSrcHandler(),
                new SetDstHandler(),
                new SetPathHandler());
    }

    // Handler classes
    /**
     * Handles the 'clear' event received from the client.
     */
    private final class ClearHandler extends RequestHandler {

        public ClearHandler() {
            super(PCEWEB_CLEAR);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            src = null;
            dst = null;
            sendMessage(TopoJson.highlightsMessage(new Highlights()));
        }
    }

    /**
     * Handles the 'set source' event received from the client.
     */
    private final class SetSrcHandler extends RequestHandler {

        public SetSrcHandler() {
            super(PCEWEB_SET_SRC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            log.info("PCE WEB Set source process method invoked");
            String id = string(payload, ID);
            src = elementId(id);
            srcType = string(payload, TYPE);
            if (src.equals(dst)) {
                dst = null;
            }
            sendMessage(TopoJson.highlightsMessage(addBadge(new Highlights(),
                    srcType, src.toString(), SRC)));

        }
    }

    /**
     * Handles the 'set destination' event received from the client.
     */
    private final class SetDstHandler extends RequestHandler {

        public SetDstHandler() {
            super(PCEWEB_SET_DST);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String id = string(payload, ID);
            dst = elementId(id);
            dstType = string(payload, TYPE);
            if (src.equals(dst)) {
                src = null;
            }

            sendMessage(TopoJson.highlightsMessage(addBadge(new Highlights(),
                    dstType, dst.toString(), DST)));

        }
    }

    /**
     * Handles the 'patchcalculation' event received from the client.
     */
    private final class SetPathHandler extends RequestHandler {

        public SetPathHandler() {
            super(PCEWEB_SET_PATH);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String mode = string(payload, MODE);

            // TODO: Read user input[constraints] and call the path calculation based on
            //given constrainsts.
            findAndSendPaths();
        }
    }

    // === ------------
    /**
     * provides the element id.
     */
    private ElementId elementId(String id) {
        try {
            return DeviceId.deviceId(id);
        } catch (IllegalArgumentException e) {
            return HostId.hostId(id);
        }
    }
    //TODO: Need to pass constraints to this method
    private void findAndSendPaths() {
        log.info("src={}; dst={};", src, dst);
        if (src != null && dst != null) {
            //TBD: Need to call pathcalulation API here
            hilightAndSendPaths();

        }

    }

    //TODO: The below code is not used. Once get path from PCE app then below code will be use.
    // the below code will get path and it will highlight the selected path.
    //Currently primary path in use, there is no use of secondary path.
    //secondary path need to remove based on path received by PCE app.
    private ImmutableSet.Builder<Link> buildPaths(
            ImmutableSet.Builder<Link> pathBuilder) {
        paths.forEach(path -> path.links().forEach(pathBuilder::add));
        return pathBuilder;
    }

    private ImmutableSet.Builder<Link> buildDisjointPaths(
            ImmutableSet.Builder<Link> pathBuilder) {
        paths.forEach(path -> {
            DisjointPath dp = (DisjointPath) path;
            pathBuilder.addAll(dp.primary().links());
            pathBuilder.addAll(dp.backup().links());
        });
        return pathBuilder;
    }

    private void hilightAndSendPaths() {
        PceWebLinkMap linkMap = new PceWebLinkMap();
        allPathLinks.forEach(linkMap::add);

        Set<Link> selectedPathLinks;

        selectedPathLinks = paths.isEmpty() ? ImmutableSet.of()
                    : ImmutableSet.copyOf(paths.get(pathIndex).links());

        Highlights highlights = new Highlights();
        if (highlightDelay > 0) {
            highlights.delay(highlightDelay);
        }
        for (PceWebLink plink : linkMap.biLinks()) {
            plink.computeHilight(selectedPathLinks, allPathLinks);
            highlights.add(plink.highlight(null));
        }
        if (src != null) {
            highlights = addBadge(highlights, srcType, src.toString(), SRC);
        }
        if (dst != null) {
            highlights = addBadge(highlights, dstType, dst.toString(), DST);
        }
        sendMessage(TopoJson.highlightsMessage(highlights));
    }

    private Highlights addBadge(Highlights highlights, String type,
            String elemId, String src) {
        if (SWITCH.equals(type)) {
            highlights = addDeviceBadge(highlights, elemId, src);
        } else if (ENDSTATION.equals(type)) {
            highlights = addHostBadge(highlights, elemId, src);
        }
        return highlights;
    }

    private Highlights addDeviceBadge(Highlights h, String elemId, String type) {
        DeviceHighlight dh = new DeviceHighlight(elemId);
        dh.setBadge(createBadge(type));
        h.add(dh);
        return h;
    }

    private Highlights addHostBadge(Highlights h, String elemId, String type) {
        HostHighlight hh = new HostHighlight(elemId);
        hh.setBadge(createBadge(type));
        h.add(hh);
        return h;
    }

    private NodeBadge createBadge(String type) {
        return NodeBadge.text(type);
    }

    //TODO: Listeners need to add.
    //If topology changes then path need to be re calculate.

}
