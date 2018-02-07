/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.pathpainter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.GeoDistanceLinkWeight;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.NodeBadge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.onosproject.ui.topo.TopoJson.highlightsMessage;

/**
 * ONOS UI PathPainter Topology-Overlay message handler.
 */
public class PathPainterTopovMessageHandler extends UiMessageHandler {

    private static final String PAINTER_CLEAR = "ppTopovClear";
    private static final String PAINTER_SET_SRC = "ppTopovSetSrc";
    private static final String PAINTER_SET_DST = "ppTopovSetDst";
    private static final String PAINTER_SWAP_SRC_DST = "ppTopovSwapSrcDst";
    private static final String PAINTER_SET_MODE = "ppTopovSetMode";

    private static final String PAINTER_NEXT_PATH = "ppTopovNextPath";
    private static final String PAINTER_PREV_PATH = "ppTopovPrevPath";

    private static final String ID = "id";
    private static final String MODE = "mode";
    private static final String TYPE = "type";
    private static final String SWITCH = "switch";
    private static final String ENDSTATION = "endstation";
    private static final String DST = "Dst";
    private static final String SRC = "Src";
    // Delay for showHighlights event processing on GUI client side to
    // account for addLink animation.
    private static final int DELAY_MS = 1100;

    private final TopologyListener topologyListener = new InternalTopologyListener();

    private Set<Link> allPathLinks;
    private boolean listenersRemoved;
    private LinkWeigher linkData;
    private int highlightDelay;

    private enum Mode {
        SHORTEST, DISJOINT, GEODATA, SRLG, INVALID
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private PathService pathService;

    private ElementId src, dst;
    private String srcType, dstType;
    private Mode currentMode = Mode.SHORTEST;
    private List<Path> paths;
    private int pathIndex;

    protected TopologyService topologyService;


    // ===============-=-=-=-=-=-======================-=-=-=-=-=-=-===========


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        pathService = directory.get(PathService.class);
        topologyService = directory.get(TopologyService.class);
        linkData = new GeoDistanceLinkWeight(directory.get(DeviceService.class));
        addListeners();
    }


