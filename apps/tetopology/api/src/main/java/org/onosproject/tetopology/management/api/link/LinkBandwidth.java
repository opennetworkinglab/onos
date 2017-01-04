/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.Arrays;

/**
 * Representation the TE link bandwidths.
 */
public class LinkBandwidth {
    /**
     * Maximum bandwidth, Size is MAX_PRIORITY + 1.
     */
    private final float[] maxBandwidth;

    /**
     * Unreserved bandwidth, Size is MAX_PRIORITY + 1.
     */
    private final float[] availBandwidth;

    /**
     * Maximum available bandwidth for a LSP.
     */
    private final float[] maxAvailLspBandwidth;

    /**
     * Minimum available bandwidth for a LSP.
     */
    private final float[] minAvailLspBandwidth;

    /**
     * ODU resources.
     */
    private final OduResource odu;

    /**
     * Creates an instance of link bandwidth.
     *
     * @param maxBandwidth         the maximum bandwidth at each priority level
     * @param availBandwidth       the available bandwidth at each priority level
     * @param maxAvailLspBandwidth the maximum available bandwidth for a LSP at
     *                             each priority level
     * @param minAvailLspBandwidth the minimum available bandwidth for a LSP at
     *                             each priority level
     * @param odu                  ODU resources
     */
    public LinkBandwidth(float[] maxBandwidth,
                         float[] availBandwidth,
                         float[] maxAvailLspBandwidth,
                         float[] minAvailLspBandwidth,
                         OduResource odu) {
        this.maxBandwidth = maxBandwidth != null ?
                Arrays.copyOf(maxBandwidth, maxBandwidth.length) : null;
        this.availBandwidth = availBandwidth != null ?
                Arrays.copyOf(availBandwidth, availBandwidth.length) : null;
        this.maxAvailLspBandwidth = maxAvailLspBandwidth != null ?
                Arrays.copyOf(maxAvailLspBandwidth,
                              maxAvailLspBandwidth.length) : null;
        this.minAvailLspBandwidth = minAvailLspBandwidth != null ?
                Arrays.copyOf(minAvailLspBandwidth,
                              minAvailLspBandwidth.length) : null;
        this.odu = odu;
    }

    /**
     * Creates an instance of link bandwidth with a TE link.
     *
     * @param link the TE link
     */
    public LinkBandwidth(TeLink link) {
        this.maxBandwidth = link.maxBandwidth();
        this.availBandwidth = link.maxAvailLspBandwidth();
        this.maxAvailLspBandwidth = link.maxAvailLspBandwidth();
        this.minAvailLspBandwidth = link.minAvailLspBandwidth();
        this.odu = link.oduResource();
    }

    /**
     * Returns the maximum bandwidth at each priority level.
     *
     * @return the maxBandwidth
     */
    public float[] maxBandwidth() {
        if (maxBandwidth == null) {
            return null;
        }
        return Arrays.copyOf(maxBandwidth, maxBandwidth.length);
    }

    /**
     * Returns the available bandwidth at each priority level.
     *
     * @return the available bandwidth
     */
    public float[] availBandwidth() {
        if (availBandwidth == null) {
            return null;
        }
        return Arrays.copyOf(availBandwidth, availBandwidth.length);
    }

    /**
     * Returns the maximum available bandwidth for a LSP at each priority
     * level.
     *
     * @return the maximum available bandwidth
     */
    public float[] maxAvailLspBandwidth() {
        if (maxAvailLspBandwidth == null) {
            return null;
        }
        return Arrays.copyOf(maxAvailLspBandwidth, maxAvailLspBandwidth.length);
    }

    /**
     * Returns the minimum available bandwidth for a LSP at each priority level.
     *
     * @return the minimum available bandwidth
     */
    public float[] minAvailLspBandwidth() {
        if (minAvailLspBandwidth == null) {
            return null;
        }
        return Arrays.copyOf(minAvailLspBandwidth, minAvailLspBandwidth.length);
    }

    /**
     * Returns the link ODUk resources.
     *
     * @return the ODUk resources
     */
    public OduResource oduResource() {
        return odu;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Arrays.hashCode(maxBandwidth),
                Arrays.hashCode(availBandwidth),
                Arrays.hashCode(maxAvailLspBandwidth),
                Arrays.hashCode(minAvailLspBandwidth),
                odu);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof LinkBandwidth) {
            LinkBandwidth that = (LinkBandwidth) object;
            return Arrays.equals(maxBandwidth, that.maxBandwidth) &&
                    Arrays.equals(availBandwidth, that.availBandwidth) &&
                    Arrays.equals(maxAvailLspBandwidth, that.maxAvailLspBandwidth) &&
                    Arrays.equals(minAvailLspBandwidth, that.minAvailLspBandwidth) &&
                    Objects.equal(odu, that.odu);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("maxBandwidth", maxBandwidth)
                .add("availBandwidth", availBandwidth)
                .add("maxAvailLspBandwidth", maxAvailLspBandwidth)
                .add("minAvailLspBandwidth", minAvailLspBandwidth)
                .add("odu", odu)
                .toString();
    }
}
