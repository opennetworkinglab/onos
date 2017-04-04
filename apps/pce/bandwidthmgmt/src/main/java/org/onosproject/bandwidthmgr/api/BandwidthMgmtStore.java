/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.bandwidthmgr.api;

import org.onosproject.net.LinkKey;

import java.util.Set;

/**
 * Abstraction of an entity providing pool of available labels to devices, links and tunnels.
 */
public interface BandwidthMgmtStore {

    /**
     * Allocate local bandwidth(non rsvp-te) to linkKey mapping.
     *
     * @param linkkey link key of the link
     * @param bandwidth requested local bandwidth
     * @return success or failure
     */
    boolean allocLocalReservedBw(LinkKey linkkey, Double bandwidth);


    /**
     * Release local bandwidth(non rsvp-te) to linkKey mapping.
     *
     * @param linkkey link key of the link
     * @param bandwidth releasing local bandwidth
     * @return success or failure
     */
    boolean releaseLocalReservedBw(LinkKey linkkey, Double bandwidth);

    /**
     * Get local allocated bandwidth of the link.
     *
     * @param linkkey link key of the link
     * @return allocated bandwidth
     */
    Double getAllocatedLocalReservedBw(LinkKey linkkey);

    /**
     * Add unreserved bandwidth to linkKey mapping.
     *
     * @param linkkey link key of the link
     * @param bandwidth set of unreserved bandwidth
     * @return success or failure
     */
    boolean addUnreservedBw(LinkKey linkkey, Set<Double> bandwidth);

    /**
     * Remove unreserved bandwidth to linkKey mapping.
     *
     * @param linkkey link key of the link
     * @return success or failure
     */
    boolean removeUnreservedBw(LinkKey linkkey);

    /**
     * Get list of unreserved Bandwidth of the link.
     *
     * @param linkkey link key of the link
     * @return Set of unreserved bandwidth
     */
    Set<Double> getUnreservedBw(LinkKey linkkey);

    /**
     * Returns Te cost for the specified link key.
     *
     * @param linkKey connect point of source and destination of the link
     * @return Te cost for the linkKey
     */
    Double getTeCost(LinkKey linkKey);
}
