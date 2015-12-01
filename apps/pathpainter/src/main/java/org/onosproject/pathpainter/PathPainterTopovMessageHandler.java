/*
 * Copyright 2014,2015 Open Networking Laboratory
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
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.topology.PathService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.TopoJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Skeletal ONOS UI Topology-Overlay message handler.
 */
public class PathPainterTopovMessageHandler extends UiMessageHandler {

    private static final String PAINTER_SET_SRC = "ppTopovSetSrc";
    private static final String PAINTER_SET_DST = "ppTopovSetDst";
    private static final String PAINTER_SWAP_SRC_DST = "ppTopovSwapSrcDst";
    private static final String PAINTER_SET_MODE = "ppTopovSetMode";

    private static final String PAINTER_NEXT_PATH = "ppTopovNextPath";
    private static final String PAINTER_PREV_PATH = "ppTopovPrevPath";

    private static final String ID = "id";
    private static final String MODE = "mode";

    private Set<Link> allPathLinks;

    private enum Mode {
        SHORTEST, DISJOINT, SRLG
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private PathService pathService;

    private Mode currentMode = Mode.SHORTEST;
    private ElementId src, dst;
    private Mode mode = Mode.SHORTEST;
    private List<Path> paths;
    private int pathIndex;


    // ===============-=-=-=-=-=-======================-=-=-=-=-=-=-================================


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        pathService = directory.get(PathService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new SetSrcHandler(),
                new SetDstHandler(),
                new SwapSrcDstHandler(),
                new NextPathHandler(),
                new PrevPathHandler()
        );
    }

    // === -------------------------
    // === Handler classes

    private final class SetSrcHandler extends RequestHandler {
        public SetSrcHandler() {
            super(PAINTER_SET_SRC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String id = string(payload, ID);
            src = elementId(id);
            if (src.equals(dst)) {
                dst = null;
            }
            findAndSendPaths();
        }
    }

    private final class SetDstHandler extends RequestHandler {
        public SetDstHandler() {
            super(PAINTER_SET_DST);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String id = string(payload, ID);
            dst = elementId(id);
            if (src.equals(dst)) {
                src = null;
            }
            findAndSendPaths();
        }
    }

    private final class SwapSrcDstHandler extends RequestHandler {
        public SwapSrcDstHandler() {
            super(PAINTER_SWAP_SRC_DST);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            ElementId temp = src;
            src = dst;
            dst = temp;
            findAndSendPaths();
        }
    }

    private final class NextPathHandler extends RequestHandler {
        public NextPathHandler() {
            super(PAINTER_NEXT_PATH);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            pathIndex = (pathIndex >= paths.size() - 1 ? 0 : pathIndex + 1);
            hilightAndSendPaths();
        }
    }

    private final class PrevPathHandler extends RequestHandler {
        public PrevPathHandler() {
            super(PAINTER_PREV_PATH);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            pathIndex = (pathIndex <= 0 ? paths.size() - 1 : pathIndex - 1);
            hilightAndSendPaths();
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

    private void findAndSendPaths() {
        log.info("src={}; dst={}; mode={}", src, dst, mode);
        if (src != null && dst != null) {
            paths = ImmutableList.copyOf(pathService.getPaths(src, dst));
            pathIndex = 0;

            ImmutableSet.Builder<Link> builder = ImmutableSet.builder();
            paths.forEach(path -> path.links().forEach(builder::add));
            allPathLinks = builder.build();
        } else {
            paths = ImmutableList.of();
            allPathLinks = ImmutableSet.of();
        }
        hilightAndSendPaths();
    }

    private void hilightAndSendPaths() {
        PathLinkMap linkMap = new PathLinkMap();
        allPathLinks.forEach(linkMap::add);

        // Prepare two working sets; one containing selected path links and
        // the other containing all paths links.
        Set<Link> selectedPathLinks = paths.isEmpty() ?
                ImmutableSet.of() : ImmutableSet.copyOf(paths.get(pathIndex).links());

        Highlights highlights = new Highlights();
        for (PathLink plink : linkMap.biLinks()) {
            plink.computeHilight(selectedPathLinks, allPathLinks);
            highlights.add(plink.highlight(null));
        }

        sendMessage(TopoJson.highlightsMessage(highlights));
    }

    /*
    private void addDeviceBadge(Highlights h, DeviceId devId, int n) {
        DeviceHighlight dh = new DeviceHighlight(devId.toString());
        dh.setBadge(createBadge(n));
        h.add(dh);
    }

    private NodeBadge createBadge(int n) {
        Status status = n > 3 ? Status.ERROR : Status.WARN;
        String noun = n > 3 ? "(critical)" : "(problematic)";
        String msg = "Egress links: " + n + " " + noun;
        return NodeBadge.number(status, n, msg);
    }
   */

}