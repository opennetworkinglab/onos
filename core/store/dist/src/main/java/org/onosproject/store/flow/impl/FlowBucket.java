/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.flow.impl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.store.LogicalTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for a bucket of flows assigned to a specific device.
 * <p>
 * The bucket is mutable. When changes are made to the bucket, the term and timestamp in which the change
 * occurred is recorded for ordering changes.
 */
public class FlowBucket {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowBucket.class);
    private final BucketId bucketId;
    private volatile long term;
    private volatile LogicalTimestamp timestamp;
    private final Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> flowBucket;

    FlowBucket(BucketId bucketId) {
        this(bucketId, 0, new LogicalTimestamp(0), Maps.newConcurrentMap());
    }

    private FlowBucket(
        BucketId bucketId,
        long term,
        LogicalTimestamp timestamp,
        Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> flowBucket) {
        this.bucketId = bucketId;
        this.term = term;
        this.timestamp = timestamp;
        this.flowBucket = flowBucket;
    }

    /**
     * Returns the flow bucket identifier.
     *
     * @return the flow bucket identifier
     */
    public BucketId bucketId() {
        return bucketId;
    }

    /**
     * Returns the flow bucket term.
     *
     * @return the flow bucket term
     */
    public long term() {
        return term;
    }

    /**
     * Returns the flow bucket timestamp.
     *
     * @return the flow bucket timestamp
     */
    public LogicalTimestamp timestamp() {
        return timestamp;
    }

    /**
     * Returns the digest for the bucket.
     *
     * @return the digest for the bucket
     */
    public FlowBucketDigest getDigest() {
        return new FlowBucketDigest(bucketId().bucket(), term(), timestamp());
    }

    /**
     * Returns the flow entries in the bucket.
     *
     * @return the flow entries in the bucket
     */
    public Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> getFlowBucket() {
        return flowBucket;
    }

    /**
     * Returns the flow entries for the given flow.
     *
     * @param flowId the flow identifier
     * @return the flows for the given flow ID
     */
    public Map<StoredFlowEntry, StoredFlowEntry> getFlowEntries(FlowId flowId) {
        Map<StoredFlowEntry, StoredFlowEntry> flowEntries = flowBucket.get(flowId);
        return flowEntries != null ? flowEntries : flowBucket.computeIfAbsent(flowId, id -> Maps.newConcurrentMap());
    }

    /**
     * Counts the flows in the bucket.
     *
     * @return the number of flows in the bucket
     */
    public int count() {
        return flowBucket.values()
            .stream()
            .mapToInt(entry -> entry.values().size())
            .sum();
    }

    /**
     * Returns a new copy of the flow bucket.
     *
     * @return a new copy of the flow bucket
     */
    FlowBucket copy() {
        return new FlowBucket(
            bucketId,
            term,
            timestamp,
            flowBucket.entrySet()
                .stream()
                .map(e -> Maps.immutableEntry(e.getKey(), Maps.newHashMap(e.getValue())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
    }

    /**
     * Records an update to the bucket.
     */
    private void recordUpdate(long term, LogicalTimestamp timestamp) {
        this.term = term;
        this.timestamp = timestamp;
    }

    /**
     * Adds the given flow rule to the bucket.
     *
     * @param rule  the rule to add
     * @param term  the term in which the change occurred
     * @param clock the logical clock
     */
    public void add(FlowEntry rule, long term, LogicalClock clock) {
        Map<StoredFlowEntry, StoredFlowEntry> flowEntries = flowBucket.get(rule.id());
        if (flowEntries == null) {
            flowEntries = flowBucket.computeIfAbsent(rule.id(), id -> Maps.newConcurrentMap());
        }
        flowEntries.put((StoredFlowEntry) rule, (StoredFlowEntry) rule);
        recordUpdate(term, clock.getTimestamp());
    }

    /**
     * Updates the given flow rule in the bucket.
     *
     * @param rule  the rule to update
     * @param term  the term in which the change occurred
     * @param clock the logical clock
     */
    public void update(FlowEntry rule, long term, LogicalClock clock) {
        Map<StoredFlowEntry, StoredFlowEntry> flowEntries = flowBucket.get(rule.id());
        if (flowEntries == null) {
            flowEntries = flowBucket.computeIfAbsent(rule.id(), id -> Maps.newConcurrentMap());
        }
        flowEntries.computeIfPresent((StoredFlowEntry) rule, (k, stored) -> {
            if (rule instanceof DefaultFlowEntry) {
                DefaultFlowEntry updated = (DefaultFlowEntry) rule;
                if (stored instanceof DefaultFlowEntry) {
                    DefaultFlowEntry storedEntry = (DefaultFlowEntry) stored;
                    if (updated.created() >= storedEntry.created()) {
                        recordUpdate(term, clock.getTimestamp());
                        return updated;
                    } else {
                        LOGGER.debug("Trying to update more recent flow entry {} (stored: {})", updated, stored);
                        return stored;
                    }
                }
            }
            return stored;
        });
    }

    /**
     * Applies the given update function to the rule.
     *
     * @param rule     the rule to update
     * @param function the update function to apply
     * @param term     the term in which the change occurred
     * @param clock    the logical clock
     * @param <T>      the result type
     * @return the update result or {@code null} if the rule was not updated
     */
    public <T> T update(FlowRule rule, Function<StoredFlowEntry, T> function, long term, LogicalClock clock) {
        Map<StoredFlowEntry, StoredFlowEntry> flowEntries = flowBucket.get(rule.id());
        if (flowEntries == null) {
            flowEntries = flowBucket.computeIfAbsent(rule.id(), id -> Maps.newConcurrentMap());
        }

        AtomicReference<T> resultRef = new AtomicReference<>();
        flowEntries.computeIfPresent(new DefaultFlowEntry(rule), (k, stored) -> {
            if (stored != null) {
                T result = function.apply(stored);
                if (result != null) {
                    recordUpdate(term, clock.getTimestamp());
                    resultRef.set(result);
                }
            }
            return stored;
        });
        return resultRef.get();
    }

    /**
     * Removes the given flow rule from the bucket.
     *
     * @param rule  the rule to remove
     * @param term  the term in which the change occurred
     * @param clock the logical clock
     * @return the removed flow entry
     */
    public FlowEntry remove(FlowEntry rule, long term, LogicalClock clock) {
        final AtomicReference<FlowEntry> removedRule = new AtomicReference<>();
        flowBucket.computeIfPresent(rule.id(), (flowId, flowEntries) -> {
            flowEntries.computeIfPresent((StoredFlowEntry) rule, (k, stored) -> {
                if (rule instanceof DefaultFlowEntry) {
                    DefaultFlowEntry toRemove = (DefaultFlowEntry) rule;
                    if (stored instanceof DefaultFlowEntry) {
                        DefaultFlowEntry storedEntry = (DefaultFlowEntry) stored;
                        if (toRemove.created() < storedEntry.created()) {
                            LOGGER.debug("Trying to remove more recent flow entry {} (stored: {})", toRemove, stored);
                            // the key is not updated, removedRule remains null
                            return stored;
                        }
                    }
                }
                removedRule.set(stored);
                return null;
            });
            return flowEntries.isEmpty() ? null : flowEntries;
        });

        if (removedRule.get() != null) {
            recordUpdate(term, clock.getTimestamp());
            return removedRule.get();
        } else {
            return null;
        }
    }

    /**
     * Purges the bucket.
     */
    public void purge() {
        flowBucket.clear();
    }

    /**
     * Purge the entries with the given application ID.
     *
     * @param appId the application ID
     * @param term  the term in which the purge occurred
     * @param clock the logical clock
     */
    public void purge(ApplicationId appId, long term, LogicalClock clock) {
        boolean anythingRemoved = flowBucket.values().removeIf(flowEntryMap -> {
            flowEntryMap.values().removeIf(storedFlowEntry -> storedFlowEntry.appId() == appId.id());
            return flowEntryMap.isEmpty();
        });
        if (anythingRemoved) {
            recordUpdate(term, clock.getTimestamp());
        }
    }


    /**
     * Clears the bucket.
     */
    public void clear() {
        term = 0;
        timestamp = new LogicalTimestamp(0);
        flowBucket.clear();
    }
}
