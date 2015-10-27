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
package org.onosproject.net.resource.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.Link;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceRequest;
import org.onosproject.net.resource.ResourceType;
import org.onosproject.net.resource.link.BandwidthResourceAllocation;
import org.onosproject.net.resource.link.BandwidthResourceRequest;
import org.onosproject.net.resource.link.DefaultLinkResourceAllocations;
import org.onosproject.net.resource.link.LambdaResource;
import org.onosproject.net.resource.link.LambdaResourceAllocation;
import org.onosproject.net.resource.link.LambdaResourceRequest;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.onosproject.net.resource.link.LinkResourceEvent;
import org.onosproject.net.resource.link.LinkResourceListener;
import org.onosproject.net.resource.link.LinkResourceRequest;
import org.onosproject.net.resource.link.LinkResourceService;
import org.onosproject.net.resource.link.LinkResourceStore;
import org.onosproject.net.resource.link.LinkResourceStoreDelegate;
import org.onosproject.net.resource.link.MplsLabel;
import org.onosproject.net.resource.link.MplsLabelResourceAllocation;
import org.onosproject.net.resource.link.MplsLabelResourceRequest;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppPermission.Type.*;


/**
 * Provides basic implementation of link resources allocation.
 */
@Component(immediate = true)
@Service
public class LinkResourceManager
        extends AbstractListenerManager<LinkResourceEvent, LinkResourceListener>
        implements LinkResourceService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LinkResourceStore store;

    @Activate
    public void activate() {
        eventDispatcher.addSink(LinkResourceEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(LinkResourceEvent.class);
        log.info("Stopped");
    }

    /**
     * Returns available lambdas on specified link.
     *
     * @param link the link
     * @return available lambdas on specified link
     */
    private Set<LambdaResource> getAvailableLambdas(Link link) {
        checkNotNull(link);
        Set<ResourceAllocation> resAllocs = store.getFreeResources(link);
        if (resAllocs == null) {
            return Collections.emptySet();
        }
        Set<LambdaResource> lambdas = new HashSet<>();
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
    private Collection<LambdaResource> getAvailableLambdas(Iterable<Link> links) {
        checkNotNull(links);
        return ImmutableList.copyOf(links).stream()
                .map(this::getAvailableLambdas)
                .reduce(Sets::intersection)
                .orElse(Collections.emptySet());
    }


    /**
     * Returns available MPLS label on specified link.
     *
     * @param link the link
     * @return available MPLS labels on specified link
     */
    private Iterable<MplsLabel> getAvailableMplsLabels(Link link) {
        Set<ResourceAllocation> resAllocs = store.getFreeResources(link);
        if (resAllocs == null) {
            return Collections.emptySet();
        }
        Set<MplsLabel> mplsLabels = new HashSet<>();
        for (ResourceAllocation res : resAllocs) {
            if (res.type() == ResourceType.MPLS_LABEL) {

                mplsLabels.add(((MplsLabelResourceAllocation) res).mplsLabel());
            }
        }

        return mplsLabels;
    }

    @Override
    public LinkResourceAllocations requestResources(LinkResourceRequest req) {
        checkPermission(LINK_WRITE);

        // TODO Concatenate multiple bandwidth requests.
        // TODO Support multiple lambda resource requests.
        // TODO Throw appropriate exception.
        Set<ResourceAllocation> allocs = new HashSet<>();
        Map<Link, Set<ResourceAllocation>> allocsPerLink = new HashMap<>();
        for (ResourceRequest r : req.resources()) {
            switch (r.type()) {
            case BANDWIDTH:
                BandwidthResourceRequest br = (BandwidthResourceRequest) r;
                allocs.add(new BandwidthResourceAllocation(br.bandwidth()));
                break;
            case LAMBDA:
                Iterator<LambdaResource> lambdaIterator =
                        getAvailableLambdas(req.links()).iterator();
                if (lambdaIterator.hasNext()) {
                    allocs.add(new LambdaResourceAllocation(lambdaIterator.next()));
                } else {
                    log.info("Failed to allocate lambda resource.");
                    return null;
                }
                break;
            case MPLS_LABEL:
                for (Link link : req.links()) {
                    if (allocsPerLink.get(link) == null) {
                        allocsPerLink.put(link, new HashSet<>());
                    }
                    Iterator<MplsLabel> mplsIter = getAvailableMplsLabels(link)
                            .iterator();
                    if (mplsIter.hasNext()) {
                        allocsPerLink.get(link)
                                .add(new MplsLabelResourceAllocation(mplsIter
                                             .next()));
                    } else {
                        log.info("Failed to allocate MPLS resource.");
                        break;
                    }
                }
                break;
            default:
                break;
            }
        }

        Map<Link, Set<ResourceAllocation>> allocations = new HashMap<>();
        for (Link link : req.links()) {
            allocations.put(link, new HashSet<>(allocs));
            Set<ResourceAllocation> linkAllocs = allocsPerLink.get(link);
            if (linkAllocs != null) {
                allocations.get(link).addAll(linkAllocs);
            }
        }
        LinkResourceAllocations result =
                new DefaultLinkResourceAllocations(req, allocations);
        store.allocateResources(result);
        return result;

    }

    @Override
    public void releaseResources(LinkResourceAllocations allocations) {
        checkPermission(LINK_WRITE);
        final LinkResourceEvent event = store.releaseResources(allocations);
        if (event != null) {
            post(event);
        }
    }

    @Override
    public LinkResourceAllocations updateResources(LinkResourceRequest req,
            LinkResourceAllocations oldAllocations) {
        checkPermission(LINK_WRITE);
        releaseResources(oldAllocations);
         return requestResources(req);
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations() {
        checkPermission(LINK_READ);
        return store.getAllocations();
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations(Link link) {
        checkPermission(LINK_READ);
        return store.getAllocations(link);
    }

    @Override
    public LinkResourceAllocations getAllocations(IntentId intentId) {
        checkPermission(LINK_READ);
        return store.getAllocations(intentId);
    }

    @Override
    public Iterable<ResourceRequest> getAvailableResources(Link link) {
        checkPermission(LINK_READ);

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
            case MPLS_LABEL:
                result.add(new MplsLabelResourceRequest());
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
        checkPermission(LINK_READ);

        Set<ResourceAllocation> allocatedRes = allocations.getResourceAllocation(link);
        Set<ResourceRequest> result = Sets.newHashSet(getAvailableResources(link));
        result.removeAll(allocatedRes);
        return result;
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
