/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.store.trivial;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Bandwidth;
import org.onlab.util.PositionalParameterStringFormatter;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.Link;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.link.BandwidthResource;
import org.onosproject.net.resource.link.BandwidthResourceAllocation;
import org.onosproject.net.resource.link.LambdaResource;
import org.onosproject.net.resource.link.LambdaResourceAllocation;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.onosproject.net.resource.link.LinkResourceEvent;
import org.onosproject.net.resource.link.LinkResourceStore;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceAllocationException;
import org.onosproject.net.resource.ResourceType;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages link resources using trivial in-memory structures implementation.
 *
 * @deprecated in Emu Release
 */
@Deprecated
@Component(immediate = true)
@Service
public class SimpleLinkResourceStore implements LinkResourceStore {
    private static final BandwidthResource DEFAULT_BANDWIDTH = new BandwidthResource(Bandwidth.mbps(1_000));
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
    private synchronized Set<ResourceAllocation> readOriginalFreeResources(Link link) {
        Annotations annotations = link.annotations();
        Set<ResourceAllocation> allocations = new HashSet<>();

        try {
            int waves = Integer.parseInt(annotations.value(AnnotationKeys.OPTICAL_WAVES));
            for (int i = 1; i <= waves; i++) {
                allocations.add(new LambdaResourceAllocation(LambdaResource.valueOf(i)));
            }
        } catch (NumberFormatException e) {
            log.debug("No optical.wave annotation on link %s", link);
        }

        BandwidthResource bandwidth = DEFAULT_BANDWIDTH;
        try {
            bandwidth = new BandwidthResource(
                    Bandwidth.mbps((Double.parseDouble(annotations.value(AnnotationKeys.BANDWIDTH)))));
        } catch (NumberFormatException e) {
            log.debug("No bandwidth annotation on link %s", link);
        }
        allocations.add(
                new BandwidthResourceAllocation(bandwidth));
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
    private synchronized BandwidthResourceAllocation getBandwidth(
            Set<ResourceAllocation> freeRes) {
        for (ResourceAllocation res : freeRes) {
            if (res.type() == ResourceType.BANDWIDTH) {
                return (BandwidthResourceAllocation) res;
            }
        }
        return new BandwidthResourceAllocation(new BandwidthResource(Bandwidth.bps(0)));
    }

    /**
     * Subtracts given resources from free resources for given link.
     *
     * @param link the target link
     * @param allocations the resources to be subtracted
     */
    private synchronized void subtractFreeResources(Link link,
            LinkResourceAllocations allocations) {
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
                if (newBandwidth < 0.0) {
                    throw new ResourceAllocationException(
                            PositionalParameterStringFormatter.format(
                            "Unable to allocate bandwidth for link {} "
                            + "requested amount is {} current allocation is {}",
                                    link,
                                    requestedBandwidth,
                                    ba));
                }
                freeRes.remove(ba);
                freeRes.add(new BandwidthResourceAllocation(
                        new BandwidthResource(Bandwidth.bps(newBandwidth))));
                break;
            case LAMBDA:
                final boolean lambdaAvailable = freeRes.remove(res);
                if (!lambdaAvailable) {
                    int requestedLambda =
                            ((LambdaResourceAllocation) res).lambda().toInt();
                    throw new ResourceAllocationException(
                            PositionalParameterStringFormatter.format(
                                    "Unable to allocate lambda for link {} lambda is {}",
                                    link,
                                    requestedLambda));
                }
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
    private synchronized void addFreeResources(Link link,
            LinkResourceAllocations allocations) {
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
                        new BandwidthResource(Bandwidth.bps(newBandwidth))));
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
    public synchronized Set<ResourceAllocation> getFreeResources(Link link) {
        checkNotNull(link);
        Set<ResourceAllocation> freeRes = freeResources.get(link);
        if (freeRes == null) {
            freeRes = readOriginalFreeResources(link);
        }

        return freeRes;
    }

    @Override
    public synchronized void allocateResources(LinkResourceAllocations allocations) {
        checkNotNull(allocations);
        linkResourceAllocationsMap.put(allocations.intentId(), allocations);
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
    public synchronized LinkResourceEvent releaseResources(LinkResourceAllocations allocations) {
        checkNotNull(allocations);
        linkResourceAllocationsMap.remove(allocations.intentId());
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

        final List<LinkResourceAllocations> releasedResources =
                ImmutableList.of(allocations);

        return new LinkResourceEvent(
                LinkResourceEvent.Type.ADDITIONAL_RESOURCES_AVAILABLE,
                releasedResources);
    }

    @Override
    public synchronized LinkResourceAllocations getAllocations(IntentId intentId) {
        checkNotNull(intentId);
        return linkResourceAllocationsMap.get(intentId);
    }

    @Override
    public synchronized Iterable<LinkResourceAllocations> getAllocations(Link link) {
        checkNotNull(link);
        Set<LinkResourceAllocations> result = allocatedResources.get(link);
        if (result == null) {
            result = Collections.emptySet();
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public synchronized Iterable<LinkResourceAllocations> getAllocations() {
        return Collections.unmodifiableCollection(linkResourceAllocationsMap.values());
    }


}
