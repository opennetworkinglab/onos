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
package org.onosproject.net.resource.impl;

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
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.Link;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.BandwidthResourceAllocation;
import org.onosproject.net.resource.BandwidthResourceRequest;
import org.onosproject.net.resource.DefaultLinkResourceAllocations;
import org.onosproject.net.resource.Lambda;
import org.onosproject.net.resource.LambdaResourceAllocation;
import org.onosproject.net.resource.LambdaResourceRequest;
import org.onosproject.net.resource.LinkResourceAllocations;
import org.onosproject.net.resource.LinkResourceEvent;
import org.onosproject.net.resource.LinkResourceListener;
import org.onosproject.net.resource.LinkResourceRequest;
import org.onosproject.net.resource.LinkResourceService;
import org.onosproject.net.resource.LinkResourceStore;
import org.onosproject.net.resource.LinkResourceStoreDelegate;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceRequest;
import org.onosproject.net.resource.ResourceType;
import org.slf4j.Logger;

/**
 * Provides basic implementation of link resources allocation.
 */
@Component(immediate = true)
@Service
public class LinkResourceManager implements LinkResourceService {

    private final Logger log = getLogger(getClass());

    protected final AbstractListenerRegistry<LinkResourceEvent, LinkResourceListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LinkResourceStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        eventDispatcher.addSink(LinkResourceEvent.class, listenerRegistry);
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
        final LinkResourceEvent event = store.releaseResources(allocations);
        if (event != null) {
            post(event);
        }
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

    @Override
    public void addListener(LinkResourceListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(LinkResourceListener listener) {
        listenerRegistry.removeListener(listener);
    }

    /**
     * Posts the specified event to the local event dispatcher.
     */
    private void post(LinkResourceEvent event) {
        if (event != null) {
            eventDispatcher.post(event);
        }
    }

    /**
     * Store delegate to re-post events emitted from the store.
     */
    private class InternalStoreDelegate implements LinkResourceStoreDelegate {
        @Override
        public void notify(LinkResourceEvent event) {
            post(event);
        }
    }
}
