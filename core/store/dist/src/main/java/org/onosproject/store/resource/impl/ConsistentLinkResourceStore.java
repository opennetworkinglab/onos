/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.util.Bandwidth;
import org.onosproject.net.OmsPort;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;
import org.onlab.util.PositionalParameterStringFormatter;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.link.BandwidthResource;
import org.onosproject.net.resource.link.BandwidthResourceAllocation;
import org.onosproject.net.resource.link.LambdaResource;
import org.onosproject.net.resource.link.LambdaResourceAllocation;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.onosproject.net.resource.link.LinkResourceEvent;
import org.onosproject.net.resource.link.LinkResourceStore;
import org.onosproject.net.resource.link.LinkResourceStoreDelegate;
import org.onosproject.net.resource.link.MplsLabel;
import org.onosproject.net.resource.link.MplsLabelResourceAllocation;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceAllocationException;
import org.onosproject.net.resource.ResourceType;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionException;
import org.onosproject.store.service.TransactionalMap;
import org.onosproject.store.service.Versioned;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.net.AnnotationKeys.BANDWIDTH;

/**
 * Store that manages link resources using Copycat-backed TransactionalMaps.
 */
@Component(immediate = true, enabled = true)
@Service
public class ConsistentLinkResourceStore extends
        AbstractStore<LinkResourceEvent, LinkResourceStoreDelegate> implements
        LinkResourceStore {

    private final Logger log = getLogger(getClass());

    private static final BandwidthResource DEFAULT_BANDWIDTH = new BandwidthResource(Bandwidth.mbps(1_000));
    private static final BandwidthResource EMPTY_BW = new BandwidthResource(Bandwidth.bps(0));

    // Smallest non-reserved MPLS label
    private static final int MIN_UNRESERVED_LABEL = 0x10;
    // Max non-reserved MPLS label = 239
    private static final int MAX_UNRESERVED_LABEL = 0xEF;

    // table to store current allocations
    /** LinkKey -> List<LinkResourceAllocations>. */
    private static final String LINK_RESOURCE_ALLOCATIONS = "LinkAllocations";

    /** IntentId -> LinkResourceAllocations. */
    private static final String INTENT_ALLOCATIONS = "LinkIntentAllocations";

    private static final Serializer SERIALIZER = Serializer.using(KryoNamespaces.API);

    // for reading committed values.
    private ConsistentMap<IntentId, LinkResourceAllocations> intentAllocMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        intentAllocMap = storageService.<IntentId, LinkResourceAllocations>consistentMapBuilder()
                .withName(INTENT_ALLOCATIONS)
                .withSerializer(SERIALIZER)
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    private TransactionalMap<IntentId, LinkResourceAllocations> getIntentAllocs(TransactionContext tx) {
        return tx.getTransactionalMap(INTENT_ALLOCATIONS, SERIALIZER);
    }

    private TransactionalMap<LinkKey, List<LinkResourceAllocations>> getLinkAllocs(TransactionContext tx) {
        return tx.getTransactionalMap(LINK_RESOURCE_ALLOCATIONS, SERIALIZER);
    }

    private TransactionContext getTxContext() {
        return storageService.transactionContextBuilder().build();
    }

    private Set<ResourceAllocation> getResourceCapacity(ResourceType type, Link link) {
        switch (type) {
            case BANDWIDTH:
                return ImmutableSet.of(getBandwidthResourceCapacity(link));
            case LAMBDA:
                return getLambdaResourceCapacity(link);
            case MPLS_LABEL:
                return getMplsResourceCapacity();
            default:
                return ImmutableSet.of();
        }
    }

    private Set<ResourceAllocation> getLambdaResourceCapacity(Link link) {
        Port port = deviceService.getPort(link.src().deviceId(), link.src().port());
        if (!(port instanceof OmsPort)) {
            return Collections.emptySet();
        }

        OmsPort omsPort = (OmsPort) port;
        Set<ResourceAllocation> allocations = new HashSet<>();
        // Assume fixed grid for now
        for (int i = 0; i < omsPort.totalChannels(); i++) {
            allocations.add(new LambdaResourceAllocation(LambdaResource.valueOf(i)));
        }
        return allocations;
    }

    private BandwidthResourceAllocation getBandwidthResourceCapacity(Link link) {

        // if Link annotation exist, use them
        // if all fails, use DEFAULT_BANDWIDTH
        BandwidthResource bandwidth = DEFAULT_BANDWIDTH;
        String strBw = link.annotations().value(BANDWIDTH);
        if (strBw == null) {
            return new BandwidthResourceAllocation(bandwidth);
        }

        try {
            bandwidth = new BandwidthResource(Bandwidth.mbps(Double.parseDouble(strBw)));
        } catch (NumberFormatException e) {
            // do nothings, use default bandwidth
            bandwidth = DEFAULT_BANDWIDTH;
        }
        return new BandwidthResourceAllocation(bandwidth);
    }

    private Set<ResourceAllocation> getMplsResourceCapacity() {
        Set<ResourceAllocation> allocations = new HashSet<>();
        //Ignoring reserved labels of 0 through 15
        for (int i = MIN_UNRESERVED_LABEL; i <= MAX_UNRESERVED_LABEL; i++) {
            allocations.add(new MplsLabelResourceAllocation(MplsLabel
                    .valueOf(i)));

        }
        return allocations;
    }

    private Map<ResourceType, Set<ResourceAllocation>> getResourceCapacity(Link link) {
        Map<ResourceType, Set<ResourceAllocation>> caps = new HashMap<>();
        for (ResourceType type : ResourceType.values()) {
            Set<ResourceAllocation> cap = getResourceCapacity(type, link);
            caps.put(type, cap);
        }
        return caps;
    }

    @Override
    public Set<ResourceAllocation> getFreeResources(Link link) {
        TransactionContext tx = getTxContext();

        tx.begin();
        try {
            Map<ResourceType, Set<ResourceAllocation>> freeResources = getFreeResourcesEx(tx, link);
            return freeResources.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        } finally {
            tx.abort();
        }
    }

    private Map<ResourceType, Set<ResourceAllocation>> getFreeResourcesEx(TransactionContext tx, Link link) {
        checkNotNull(tx);
        checkNotNull(link);

        Map<ResourceType, Set<ResourceAllocation>> free = new HashMap<>();
        final Map<ResourceType, Set<ResourceAllocation>> caps = getResourceCapacity(link);
        final List<LinkResourceAllocations> allocations = ImmutableList.copyOf(getAllocations(tx, link));

        Set<ResourceAllocation> bw = caps.get(ResourceType.BANDWIDTH);
        Set<ResourceAllocation> value = getFreeBandwidthResources(link, bw, allocations);
        free.put(ResourceType.BANDWIDTH, value);

        Set<ResourceAllocation> lmd = caps.get(ResourceType.LAMBDA);
        Set<ResourceAllocation> freeL = getFreeResources(link, lmd, allocations,
                LambdaResourceAllocation.class);
        free.put(ResourceType.LAMBDA, freeL);

        Set<ResourceAllocation> mpls = caps.get(ResourceType.MPLS_LABEL);
        Set<ResourceAllocation> freeLabel = getFreeResources(link, mpls, allocations,
                MplsLabelResourceAllocation.class);
        free.put(ResourceType.MPLS_LABEL, freeLabel);

        return free;
    }

    private Set<ResourceAllocation> getFreeBandwidthResources(Link link, Set<ResourceAllocation> bw,
                                                              List<LinkResourceAllocations> allocations) {
        if (bw == null || bw.isEmpty()) {
            bw = Sets.newHashSet(new BandwidthResourceAllocation(EMPTY_BW));
        }

        BandwidthResourceAllocation cap = (BandwidthResourceAllocation) bw.iterator().next();
        double freeBw = cap.bandwidth().toDouble();

        // enumerate current allocations, subtracting resources
        double allocatedBw = allocations.stream()
                .flatMap(x -> x.getResourceAllocation(link).stream())
                .filter(x -> x instanceof BandwidthResourceAllocation)
                .map(x -> (BandwidthResourceAllocation) x)
                .mapToDouble(x -> x.bandwidth().toDouble())
                .sum();
        freeBw -= allocatedBw;
        return Sets.newHashSet(
                new BandwidthResourceAllocation(new BandwidthResource(Bandwidth.bps(freeBw))));
    }

    private Set<ResourceAllocation> getFreeResources(Link link,
                                                     Set<ResourceAllocation> resources,
                                                     List<LinkResourceAllocations> allocations,
                                                     Class<? extends ResourceAllocation> cls) {
        if (resources == null || resources.isEmpty()) {
            // nothing left
            return Collections.emptySet();
        }
        Set<ResourceAllocation> freeL = resources.stream()
                .filter(cls::isInstance)
                .collect(Collectors.toSet());

        // enumerate current allocations, removing resources
        List<ResourceAllocation> allocated = allocations.stream()
                .flatMap(x -> x.getResourceAllocation(link).stream())
                .filter(cls::isInstance)
                .collect(Collectors.toList());
        freeL.removeAll(allocated);
        return freeL;
    }

    @Override
    public void allocateResources(LinkResourceAllocations allocations) {
        checkNotNull(allocations);
        TransactionContext tx = getTxContext();

        tx.begin();
        try {
            TransactionalMap<IntentId, LinkResourceAllocations> intentAllocs = getIntentAllocs(tx);
            intentAllocs.put(allocations.intentId(), allocations);
            allocations.links().forEach(link -> allocateLinkResource(tx, link, allocations));
            tx.commit();
        } catch (TransactionException | ResourceAllocationException e) {
            log.error("Exception thrown, rolling back", e);
            tx.abort();
        } catch (Exception e) {
            log.error("Exception thrown, rolling back", e);
            tx.abort();
            throw e;
        }
    }

    private void allocateLinkResource(TransactionContext tx, Link link,
            LinkResourceAllocations allocations) {
        // requested resources
        Set<ResourceAllocation> reqs = allocations.getResourceAllocation(link);
        Map<ResourceType, Set<ResourceAllocation>> available = getFreeResourcesEx(tx, link);
        for (ResourceAllocation req : reqs) {
            Set<ResourceAllocation> avail = available.get(req.type());
            if (req instanceof BandwidthResourceAllocation) {
                // check if allocation should be accepted
                if (avail.isEmpty()) {
                    throw new ResourceAllocationException(String.format("There's no Bandwidth resource on %s?", link));
                }
                BandwidthResourceAllocation bw = (BandwidthResourceAllocation) avail.iterator().next();
                double bwLeft = bw.bandwidth().toDouble();
                BandwidthResourceAllocation bwReq = ((BandwidthResourceAllocation) req);
                bwLeft -= bwReq.bandwidth().toDouble();
                if (bwLeft < 0) {
                    throw new ResourceAllocationException(
                            PositionalParameterStringFormatter.format(
                                    "Unable to allocate bandwidth for link {} "
                                        + " requested amount is {} current allocation is {}",
                                    link,
                                    bwReq.bandwidth().toDouble(),
                                    bw));
                }
            } else if (req instanceof LambdaResourceAllocation) {
                LambdaResourceAllocation lambdaAllocation = (LambdaResourceAllocation) req;
                // check if allocation should be accepted
                if (!avail.contains(req)) {
                    // requested lambda was not available
                    throw new ResourceAllocationException(
                            PositionalParameterStringFormatter.format(
                                "Unable to allocate lambda for link {} lambda is {}",
                                    link,
                                    lambdaAllocation.lambda().toInt()));
                }
            } else if (req instanceof MplsLabelResourceAllocation) {
                MplsLabelResourceAllocation mplsAllocation = (MplsLabelResourceAllocation) req;
                if (!avail.contains(req)) {
                    throw new ResourceAllocationException(
                                                          PositionalParameterStringFormatter
                                                                  .format("Unable to allocate MPLS label for link "
                                                                          + "{} MPLS label is {}",
                                                                          link,
                                                                          mplsAllocation
                                                                                  .mplsLabel()
                                                                                  .toString()));
                }
            }
        }
        // all requests allocatable => add allocation
        final LinkKey linkKey = LinkKey.linkKey(link);
        TransactionalMap<LinkKey, List<LinkResourceAllocations>> linkAllocs = getLinkAllocs(tx);
        List<LinkResourceAllocations> before = linkAllocs.get(linkKey);
        if (before == null) {
            List<LinkResourceAllocations> after = new ArrayList<>();
            after.add(allocations);
            linkAllocs.putIfAbsent(linkKey, after);
        } else {
            List<LinkResourceAllocations> after = new ArrayList<>(before.size() + 1);
            after.addAll(before);
            after.add(allocations);
            linkAllocs.replace(linkKey, before, after);
        }
    }

    @Override
    public LinkResourceEvent releaseResources(LinkResourceAllocations allocations) {
        checkNotNull(allocations);

        final IntentId intentId = allocations.intentId();
        final Collection<Link> links = allocations.links();
        boolean success = false;
        do {
            TransactionContext tx = getTxContext();
            tx.begin();
            try {
                TransactionalMap<IntentId, LinkResourceAllocations> intentAllocs = getIntentAllocs(tx);
                intentAllocs.remove(intentId);

                TransactionalMap<LinkKey, List<LinkResourceAllocations>> linkAllocs = getLinkAllocs(tx);
                links.forEach(link -> {
                    final LinkKey linkId = LinkKey.linkKey(link);

                    List<LinkResourceAllocations> before = linkAllocs.get(linkId);
                    if (before == null || before.isEmpty()) {
                        // something is wrong, but it is already freed
                        log.warn("There was no resource left to release on {}", linkId);
                        return;
                    }
                    List<LinkResourceAllocations> after = new ArrayList<>(before);
                    after.remove(allocations);
                    linkAllocs.replace(linkId, before, after);
                });
                tx.commit();
                success = true;
            } catch (TransactionException e) {
                log.debug("Transaction failed, retrying", e);
                tx.abort();
            } catch (Exception e) {
                log.error("Exception thrown during releaseResource {}", allocations, e);
                tx.abort();
                throw e;
            }
        } while (!success);

        // Issue events to force recompilation of intents.
        final List<LinkResourceAllocations> releasedResources = ImmutableList.of(allocations);
        return new LinkResourceEvent(
                LinkResourceEvent.Type.ADDITIONAL_RESOURCES_AVAILABLE,
                releasedResources);

    }

    @Override
    public LinkResourceAllocations getAllocations(IntentId intentId) {
        checkNotNull(intentId);
        Versioned<LinkResourceAllocations> alloc = null;
        try {
            alloc = intentAllocMap.get(intentId);
        } catch (Exception e) {
            log.warn("Could not read resource allocation information", e);
        }
        return alloc == null ? null : alloc.value();
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations(Link link) {
        checkNotNull(link);
        TransactionContext tx = getTxContext();
        Iterable<LinkResourceAllocations> res = null;
        tx.begin();
        try {
            res = getAllocations(tx, link);
        } finally {
            tx.abort();
        }
        return res == null ? Collections.emptyList() : res;
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations() {
        try {
            Set<LinkResourceAllocations> allocs =
                    intentAllocMap.values().stream().map(Versioned::value).collect(Collectors.toSet());
            return ImmutableSet.copyOf(allocs);
        } catch (Exception e) {
            log.warn("Could not read resource allocation information", e);
        }
        return ImmutableSet.of();
    }

    private Iterable<LinkResourceAllocations> getAllocations(TransactionContext tx, Link link) {
        checkNotNull(tx);
        checkNotNull(link);
        final LinkKey key = LinkKey.linkKey(link);
        TransactionalMap<LinkKey, List<LinkResourceAllocations>> linkAllocs = getLinkAllocs(tx);

        List<LinkResourceAllocations> res = linkAllocs.get(key);
        if (res != null) {
            return res;
        }

        res = linkAllocs.putIfAbsent(key, new ArrayList<>());
        if (res == null) {
            return Collections.emptyList();
        } else {
            return res;
        }
    }

}
