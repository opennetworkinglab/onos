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
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.service.BatchWriteRequest;
import org.onosproject.store.service.BatchWriteRequest.Builder;
import org.onosproject.store.service.BatchWriteResult;
import org.onosproject.store.service.DatabaseAdminService;
import org.onosproject.store.service.DatabaseException;
import org.onosproject.store.service.DatabaseService;
import org.onosproject.store.service.VersionedValue;
import org.onosproject.store.service.WriteRequest;
import org.onosproject.store.service.WriteResult;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static org.onlab.util.HexString.toHexString;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages link resources using database service.
 */
@Component(immediate = true, enabled = false)
@Service
public class DistributedLinkResourceStore implements LinkResourceStore {

    private final Logger log = getLogger(getClass());

    private static final Bandwidth DEFAULT_BANDWIDTH = Bandwidth.valueOf(1_000);

    // table to store current allocations
    /** LinkKey -> List<LinkResourceAllocations>. */
    private static final String LINK_RESOURCE_ALLOCATIONS = "LinkResourceAllocations";

    /** IntentId -> LinkResourceAllocations. */
    private static final String INTENT_ALLOCATIONS = "IntentAllocations";

    private static final Bandwidth EMPTY_BW = Bandwidth.valueOf(0);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DatabaseAdminService databaseAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DatabaseService databaseService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    // Link annotation key name to use as bandwidth
    private String bandwidthAnnotation = AnnotationKeys.BANDWIDTH;
    // Link annotation key name to use as max lambda
    private String wavesAnnotation = AnnotationKeys.OPTICAL_WAVES;

    private StoreSerializer serializer;


