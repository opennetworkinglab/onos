/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.segmentrouting;

import org.onlab.graph.DefaultEdgeWeigher;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;

import java.util.Set;

/**
 * Link weigher for multicast related path computations.
 */
public final class SRLinkWeigher
        extends DefaultEdgeWeigher<TopologyVertex, TopologyEdge>
        implements LinkWeigher {

    private final SegmentRoutingManager srManager;
    private final DeviceId srcPath;
    private final Set<Link> linksToEnforce;

    // Weight for the link to avoids. The high level idea is to build
    // a constrained shortest path computation. 100 should provide a good
    // threshold
    public static final ScalarWeight LINK_TO_AVOID_WEIGHT = new ScalarWeight(HOP_WEIGHT_VALUE + 100);

    /**
     * Creates a SRLinkWeigher object.
     *
     * @param srManager SegmentRoutingManager object
     * @param srcPath the source of the paths
     * @param linksToEnforce links to be enforced by the path computation
     */
    public SRLinkWeigher(SegmentRoutingManager srManager, DeviceId srcPath,
                         Set<Link> linksToEnforce) {
        this.srManager = srManager;
        this.srcPath = srcPath;
        this.linksToEnforce = linksToEnforce;
    }

    @Override
    public Weight weight(TopologyEdge edge) {
        // 1) We need to avoid some particular paths like leaf-spine-leaf-*
        // 2) Properly handle the pair links

        // If the link is a pair link just return infinite value
        if (isPairLink(edge.link())) {
            return ScalarWeight.NON_VIABLE_WEIGHT;
        }

        // To avoid that the paths go through other leaves we need to influence
        // the path computation to return infinite value for all other links having
        // as a src a leaf different from the source we are passing to the weigher
        DeviceId srcDeviceLink = edge.link().src().deviceId();
        // Identify the link as leaf-spine link
        boolean isLeafSpine;
        try {
            isLeafSpine = srManager.deviceConfiguration().isEdgeDevice(srcDeviceLink);
        } catch (DeviceConfigNotFoundException e) {
            isLeafSpine = false;
        }
        // If it is not the source just return infinite value
        if (isLeafSpine && !srcDeviceLink.equals(srcPath)) {
            return ScalarWeight.NON_VIABLE_WEIGHT;
        }

        // If the links are not in the list of the links to be enforce
        if (!linksToEnforce.isEmpty() && !linksToEnforce.contains(edge.link())) {
            // 100 should be a good confidence threshold
            return LINK_TO_AVOID_WEIGHT;
        }

        // All other cases we return
        return new ScalarWeight(HOP_WEIGHT_VALUE);
    }

    // Utility method to verify is a link is a pair-link
    private boolean isPairLink(Link link) {
        // Take src id, src port, dst id and dst port
        final DeviceId srcId = link.src().deviceId();
        final PortNumber srcPort = link.src().port();
        final DeviceId dstId = link.dst().deviceId();
        final PortNumber dstPort = link.dst().port();
        // init as true
        boolean isPairLink = true;
        try {
            // If one of this condition is not true; it is not a pair link
            if (!(srManager.deviceConfiguration().isEdgeDevice(srcId) &&
                    srManager.deviceConfiguration().isEdgeDevice(dstId) &&
                    srManager.deviceConfiguration().getPairDeviceId(srcId).equals(dstId) &&
                    srManager.deviceConfiguration().getPairLocalPort(srcId).equals(srcPort) &&
                    srManager.deviceConfiguration().getPairLocalPort(dstId).equals(dstPort))) {
                isPairLink = false;
            }
        } catch (DeviceConfigNotFoundException e) {
            // Configuration not provided
            isPairLink = false;
        }
        return isPairLink;
    }

}
