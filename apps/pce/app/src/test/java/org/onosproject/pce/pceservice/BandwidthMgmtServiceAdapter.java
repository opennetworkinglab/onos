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
package org.onosproject.pce.pceservice;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.bandwidthmgr.api.BandwidthMgmtService;

import java.util.Set;

/**
 * Adapter for Bandwidth Management service.
 */
public class BandwidthMgmtServiceAdapter implements BandwidthMgmtService {
    @Override
    public boolean allocLocalReservedBw(LinkKey linkkey, Double bandwidth) {
        return false;
    }

    @Override
    public boolean releaseLocalReservedBw(LinkKey linkkey, Double bandwidth) {
        return false;
    }

    @Override
    public Double getAllocatedLocalReservedBw(LinkKey linkkey) {
        return null;
    }

    @Override
    public boolean addUnreservedBw(LinkKey linkkey, Set<Double> bandwidth) {
        return false;
    }

    @Override
    public boolean removeUnreservedBw(LinkKey linkkey) {
        return false;
    }

    @Override
    public Set<Double> getUnreservedBw(LinkKey linkkey) {
        return null;
    }

    @Override
    public boolean isBandwidthAvailable(Link link, Double bandwidth) {
        return false;
    }

    @Override
    public Double getTeCost(LinkKey linkKey) {
        return null;
    }

    @Override
    public Double getAvailableBandwidth(LinkKey linkKey) {
        return null;
    }
}