    void createTable(String tableName) {
        boolean tableReady = false;
        do {
            try {
                if (!databaseAdminService.listTables().contains(tableName)) {
                    databaseAdminService.createTable(tableName);
                }
                tableReady = true;
            } catch (DatabaseException e) {
                log.debug("Failed creating table, retrying", e);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    throw new DatabaseException(e1);
                }
            }
        } while (!tableReady);
    }

    @Activate
    public void activate() {

        serializer = new KryoSerializer();

        createTable(LINK_RESOURCE_ALLOCATIONS);
        createTable(INTENT_ALLOCATIONS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
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
        Map<ResourceType, Set<? extends ResourceAllocation>> freeResources = getFreeResourcesEx(link);
        Set<ResourceAllocation> allFree = new HashSet<>();
        for (Set<? extends ResourceAllocation> r:freeResources.values()) {
            allFree.addAll(r);
        }
        return allFree;
    }

    private Map<ResourceType, Set<? extends ResourceAllocation>> getFreeResourcesEx(Link link) {
        // returns capacity - allocated

        checkNotNull(link);
        Map<ResourceType, Set<? extends ResourceAllocation>> free = new HashMap<>();
        final Map<ResourceType, Set<? extends ResourceAllocation>> caps = getResourceCapacity(link);
        final Iterable<LinkResourceAllocations> allocations = getAllocations(link);

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

    private LinkResourceAllocations getIntentAllocations(IntentId id) {
        VersionedValue vv
            = databaseService.get(INTENT_ALLOCATIONS, toIntentDbKey(checkNotNull(id)));
        if (vv == null || vv.value() == null) {
            return null;
        }
        return decodeIntentAllocations(vv.value());
    }

    private Builder putIntentAllocations(Builder ctx,
                                         IntentId id,
                                         LinkResourceAllocations alloc) {
        return ctx.put(INTENT_ALLOCATIONS,
                       toIntentDbKey(id),
                       encodeIntentAllocations(alloc));
    }


    @Override
    public void allocateResources(LinkResourceAllocations allocations) {
        checkNotNull(allocations);

        Builder tx = BatchWriteRequest.newBuilder();

        // TODO: Should IntentId -> Allocation be updated conditionally?
        putIntentAllocations(tx, allocations.intendId(), allocations);

        for (Link link : allocations.links()) {
            allocateLinkResource(tx, link, allocations);
        }

        BatchWriteRequest batch = tx.build();
//        log.info("Intent: {}", databaseService.getAll(INTENT_ALLOCATIONS));
//        log.info("Link: {}", databaseService.getAll(LINK_RESOURCE_ALLOCATIONS));

        BatchWriteResult result = databaseService.batchWrite(batch);
        if (!result.isSuccessful()) {
            log.error("Allocation Failed.");
            if (log.isDebugEnabled()) {
                logFailureDetail(batch, result);
            }
            checkState(result.isSuccessful(), "Allocation failed");
        }
    }

    private void logFailureDetail(BatchWriteRequest batch,
                                  BatchWriteResult result) {
        for (int i = 0; i < batch.batchSize(); ++i) {
            final WriteRequest req = batch.getAsList().get(i);
            final WriteResult fail = result.getAsList().get(i);
            switch (fail.status()) {
            case ABORTED:
                log.debug("ABORTED: {}@{}", req.key(), req.tableName());
                break;
            case PRECONDITION_VIOLATION:
                switch (req.type()) {
                case PUT_IF_ABSENT:
                    log.debug("{}: {}@{} : {}", req.type(),
                              req.key(), req.tableName(), fail.previousValue());
                    break;
                case PUT_IF_VALUE:
                case REMOVE_IF_VALUE:
                    log.debug("{}: {}@{} : was {}, expected {}", req.type(),
                              req.key(), req.tableName(),
                              fail.previousValue(),
                              toHexString(req.oldValue()));
                    break;
                case PUT_IF_VERSION:
                case REMOVE_IF_VERSION:
                    log.debug("{}: {}@{} : was {}, expected {}", req.type(),
                              req.key(), req.tableName(),
                              fail.previousValue().version(),
                              req.previousVersion());
                    break;
                default:
                    log.error("Should never reach here.");
                    break;
                }
                break;
            default:
                log.error("Should never reach here.");
                break;
            }
        }
    }

    private Builder allocateLinkResource(Builder builder, Link link,
                                         LinkResourceAllocations allocations) {

        // requested resources
        Set<ResourceAllocation> reqs = allocations.getResourceAllocation(link);

        Map<ResourceType, Set<? extends ResourceAllocation>> available = getFreeResourcesEx(link);
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
                bwLeft -= ((BandwidthResourceAllocation) req).bandwidth().toDouble();
                BandwidthResourceAllocation bwReq = ((BandwidthResourceAllocation) req);
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
                final LambdaResourceAllocation lambdaAllocation = (LambdaResourceAllocation) req;
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
        final List<LinkResourceAllocations> before = getAllocations(link);
        List<LinkResourceAllocations> after = new ArrayList<>(before.size());
        after.addAll(before);
        after.add(allocations);
        replaceLinkAllocations(builder, LinkKey.linkKey(link), before, after);
        return builder;
    }

    private Builder replaceLinkAllocations(Builder builder, LinkKey linkKey,
                                           List<LinkResourceAllocations> before,
                                           List<LinkResourceAllocations> after) {

        byte[] oldValue = encodeLinkAllocations(before);
        byte[] newValue = encodeLinkAllocations(after);
        builder.putIfValueMatches(LINK_RESOURCE_ALLOCATIONS, toLinkDbKey(linkKey), oldValue, newValue);
        return builder;
    }

    @Override
    public LinkResourceEvent releaseResources(LinkResourceAllocations allocations) {
        checkNotNull(allocations);

        final IntentId intendId = allocations.intendId();
        final String dbIntentId = toIntentDbKey(intendId);
        final Collection<Link> links = allocations.links();

        boolean success;
        do {
            Builder tx = BatchWriteRequest.newBuilder();

            // TODO: Should IntentId -> Allocation be updated conditionally?
            tx.remove(INTENT_ALLOCATIONS, dbIntentId);

            for (Link link : links) {
                final LinkKey linkId = LinkKey.linkKey(link);
                final String dbLinkId = toLinkDbKey(linkId);
                VersionedValue vv = databaseService.get(LINK_RESOURCE_ALLOCATIONS, dbLinkId);
                if (vv == null || vv.value() == null) {
                    // something is wrong, but it is already freed
                    log.warn("There was no resource left to release on {}", linkId);
                    continue;
                }
                List<LinkResourceAllocations> before = decodeLinkAllocations(vv.value());
                List<LinkResourceAllocations> after = new ArrayList<>(before);
                after.remove(allocations);
                byte[] oldValue = encodeLinkAllocations(before);
                byte[] newValue = encodeLinkAllocations(after);
                tx.putIfValueMatches(LINK_RESOURCE_ALLOCATIONS, dbLinkId, oldValue, newValue);
            }

            BatchWriteResult batchWrite = databaseService.batchWrite(tx.build());
            success = batchWrite.isSuccessful();
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
        VersionedValue vv = databaseService.get(INTENT_ALLOCATIONS, toIntentDbKey(intentId));
        if (vv == null) {
            return null;
        }
        LinkResourceAllocations allocations = decodeIntentAllocations(vv.value());
        return allocations;
    }

    private String toLinkDbKey(LinkKey linkid) {
        // introduce cache if necessary
        return linkid.toString();
        // Note: Above is irreversible, if we need reverse conversion
        // we may need something like below, due to String only limitation
//        byte[] bytes = serializer.encode(linkid);
//        StringBuilder builder = new StringBuilder(bytes.length * 4);
//        boolean isFirst = true;
//        for (byte b : bytes) {
//            if (!isFirst) {
//                builder.append(',');
//            }
//            builder.append(b);
//            isFirst = false;
//        }
//        return builder.toString();
    }

//    private LinkKey toLinkKey(String linkKey) {
//        String[] bytes = linkKey.split(",");
//        ByteBuffer buf = ByteBuffer.allocate(bytes.length);
//        for (String bs : bytes) {
//            buf.put(Byte.parseByte(bs));
//        }
//        buf.flip();
//        return serializer.decode(buf);
//    }

    private String toIntentDbKey(IntentId intentid) {
        return intentid.toString();
    }

    private IntentId toIntentId(String intentid) {
        checkArgument(intentid.startsWith("0x"));
        return IntentId.valueOf(Long.parseLong(intentid.substring(2)));
    }

    private LinkResourceAllocations decodeIntentAllocations(byte[] bytes) {
        return serializer.decode(bytes);
    }

    private byte[] encodeIntentAllocations(LinkResourceAllocations alloc) {
        return serializer.encode(checkNotNull(alloc));
    }

    private List<LinkResourceAllocations> decodeLinkAllocations(byte[] bytes) {
        return serializer.decode(bytes);
    }

    private byte[] encodeLinkAllocations(List<LinkResourceAllocations> alloc) {
        return serializer.encode(checkNotNull(alloc));
    }

    @Override
    public List<LinkResourceAllocations> getAllocations(Link link) {
        checkNotNull(link);
        final LinkKey key = LinkKey.linkKey(link);
        final String dbKey = toLinkDbKey(key);
        VersionedValue vv = databaseService.get(LINK_RESOURCE_ALLOCATIONS, dbKey);
        if (vv == null) {
            // write empty so that all other update can be replace operation
            byte[] emptyList = encodeLinkAllocations(new ArrayList<>());
            boolean written = databaseService.putIfAbsent(LINK_RESOURCE_ALLOCATIONS, dbKey, emptyList);
            log.trace("Empty allocation write success? {}", written);
            vv = databaseService.get(LINK_RESOURCE_ALLOCATIONS, dbKey);
            if (vv == null) {
                log.error("Failed to re-read allocation for {}", dbKey);
                // note: cannot be Collections.emptyList();
                return new ArrayList<>();
            }
        }
        List<LinkResourceAllocations> allocations = decodeLinkAllocations(vv.value());
        return allocations;
    }

    @Override
    public Iterable<LinkResourceAllocations> getAllocations() {
        //IntentId -> LinkResourceAllocations
        Map<String, VersionedValue> all = databaseService.getAll(INTENT_ALLOCATIONS);

        return FluentIterable.from(all.values())
            .transform(new Function<VersionedValue, LinkResourceAllocations>() {

                @Override
                public LinkResourceAllocations apply(VersionedValue input) {
                    if (input == null || input.value() == null) {
                        return null;
                    }
                    return decodeIntentAllocations(input.value());
                }
            })
            .filter(notNull());
    }
}
