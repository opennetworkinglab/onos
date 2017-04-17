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
package org.onosproject.bandwidthmgr;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.bandwidthmgr.api.BandwidthMgmtService;
import org.onosproject.bandwidthmgr.api.BandwidthMgmtStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of PCE service.
 */
@Component(immediate = true)
@Service
public class BandwidthManager implements BandwidthMgmtService {
    private static final Logger log = LoggerFactory.getLogger(BandwidthManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BandwidthMgmtStore store;

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Double getTeCost(LinkKey linkKey) {
        checkNotNull(linkKey);
        return store.getTeCost(linkKey);
    }

    @Override
    public Double getAvailableBandwidth(LinkKey linkKey) {
        checkNotNull(linkKey);
        Set<Double> unResvBw = getUnreservedBw(linkKey);
        Double localReservedBw = getAllocatedLocalReservedBw(linkKey);
        if (unResvBw != null && localReservedBw != null) {

            return unResvBw.iterator().next().doubleValue()
                    - localReservedBw.doubleValue();
        }
        if (unResvBw != null) {
            return unResvBw.iterator().next().doubleValue();
        } else {
            return null;
        }
    }

    @Override
    public boolean allocLocalReservedBw(LinkKey linkKey, Double bandwidth) {
        checkNotNull(linkKey);
        checkNotNull(bandwidth);
        return store.allocLocalReservedBw(linkKey, bandwidth);
    }

    @Override
    public boolean releaseLocalReservedBw(LinkKey linkkey, Double bandwidth) {
        checkNotNull(linkkey);
        checkNotNull(bandwidth);
        return store.releaseLocalReservedBw(linkkey, bandwidth);
    }

    @Override
    public Double getAllocatedLocalReservedBw(LinkKey linkkey) {
        checkNotNull(linkkey);
        return store.getAllocatedLocalReservedBw(linkkey);
    }

    @Override
    public boolean addUnreservedBw(LinkKey linkKey, Set<Double> bandwidth) {
        checkNotNull(linkKey);
        checkNotNull(bandwidth);
        return store.addUnreservedBw(linkKey, bandwidth);
    }

    @Override
    public boolean removeUnreservedBw(LinkKey linkkey) {
        checkNotNull(linkkey);
        return store.removeUnreservedBw(linkkey);
    }

    @Override
    public Set<Double> getUnreservedBw(LinkKey linkkey) {
        checkNotNull(linkkey);
        return store.getUnreservedBw(linkkey);
    }

    @Override
    public boolean isBandwidthAvailable(Link link, Double bandwidth) {
        checkNotNull(link);
        checkNotNull(bandwidth);

        LinkKey linkKey = LinkKey.linkKey(link);
        Double localAllocBw = getAllocatedLocalReservedBw(linkKey);

        Set<Double> unResvBw = getUnreservedBw(linkKey);
        Double prirZeroBw = unResvBw.iterator().next();

        return (bandwidth <= prirZeroBw -  (localAllocBw != null ? localAllocBw : 0));
    }
}
