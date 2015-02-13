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
package org.onosproject.store.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.PositionalParameterStringFormatter;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.resource.Bandwidth;
import org.onosproject.net.resource.BandwidthResourceAllocation;
import org.onosproject.net.resource.Lambda;
import org.onosproject.net.resource.LambdaResourceAllocation;
import org.onosproject.net.resource.LinkResourceAllocations;
import org.onosproject.net.resource.LinkResourceEvent;
import org.onosproject.net.resource.LinkResourceStore;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceAllocationException;
import org.onosproject.net.resource.ResourceType;
import org.onosproject.store.StoreDelegate;
import org.onosproject.store.hz.AbstractHazelcastStore;
import org.onosproject.store.hz.STxMap;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionException;
import com.hazelcast.transaction.TransactionOptions;
import com.hazelcast.transaction.TransactionOptions.TransactionType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages link resources using Hazelcast.
 */
@Component(immediate = true, enabled = true)
@Service
public class HazelcastLinkResourceStore
    extends AbstractHazelcastStore<LinkResourceEvent, StoreDelegate<LinkResourceEvent>>
    implements LinkResourceStore {


    private final Logger log = getLogger(getClass());

    private static final Bandwidth DEFAULT_BANDWIDTH = Bandwidth.valueOf(1_000);

    private static final Bandwidth EMPTY_BW = Bandwidth.valueOf(0);

    // table to store current allocations
    /** LinkKey -> List<LinkResourceAllocations>. */
    private static final String LINK_RESOURCE_ALLOCATIONS = "LinkResourceAllocations";

    /** IntentId -> LinkResourceAllocations. */
    private static final String INTENT_ALLOCATIONS = "IntentAllocations";


    // TODO make this configurable
    // number of retries to attempt on allocation failure, due to
    // concurrent update
    private static int maxAllocateRetries = 5;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    // Link annotation key name to use as bandwidth
    private String bandwidthAnnotation = AnnotationKeys.BANDWIDTH;
    // Link annotation key name to use as max lambda
    private String wavesAnnotation = AnnotationKeys.OPTICAL_WAVES;

    @Override
    @Activate
    public void activate() {
        super.activate();

        final Config config = theInstance.getConfig();

        MapConfig linkCfg = config.getMapConfig(LINK_RESOURCE_ALLOCATIONS);
        linkCfg.setAsyncBackupCount(MapConfig.MAX_BACKUP_COUNT - linkCfg.getBackupCount());

        MapConfig intentCfg = config.getMapConfig(INTENT_ALLOCATIONS);
        intentCfg.setAsyncBackupCount(MapConfig.MAX_BACKUP_COUNT - intentCfg.getBackupCount());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    private STxMap<IntentId, LinkResourceAllocations> getIntentAllocs(TransactionContext tx) {
        TransactionalMap<byte[], byte[]> raw = tx.getMap(INTENT_ALLOCATIONS);
        return new STxMap<>(raw, serializer);
    }

    private STxMap<LinkKey, List<LinkResourceAllocations>> getLinkAllocs(TransactionContext tx) {
        TransactionalMap<byte[], byte[]> raw = tx.getMap(LINK_RESOURCE_ALLOCATIONS);
        return new STxMap<>(raw, serializer);
    }

    private Set<? extends ResourceAllocation> getResourceCapacity(ResourceType type, Link link) {
        if (type == ResourceType.BANDWIDTH) {
            return ImmutableSet.of(getBandwidthResourceCapacity(link));
        }
        if (type == ResourceType.LAMBDA) {
            return getLambdaResourceCapacity(link);
        }
        return null;
    }

    private Set<LambdaResourceAllocation> getLambdaResourceCapacity(Link link) {
        Set<LambdaResourceAllocation> allocations = new HashSet<>();
        try {
            final int waves = Integer.parseInt(link.annotations().value(wavesAnnotation));
            for (int i = 1; i <= waves; i++) {
                allocations.add(new LambdaResourceAllocation(Lambda.valueOf(i)));
            }
        } catch (NumberFormatException e) {
            log.debug("No {} annotation on link %s", wavesAnnotation, link);
        }
        return allocations;
    }

    private BandwidthResourceAllocation getBandwidthResourceCapacity(Link link) {

        // if Link annotation exist, use them
        // if all fails, use DEFAULT_BANDWIDTH

        Bandwidth bandwidth = null;
        String strBw = link.annotations().value(bandwidthAnnotation);
        if (strBw != null) {
            try {
                bandwidth = Bandwidth.valueOf(Double.parseDouble(strBw));
            } catch (NumberFormatException e) {
                // do nothings
                bandwidth = null;
            }
        }

        if (bandwidth == null) {
            // fall back, use fixed default
            bandwidth = DEFAULT_BANDWIDTH;
        }
        return new BandwidthResourceAllocation(bandwidth);
    }

    private Map<ResourceType, Set<? extends ResourceAllocation>> getResourceCapacity(Link link) {
        Map<ResourceType, Set<? extends ResourceAllocation>> caps = new HashMap<>();
        for (ResourceType type : ResourceType.values()) {
            Set<? extends ResourceAllocation> cap = getResourceCapacity(type, link);
            if (cap != null) {
                caps.put(type, cap);
            }
        }
        return caps;
    }

    @Override
    public Set<ResourceAllocation> getFreeResources(Link link) {
        TransactionOptions opt = new TransactionOptions();
        // read-only and will never be commited, thus does not need durability
        opt.setTransactionType(TransactionType.LOCAL);
        TransactionContext tx = theInstance.newTransactionContext(opt);
        tx.beginTransaction();
        try {
            Map<ResourceType, Set<? extends ResourceAllocation>> freeResources = getFreeResourcesEx(tx, link);
            Set<ResourceAllocation> allFree = new HashSet<>();
            for (Set<? extends ResourceAllocation> r : freeResources.values()) {
                allFree.addAll(r);
            }
            return allFree;
        } finally {
            tx.rollbackTransaction();
        }

    }

    private Map<ResourceType, Set<? extends ResourceAllocation>> getFreeResourcesEx(TransactionContext tx, Link link) {
        // returns capacity - allocated

        checkNotNull(link);
        Map<ResourceType, Set<? extends ResourceAllocation>> free = new HashMap<>();
        final Map<ResourceType, Set<? extends ResourceAllocation>> caps = getResourceCapacity(link);
        final Iterable<LinkResourceAllocations> allocations = getAllocations(tx, link);

        for (ResourceType type : ResourceType.values()) {
            // there should be class/category of resources
            switch (type) {
            case BANDWIDTH:
            {
                Set<? extends ResourceAllocation> bw = caps.get(ResourceType.BANDWIDTH);
                if (bw == null || bw.isEmpty()) {
                    bw = Sets.newHashSet(new BandwidthResourceAllocation(EMPTY_BW));
                }

                BandwidthResourceAllocation cap = (BandwidthResourceAllocation) bw.iterator().next();
                double freeBw = cap.bandwidth().toDouble();

                // enumerate current allocations, subtracting resources
                for (LinkResourceAllocations alloc : allocations) {
                    Set<ResourceAllocation> types = alloc.getResourceAllocation(link);
                    for (ResourceAllocation a : types) {
                        if (a instanceof BandwidthResourceAllocation) {
                            BandwidthResourceAllocation bwA = (BandwidthResourceAllocation) a;
                            freeBw -= bwA.bandwidth().toDouble();
                        }
                    }
                }

                free.put(type, Sets.newHashSet(new BandwidthResourceAllocation(Bandwidth.valueOf(freeBw))));
                break;
            }

            case LAMBDA:
            {
                Set<? extends ResourceAllocation> lmd = caps.get(type);
                if (lmd == null || lmd.isEmpty()) {
                    // nothing left
                    break;
                }
                Set<LambdaResourceAllocation> freeL = new HashSet<>();
                for (ResourceAllocation r : lmd) {
                    if (r instanceof LambdaResourceAllocation) {
                        freeL.add((LambdaResourceAllocation) r);
                    }
                }

                // enumerate current allocations, removing resources
                for (LinkResourceAllocations alloc : allocations) {
                    Set<ResourceAllocation> types = alloc.getResourceAllocation(link);
                    for (ResourceAllocation a : types) {
                        if (a instanceof LambdaResourceAllocation) {
                            freeL.remove(a);
                        }
                    }
                }

                free.put(type, freeL);
                break;
            }

            default:
                break;
            }
        }
        return free;
    }

    @Override
    public void allocateResources(LinkResourceAllocations allocations) {
        checkNotNull(allocations);

        for (int i = 0; i < maxAllocateRetries; ++i) {
            TransactionContext tx = theInstance.newTransactionContext();
            tx.beginTransaction();
            try {

                STxMap<IntentId, LinkResourceAllocations> intentAllocs = getIntentAllocs(tx);
                // should this be conditional write?
                intentAllocs.put(allocations.intendId(), allocations);

                for (Link link : allocations.links()) {
                    allocateLinkResource(tx, link, allocations);
                }

                tx.commitTransaction();
                return;
            } catch (TransactionException e) {
                log.debug("Failed to commit allocations for {}. [retry={}]",
                          allocations.intendId(), i);
                log.trace(" details {} ", allocations, e);
                continue;
            } catch (Exception e) {
                log.error("Exception thrown, rolling back", e);
                tx.rollbackTransaction();
                throw e;
            }
        }
    }

    private void allocateLinkResource(TransactionContext tx, Link link,
                                      LinkResourceAllocations allocations) {

        // requested resources
        Set<ResourceAllocation> reqs = allocations.getResourceAllocation(link);

        Map<ResourceType, Set<? extends ResourceAllocation>> available = getFreeResourcesEx(tx, link);
        for (ResourceAllocation req : reqs) {
            Set<? extends ResourceAllocation> avail = available.get(req.type());
            if (req instanceof BandwidthResourceAllocation) {
                // check if allocation should be accepted
                if (avail.isEmpty()) {
                    checkState(!avail.isEmpty(),
                               "There's no Bandwidth resource on %s?",
                               link);
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
                                "Unable to allocate lambda for link {} lamdba is {}",
                                    link,
                                    lambdaAllocation.lambda().toInt()));
                }
            }
        }
        // all requests allocatable => add allocation
        final LinkKey linkKey = LinkKey.linkKey(link);
        STxMap<LinkKey, List<LinkResourceAllocations>> linkAllocs = getLinkAllocs(tx);
        List<LinkResourceAllocations> before = linkAllocs.get(linkKey);
        if (before == null) {
            List<LinkResourceAllocations> after = new ArrayList<>();
            after.add(allocations);
            before = linkAllocs.putIfAbsent(linkKey, after);
            if (before != null) {
                // concurrent allocation detected, retry transaction
                throw new TransactionException("Concurrent Allocation, retry");
            }
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

        final IntentId intendId = allocations.intendId();
        final Collection<Link> links = allocations.links();

        boolean success = false;
        do {
            // Note: might want to break it down into smaller tx unit
            // to lower the chance of collisions.
            TransactionContext tx = theInstance.newTransactionContext();
            tx.beginTransaction();
            try {
                STxMap<IntentId, LinkResourceAllocations> intentAllocs = getIntentAllocs(tx);
                intentAllocs.remove(intendId);

                STxMap<LinkKey, List<LinkResourceAllocations>> linkAllocs = getLinkAllocs(tx);

                for (Link link : links) {
                    final LinkKey linkId = LinkKey.linkKey(link);

                    List<LinkResourceAllocations> before = linkAllocs.get(linkId);
                    if (before == null || before.isEmpty()) {
                        // something is wrong, but it is already freed
                        log.warn("There was no resource left to release on {}", linkId);
                        continue;
                    }
                    List<LinkResourceAllocations> after = new ArrayList<>(before);
                    after.remove(allocations);
                    linkAllocs.replace(linkId, before, after);
                }

                tx.commitTransaction();
                success = true;
            } catch (TransactionException e) {
                log.debug("Transaction failed, retrying");
            } catch (Exception e) {
                log.error("Exception thrown during releaseResource {}",
                          allocations, e);
                tx.rollbackTransaction();
                throw e;
            }
        } while (!success);

        // Issue events to force recompilation of intents.
        final List<LinkResourceAllocations> releasedResources =
                ImmutableList.of(allocations);
        return new LinkResourceEvent(
                LinkResourceEvent.Type.ADDITIONAL_RESOURCES_AVAILABLE,
                releasedResources);
    }

    @Override
    public LinkResourceAllocations getAllocations(IntentId intentId) {
        checkNotNull(intentId);
        TransactionOptions opt = new TransactionOptions();
        // read-only and will never be commited, thus does not need durability
        opt.setTransactionType(TransactionType.LOCAL);
        TransactionContext tx = theInstance.newTransactionContext(opt);
        tx.beginTransaction();
        try {
            STxMap<IntentId, LinkResourceAllocations> intentAllocs = getIntentAllocs(tx);
            return intentAllocs.get(intentId);
        } finally {
            tx.rollbackTransaction();
        }
    }

    @Override
    public List<LinkResourceAllocations> getAllocations(Link link) {
        checkNotNull(link);
        final LinkKey key = LinkKey.linkKey(link);

        TransactionOptions opt = new TransactionOptions();
        // read-only and will never be commited, thus does not need durability
        opt.setTransactionType(TransactionType.LOCAL);
        TransactionContext tx = theInstance.newTransactionContext(opt);
        tx.beginTransaction();
        List<LinkResourceAllocations> res = null;
        try {
            STxMap<LinkKey, List<LinkResourceAllocations>> linkAllocs = getLinkAllocs(tx);
            res = linkAllocs.get(key);
        } finally {
            tx.rollbackTransaction();
        }

        if (res == null) {
            // try to add empty list
            TransactionContext tx2 = theInstance.newTransactionContext();
            tx2.beginTransaction();
            try {
                res = getLinkAllocs(tx2).putIfAbsent(key, new ArrayList<>());
                tx2.commitTransaction();
                if (res == null) {
                    return Collections.emptyList();
                } else {
                    return res;
                }
            } catch (TransactionException e) {
                // concurrently added?
                return getAllocations(link);
            } catch (Exception e) {
                tx.rollbackTransaction();
            }
        }
        return res;
    }

    private Iterable<LinkResourceAllocations> getAllocations(TransactionContext tx,
                                                             Link link) {
        checkNotNull(tx);
        checkNotNull(link);
        final LinkKey key = LinkKey.linkKey(link);

        STxMap<LinkKey, List<LinkResourceAllocations>> linkAllocs = getLinkAllocs(tx);
        List<LinkResourceAllocations> res = null;
        res = linkAllocs.get(key);
        if (res == null) {
            res = linkAllocs.putIfAbsent(key, new ArrayList<>());
            if (res == null) {
                return Collections.emptyList();
            } else {
                return res;
            }
        }
        return res;
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations() {
        TransactionContext tx = theInstance.newTransactionContext();
        tx.beginTransaction();
        try {
            STxMap<IntentId, LinkResourceAllocations> intentAllocs = getIntentAllocs(tx);
            return intentAllocs.values();
        } finally {
            tx.rollbackTransaction();
        }
    }
}
