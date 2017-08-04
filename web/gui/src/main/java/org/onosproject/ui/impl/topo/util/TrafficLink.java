/*
 *  Copyright 2016-present Open Networking Foundation
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
import org.onosproject.ui.model.topo.UiLinkId;
import org.onosproject.ui.topo.BiLink;
import org.onosproject.ui.topo.LinkHighlight;
import org.onosproject.ui.topo.LinkHighlight.Flavor;
import org.onosproject.ui.topo.Mod;
import org.onosproject.ui.topo.TopoUtils.Magnitude;
import org.onosproject.ui.topo.TopoUtils.ValueLabel;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.ui.topo.LinkHighlight.Flavor.NO_HIGHLIGHT;
import static org.onosproject.ui.topo.LinkHighlight.Flavor.PRIMARY_HIGHLIGHT;
import static org.onosproject.ui.topo.LinkHighlight.Flavor.SECONDARY_HIGHLIGHT;
import static org.onosproject.ui.topo.TopoUtils.formatBytes;
import static org.onosproject.ui.topo.TopoUtils.formatClippedBitRate;
import static org.onosproject.ui.topo.TopoUtils.formatFlows;
import static org.onosproject.ui.topo.TopoUtils.formatPacketRate;

/**
 * Representation of a link and its inverse, and associated traffic data.
 * This class understands how to generate the appropriate
 * {@link LinkHighlight}s for showing traffic data on the topology view.
 */
public class TrafficLink extends BiLink {
    private static final Mod PORT_TRAFFIC_GREEN = new Mod("port-traffic-green");
    private static final Mod PORT_TRAFFIC_YELLOW = new Mod("port-traffic-yellow");
    private static final Mod PORT_TRAFFIC_ORANGE = new Mod("port-traffic-orange");
    private static final Mod PORT_TRAFFIC_RED = new Mod("port-traffic-red");

    private static final String EMPTY = "";

