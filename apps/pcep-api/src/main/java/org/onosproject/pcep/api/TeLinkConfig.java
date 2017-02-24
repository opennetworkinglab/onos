/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.pcep.api;

import org.onosproject.net.LinkKey;
import org.onosproject.net.config.Config;

/**
 * Configuration to specify traffic engineering parameters of the link.
 */
public class TeLinkConfig extends Config<LinkKey> {
    public static final String MAX_RESV_BW = "maxRervableBandwidth";
    public static final String UNRESV_BWS = "unReservedBandwidths";
    public static final String IGP_COST = "igpCost";
    public static final String TE_COST = "teCost";

    @Override
    public boolean isValid() {
        return hasOnlyFields(MAX_RESV_BW, UNRESV_BWS, IGP_COST, TE_COST);
    }

    /**
     * Gets the maximum reservable bandwidth of the link.
     *
     * @return maximum reservable bandwidth
     */
    public Double maxResvBandwidth() {

        String resvBw = get(MAX_RESV_BW, null);
        return resvBw != null ?
                Double.valueOf(resvBw) :
                0.0;
    }

    /**
     * Gets the set of unreserved bandwidth of the link.
     *
     * @return set of unreserved bandwidth
     */
    public Double unResvBandwidth() {
        String unResvBw = get(UNRESV_BWS, null);
        return unResvBw != null ? Double.valueOf(unResvBw) : 0.0;
    }

    /**
     * Gets the igp cost of the link.
     *
     * @return igp cost of the link
     */
    public int igpCost() {
        return get(IGP_COST, 0);
    }

    /**
     * Gets the te cost of the link.
     *
     * @return te cost of the link
     */
    public int teCost() {
        return get(TE_COST, 0);
    }

    /**
     * Sets the maximum reservable bandwidth of the link.
     *
     * @param maxResvBw maximum reservable bandwidth of link
     * @return te link configuration
     */
    public TeLinkConfig maxResvBandwidth(Double maxResvBw) {
        return (TeLinkConfig) setOrClear(MAX_RESV_BW, maxResvBw);
    }

    /**
     * Sets unreserved bandwidths of the link in priority order.
     *
     * @param unResvBw unreserved bandwidths of the link in priority order
     * @return te link configuration
     */
    public TeLinkConfig unResvBandwidth(Double unResvBw) {
        return (TeLinkConfig) setOrClear(UNRESV_BWS, unResvBw);
    }

    /**
     * Sets the igp cost of the link.
     *
     * @param igpCost igp cost of link
     * @return te link configuration
     */
    public TeLinkConfig igpCost(int igpCost) {
        return (TeLinkConfig) setOrClear(IGP_COST, igpCost);
    }

    /**
     * Sets the te cost of the link.
     *
     * @param teCost te cost of link
     * @return te link configuration
     */
    public TeLinkConfig teCost(int teCost) {
        return (TeLinkConfig) setOrClear(TE_COST, teCost);
    }


}

