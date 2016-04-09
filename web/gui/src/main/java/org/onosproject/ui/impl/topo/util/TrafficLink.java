/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.impl.topo.util;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.statistic.Load;
import org.onosproject.ui.topo.BiLink;
import org.onosproject.ui.topo.LinkHighlight;
import org.onosproject.ui.topo.LinkHighlight.Flavor;
import org.onosproject.ui.topo.TopoUtils;

import static org.onosproject.ui.topo.LinkHighlight.Flavor.NO_HIGHLIGHT;
import static org.onosproject.ui.topo.LinkHighlight.Flavor.PRIMARY_HIGHLIGHT;
import static org.onosproject.ui.topo.LinkHighlight.Flavor.SECONDARY_HIGHLIGHT;

/**
 * Representation of a link and its inverse, and associated traffic data.
 * This class understands how to generate the appropriate
 * {@link LinkHighlight}s for showing traffic data on the topology view.
 */
public class TrafficLink extends BiLink {

    private static final String EMPTY = "";
    private static final String QUE = "?";

    private long bytes = 0;
    private long rate = 0;
    private long flows = 0;
    private Flavor taggedFlavor = NO_HIGHLIGHT;
    private boolean hasTraffic = false;
    private boolean isOptical = false;
    private boolean antMarch = false;

    /**
     * Constructs a traffic link for the given key and initial link.
     *
     * @param key  canonical key for this traffic link
     * @param link first link
     */
    public TrafficLink(LinkKey key, Link link) {
        super(key, link);
    }

    /**
     * Sets the optical flag to the given value.
     *
     * @param b true if an optical link
     * @return self, for chaining
     */
    public TrafficLink optical(boolean b) {
        isOptical = b;
        return this;
    }

    /**
     * Sets the ant march flag to the given value.
     *
     * @param b true if marching ants required
     * @return self, for chaining
     */
    public TrafficLink antMarch(boolean b) {
        antMarch = b;
        return this;
    }

    /**
     * Tags this traffic link with the flavor to be used in visual rendering.
     *
     * @param flavor the flavor to tag
     * @return self, for chaining
     */
    public TrafficLink tagFlavor(Flavor flavor) {
        this.taggedFlavor = flavor;
        return this;
    }

    /**
     * Adds load statistics, marks the traffic link as having traffic.
     *
     * @param load load to add
     */
    public void addLoad(Load load) {
        addLoad(load, 0);
    }

    /**
     * Adds load statistics, marks the traffic link as having traffic, if the
     * load {@link Load#rate rate} is greater than the given threshold
     * (expressed in bytes per second).
     *
     * @param load load to add
     * @param threshold threshold to register traffic
     */
    public void addLoad(Load load, double threshold) {
        if (load != null) {
            this.hasTraffic = hasTraffic || load.rate() > threshold;
            this.bytes += load.latest();
            this.rate += load.rate();
        }
    }

    /**
     * Adds the given count of flows to this traffic link.
     *
     * @param count count of flows
     */
    public void addFlows(int count) {
        this.flows += count;
    }

    @Override
    public LinkHighlight highlight(Enum<?> type) {
        StatsType statsType = (StatsType) type;
        switch (statsType) {
            case FLOW_COUNT:
                return highlightForFlowCount(statsType);

            case FLOW_STATS:
            case PORT_STATS:
                return highlightForStats(statsType);

            case TAGGED:
                return highlightForTagging(statsType);

            default:
                throw new IllegalStateException("unexpected case: " + statsType);
        }
    }

    private LinkHighlight highlightForStats(StatsType type) {
        return new LinkHighlight(linkId(), SECONDARY_HIGHLIGHT)
                .setLabel(generateLabel(type));
    }

    private LinkHighlight highlightForFlowCount(StatsType type) {
        Flavor flavor = flows > 0 ? PRIMARY_HIGHLIGHT : SECONDARY_HIGHLIGHT;
        return new LinkHighlight(linkId(), flavor)
                .setLabel(generateLabel(type));
    }

    private LinkHighlight highlightForTagging(StatsType type) {
        LinkHighlight hlite = new LinkHighlight(linkId(), taggedFlavor)
                .setLabel(generateLabel(type));
        if (isOptical) {
            hlite.addMod(LinkHighlight.MOD_OPTICAL);
        }
        if (antMarch) {
            hlite.addMod(LinkHighlight.MOD_ANIMATED);
        }
        return hlite;
    }

    // Generates a string representation of the load, to be used as a label
    private String generateLabel(StatsType type) {
        switch (type) {
            case FLOW_COUNT:
                return TopoUtils.formatFlows(flows);

            case FLOW_STATS:
                return TopoUtils.formatBytes(bytes);

            case PORT_STATS:
                return TopoUtils.formatBitRate(rate);

            case TAGGED:
                return hasTraffic ? TopoUtils.formatBytes(bytes) : EMPTY;

            default:
                return QUE;
        }
    }

    /**
     * Returns true if this link has been deemed to have enough traffic
     * to register on the topology view in the web UI.
     *
     * @return true if this link has displayable traffic
     */
    public boolean hasTraffic() {
        return hasTraffic;
    }

    /**
     * Designates type of traffic statistics to report on a highlighted link.
     */
    public enum StatsType {
        /**
         * Number of flows.
         */
        FLOW_COUNT,

        /**
         * Number of bytes.
         */
        FLOW_STATS,

        /**
         * Number of bits per second.
         */
        PORT_STATS,

        /**
         * Custom tagged information.
         */
        TAGGED
    }
}