    private long bytes = 0;
    private long rate = 0;
    private long flows = 0;
    private Flavor taggedFlavor = NO_HIGHLIGHT;
    private boolean hasTraffic = false;
    private boolean isOptical = false;
    private boolean antMarch = false;
    private Set<Mod> mods = new HashSet<>();

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
     * Returns an "empty" traffic link (one with no underlying links or stats)
     * with the given identifier. This is useful when we want to aggregate
     * stats from other links into a single entity (such as a region-region
     * link reporting the stats for the links that compose it).
     *
     * @param id the link identifier
     */
    public TrafficLink(UiLinkId id) {
        super(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TrafficLink that = (TrafficLink) o;

        return bytes == that.bytes && rate == that.rate &&
                flows == that.flows && hasTraffic == that.hasTraffic &&
                isOptical == that.isOptical && antMarch == that.antMarch &&
                taggedFlavor == that.taggedFlavor && mods.equals(that.mods);
    }

    @Override
    public int hashCode() {
        int result = (int) (bytes ^ (bytes >>> 32));
        result = 31 * result + (int) (rate ^ (rate >>> 32));
        result = 31 * result + (int) (flows ^ (flows >>> 32));
        result = 31 * result + taggedFlavor.hashCode();
        result = 31 * result + (hasTraffic ? 1 : 0);
        result = 31 * result + (isOptical ? 1 : 0);
        result = 31 * result + (antMarch ? 1 : 0);
        result = 31 * result + mods.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("linkId", linkId())
                .add("bytes", bytes)
                .add("rate", rate)
                .add("flows", flows)
                .toString();
    }

    /**
     * Returns the count of bytes.
     *
     * @return the byte count
     */
    public long bytes() {
        return bytes;
    }

    /**
     * Returns the rate.
     *
     * @return the rate
     */
    public long rate() {
        return rate;
    }

    /**
     * Returns the flows.
     *
     * @return flow count
     */
    public long flows() {
        return flows;
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
        taggedFlavor = flavor;
        return this;
    }

    /**
     * Tags this traffic link with the mods to be used in visual rendering.
     *
     * @param mods the mods to tag on this link
     * @return self, for chaining
     */
    public TrafficLink tagMods(Set<Mod> mods) {
        if (mods != null) {
            this.mods.addAll(mods);
        }
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
     * @param load      load to add
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

    /**
     * Merges the load recorded on the given traffic link into this one.
     *
     * @param other the other traffic link
     */
    public void mergeStats(TrafficLink other) {
        this.bytes += other.bytes;
        this.rate += other.rate;
        this.flows += other.flows;
    }


    @Override
    public LinkHighlight highlight(Enum<?> type) {
        StatsType statsType = (StatsType) type;
        switch (statsType) {
            case FLOW_COUNT:
                return highlightForFlowCount();

            case FLOW_STATS:
            case PORT_STATS:
            case PORT_PACKET_STATS:
                return highlightForStats(statsType);

            case TAGGED:
                return highlightForTagging();

            default:
                throw new IllegalStateException("unexpected case: " + statsType);
        }
    }

    private LinkHighlight highlightForStats(StatsType type) {
        ValueLabel vl = null;
        Mod m = null;

        // based on the type of stats, need to determine the label and "color"...
        switch (type) {
            case FLOW_STATS:
                vl = formatBytes(bytes);
                // default to "secondary highlighting" of link
                break;

            case PORT_STATS:
                vl = formatClippedBitRate(rate);

                // set color based on bits per second...
                if (vl.magnitude() == Magnitude.ONE ||
                        vl.magnitude() == Magnitude.KILO) {
                    m = PORT_TRAFFIC_GREEN;

                } else if (vl.magnitude() == Magnitude.MEGA) {
                    m = PORT_TRAFFIC_YELLOW;

                } else if (vl.magnitude() == Magnitude.GIGA) {
                    m = vl.clipped() ? PORT_TRAFFIC_RED : PORT_TRAFFIC_ORANGE;
                }
                break;

            case PORT_PACKET_STATS:
                vl = formatPacketRate(rate);

                // FIXME: Provisional color threshold parameters for packets
                // set color based on bits per second...
                if (rate < 10) {
                    m = PORT_TRAFFIC_GREEN;

                } else if (rate < 1000) {
                    m = PORT_TRAFFIC_YELLOW;

                } else if (rate < 100000) {
                    m = PORT_TRAFFIC_ORANGE;
                } else {
                    m = PORT_TRAFFIC_RED;
                }
                break;

            default:
                break;
        }

        LinkHighlight hlite = new LinkHighlight(linkId(), SECONDARY_HIGHLIGHT);
        if (vl != null) {
            hlite.setLabel(vl.toString());
        }
        if (m != null) {
            hlite.addMod(m);
        }

        return addCustomMods(hlite);
    }

    private LinkHighlight highlightForFlowCount() {
        Flavor flavor = flows > 0 ? PRIMARY_HIGHLIGHT : SECONDARY_HIGHLIGHT;
        LinkHighlight hlite = new LinkHighlight(linkId(), flavor)
                .setLabel(formatFlows(flows));

        return addCustomMods(hlite);
    }

    private LinkHighlight highlightForTagging() {
        LinkHighlight hlite = new LinkHighlight(linkId(), taggedFlavor)
                .setLabel(hasTraffic ? formatBytes(bytes).toString() : EMPTY);

        if (isOptical) {
            hlite.addMod(LinkHighlight.MOD_OPTICAL);
        }
        if (antMarch) {
            hlite.addMod(LinkHighlight.MOD_ANIMATED);
        }
        return addCustomMods(hlite);
    }

    private LinkHighlight addCustomMods(LinkHighlight hlite) {
        if (!mods.isEmpty()) {
            mods.forEach(hlite::addMod);
        }
        return hlite;
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
         * Number of packets per second.
         */
        PORT_PACKET_STATS,

        /**
         * Custom tagged information.
         */
        TAGGED
    }
}
