/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.intentsync;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentUtils;
import org.onosproject.net.intent.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Synchronizes intents between an in-memory intent store and the IntentService.
 */
@Service
@Component(immediate = true)
public class IntentSynchronizer implements IntentSynchronizationService,
        IntentSynchronizationAdminService {

    private static final Logger log = LoggerFactory.getLogger(IntentSynchronizer.class);

    private static final String APP_NAME = "org.onosproject.intentsynchronizer";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    private NodeId localNodeId;
    private ApplicationId appId;

    private final InternalLeadershipListener leadershipEventListener =
            new InternalLeadershipListener();

    private final Map<Key, Intent> intents = new ConcurrentHashMap<>();

    private ExecutorService intentsSynchronizerExecutor;

    private volatile boolean isElectedLeader = false;
    private volatile boolean isActivatedLeader = false;

    @Activate
    public void activate() {
        this.localNodeId = clusterService.getLocalNode().id();
        this.appId = coreService.registerApplication(APP_NAME);
        intentsSynchronizerExecutor = createExecutor();

        leadershipService.addListener(leadershipEventListener);
        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        leadershipService.withdraw(appId.name());
        leadershipService.removeListener(leadershipEventListener);

        synchronized (this) {
            intentsSynchronizerExecutor.shutdownNow();
        }

        log.info("Stopped");
    }

    /**
     * Creates an executor that will be used for synchronization tasks.
     * <p>
     * Can be overridden to change the type of executor used.
     * </p>
     *
     * @return executor service
     */
    protected ExecutorService createExecutor() {
        return newSingleThreadExecutor(groupedThreads("onos/" + appId, "sync", log));
    }

    @Override
    public void removeIntents() {
        if (!isElectedLeader) {
            // Only leader will withdraw intents
            return;
        }

        log.debug("Intent Synchronizer shutdown: withdrawing all intents...");

        for (Entry<Key, Intent> entry : intents.entrySet()) {
            intentService.withdraw(entry.getValue());
            log.debug("Intent Synchronizer withdrawing intent: {}",
                      entry.getValue());
        }

        intents.clear();
        log.info("Tried to clean all intents");
    }

    @Override
    public void removeIntentsByAppId(ApplicationId appId) {
        if (!isElectedLeader) {
            // Only leader will withdraw intents
            return;
        }

        log.debug("Withdrawing intents for app {}...",
                  appId);

        intents.entrySet()
                .stream()
                .filter(intent -> intent.getValue().appId().equals(appId))
                .forEach(intent -> {
                    log.debug("Intent Synchronizer withdrawing intent: {}",
                              intent);
                    intentService.withdraw(intent.getValue());
                    intents.remove(intent.getKey(), intent.getValue());
                    log.info("Tried to clean intents for app: {}", appId);
                });
    }

    @Override
    public void submit(Intent intent) {
        synchronized (this) {
            intents.put(intent.key(), intent);
            if (isElectedLeader && isActivatedLeader) {
                log.trace("Submitting intent: {}", intent);
                intentService.submit(intent);
            }
        }
    }

    @Override
    public void withdraw(Intent intent) {
        synchronized (this) {
            intents.remove(intent.key(), intent);
            if (isElectedLeader && isActivatedLeader) {
                log.trace("Withdrawing intent: {}", intent);
                intentService.withdraw(intent);
            }
        }
    }

    /**
     * Signals the synchronizer that the leadership has changed.
     *
     * @param isLeader true if this instance is now the leader, otherwise false
     */
    private void leaderChanged(boolean isLeader) {
        log.debug("Leader changed: {}", isLeader);

        if (!isLeader) {
            this.isElectedLeader = false;
            this.isActivatedLeader = false;
            // Nothing to do
            return;
        }
        this.isActivatedLeader = false;
        this.isElectedLeader = true;

        // Run the synchronization task
        intentsSynchronizerExecutor.execute(this::synchronizeIntents);
    }

    private void synchronizeIntents() {
        Map<Key, Intent> serviceIntents = new HashMap<>();
        intentService.getIntents().forEach(i -> {
            if (i.appId().equals(appId)) {
                serviceIntents.put(i.key(), i);
            }
        });

        List<Intent> intentsToAdd = new LinkedList<>();
        List<Intent> intentsToRemove = new LinkedList<>();

        for (Intent localIntent : intents.values()) {
            Intent serviceIntent = serviceIntents.remove(localIntent.key());
            if (serviceIntent == null) {
                intentsToAdd.add(localIntent);
            } else {
                IntentState state = intentService.getIntentState(serviceIntent.key());
                if (!IntentUtils.intentsAreEqual(serviceIntent, localIntent) || state == null ||
                        state == IntentState.WITHDRAW_REQ ||
                        state == IntentState.WITHDRAWING ||
                        state == IntentState.WITHDRAWN) {
                    intentsToAdd.add(localIntent);
                }
            }
        }

        for (Intent serviceIntent : serviceIntents.values()) {
            IntentState state = intentService.getIntentState(serviceIntent.key());
            if (state != null && state != IntentState.WITHDRAW_REQ
                    && state != IntentState.WITHDRAWING
                    && state != IntentState.WITHDRAWN) {
                intentsToRemove.add(serviceIntent);
            }
        }

        log.debug("Intent Synchronizer: submitting {}, withdrawing {}",
                intentsToAdd.size(), intentsToRemove.size());

        // Withdraw Intents
        for (Intent intent : intentsToRemove) {
            intentService.withdraw(intent);
            log.trace("Intent Synchronizer: withdrawing intent: {}",
                    intent);
        }
        if (!isElectedLeader) {
            log.debug("Intent Synchronizer: cannot withdraw intents: " +
                    "not elected leader anymore");
            isActivatedLeader = false;
            return;
        }

        // Add Intents
        for (Intent intent : intentsToAdd) {
            intentService.submit(intent);
            log.trace("Intent Synchronizer: submitting intent: {}",
                    intent);
        }
        if (!isElectedLeader) {
            log.debug("Intent Synchronizer: cannot submit intents: " +
                    "not elected leader anymore");
            isActivatedLeader = false;
            return;
        }

        if (isElectedLeader) {
            // Allow push of Intents
            isActivatedLeader = true;
        } else {
            isActivatedLeader = false;
        }
        log.debug("Intent synchronization completed");
    }

    @Override
    public void modifyPrimary(boolean isPrimary) {
        leaderChanged(isPrimary);
    }

    /**
     * A listener for leadership events.
     */
    private class InternalLeadershipListener implements LeadershipEventListener {

        @Override
        public boolean isRelevant(LeadershipEvent event) {
            return event.subject().topic().equals(appId.name());
        }

        @Override
        public void event(LeadershipEvent event) {
            switch (event.type()) {
            case LEADER_CHANGED:
            case LEADER_AND_CANDIDATES_CHANGED:
                if (localNodeId.equals(event.subject().leaderNodeId())) {
                    log.info("IntentSynchronizer gained leadership");
                    leaderChanged(true);
                } else {
                    log.info("IntentSynchronizer leader changed. New leader is {}", event.subject().leaderNodeId());
                    leaderChanged(false);
                }
            default:
                break;
            }
        }
    }
}
