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

package org.onosproject.ui.topo;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;

import java.text.DecimalFormat;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.LinkKey.linkKey;

/**
 * Utility methods for helping out with formatting data for the Topology View
 * in the web client.
 */
public final class TopoUtils {

    // explicit decision made to not 'javadoc' these constants
    public static final double N_KILO = 1024;
    public static final double N_MEGA = 1024 * N_KILO;
    public static final double N_GIGA = 1024 * N_MEGA;

    public static final String BITS_UNIT = "b";
    public static final String BYTES_UNIT = "B";
    public static final String PACKETS_UNIT = "p";

    private static final DecimalFormat DF2 = new DecimalFormat("#,###.##");

    private static final String COMPACT = "%s/%s-%s/%s";
    private static final String EMPTY = "";
    private static final String SPACE = " ";
    private static final String FLOW = "flow";
    private static final String FLOWS = "flows";

    // non-instantiable
    private TopoUtils() { }

    /**
     * Returns a compact identity for the given link, in the form
     * used to identify links in the Topology View on the client.
     *
     * @param link link
     * @return compact link identity
     */
    public static String compactLinkString(Link link) {
        return String.format(COMPACT, link.src().elementId(), link.src().port(),
                link.dst().elementId(), link.dst().port());
    }

    /**
     * Produces a canonical link key, that is, one that will match both a link
     * and its inverse.
     *
     * @param link the link
     * @return canonical key
     */
    public static LinkKey canonicalLinkKey(Link link) {
        String sn = link.src().elementId().toString();
        String dn = link.dst().elementId().toString();
        return sn.compareTo(dn) < 0 ?
                linkKey(link.src(), link.dst()) : linkKey(link.dst(), link.src());
    }

    /**
     * Returns a value representing a count of bytes.
     *
     * @param bytes number of bytes
     * @return value representing bytes
     */
    public static ValueLabel formatBytes(long bytes) {
        return new ValueLabel(bytes, BYTES_UNIT);
    }

    /**
     * Returns a value representing a count of packets per second.
     *
     * @param packets number of packets (per second)
     * @return value representing packets per second
     */
    public static ValueLabel formatPacketRate(long packets) {
        return new ValueLabel(packets, PACKETS_UNIT).perSec();
    }


    /**
     * Returns a value representing a count of bits per second,
     * (clipped to a maximum of 100 Gbps).
     * Note that the input is bytes per second.
     *
     * @param bytes bytes per second
     * @return value representing bits per second
     */
    public static ValueLabel formatClippedBitRate(long bytes) {
        return new ValueLabel(bytes * 8, BITS_UNIT).perSec().clipG(100.0);
    }

    /**
     * Returns human readable flow count, to be displayed as a label.
     *
     * @param flows number of flows
     * @return formatted flow count
     */
    public static String formatFlows(long flows) {
        if (flows < 1) {
            return EMPTY;
        }
        return String.valueOf(flows) + SPACE + (flows > 1 ? FLOWS : FLOW);
    }


    /**
     * Enumeration of magnitudes.
     */
    public enum Magnitude {
        ONE("", 1),
        KILO("K", N_KILO),
        MEGA("M", N_MEGA),
        GIGA("G", N_GIGA);

        private final String label;
        private final double mult;

        Magnitude(String label, double mult) {
            this.label = label;
            this.mult = mult;
        }

        @Override
        public String toString() {
            return label;
        }

        private double mult() {
            return mult;
        }
    }


    /**
     * Encapsulates a value to be used as a label.
     */
    public static class ValueLabel {
        private final long value;
        private final String unit;

        private double divDown;
        private Magnitude mag;

        private boolean perSec = false;
        private boolean clipped = false;

        /**
         * Creates a value label with the given base value and unit. For
         * example:
         * <pre>
         * ValueLabel bits = new ValueLabel(2_050, "b");
         * ValueLabel bytesPs = new ValueLabel(3_000_000, "B").perSec();
         * </pre>
         * Generating labels:
         * <pre>
         *   bits.toString()     ...  "2.00 Kb"
         *   bytesPs.toString()  ...  "2.86 MBps"
         * </pre>
         *
         * @param value the base value
         * @param unit  the value unit
         */
        public ValueLabel(long value, String unit) {
            this.value = value;
            this.unit = unit;
            computeAdjusted();
        }

        private void computeAdjusted() {
            if (value >= N_GIGA) {
                divDown = value / N_GIGA;
                mag = Magnitude.GIGA;
            } else if (value >= N_MEGA) {
                divDown = value / N_MEGA;
                mag = Magnitude.MEGA;
            } else if (value >= N_KILO) {
                divDown = value / N_KILO;
                mag = Magnitude.KILO;
            } else {
                divDown = value;
                mag = Magnitude.ONE;
            }
        }

        /**
         * Mark this value to be expressed as a rate. That is, "ps" (per sec)
         * will be appended in the string representation.
         *
         * @return self, for chaining
         */
        public ValueLabel perSec() {
            perSec = true;
            return this;
        }

        /**
         * Clips the (adjusted) value to the given threshold expressed in
         * Giga units. That is, if the adjusted value exceeds the threshold,
         * it will be set to the threshold value and the clipped flag
         * will be set. For example,
         * <pre>
         * ValueLabel tooMuch = new ValueLabel(12_000_000_000, "b")
         *      .perSec().clipG(10.0);
         *
         * tooMuch.toString()    ...  "10.00 Gbps"
         * tooMuch.clipped()     ...  true
         * </pre>
         *
         * @param threshold the clip threshold (Giga)
         * @return self, for chaining
         */
        public ValueLabel clipG(double threshold) {
            return clip(threshold, Magnitude.GIGA);
        }

        private ValueLabel clip(double threshold, Magnitude m) {
            checkArgument(threshold >= 1.0, "threshold must be 1.0 or more");
            double clipAt = threshold * m.mult();
            if (value > clipAt) {
                divDown = threshold;
                mag = m;
                clipped = true;
            }
            return this;
        }

        /**
         * Returns true if this value was clipped to a maximum threshold.
         *
         * @return true if value was clipped
         */
        public boolean clipped() {
            return clipped;
        }

        /**
         * Returns the magnitude value.
         *
         * @return the magnitude
         */
        public Magnitude magnitude() {
            return mag;
        }

        @Override
        public String toString() {
            return DF2.format(divDown) + SPACE + mag + unit + (perSec ? "ps" : "");
        }
    }
}
