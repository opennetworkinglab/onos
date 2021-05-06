/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.store.cluster.impl;

import com.google.common.collect.Maps;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipStore;
import org.onosproject.cluster.LeadershipStoreDelegate;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.Version;
import org.onosproject.core.VersionService;
import org.onosproject.event.Change;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.CoordinationService;
import org.onosproject.store.service.DistributedPrimitive.Status;
import org.onosproject.store.service.LeaderElector;
import org.onosproject.upgrade.UpgradeEvent;
import org.onosproject.upgrade.UpgradeEventListener;
import org.onosproject.upgrade.UpgradeService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.store.OsgiPropertyConstants.*;

/**
 * Implementation of {@code LeadershipStore} that makes use of a {@link LeaderElector}
 * primitive.
 */
@Component(
        immediate = true,
        service = LeadershipStore.class,
        property = {
                ELECTION_TIMEOUT_MILLIS + ":Long=" + ELECTION_TIMEOUT_MILLIS_DEFAULT
        }
)
public class DistributedLeadershipStore
    extends AbstractStore<LeadershipEvent, LeadershipStoreDelegate>
    implements LeadershipStore {

    private static final char VERSION_SEP = '|';

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = MANDATORY)
    protected CoordinationService storageService;

    @Reference(cardinality = MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = MANDATORY)
    protected VersionService versionService;

    @Reference(cardinality = MANDATORY)
    protected UpgradeService upgradeService;

    /** Leader election timeout in milliseconds. */
    private long electionTimeoutMillis = ELECTION_TIMEOUT_MILLIS_DEFAULT;

    private ExecutorService statusChangeHandler;
    private NodeId localNodeId;
    private LeaderElector leaderElector;
    private final Map<String, Leadership> localLeaderCache = Maps.newConcurrentMap();
    private final UpgradeEventListener upgradeListener = new InternalUpgradeEventListener();

    private final Consumer<Change<Leadership>> leadershipChangeListener =
            change -> {
                Leadership oldValue = change.oldValue();
                Leadership newValue = change.newValue();

                // If the topic is not relevant to this version, skip the event.
                if (!isLocalTopic(newValue.topic())) {
                    return;
                }

                boolean leaderChanged = !Objects.equals(oldValue.leader(), newValue.leader());
                boolean candidatesChanged = !Objects.equals(oldValue.candidates(), newValue.candidates());

                LeadershipEvent.Type eventType = null;
                if (leaderChanged && candidatesChanged) {
                    eventType = LeadershipEvent.Type.LEADER_AND_CANDIDATES_CHANGED;
                }
                if (leaderChanged && !candidatesChanged) {
                    eventType = LeadershipEvent.Type.LEADER_CHANGED;
                }
                if (!leaderChanged && candidatesChanged) {
                    eventType = LeadershipEvent.Type.CANDIDATES_CHANGED;
                }
                notifyDelegate(new LeadershipEvent(eventType, new Leadership(
                        parseTopic(change.newValue().topic()),
                        change.newValue().leader(),
                        change.newValue().candidates())));
                // Update local cache of currently held leaderships
                if (Objects.equals(newValue.leaderNodeId(), localNodeId)) {
                    localLeaderCache.put(newValue.topic(), newValue);
                } else {
                    localLeaderCache.remove(newValue.topic());
                }
            };

    private final Consumer<Status> clientStatusListener = status ->
            statusChangeHandler.execute(() -> handleStatusChange(status));

    private void handleStatusChange(Status status) {
        // Notify mastership Service of disconnect and reconnect
        if (status == Status.ACTIVE) {
            // Service Restored
            localLeaderCache.forEach((topic, leadership) -> leaderElector.run(topic, localNodeId));
            leaderElector.getLeaderships().forEach((topic, leadership) ->
                    notifyDelegate(new LeadershipEvent(
                            LeadershipEvent.Type.SERVICE_RESTORED,
                            new Leadership(
                                    parseTopic(leadership.topic()),
                                    leadership.leader(),
                                    leadership.candidates()))));
        } else if (status == Status.SUSPENDED) {
            // Service Suspended
            localLeaderCache.forEach((topic, leadership) ->
                    notifyDelegate(new LeadershipEvent(
                            LeadershipEvent.Type.SERVICE_DISRUPTED,
                            new Leadership(
                                    parseTopic(leadership.topic()),
                                    leadership.leader(),
                                    leadership.candidates()))));
        } else {
            // Should be only inactive state
            return;
        }
    }

    @Activate
    public void activate() {
        configService.registerProperties(getClass());
        statusChangeHandler = Executors.newSingleThreadExecutor(
                groupedThreads("onos/store/dist/cluster/leadership", "status-change-handler", log));
        localNodeId = clusterService.getLocalNode().id();
        leaderElector = storageService.leaderElectorBuilder()
                      .withName("onos-leadership-elections")
                      .withElectionTimeout(electionTimeoutMillis)
                      .withRelaxedReadConsistency()
                      .build()
                      .asLeaderElector();
        leaderElector.addChangeListener(leadershipChangeListener);
        leaderElector.addStatusChangeListener(clientStatusListener);
        upgradeService.addListener(upgradeListener);
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        long newElectionTimeoutMillis;
        try {
            String s = get(properties, ELECTION_TIMEOUT_MILLIS);
            newElectionTimeoutMillis = isNullOrEmpty(s) ? electionTimeoutMillis : Long.parseLong(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            log.warn("Malformed configuration detected; using defaults", e);
            newElectionTimeoutMillis = ELECTION_TIMEOUT_MILLIS_DEFAULT;
        }

        if (newElectionTimeoutMillis != electionTimeoutMillis) {
            electionTimeoutMillis = newElectionTimeoutMillis;
            leaderElector = storageService.leaderElectorBuilder()
                    .withName("onos-leadership-elections")
                    .withElectionTimeout(electionTimeoutMillis)
                    .withRelaxedReadConsistency()
                    .build()
                    .asLeaderElector();
        }
    }

    @Deactivate
    public void deactivate() {
        leaderElector.removeChangeListener(leadershipChangeListener);
        leaderElector.removeStatusChangeListener(clientStatusListener);
        upgradeService.removeListener(upgradeListener);
        statusChangeHandler.shutdown();
        configService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    @Override
    public Leadership addRegistration(String topic) {
        leaderElector.run(getLocalTopic(topic), localNodeId);
        return getLeadership(topic);
    }

    @Override
    public void removeRegistration(String topic) {
        leaderElector.withdraw(getLocalTopic(topic));
    }

    @Override
    public void removeRegistration(NodeId nodeId) {
        leaderElector.evict(nodeId);
    }

    @Override
    public boolean moveLeadership(String topic, NodeId toNodeId) {
        return leaderElector.anoint(getTopicFor(topic, toNodeId), toNodeId);
    }

    @Override
    public boolean makeTopCandidate(String topic, NodeId nodeId) {
        return leaderElector.promote(getTopicFor(topic, nodeId), nodeId);
    }

    @Override
    public Leadership getLeadership(String topic) {
        Leadership leadership = leaderElector.getLeadership(getActiveTopic(topic));
        return leadership != null ? new Leadership(
                parseTopic(leadership.topic()),
                leadership.leader(),
                leadership.candidates()) : null;
    }

    @Override
    public Map<String, Leadership> getLeaderships() {
        Map<String, Leadership> leaderships = leaderElector.getLeaderships();
        return leaderships.entrySet().stream()
                .filter(e -> isActiveTopic(e.getKey()))
                .collect(Collectors.toMap(e -> parseTopic(e.getKey()),
                        e -> new Leadership(parseTopic(e.getKey()), e.getValue().leader(), e.getValue().candidates())));
    }

    @Override
    public boolean demote(String topic, NodeId nodeId) {
        return leaderElector.demote(getTopicFor(topic, nodeId), nodeId);
    }

    /**
     * Returns a leader elector topic namespaced with the local node's version.
     *
     * @param topic the base topic
     * @return a topic string namespaced with the local node's version
     */
    private String getLocalTopic(String topic) {
        return topic + VERSION_SEP + versionService.version();
    }

    /**
     * Returns a leader elector topic namespaced with the current cluster version.
     *
     * @param topic the base topic
     * @return a topic string namespaced with the current cluster version
     */
    private String getActiveTopic(String topic) {
        return topic + VERSION_SEP + upgradeService.getVersion();
    }

    /**
     * Returns whether the given topic is a topic for the local version.
     *
     * @param topic the topic to check
     * @return whether the given topic is relevant to the local version
     */
    private boolean isLocalTopic(String topic) {
        return topic.endsWith(versionService.version().toString());
    }

    /**
     * Returns whether the given topic is a topic for the current cluster version.
     *
     * @param topic the topic to check
     * @return whether the given topic is relevant to the current cluster version
     */
    private boolean isActiveTopic(String topic) {
        return topic.endsWith(VERSION_SEP + upgradeService.getVersion().toString());
    }

    /**
     * Parses a topic string, returning the base topic.
     *
     * @param topic the topic string to parse
     * @return the base topic string
     */
    private String parseTopic(String topic) {
        return topic.substring(0, topic.lastIndexOf(VERSION_SEP));
    }

    /**
     * Returns the versioned topic for the given node.
     *
     * @param topic the topic for the given node
     * @param nodeId the node for which to return the namespaced topic
     * @return the versioned topic for the given node
     */
    private String getTopicFor(String topic, NodeId nodeId) {
        Version nodeVersion = clusterService.getVersion(nodeId);
        return nodeVersion != null ? topic + VERSION_SEP + nodeVersion : topic + VERSION_SEP + versionService.version();
    }

    /**
     * Internal upgrade event listener.
     */
    private class InternalUpgradeEventListener implements UpgradeEventListener {
        @Override
        public void event(UpgradeEvent event) {
            if (event.type() == UpgradeEvent.Type.UPGRADED || event.type() == UpgradeEvent.Type.ROLLED_BACK) {
                // Iterate through all current leaderships for the new version and trigger events.
                for (Leadership leadership : getLeaderships().values()) {
                    notifyDelegate(new LeadershipEvent(
                            LeadershipEvent.Type.LEADER_AND_CANDIDATES_CHANGED,
                            leadership));
                }
            }
        }
    }
}
