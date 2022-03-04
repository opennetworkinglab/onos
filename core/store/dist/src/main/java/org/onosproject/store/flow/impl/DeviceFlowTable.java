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

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleStoreException;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.store.LogicalTimestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flow table for all flows associated with a specific device.
 * <p>
 * Flows in the table are stored in buckets. Each bucket is mutated and replicated as a single unit. The device flow
 * table performs communication independent of other device flow tables for more parallelism.
 * <p>
 * This implementation uses several different replication protocols. Changes that occur on the device master are
 * replicated to the backups provided in the {@link DeviceReplicaInfo} for the master's term. Additionally, a periodic
 * anti-entropy protocol is used to detect missing flows on backups (e.g. due to a node restart). Finally, when a
 * device mastership change occurs, the new master synchronizes flows with the prior master and/or backups for the
 * device, allowing mastership to be reassigned to non-backup nodes.
 */
public class DeviceFlowTable {
    private static final int NUM_BUCKETS = 128;
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
        .register(KryoNamespaces.API)
        .register(BucketId.class)
        .register(FlowBucket.class)
        .register(FlowBucketDigest.class)
        .register(LogicalTimestamp.class)
        .register(Timestamped.class)
        .build());
    private static final int GET_FLOW_ENTRIES_TIMEOUT = 15; // seconds

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final MessageSubject getDigestsSubject;
    private final MessageSubject getBucketSubject;
    private final MessageSubject backupSubject;
    private final MessageSubject getFlowsSubject;

    private final DeviceId deviceId;
    private final ClusterCommunicationService clusterCommunicator;
    private final ClusterService clusterService;
    private final DeviceService deviceService;
    private final LifecycleManager lifecycleManager;
    private final ScheduledExecutorService scheduler;
    private final Executor executor;
    private final NodeId localNodeId;

    private final LogicalClock clock = new LogicalClock();

    private volatile DeviceReplicaInfo replicaInfo;
    private volatile long activeTerm;

    private long backupPeriod;

    private final LifecycleEventListener lifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void event(LifecycleEvent event) {
            executor.execute(() -> onLifecycleEvent(event));
        }
    };

    private ScheduledFuture<?> antiEntropyFuture;

    private final Map<Integer, Queue<Runnable>> flowTasks = Maps.newConcurrentMap();
    private final Map<Integer, FlowBucket> flowBuckets = Maps.newConcurrentMap();

    private final Map<BackupOperation, LogicalTimestamp> lastBackupTimes = Maps.newConcurrentMap();
    private final Set<BackupOperation> inFlightUpdates = Sets.newConcurrentHashSet();

    DeviceFlowTable(
        DeviceId deviceId,
        ClusterService clusterService,
        ClusterCommunicationService clusterCommunicator,
        LifecycleManager lifecycleManager,
        DeviceService deviceService,
        ScheduledExecutorService scheduler,
        Executor executor,
        long backupPeriod,
        long antiEntropyPeriod) {
        this.deviceId = deviceId;
        this.clusterCommunicator = clusterCommunicator;
        this.clusterService = clusterService;
        this.lifecycleManager = lifecycleManager;
        this.deviceService = deviceService;
        this.scheduler = scheduler;
        this.executor = executor;
        this.localNodeId = clusterService.getLocalNode().id();
        this.replicaInfo = lifecycleManager.getReplicaInfo();

        for (int i = 0; i < NUM_BUCKETS; i++) {
            flowBuckets.put(i, new FlowBucket(new BucketId(deviceId, i)));
        }

        getDigestsSubject = new MessageSubject(String.format("flow-store-%s-digests", deviceId));
        getBucketSubject = new MessageSubject(String.format("flow-store-%s-bucket", deviceId));
        backupSubject = new MessageSubject(String.format("flow-store-%s-backup", deviceId));
        getFlowsSubject = new MessageSubject(String.format("flow-store-%s-flows", deviceId));

        addListeners();

        setBackupPeriod(backupPeriod);
        setAntiEntropyPeriod(antiEntropyPeriod);
        registerSubscribers();

        scheduleBackups();

        activateMaster(replicaInfo);
    }

    /**
     * Sets the flow table backup period.
     *
     * @param backupPeriod the flow table backup period in milliseconds
     */
    synchronized void setBackupPeriod(long backupPeriod) {
        this.backupPeriod = backupPeriod;
    }

    /**
     * Sets the flow table anti-entropy period.
     *
     * @param antiEntropyPeriod the flow table anti-entropy period in milliseconds
     */
    synchronized void setAntiEntropyPeriod(long antiEntropyPeriod) {
        ScheduledFuture<?> antiEntropyFuture = this.antiEntropyFuture;
        if (antiEntropyFuture != null) {
            antiEntropyFuture.cancel(false);
        }
        this.antiEntropyFuture = scheduler.scheduleAtFixedRate(
                () -> executor.execute(this::runAntiEntropy),
                antiEntropyPeriod,
                antiEntropyPeriod,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Counts the flows in the table.
     *
     * @return the total number of flows in the table
     */
    public int count() {
        return flowBuckets.values().stream()
            .mapToInt(FlowBucket::count)
            .sum();
    }

    /**
     * Returns the flow entry for the given rule.
     *
     * @param rule the rule for which to lookup the flow entry
     * @return the flow entry for the given rule
     */
    public StoredFlowEntry getFlowEntry(FlowRule rule) {
        return getBucket(rule.id())
            .getFlowEntries(rule.id())
            .get(rule);
    }

    /**
     * Returns the set of flow entries in the table.
     *
     * @return the set of flow entries in the table
     */
    public CompletableFuture<Iterable<FlowEntry>> getFlowEntries() {
        // Fetch the entries for each bucket in parallel and then concatenate the sets
        // to create a single iterable.
        return Tools.allOf(flowBuckets.values()
            .stream()
            .map(this::getFlowEntries)
            .collect(Collectors.toList()))
            .thenApply(Iterables::concat);
    }

    /**
     * Fetches the set of flow entries in the given bucket.
     *
     * @param bucketId the bucket for which to fetch flow entries
     * @return a future to be completed once the flow entries have been retrieved
     */
    private CompletableFuture<Set<FlowEntry>> getFlowEntries(BucketId bucketId) {
        return getFlowEntries(getBucket(bucketId.bucket()));
    }

    /**
     * Fetches the set of flow entries in the given bucket.
     *
     * @param bucket the bucket for which to fetch flow entries
     * @return a future to be completed once the flow entries have been retrieved
     */
    private CompletableFuture<Set<FlowEntry>> getFlowEntries(FlowBucket bucket) {
        DeviceReplicaInfo replicaInfo = lifecycleManager.getReplicaInfo();
        // If the local node is the master, fetch the entries locally. Otherwise, request the entries
        // from the current master. Note that there's a change of a brief cycle during a mastership change.
        if (replicaInfo.isMaster(localNodeId)) {
            return CompletableFuture.completedFuture(
                bucket.getFlowBucket().values().stream()
                    .flatMap(entries -> entries.values().stream())
                    .collect(Collectors.toSet()));
        } else if (replicaInfo.master() != null) {
            return clusterCommunicator.sendAndReceive(
                bucket.bucketId(),
                getFlowsSubject,
                SERIALIZER::encode,
                SERIALIZER::decode,
                replicaInfo.master(),
                Duration.ofSeconds(GET_FLOW_ENTRIES_TIMEOUT));
        } else if (deviceService.isAvailable(deviceId)) {
            throw new FlowRuleStoreException("There is no master for available device " + deviceId);
        } else if (clusterService.getNodes().size() <= 1 + ECFlowRuleStore.backupCount) {
            //TODO remove this check when [ONOS-8080] is fixed
            //When device is not available and has no master and
            // the number of nodes surpasses the guaranteed backup count,
            // we are certain that this node has a replica.
            // -- DISCLAIMER --
            // You manually need to set the backup count for clusters > 3 nodes,
            // the default is 2, which handles the single instance and 3 node scenarios
            return CompletableFuture.completedFuture(
                    bucket.getFlowBucket().values().stream()
                            .flatMap(entries -> entries.values().stream())
                            .collect(Collectors.toSet()));
        } else {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }
    }

    /**
     * Returns the bucket for the given flow identifier.
     *
     * @param flowId the flow identifier
     * @return the bucket for the given flow identifier
     */
    private FlowBucket getBucket(FlowId flowId) {
        return getBucket(bucket(flowId));
    }

    /**
     * Returns the bucket with the given identifier.
     *
     * @param bucketId the bucket identifier
     * @return the bucket with the given identifier
     */
    private FlowBucket getBucket(int bucketId) {
        return flowBuckets.get(bucketId);
    }

    /**
     * Returns the bucket number for the given flow identifier.
     *
     * @param flowId the flow identifier
     * @return the bucket number for the given flow identifier
     */
    private int bucket(FlowId flowId) {
        return Math.abs((int) (flowId.id() % NUM_BUCKETS));
    }

    /**
     * Returns the digests for all buckets in the flow table for the device.
     *
     * @return the set of digests for all buckets for the device
     */
    private Set<FlowBucketDigest> getDigests() {
        return flowBuckets.values()
            .stream()
            .map(bucket -> bucket.getDigest())
            .collect(Collectors.toSet());
    }

    /**
     * Returns the digest for the given bucket.
     *
     * @param bucket the bucket for which to return the digest
     * @return the digest for the given bucket
     */
    private FlowBucketDigest getDigest(int bucket) {
        return flowBuckets.get(bucket).getDigest();
    }

    /**
     * Adds an entry to the table.
     *
     * @param rule the rule to add
     * @return a future to be completed once the rule has been added
     */
    public CompletableFuture<Void> add(FlowEntry rule) {
        return runInTerm(rule.id(), (bucket, term) -> {
            bucket.add(rule, term, clock);
            return null;
        });
    }

    /**
     * Updates an entry in the table.
     *
     * @param rule the rule to update
     * @return a future to be completed once the rule has been updated
     */
    public CompletableFuture<Void> update(FlowEntry rule) {
        return runInTerm(rule.id(), (bucket, term) -> {
            bucket.update(rule, term, clock);
            return null;
        });
    }

    /**
     * Applies the given update function to the rule.
     *
     * @param rule     the rule to update
     * @param function the update function to apply
     * @param <T>      the result type
     * @return a future to be completed with the update result or {@code null} if the rule was not updated
     */
    public <T> CompletableFuture<T> update(FlowRule rule, Function<StoredFlowEntry, T> function) {
        return runInTerm(rule.id(), (bucket, term) -> bucket.update(rule, function, term, clock));
    }

    /**
     * Removes an entry from the table.
     *
     * @param rule the rule to remove
     * @return a future to be completed once the rule has been removed
     */
    public CompletableFuture<FlowEntry> remove(FlowEntry rule) {
        return runInTerm(rule.id(), (bucket, term) -> bucket.remove(rule, term, clock));
    }

    /**
     * Runs the given function in the current term.
     *
     * @param flowId   the flow identifier indicating the bucket in which to run the function
     * @param function the function to execute in the current term
     * @param <T>      the future result type
     * @return a future to be completed with the function result once it has been run
     */
    private <T> CompletableFuture<T> runInTerm(FlowId flowId, BiFunction<FlowBucket, Long, T> function) {
        DeviceReplicaInfo replicaInfo = lifecycleManager.getReplicaInfo();
        if (!replicaInfo.isMaster(localNodeId)) {
            return Tools.exceptionalFuture(new IllegalStateException());
        }

        FlowBucket bucket = getBucket(flowId);

        // If the master's term is not currently active (has not been synchronized with prior replicas), enqueue
        // the change to be executed once the master has been synchronized.
        final long term = replicaInfo.term();
        CompletableFuture<T> future = new CompletableFuture<>();
        if (activeTerm < term) {
            log.debug("Enqueueing operation for device {}", deviceId);
            flowTasks.computeIfAbsent(bucket.bucketId().bucket(), b -> new LinkedList<>())
                    .add(() -> future.complete(apply(function, bucket, term)));
        } else {
            future.complete(apply(function, bucket, term));
        }
        return future;
    }

    /**
     * Applies the given function to the given bucket.
     *
     * @param function the function to apply
     * @param bucket the bucket to which to apply the function
     * @param term the term in which to apply the function
     * @param <T> the expected result type
     * @return the function result
     */
    private <T> T apply(BiFunction<FlowBucket, Long, T> function, FlowBucket bucket, long term) {
        synchronized (bucket) {
            return function.apply(bucket, term);
        }
    }

    /**
     * Schedules bucket backups.
     */
    private void scheduleBackups() {
        flowBuckets.values().forEach(bucket -> backupBucket(bucket).whenComplete((result, error) -> {
            scheduleBackup(bucket);
        }));
    }

    /**
     * Schedules a backup for the given bucket.
     *
     * @param bucket the bucket for which to schedule the backup
     */
    private void scheduleBackup(FlowBucket bucket) {
        scheduler.schedule(
                () -> executor.execute(() -> backupBucket(bucket)),
                backupPeriod,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Backs up all buckets in the given device to the given node.
     */
    private CompletableFuture<Void> backupAll() {
        CompletableFuture<?>[] futures = flowBuckets.values()
                .stream()
                .map(bucket -> backupBucket(bucket))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    /**
     * Backs up the given flow bucket.
     *
     * @param bucket the flow bucket to backup
     */
    private CompletableFuture<Void> backupBucket(FlowBucket bucket) {
        DeviceReplicaInfo replicaInfo = lifecycleManager.getReplicaInfo();

        // Only replicate if the bucket's term matches the replica term and the local node is the current master.
        // This ensures that the bucket has been synchronized prior to a new master replicating changes to backups.
        // Only replicate if the local node is the current master.
        if (bucket.term() == replicaInfo.term() && replicaInfo.isMaster(localNodeId)) {
            // Replicate the bucket to each of the backup nodes.
            CompletableFuture<?>[] futures = replicaInfo.backups()
                    .stream()
                    .map(nodeId -> backupBucketToNode(bucket, nodeId))
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Backs up the given flow bucket to the given node.
     *
     * @param bucket the bucket to backup
     * @param nodeId the node to which to back up the bucket
     * @return a future to be completed once the bucket has been backed up
     */
    private CompletableFuture<Void> backupBucketToNode(FlowBucket bucket, NodeId nodeId) {
        // Record the logical timestamp from the bucket to keep track of the highest logical time replicated.
        LogicalTimestamp timestamp = bucket.timestamp();

        // If the backup can be run (no concurrent backup to the node in progress) then run it.
        BackupOperation operation = new BackupOperation(nodeId, bucket.bucketId().bucket());
        if (startBackup(operation, timestamp)) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            backup(bucket, nodeId).whenCompleteAsync((succeeded, error) -> {
                if (error != null) {
                    log.debug("Backup operation {} failed", operation, error);
                    failBackup(operation);
                } else if (succeeded) {
                    succeedBackup(operation, timestamp);
                } else {
                    log.debug("Backup operation {} failed: term mismatch", operation);
                    failBackup(operation);
                }
                future.complete(null);
            }, executor);
            return future;
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns a boolean indicating whether the given {@link BackupOperation} can be started.
     * <p>
     * The backup can be started if no backup for the same device/bucket/node is already in progress and changes
     * are pending replication for the backup operation.
     *
     * @param operation the operation to start
     * @param timestamp the timestamp for which to start the backup operation
     * @return indicates whether the given backup operation should be started
     */
    private boolean startBackup(BackupOperation operation, LogicalTimestamp timestamp) {
        LogicalTimestamp lastBackupTime = lastBackupTimes.get(operation);
        return timestamp != null
            && (lastBackupTime == null || lastBackupTime.isOlderThan(timestamp))
            && inFlightUpdates.add(operation);
    }

    /**
     * Fails the given backup operation.
     *
     * @param operation the backup operation to fail
     */
    private void failBackup(BackupOperation operation) {
        inFlightUpdates.remove(operation);
    }

    /**
     * Succeeds the given backup operation.
     * <p>
     * The last backup time for the operation will be updated and the operation will be removed from
     * in-flight updates.
     *
     * @param operation the operation to succeed
     * @param timestamp the timestamp at which the operation was <em>started</em>
     */
    private void succeedBackup(BackupOperation operation, LogicalTimestamp timestamp) {
        lastBackupTimes.put(operation, timestamp);
        inFlightUpdates.remove(operation);
    }

    /**
     * Resets the last completion time for the given backup operation to ensure it's replicated again.
     *
     * @param operation the backup operation to reset
     */
    private void resetBackup(BackupOperation operation) {
        lastBackupTimes.remove(operation);
    }

    /**
     * Performs the given backup operation.
     *
     * @param bucket the bucket to backup
     * @param nodeId the node to which to backup the bucket
     * @return a future to be completed with a boolean indicating whether the backup operation was successful
     */
    private CompletableFuture<Boolean> backup(FlowBucket bucket, NodeId nodeId) {
        if (log.isDebugEnabled()) {
            log.debug("Backing up {} flow entries in bucket {} to {}", bucket.count(), bucket.bucketId(), nodeId);
        }
        synchronized (bucket) {
            return sendWithTimestamp(bucket, backupSubject, nodeId);
        }
    }

    /**
     * Handles a flow bucket backup from a remote peer.
     *
     * @param flowBucket the flow bucket to back up
     * @return the set of flows that could not be backed up
     */
    private boolean onBackup(FlowBucket flowBucket) {
        if (log.isDebugEnabled()) {
            log.debug("{} - Received {} flow entries in bucket {} to backup",
                deviceId, flowBucket.count(), flowBucket.bucketId());
        }

        try {
            DeviceReplicaInfo replicaInfo = lifecycleManager.getReplicaInfo();

            // If the backup is for a different term, reject the request until we learn about the new term.
            if (flowBucket.term() != replicaInfo.term()) {
                log.debug("Term mismatch for device {}: {} != {}", deviceId, flowBucket.term(), replicaInfo);
                return false;
            }

            flowBuckets.compute(flowBucket.bucketId().bucket(),
                (id, bucket) -> flowBucket.getDigest().isNewerThan(bucket.getDigest()) ? flowBucket : bucket);
            return true;
        } catch (Exception e) {
            log.warn("Failure processing backup request", e);
            return false;
        }
    }

    /**
     * Runs the anti-entropy protocol.
     */
    private void runAntiEntropy() {
        DeviceReplicaInfo replicaInfo = lifecycleManager.getReplicaInfo();
        if (!replicaInfo.isMaster(localNodeId)) {
            return;
        }

        for (NodeId nodeId : replicaInfo.backups()) {
            runAntiEntropy(nodeId);
        }
    }

    /**
     * Runs the anti-entropy protocol against the given peer.
     *
     * @param nodeId the node with which to execute the anti-entropy protocol
     */
    private void runAntiEntropy(NodeId nodeId) {
        backupAll().whenCompleteAsync((result, error) -> {
            requestDigests(nodeId).thenAcceptAsync((digests) -> {
                // Compute a set of missing BucketIds based on digest times and send them back to the master.
                for (FlowBucketDigest remoteDigest : digests) {
                    FlowBucket localBucket = getBucket(remoteDigest.bucket());
                    if (localBucket.getDigest().isNewerThan(remoteDigest)) {
                        log.debug("Detected missing flow entries on node {} in bucket {}/{}",
                                nodeId, deviceId, remoteDigest.bucket());
                        resetBackup(new BackupOperation(nodeId, remoteDigest.bucket()));
                    }
                }
            }, executor);
        }, executor);
    }

    /**
     * Sends a digest request to the given node.
     *
     * @param nodeId the node to which to send the request
     * @return future to be completed with the set of digests for the given device on the given node
     */
    private CompletableFuture<Set<FlowBucketDigest>> requestDigests(NodeId nodeId) {
        return sendWithTimestamp(deviceId, getDigestsSubject, nodeId);
    }

    /**
     * Synchronizes flows from the previous master or backups.
     *
     * @param prevReplicaInfo the previous replica info
     * @param newReplicaInfo  the new replica info
     */
    private void syncFlows(DeviceReplicaInfo prevReplicaInfo, DeviceReplicaInfo newReplicaInfo) {
        if (prevReplicaInfo == null) {
            activateMaster(newReplicaInfo);
        } else if (prevReplicaInfo.master() != null && !prevReplicaInfo.master().equals(localNodeId)) {
            syncFlowsOnMaster(prevReplicaInfo, newReplicaInfo);
        } else {
            syncFlowsOnBackups(prevReplicaInfo, newReplicaInfo);
        }
    }

    /**
     * Synchronizes flows from the previous master, falling back to backups if the master fails.
     *
     * @param prevReplicaInfo the previous replica info
     * @param newReplicaInfo  the new replica info
     */
    private void syncFlowsOnMaster(DeviceReplicaInfo prevReplicaInfo, DeviceReplicaInfo newReplicaInfo) {
        log.info("syncFlowsOnMaster {}", prevReplicaInfo.master());
        syncFlowsOn(prevReplicaInfo.master())
            .whenCompleteAsync((result, error) -> {
                if (error != null) {
                    log.warn("Failed to synchronize flows on previous master {}", prevReplicaInfo.master(), error);
                    syncFlowsOnBackups(prevReplicaInfo, newReplicaInfo);
                } else {
                    activateMaster(newReplicaInfo);
                }
            }, executor);
    }

    /**
     * Synchronizes flows from the previous backups.
     *
     * @param prevReplicaInfo the previous replica info
     * @param newReplicaInfo  the new replica info
     */
    private void syncFlowsOnBackups(DeviceReplicaInfo prevReplicaInfo, DeviceReplicaInfo newReplicaInfo) {
        List<NodeId> backups = prevReplicaInfo.backups()
            .stream()
            .filter(nodeId -> !nodeId.equals(localNodeId))
            .collect(Collectors.toList());
        log.info("syncFlowsOnBackups {}", backups);
        syncFlowsOn(backups)
            .whenCompleteAsync((result, error) -> {
                if (error != null) {
                    log.warn("Failed to synchronize flows on previous backup nodes {}", backups, error);
                }
                activateMaster(newReplicaInfo);
            }, executor);
    }

    /**
     * Synchronizes flows for the device on the given nodes.
     *
     * @param nodes the nodes via which to synchronize the flows
     * @return a future to be completed once flows have been synchronizes
     */
    private CompletableFuture<Void> syncFlowsOn(Collection<NodeId> nodes) {
        return nodes.isEmpty()
            ? CompletableFuture.completedFuture(null)
            : Tools.firstOf(nodes.stream()
            .map(node -> syncFlowsOn(node))
            .collect(Collectors.toList()))
            .thenApply(v -> null);
    }

    /**
     * Synchronizes flows for the device from the given node.
     *
     * @param nodeId the node from which to synchronize flows
     * @return a future to be completed once the flows have been synchronizes
     */
    private CompletableFuture<Void> syncFlowsOn(NodeId nodeId) {
        log.info("syncFlowsOn {}", nodeId);
        return requestDigests(nodeId)
            .thenCompose(digests -> Tools.allOf(digests.stream()
                .filter(digest -> digest.isNewerThan(getDigest(digest.bucket())))
                .map(digest -> syncBucketOn(nodeId, digest.bucket()))
                .collect(Collectors.toList())))
            .thenApply(v -> null);
    }

    /**
     * Synchronizes the given bucket on the given node.
     *
     * @param nodeId       the node on which to synchronize the bucket
     * @param bucketNumber the bucket to synchronize
     * @return a future to be completed once the bucket has been synchronizes
     */
    private CompletableFuture<Void> syncBucketOn(NodeId nodeId, int bucketNumber) {
        log.info("syncBucket {} on {}", bucketNumber, nodeId);
        return requestBucket(nodeId, bucketNumber)
            .thenAcceptAsync(flowBucket -> {
                flowBuckets.compute(flowBucket.bucketId().bucket(),
                    (id, bucket) -> flowBucket.getDigest().isNewerThan(bucket.getDigest()) ? flowBucket : bucket);
            }, executor);
    }

    /**
     * Requests the given bucket from the given node.
     *
     * @param nodeId the node from which to request the bucket
     * @param bucket the bucket to request
     * @return a future to be completed with the bucket
     */
    private CompletableFuture<FlowBucket> requestBucket(NodeId nodeId, int bucket) {
        log.info("Requesting flow bucket {} from {}", bucket, nodeId);
        return sendWithTimestamp(bucket, getBucketSubject, nodeId);
    }

    /**
     * Handles a flow bucket request.
     *
     * @param bucketId the bucket number
     * @return the flow bucket
     */
    private FlowBucket onGetBucket(int bucketId) {
        return flowBuckets.get(bucketId).copy();
    }

    /**
     * Activates the new master term.
     *
     * @param replicaInfo the new replica info
     */
    private void activateMaster(DeviceReplicaInfo replicaInfo) {
        if (replicaInfo.isMaster(localNodeId)) {
            log.info("Activating term {} for device {}", replicaInfo.term(), deviceId);
            for (int i = 0; i < NUM_BUCKETS; i++) {
                activateBucket(i);
            }
            lifecycleManager.activate(replicaInfo.term());
            activeTerm = replicaInfo.term();
        }
    }

    /**
     * Activates the given bucket number.
     *
     * @param bucket the bucket number to activate
     */
    private void activateBucket(int bucket) {
        Queue<Runnable> tasks = flowTasks.remove(bucket);
        if (tasks != null) {
            log.debug("Completing enqueued operations for device {}", deviceId);
            tasks.forEach(task -> task.run());
        }
    }

    /**
     * Handles a lifecycle event.
     */
    private void onLifecycleEvent(LifecycleEvent event) {
        log.debug("Received lifecycle event for device {}: {}", deviceId, event);
        switch (event.type()) {
            case TERM_START:
                startTerm(event.subject());
                break;
            case TERM_ACTIVE:
                activateTerm(event.subject());
                break;
            case TERM_UPDATE:
                updateTerm(event.subject());
                break;
            default:
                break;
        }
    }

    /**
     * Handles a replica change at the start of a new term.
     */
    private void startTerm(DeviceReplicaInfo replicaInfo) {
        DeviceReplicaInfo oldReplicaInfo = this.replicaInfo;
        this.replicaInfo = replicaInfo;
        if (replicaInfo.isMaster(localNodeId)) {
            log.info("Synchronizing device {} flows for term {}", deviceId, replicaInfo.term());
            syncFlows(oldReplicaInfo, replicaInfo);
        }
    }

    /**
     * Handles the activation of a term.
     */
    private void activateTerm(DeviceReplicaInfo replicaInfo) {
        if (replicaInfo.term() < this.replicaInfo.term()) {
            return;
        }
        if (replicaInfo.term() > this.replicaInfo.term()) {
            this.replicaInfo = replicaInfo;
        }

        // If the local node is neither the master or a backup for the device,
        // and the number of nodes surpasses the guaranteed backup count, clear the flow table.
        if (!replicaInfo.isMaster(localNodeId) && !replicaInfo.isBackup(localNodeId) &&
            (clusterService.getNodes().size() > 1 + ECFlowRuleStore.backupCount)) {
            flowBuckets.values().forEach(bucket -> bucket.clear());
        }
        activeTerm = replicaInfo.term();
    }

    /**
     * Handles an update to a term.
     */
    private void updateTerm(DeviceReplicaInfo replicaInfo) {
        DeviceReplicaInfo oldReplicaInfo = this.replicaInfo;
        if (oldReplicaInfo != null && replicaInfo.term() == oldReplicaInfo.term()) {
            this.replicaInfo = replicaInfo;

            // If the local node is neither the master or a backup for the device *and the term is active*,
            // and the number of nodes surpasses the guaranteed backup count, clear the flow table.
            if (activeTerm == replicaInfo.term()
                && !replicaInfo.isMaster(localNodeId)
                && !replicaInfo.isBackup(localNodeId)
            && (clusterService.getNodes().size() > 1 + ECFlowRuleStore.backupCount)) {
                flowBuckets.values().forEach(bucket -> bucket.clear());
            }
        }
    }

    /**
     * Sends a message to the given node wrapped in a Lamport timestamp.
     * <p>
     * Messages are sent in a {@link Timestamped} wrapper and are expected to be received in a {@link Timestamped}
     * wrapper. The internal {@link LogicalClock} is automatically updated on both send and receive.
     *
     * @param message  the message to send
     * @param subject  the message subject
     * @param toNodeId the node to which to send the message
     * @param <M>      the message type
     * @param <R>      the response type
     * @return a future to be completed with the response
     */
    private <M, R> CompletableFuture<R> sendWithTimestamp(M message, MessageSubject subject, NodeId toNodeId) {
        return clusterCommunicator.<Timestamped<M>, Timestamped<R>>sendAndReceive(
            clock.timestamp(message), subject, SERIALIZER::encode, SERIALIZER::decode, toNodeId)
            .thenApply(response -> {
                clock.tick(response.timestamp());
                return response.value();
            });
    }

    /**
     * Receives messages to the given subject wrapped in Lamport timestamps.
     * <p>
     * Messages are expected to be received in a {@link Timestamped} wrapper and are sent back in a {@link Timestamped}
     * wrapper. The internal {@link LogicalClock} is automatically updated on both receive and send.
     *
     * @param subject  the subject for which to register the subscriber
     * @param function the raw message handler
     * @param <M>      the raw message type
     * @param <R>      the raw response type
     */
    private <M, R> void receiveWithTimestamp(MessageSubject subject, Function<M, R> function) {
        clusterCommunicator.<Timestamped<M>, Timestamped<R>>addSubscriber(subject, SERIALIZER::decode, request -> {
            clock.tick(request.timestamp());
            return clock.timestamp(function.apply(request.value()));
        }, SERIALIZER::encode, executor);
    }

    /**
     * Registers internal message subscribers.
     */
    private void registerSubscribers() {
        receiveWithTimestamp(getDigestsSubject, v -> getDigests());
        receiveWithTimestamp(getBucketSubject, this::onGetBucket);
        receiveWithTimestamp(backupSubject, this::onBackup);
        clusterCommunicator.<BucketId, Set<FlowEntry>>addSubscriber(
            getFlowsSubject, SERIALIZER::decode, this::getFlowEntries, SERIALIZER::encode);
    }

    /**
     * Unregisters internal message subscribers.
     */
    private void unregisterSubscribers() {
        clusterCommunicator.removeSubscriber(getDigestsSubject);
        clusterCommunicator.removeSubscriber(getBucketSubject);
        clusterCommunicator.removeSubscriber(backupSubject);
        clusterCommunicator.removeSubscriber(getFlowsSubject);
    }

    /**
     * Adds internal event listeners.
     */
    private void addListeners() {
        lifecycleManager.addListener(lifecycleEventListener);
    }

    /**
     * Removes internal event listeners.
     */
    private void removeListeners() {
        lifecycleManager.removeListener(lifecycleEventListener);
    }

    /**
     * Cancels recurrent scheduled futures.
     */
    private synchronized void cancelFutures() {
        ScheduledFuture<?> antiEntropyFuture = this.antiEntropyFuture;
        if (antiEntropyFuture != null) {
            antiEntropyFuture.cancel(false);
        }
    }

    /**
     * Purges the flow table.
     */
    public void purge() {
        flowTasks.clear();
        flowBuckets.values().forEach(bucket -> bucket.purge());
        lastBackupTimes.clear();
        inFlightUpdates.clear();
    }

    /**
     * Purges the flows with the given application id.
     *
     * @param appId the application id
     * @return a future to be completed once flow rules with given application
     * id have been purged on all buckets
     */
    public CompletableFuture<Void> purge(ApplicationId appId) {
        DeviceReplicaInfo replicaInfo = lifecycleManager.getReplicaInfo();
        if (!replicaInfo.isMaster(localNodeId)) {
            return Tools.exceptionalFuture(new IllegalStateException());
        }

        // If the master's term is not currently active (has not been synchronized
        // with prior replicas), enqueue the changes to be executed once the master
        // has been synchronized.
        final long term = replicaInfo.term();
        List<CompletableFuture<Void>> completablePurges = Lists.newArrayList();
        if (activeTerm < term) {
            log.debug("Enqueueing operations for device {}", deviceId);
            flowBuckets.values().forEach(
                    bucket -> {
                        CompletableFuture<Void> future = new CompletableFuture<>();
                        completablePurges.add(future);
                        flowTasks.computeIfAbsent(bucket.bucketId().bucket(),
                                                  b -> new LinkedList<>())
                                .add(() -> future.complete(apply((bkt, trm) -> {
                                    bkt.purge(appId, trm, clock);
                                    return null;
                                    }, bucket, term)));
                    });

        } else {
            flowBuckets.values().forEach(bucket -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                completablePurges.add(future);
                future.complete(apply((bkt, trm) -> {
                    bkt.purge(appId, trm, clock);
                    return null;
                    }, bucket, term));
            });
        }
        return CompletableFuture.allOf(completablePurges.toArray(new CompletableFuture[0]));
    }

    /**
     * Closes the device flow table.
     */
    public void close() {
        removeListeners();
        unregisterSubscribers();
        cancelFutures();
        lifecycleManager.close();
    }
}
