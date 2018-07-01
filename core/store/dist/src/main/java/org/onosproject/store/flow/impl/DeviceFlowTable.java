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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
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
    private static final int NUM_BUCKETS = 1024;
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
        .register(KryoNamespaces.API)
        .register(BucketId.class)
        .register(FlowBucket.class)
        .register(FlowBucketDigest.class)
        .register(LogicalTimestamp.class)
        .register(Timestamped.class)
        .build());

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final MessageSubject getDigestsSubject;
    private final MessageSubject getBucketSubject;
    private final MessageSubject backupSubject;

    private final DeviceId deviceId;
    private final ClusterCommunicationService clusterCommunicator;
    private final LifecycleManager lifecycleManager;
    private final ScheduledExecutorService executorService;
    private final NodeId localNodeId;

    private final LogicalClock clock = new LogicalClock();

    private volatile DeviceReplicaInfo replicaInfo;
    private volatile long activeTerm;

    private final LifecycleEventListener lifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void event(LifecycleEvent event) {
            executorService.execute(() -> onLifecycleEvent(event));
        }
    };

    private ScheduledFuture<?> backupFuture;
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
        ScheduledExecutorService executorService,
        long backupPeriod,
        long antiEntropyPeriod) {
        this.deviceId = deviceId;
        this.clusterCommunicator = clusterCommunicator;
        this.lifecycleManager = lifecycleManager;
        this.executorService = executorService;
        this.localNodeId = clusterService.getLocalNode().id();

        addListeners();

        for (int i = 0; i < NUM_BUCKETS; i++) {
            flowBuckets.put(i, new FlowBucket(new BucketId(deviceId, i)));
        }

        getDigestsSubject = new MessageSubject(String.format("flow-store-%s-digests", deviceId));
        getBucketSubject = new MessageSubject(String.format("flow-store-%s-bucket", deviceId));
        backupSubject = new MessageSubject(String.format("flow-store-%s-backup", deviceId));

        setBackupPeriod(backupPeriod);
        setAntiEntropyPeriod(antiEntropyPeriod);
        registerSubscribers();

        startTerm(lifecycleManager.getReplicaInfo());
    }

    /**
     * Sets the flow table backup period.
     *
     * @param backupPeriod the flow table backup period in milliseconds
     */
    synchronized void setBackupPeriod(long backupPeriod) {
        ScheduledFuture<?> backupFuture = this.backupFuture;
        if (backupFuture != null) {
            backupFuture.cancel(false);
        }
        this.backupFuture = executorService.scheduleAtFixedRate(
            this::backup, backupPeriod, backupPeriod, TimeUnit.MILLISECONDS);
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
        this.antiEntropyFuture = executorService.scheduleAtFixedRate(
            this::runAntiEntropy, antiEntropyPeriod, antiEntropyPeriod, TimeUnit.MILLISECONDS);
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
    public Set<FlowEntry> getFlowEntries() {
        return flowBuckets.values().stream()
            .flatMap(bucket -> bucket.getFlowBucket().values().stream())
            .flatMap(entries -> entries.values().stream())
            .collect(Collectors.toSet());
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
        if (activeTerm < term) {
            log.debug("Enqueueing operation for device {}", deviceId);
            synchronized (flowTasks) {
                // Double checked lock on the active term.
                if (activeTerm < term) {
                    CompletableFuture<T> future = new CompletableFuture<>();
                    flowTasks.computeIfAbsent(bucket.bucketId().bucket(), b -> new LinkedList<>())
                        .add(() -> future.complete(function.apply(bucket, term)));
                    return future;
                }
            }
        }
        return CompletableFuture.completedFuture(function.apply(bucket, term));
    }

    /**
     * Backs up all buckets in the given device to the given node.
     */
    private void backup() {
        DeviceReplicaInfo replicaInfo = lifecycleManager.getReplicaInfo();

        // If the local node is not currently the master, skip the backup.
        if (!replicaInfo.isMaster(localNodeId)) {
            return;
        }

        // Otherwise, iterate through backup nodes and backup the device.
        for (NodeId nodeId : replicaInfo.backups()) {
            try {
                backup(nodeId, replicaInfo.term());
            } catch (Exception e) {
                log.error("Backup of " + deviceId + " to " + nodeId + " failed", e);
            }
        }
    }

    /**
     * Backs up all buckets for the device to the given node.
     *
     * @param nodeId the node to which to back up the device
     * @param term   the term for which to backup to the node
     */
    private void backup(NodeId nodeId, long term) {
        for (FlowBucket bucket : flowBuckets.values()) {
            // If the bucket is not in the current term, skip it. This forces synchronization of the bucket
            // to occur prior to the new master replicating changes in the bucket to backups.
            if (bucket.term() != term) {
                continue;
            }

            // Record the logical timestamp from the bucket to keep track of the highest logical time replicated.
            LogicalTimestamp timestamp = bucket.timestamp();

            // If the backup can be run (no concurrent backup to the node in progress) then run it.
            BackupOperation operation = new BackupOperation(nodeId, bucket.bucketId().bucket());
            if (startBackup(operation, timestamp)) {
                backup(bucket.copy(), nodeId).whenCompleteAsync((succeeded, error) -> {
                    if (error != null) {
                        log.debug("Backup operation {} failed", operation, error);
                        failBackup(operation);
                    } else if (succeeded) {
                        succeedBackup(operation, timestamp);
                        backup(nodeId, term);
                    } else {
                        log.debug("Backup operation {} failed: term mismatch", operation);
                        failBackup(operation);
                    }
                }, executorService);
            }
        }
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
        return sendWithTimestamp(bucket, backupSubject, nodeId);
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
        }, executorService);
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
        syncFlowsOn(prevReplicaInfo.master())
            .whenCompleteAsync((result, error) -> {
                if (error != null) {
                    log.debug("Failed to synchronize flows on previous master {}", prevReplicaInfo.master(), error);
                    syncFlowsOnBackups(prevReplicaInfo, newReplicaInfo);
                } else {
                    activateMaster(newReplicaInfo);
                }
            }, executorService);
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
        syncFlowsOn(backups)
            .whenCompleteAsync((result, error) -> {
                if (error != null) {
                    log.debug("Failed to synchronize flows on previous backup nodes {}", backups, error);
                }
                activateMaster(newReplicaInfo);
            }, executorService);
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
        return requestBucket(nodeId, bucketNumber)
            .thenAcceptAsync(flowBucket -> {
                flowBuckets.compute(flowBucket.bucketId().bucket(),
                    (id, bucket) -> flowBucket.getDigest().isNewerThan(bucket.getDigest()) ? flowBucket : bucket);
            }, executorService);
    }

    /**
     * Requests the given bucket from the given node.
     *
     * @param nodeId the node from which to request the bucket
     * @param bucket the bucket to request
     * @return a future to be completed with the bucket
     */
    private CompletableFuture<FlowBucket> requestBucket(NodeId nodeId, int bucket) {
        log.debug("Requesting flow bucket {} from {}", bucket, nodeId);
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
        log.debug("Activating term {} for device {}", replicaInfo.term(), deviceId);
        for (int i = 0; i < NUM_BUCKETS; i++) {
            activateBucket(i);
        }
        lifecycleManager.activate(replicaInfo.term());
        activeTerm = replicaInfo.term();
    }

    /**
     * Activates the given bucket number.
     *
     * @param bucket the bucket number to activate
     */
    private void activateBucket(int bucket) {
        Queue<Runnable> tasks;
        synchronized (flowTasks) {
            tasks = flowTasks.remove(bucket);
        }
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

        // If the local node is neither the master or a backup for the device, clear the flow table.
        if (!replicaInfo.isMaster(localNodeId) && !replicaInfo.isBackup(localNodeId)) {
            flowBuckets.values().forEach(bucket -> bucket.clear());
        }
        activeTerm = replicaInfo.term();
    }

    /**
     * Handles an update to a term.
     */
    private void updateTerm(DeviceReplicaInfo replicaInfo) {
        if (replicaInfo.term() == this.replicaInfo.term()) {
            this.replicaInfo = replicaInfo;

            // If the local node is neither the master or a backup for the device *and the term is active*,
            // clear the flow table.
            if (activeTerm == replicaInfo.term()
                && !replicaInfo.isMaster(localNodeId)
                && !replicaInfo.isBackup(localNodeId)) {
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
        }, SERIALIZER::encode, executorService);
    }

    /**
     * Registers internal message subscribers.
     */
    private void registerSubscribers() {
        receiveWithTimestamp(getDigestsSubject, v -> getDigests());
        receiveWithTimestamp(getBucketSubject, this::onGetBucket);
        receiveWithTimestamp(backupSubject, this::onBackup);
    }

    /**
     * Unregisters internal message subscribers.
     */
    private void unregisterSubscribers() {
        clusterCommunicator.removeSubscriber(getDigestsSubject);
        clusterCommunicator.removeSubscriber(getBucketSubject);
        clusterCommunicator.removeSubscriber(backupSubject);
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
        ScheduledFuture<?> backupFuture = this.backupFuture;
        if (backupFuture != null) {
            backupFuture.cancel(false);
        }

        ScheduledFuture<?> antiEntropyFuture = this.antiEntropyFuture;
        if (antiEntropyFuture != null) {
            antiEntropyFuture.cancel(false);
        }
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