    @Override
    public void destroy() {
        removeListeners();
        super.destroy();
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new ClearHandler(),
                new SetSrcHandler(),
                new SetDstHandler(),
                new SwapSrcDstHandler(),
                new NextPathHandler(),
                new PrevPathHandler(),
                new SetModeHandler()
        );
    }

    // === -------------------------
    // === Handler classes

    private final class ClearHandler extends RequestHandler {

        public ClearHandler() {
            super(PAINTER_CLEAR);
        }

        @Override
        public void process(ObjectNode payload) {
            src = null;
            dst = null;
            sendMessage(highlightsMessage(new Highlights()));
        }
    }

    private final class SetSrcHandler extends RequestHandler {

        public SetSrcHandler() {
            super(PAINTER_SET_SRC);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            src = elementId(id);
            srcType = string(payload, TYPE);
            if (src.equals(dst)) {
                dst = null;
            }

            sendMessage(highlightsMessage(
                    addBadge(new Highlights(), srcType, src.toString(), SRC))
            );
            findAndSendPaths(currentMode);
        }
    }

    private final class SetDstHandler extends RequestHandler {
        public SetDstHandler() {
            super(PAINTER_SET_DST);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            dst = elementId(id);
            dstType = string(payload, TYPE);
            if (src.equals(dst)) {
                src = null;
            }

            sendMessage(highlightsMessage(
                    addBadge(new Highlights(), dstType, dst.toString(), DST))
            );
            findAndSendPaths(currentMode);
        }
    }

    private final class SwapSrcDstHandler extends RequestHandler {
        public SwapSrcDstHandler() {
            super(PAINTER_SWAP_SRC_DST);
        }

        @Override
        public void process(ObjectNode payload) {
            ElementId temp = src;
            src = dst;
            dst = temp;
            String s = srcType;
            srcType = dstType;
            dstType = s;
            findAndSendPaths(currentMode);
        }
    }


    private final class NextPathHandler extends RequestHandler {
        public NextPathHandler() {
            super(PAINTER_NEXT_PATH);
        }

        @Override
        public void process(ObjectNode payload) {
            pathIndex = (pathIndex >= paths.size() - 1 ? 0 : pathIndex + 1);
            hilightAndSendPaths();
        }
    }

    private final class PrevPathHandler extends RequestHandler {
        public PrevPathHandler() {
            super(PAINTER_PREV_PATH);
        }

        @Override
        public void process(ObjectNode payload) {
            pathIndex = (pathIndex <= 0 ? paths.size() - 1 : pathIndex - 1);
            hilightAndSendPaths();
        }
    }

    private final class SetModeHandler extends RequestHandler {
        public SetModeHandler() {
            super(PAINTER_SET_MODE);
        }

        @Override
        public void process(ObjectNode payload) {
            String mode = string(payload, MODE);
            switch (mode) {
                case "shortest":
                    currentMode = Mode.SHORTEST;
                    break;
                case "disjoint":
                    currentMode = Mode.DISJOINT;
                    break;
                case "geodata":
                    currentMode = Mode.GEODATA;
                    break;
                case "srlg":
                    currentMode = Mode.SRLG;
                    break;
                default:
                    currentMode = Mode.INVALID;
                    break;
            }
            //TODO: add support for SRLG
            findAndSendPaths(currentMode);
        }
    }

    // === ------------

    private ElementId elementId(String id) {
        try {
            return DeviceId.deviceId(id);
        } catch (IllegalArgumentException e) {
            return HostId.hostId(id);
        }
    }

    private void findAndSendPaths(Mode mode) {
        log.debug("src={}; dst={}; mode={}", src, dst, currentMode);
        if (src != null && dst != null) {
            pathIndex = 0;
            ImmutableSet.Builder<Link> builder = ImmutableSet.builder();
            if (mode.equals(Mode.SHORTEST)) {
                paths = ImmutableList.copyOf(pathService.getPaths(src, dst));
                allPathLinks = buildPaths(builder).build();
            } else if (mode.equals(Mode.DISJOINT)) {
                paths = ImmutableList.copyOf(pathService.getDisjointPaths(src, dst));
                allPathLinks = buildDisjointPaths(builder).build();
            } else if (mode.equals(Mode.GEODATA)) {
                paths = ImmutableList.copyOf(pathService.getPaths(src, dst, linkData));
                allPathLinks = buildPaths(builder).build();
            } else {
                log.warn("Unsupported MODE");
            }
        } else {
            paths = ImmutableList.of();
            allPathLinks = ImmutableSet.of();
        }
        hilightAndSendPaths();

    }

    private ImmutableSet.Builder<Link> buildPaths(ImmutableSet.Builder<Link> pathBuilder) {
        paths.forEach(path -> path.links().forEach(pathBuilder::add));
        return pathBuilder;
    }

    private ImmutableSet.Builder<Link> buildDisjointPaths(ImmutableSet.Builder<Link> pathBuilder) {
        paths.forEach(path -> {
            DisjointPath dp = (DisjointPath) path;
            pathBuilder.addAll(dp.primary().links());
            pathBuilder.addAll(dp.backup().links());
        });
        return pathBuilder;
    }

    private void hilightAndSendPaths() {
        PathLinkMap linkMap = new PathLinkMap();
        allPathLinks.forEach(linkMap::add);

        Set<Link> selectedPathLinks;

        // Prepare two working sets; one containing selected path links and
        // the other containing all paths links.
        if (currentMode.equals(Mode.DISJOINT)) {
            DisjointPath dp = (DisjointPath) paths.get(pathIndex);
            selectedPathLinks = paths.isEmpty() ?
                    ImmutableSet.of() : Sets.newHashSet(dp.primary().links());
            selectedPathLinks.addAll(dp.backup().links());
        } else {
            selectedPathLinks = paths.isEmpty() ?
                    ImmutableSet.of() : ImmutableSet.copyOf(paths.get(pathIndex).links());
        }
        Highlights highlights = new Highlights();
        if (highlightDelay > 0) {
            highlights.delay(highlightDelay);
        }
        for (PathLink plink : linkMap.biLinks()) {
            plink.computeHilight(selectedPathLinks, allPathLinks);
            highlights.add(plink.highlight(null));
        }
        if (src != null) {
            highlights = addBadge(highlights, srcType, src.toString(), SRC);
        }
        if (dst != null) {
            highlights = addBadge(highlights, dstType, dst.toString(), DST);
        }
        sendMessage(highlightsMessage(highlights));
    }

    private Highlights addBadge(Highlights highlights, String type, String elemId, String src) {
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

    private synchronized void addListeners() {
        listenersRemoved = false;
        topologyService.addListener(topologyListener);
    }

    private synchronized void removeListeners() {
        if (!listenersRemoved) {
            listenersRemoved = true;
            topologyService.removeListener(topologyListener);
        }
    }

    // Link event listener.
    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            highlightDelay = DELAY_MS;
            findAndSendPaths(currentMode);
            highlightDelay = 0;
        }
    }

}