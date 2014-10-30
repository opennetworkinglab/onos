/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.store.trivial.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.resource.Bandwidth;
import org.onlab.onos.net.resource.BandwidthResourceAllocation;
import org.onlab.onos.net.resource.Lambda;
import org.onlab.onos.net.resource.LambdaResourceAllocation;
import org.onlab.onos.net.resource.LinkResourceAllocations;
import org.onlab.onos.net.resource.LinkResourceStore;
import org.onlab.onos.net.resource.ResourceAllocation;
import org.onlab.onos.net.resource.ResourceType;
import org.slf4j.Logger;

/**
 * Manages link resources using trivial in-memory structures implementation.
 */
@Component(immediate = true)
@Service
public class SimpleLinkResourceStore implements LinkResourceStore {
    private final Logger log = getLogger(getClass());
    private Map<IntentId, LinkResourceAllocations> linkResourceAllocationsMap;
    private Map<Link, Set<LinkResourceAllocations>> allocatedResources;
    private Map<Link, Set<ResourceAllocation>> freeResources;

    @Activate
    public void activate() {
        linkResourceAllocationsMap = new HashMap<>();
        allocatedResources = new HashMap<>();
        freeResources = new HashMap<>();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    /**
     * Returns free resources for a given link obtaining from topology
     * information.
     *
     * @param link the target link
     * @return free resources
     */
    private Set<ResourceAllocation> readOriginalFreeResources(Link link) {
        // TODO read capacity and lambda resources from topology
        Set<ResourceAllocation> allocations = new HashSet<>();
        for (int i = 1; i <= 100; i++) {
            allocations.add(new LambdaResourceAllocation(Lambda.valueOf(i)));
        }
        allocations.add(new BandwidthResourceAllocation(Bandwidth.valueOf(1000000)));
        return allocations;
    }

    /**
     * Finds and returns {@link BandwidthResourceAllocation} object from a given
     * set.
     *
     * @param freeRes a set of ResourceAllocation object.
     * @return {@link BandwidthResourceAllocation} object if found, otherwise
     *         {@link BandwidthResourceAllocation} object with 0 bandwidth
     *
     */
    private BandwidthResourceAllocation getBandwidth(Set<ResourceAllocation> freeRes) {
        for (ResourceAllocation res : freeRes) {
            if (res.type() == ResourceType.BANDWIDTH) {
                return (BandwidthResourceAllocation) res;
            }
        }
        return new BandwidthResourceAllocation(Bandwidth.valueOf(0));
    }

    /**
     * Subtracts given resources from free resources for given link.
     *
     * @param link the target link
     * @param allocations the resources to be subtracted
     */
    private void subtractFreeResources(Link link, LinkResourceAllocations allocations) {
        // TODO Use lock or version for updating freeResources.
        checkNotNull(link);
        Set<ResourceAllocation> freeRes = new HashSet<>(getFreeResources(link));
        Set<ResourceAllocation> subRes = allocations.getResourceAllocation(link);
        for (ResourceAllocation res : subRes) {
            switch (res.type()) {
            case BANDWIDTH:
                BandwidthResourceAllocation ba = getBandwidth(freeRes);
                double requestedBandwidth =
                        ((BandwidthResourceAllocation) res).bandwidth().toDouble();
                double newBandwidth = ba.bandwidth().toDouble() - requestedBandwidth;
                checkState(newBandwidth >= 0.0);
                freeRes.remove(ba);
                freeRes.add(new BandwidthResourceAllocation(
                        Bandwidth.valueOf(newBandwidth)));
                break;
            case LAMBDA:
                checkState(freeRes.remove(res));
                break;
            default:
                break;
            }
        }
        freeResources.put(link, freeRes);

    }

    /**
     * Adds given resources to free resources for given link.
     *
     * @param link the target link
     * @param allocations the resources to be added
     */
    private void addFreeResources(Link link, LinkResourceAllocations allocations) {
        // TODO Use lock or version for updating freeResources.
        Set<ResourceAllocation> freeRes = new HashSet<>(getFreeResources(link));
        Set<ResourceAllocation> addRes = allocations.getResourceAllocation(link);
        for (ResourceAllocation res : addRes) {
            switch (res.type()) {
            case BANDWIDTH:
                BandwidthResourceAllocation ba = getBandwidth(freeRes);
                double requestedBandwidth =
                        ((BandwidthResourceAllocation) res).bandwidth().toDouble();
                double newBandwidth = ba.bandwidth().toDouble() + requestedBandwidth;
                freeRes.remove(ba);
                freeRes.add(new BandwidthResourceAllocation(
                        Bandwidth.valueOf(newBandwidth)));
                break;
            case LAMBDA:
                checkState(freeRes.add(res));
                break;
            default:
                break;
            }
        }
        freeResources.put(link, freeRes);
    }

    @Override
    public Set<ResourceAllocation> getFreeResources(Link link) {
        checkNotNull(link);
        Set<ResourceAllocation> freeRes = freeResources.get(link);
        if (freeRes == null) {
            freeRes = readOriginalFreeResources(link);
        }

        return freeRes;
    }

    @Override
    public void allocateResources(LinkResourceAllocations allocations) {
        checkNotNull(allocations);
        linkResourceAllocationsMap.put(allocations.intendId(), allocations);
        for (Link link : allocations.links()) {
            subtractFreeResources(link, allocations);
            Set<LinkResourceAllocations> linkAllocs = allocatedResources.get(link);
            if (linkAllocs == null) {
                linkAllocs = new HashSet<>();
            }
            linkAllocs.add(allocations);
            allocatedResources.put(link, linkAllocs);
        }
    }

    @Override
    public void releaseResources(LinkResourceAllocations allocations) {
        checkNotNull(allocations);
        linkResourceAllocationsMap.remove(allocations);
        for (Link link : allocations.links()) {
            addFreeResources(link, allocations);
            Set<LinkResourceAllocations> linkAllocs = allocatedResources.get(link);
            if (linkAllocs == null) {
                log.error("Missing resource allocation.");
            } else {
                linkAllocs.remove(allocations);
            }
            allocatedResources.put(link, linkAllocs);
        }
    }

    @Override
    public LinkResourceAllocations getAllocations(IntentId intentId) {
        checkNotNull(intentId);
        return linkResourceAllocationsMap.get(intentId);
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations(Link link) {
        checkNotNull(link);
        Set<LinkResourceAllocations> result = allocatedResources.get(link);
        if (result == null) {
            result = Collections.emptySet();
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations() {
        return Collections.unmodifiableCollection(linkResourceAllocationsMap.values());
    }

}
