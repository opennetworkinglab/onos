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
package org.onlab.onos.net.resource.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.resource.BandwidthResourceAllocation;
import org.onlab.onos.net.resource.BandwidthResourceRequest;
import org.onlab.onos.net.resource.DefaultLinkResourceAllocations;
import org.onlab.onos.net.resource.Lambda;
import org.onlab.onos.net.resource.LambdaResourceAllocation;
import org.onlab.onos.net.resource.LambdaResourceRequest;
import org.onlab.onos.net.resource.LinkResourceAllocations;
import org.onlab.onos.net.resource.LinkResourceRequest;
import org.onlab.onos.net.resource.LinkResourceService;
import org.onlab.onos.net.resource.LinkResourceStore;
import org.onlab.onos.net.resource.ResourceAllocation;
import org.onlab.onos.net.resource.ResourceRequest;
import org.onlab.onos.net.resource.ResourceType;
import org.slf4j.Logger;

/**
 * Provides basic implementation of link resources allocation.
 */
@Component(immediate = true)
@Service
public class LinkResourceManager implements LinkResourceService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LinkResourceStore store;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    /**
     * Returns available lambdas on specified link.
     *
     * @param link the link
     * @return available lambdas on specified link
     */
    private Set<Lambda> getAvailableLambdas(Link link) {
        checkNotNull(link);
        Set<ResourceAllocation> resAllocs = store.getFreeResources(link);
        if (resAllocs == null) {
            return Collections.emptySet();
        }
        Set<Lambda> lambdas = new HashSet<>();
        for (ResourceAllocation res : resAllocs) {
            if (res.type() == ResourceType.LAMBDA) {
                lambdas.add(((LambdaResourceAllocation) res).lambda());
            }
        }
        return lambdas;
    }

    /**
     * Returns available lambdas on specified links.
     *
     * @param links the links
     * @return available lambdas on specified links
     */
    private Iterable<Lambda> getAvailableLambdas(Iterable<Link> links) {
        checkNotNull(links);
        Iterator<Link> i = links.iterator();
        checkArgument(i.hasNext());
        Set<Lambda> lambdas = new HashSet<>(getAvailableLambdas(i.next()));
        while (i.hasNext()) {
            lambdas.retainAll(getAvailableLambdas(i.next()));
        }
        return lambdas;
    }

    @Override
    public LinkResourceAllocations requestResources(LinkResourceRequest req) {
        // TODO Concatenate multiple bandwidth requests.
        // TODO Support multiple lambda resource requests.
        // TODO Throw appropriate exception.

        Set<ResourceAllocation> allocs = new HashSet<>();
        for (ResourceRequest r : req.resources()) {
            switch (r.type()) {
            case BANDWIDTH:
                BandwidthResourceRequest br = (BandwidthResourceRequest) r;
                allocs.add(new BandwidthResourceAllocation(br.bandwidth()));
                break;
            case LAMBDA:
                Iterator<Lambda> lambdaIterator =
                        getAvailableLambdas(req.links()).iterator();
                if (lambdaIterator.hasNext()) {
                    allocs.add(new LambdaResourceAllocation(lambdaIterator.next()));
                } else {
                    log.info("Failed to allocate lambda resource.");
                    return null;
                }
                break;
            default:
                break;
            }
        }

        Map<Link, Set<ResourceAllocation>> allocations = new HashMap<>();
        for (Link link : req.links()) {
            allocations.put(link, allocs);
        }
        LinkResourceAllocations result =
                new DefaultLinkResourceAllocations(req, allocations);
        store.allocateResources(result);
        return result;

    }

    @Override
    public void releaseResources(LinkResourceAllocations allocations) {
        store.releaseResources(allocations);
    }

    @Override
    public LinkResourceAllocations updateResources(LinkResourceRequest req,
            LinkResourceAllocations oldAllocations) {
         releaseResources(oldAllocations);
         return requestResources(req);
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations() {
        return store.getAllocations();
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations(Link link) {
        return store.getAllocations(link);
    }

    @Override
    public LinkResourceAllocations getAllocations(IntentId intentId) {
        return store.getAllocations(intentId);
    }

    @Override
    public Iterable<ResourceRequest> getAvailableResources(Link link) {
        Set<ResourceAllocation> freeRes = store.getFreeResources(link);
        Set<ResourceRequest> result = new HashSet<>();
        for (ResourceAllocation alloc : freeRes) {
            switch (alloc.type()) {
            case BANDWIDTH:
                result.add(new BandwidthResourceRequest(
                        ((BandwidthResourceAllocation) alloc).bandwidth()));
                break;
            case LAMBDA:
                result.add(new LambdaResourceRequest());
                break;
            default:
                break;
            }
        }
        return result;
    }

    @Override
    public Iterable<ResourceRequest> getAvailableResources(Link link,
            LinkResourceAllocations allocations) {
        Set<ResourceRequest> result = new HashSet<>();
        Set<ResourceAllocation> allocatedRes = allocations.getResourceAllocation(link);
        result = (Set<ResourceRequest>) getAvailableResources(link);
        result.addAll(allocatedRes);
        return result;
    }

}
