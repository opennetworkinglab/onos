/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.statistic.Load;
import org.onosproject.ui.topo.LinkHighlight;

import static org.onosproject.ui.topo.LinkHighlight.Flavor.NO_HIGHLIGHT;
import static org.onosproject.ui.topo.LinkHighlight.Flavor.PRIMARY_HIGHLIGHT;
import static org.onosproject.ui.topo.LinkHighlight.Flavor.SECONDARY_HIGHLIGHT;

/**
 * Representation of a link and its inverse, and any associated traffic data.
 * This class understands how to generate {@link LinkHighlight}s for sending
 * back to the topology view.
 */
public class BiLink {

    private static final String EMPTY = "";

    private final LinkKey key;
    private final Link one;
    private Link two;

    private boolean hasTraffic = false;
    private long bytes = 0;
    private long rate = 0;
    private long flows = 0;
    private boolean isOptical = false;
    private LinkHighlight.Flavor taggedFlavor = NO_HIGHLIGHT;
    private boolean antMarch = false;

    /**
     * Constructs a bilink for the given key and initial link.
     *
     * @param key canonical key for this bilink
     * @param link first link
     */
    public BiLink(LinkKey key, Link link) {
        this.key = key;
        this.one = link;
    }

    /**
     * Sets the second link for this bilink.
     *
     * @param link second link
     */
    public void setOther(Link link) {
        this.two = link;
    }

    /**
     * Sets the optical flag to the given value.
     *
     * @param b true if an optical link
     */
    public void setOptical(boolean b) {
        isOptical = b;
    }

    /**
     * Sets the ant march flag to the given value.
     *
     * @param b true if marching ants required
     */
    public void setAntMarch(boolean b) {
        antMarch = b;
    }

    /**
     * Tags this bilink with a link flavor to be used in visual rendering.
     *
     * @param flavor the flavor to tag
     */
    public void tagFlavor(LinkHighlight.Flavor flavor) {
        this.taggedFlavor = flavor;
    }

    /**
     * Adds load statistics, marks the bilink as having traffic.
     *
     * @param load load to add
     */
    public void addLoad(Load load) {
        addLoad(load, 0);
    }

    /**
     * Adds load statistics, marks the bilink as having traffic, if the
     * load rate is greater than the given threshold.
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
     * Adds the given count of flows to this bilink.
     *
     * @param count count of flows
     */
    public void addFlows(int count) {
        this.flows += count;
    }

    /**
     * Generates a link highlight entity, based on state of this bilink.
     *
     * @param type the type of statistics to use to interpret the data
     * @return link highlight data for this bilink
     */
    public LinkHighlight generateHighlight(LinkStatsType type) {
        switch (type) {
            case FLOW_COUNT:
                return highlightForFlowCount(type);

            case FLOW_STATS:
            case PORT_STATS:
                return highlightForStats(type);

            case TAGGED:
                return highlightForTagging(type);

            default:
                throw new IllegalStateException("unexpected case: " + type);
        }
    }

    private LinkHighlight highlightForStats(LinkStatsType type) {
        return new LinkHighlight(linkId(), SECONDARY_HIGHLIGHT)
                .setLabel(generateLabel(type));
    }

    private LinkHighlight highlightForFlowCount(LinkStatsType type) {
        LinkHighlight.Flavor flavor = flows() > 0 ?
                PRIMARY_HIGHLIGHT : SECONDARY_HIGHLIGHT;
        return new LinkHighlight(linkId(), flavor)
                .setLabel(generateLabel(type));
    }

    private LinkHighlight highlightForTagging(LinkStatsType type) {
        LinkHighlight hlite = new LinkHighlight(linkId(), flavor())
                .setLabel(generateLabel(type));
        if (isOptical()) {
            hlite.addMod(LinkHighlight.MOD_OPTICAL);
        }
        if (isAntMarch()) {
            hlite.addMod(LinkHighlight.MOD_ANIMATED);
        }
        return hlite;
    }

    // Generates a link identifier in the form that the Topology View on the
    private String linkId() {
        return TopoUtils.compactLinkString(one);
    }

    // Generates a string representation of the load, to be used as a label
    private String generateLabel(LinkStatsType type) {
        switch (type) {
            case FLOW_COUNT:
                return TopoUtils.formatFlows(flows());

            case FLOW_STATS:
                return TopoUtils.formatBytes(bytes());

            case PORT_STATS:
                return TopoUtils.formatBitRate(rate());

            case TAGGED:
                return hasTraffic() ? TopoUtils.formatBytes(bytes()) : EMPTY;

            default:
                return "?";
        }
    }

    // === ----------------------------------------------------------------
    // accessors

    public LinkKey key() {
        return key;
    }

    public Link one() {
        return one;
    }

    public Link two() {
        return two;
    }

    public boolean hasTraffic() {
        return hasTraffic;
    }

    public boolean isOptical() {
        return isOptical;
    }

    public boolean isAntMarch() {
        return antMarch;
    }

    public LinkHighlight.Flavor flavor() {
        return taggedFlavor;
    }

    public long bytes() {
        return bytes;
    }

    public long rate() {
        return rate;
    }

    public long flows() {
        return flows;
    }
}
