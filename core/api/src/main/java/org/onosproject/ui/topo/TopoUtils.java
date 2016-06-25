/*
 * Copyright 2015-present Open Networking Laboratory
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

import static org.onosproject.net.LinkKey.linkKey;

/**
 * Utility methods for helping out with formatting data for the Topology View
 * in the web client.
 */
public final class TopoUtils {

    // explicit decision made to not 'javadoc' these self explanatory constants
    public static final double KILO = 1024;
    public static final double MEGA = 1024 * KILO;
    public static final double GIGA = 1024 * MEGA;

    public static final String GBITS_UNIT = "Gb";
    public static final String MBITS_UNIT = "Mb";
    public static final String KBITS_UNIT = "Kb";
    public static final String BITS_UNIT = "b";
    public static final String GBYTES_UNIT = "GB";
    public static final String MBYTES_UNIT = "MB";
    public static final String KBYTES_UNIT = "KB";
    public static final String BYTES_UNIT = "B";


    private static final DecimalFormat DF2 = new DecimalFormat("#,###.##");

    private static final String COMPACT = "%s/%s-%s/%s";
    private static final String EMPTY = "";
    private static final String SPACE = " ";
    private static final String PER_SEC = "ps";
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
     * Returns human readable count of bytes, to be displayed as a label.
     *
     * @param bytes number of bytes
     * @return formatted byte count
     */
    public static String formatBytes(long bytes) {
        String unit;
        double value;
        if (bytes > GIGA) {
            value = bytes / GIGA;
            unit = GBYTES_UNIT;
        } else if (bytes > MEGA) {
            value = bytes / MEGA;
            unit = MBYTES_UNIT;
        } else if (bytes > KILO) {
            value = bytes / KILO;
            unit = KBYTES_UNIT;
        } else {
            value = bytes;
            unit = BYTES_UNIT;
        }
        return DF2.format(value) + SPACE + unit;
    }

    /**
     * Returns human readable bit rate, to be displayed as a label.
     *
     * @param bytes bytes per second
     * @return formatted bits per second
     */
    public static String formatBitRate(long bytes) {
        String unit;
        double value;

        //Convert to bits
        long bits = bytes * 8;
        if (bits > GIGA) {
            value = bits / GIGA;
            unit = GBITS_UNIT;

            // NOTE: temporary hack to clip rate at 10.0 Gbps
            //  Added for the CORD Fabric demo at ONS 2015
            // TODO: provide a more elegant solution to this issue
            if (value > 10.0) {
                value = 10.0;
            }

        } else if (bits > MEGA) {
            value = bits / MEGA;
            unit = MBITS_UNIT;
        } else if (bits > KILO) {
            value = bits / KILO;
            unit = KBITS_UNIT;
        } else {
            value = bits;
            unit = BITS_UNIT;
        }
        return DF2.format(value) + SPACE + unit + PER_SEC;
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
}
