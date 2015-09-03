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
package org.onosproject.sdnip;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.routing.IntentSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Synchronizes intents between the in-memory intent store and the
 * IntentService.
 */
public class IntentSynchronizer implements IntentSynchronizationService {

    private static final Logger log =
        LoggerFactory.getLogger(IntentSynchronizer.class);

    private final ApplicationId appId;
    private final IntentService intentService;

    private final Map<Key, Intent> intents;

    //
    // State to deal with SDN-IP Leader election and pushing Intents
    //
    private final ExecutorService bgpIntentsSynchronizerExecutor;
    private volatile boolean isElectedLeader = false;
    private volatile boolean isActivatedLeader = false;

    /**
     * Class constructor.
     *
     * @param appId the Application ID
     * @param intentService the intent service
     */
    IntentSynchronizer(ApplicationId appId, IntentService intentService) {
        this(appId, intentService,
                newSingleThreadExecutor(groupedThreads("onos/sdnip", "sync")));
    }

    /**
     * Class constructor.
     *
     * @param appId the Application ID
     * @param intentService the intent service
     * @param executorService executor service for synchronization thread
     */
    IntentSynchronizer(ApplicationId appId, IntentService intentService,
                       ExecutorService executorService) {
        this.appId = appId;
        this.intentService = intentService;

        intents = new ConcurrentHashMap<>();

        bgpIntentsSynchronizerExecutor = executorService;
    }

    /**
     * Starts the synchronizer.
     */
    public void start() {

    }

    /**
     * Stops the synchronizer.
     */
    public void stop() {
        synchronized (this) {
            // Stop the thread(s)
            bgpIntentsSynchronizerExecutor.shutdownNow();

            //
            // Withdraw all SDN-IP intents
            //
            if (!isElectedLeader) {
                return;         // Nothing to do: not the leader anymore
            }

            //
            // NOTE: We don't withdraw the intents during shutdown, because
            // it creates flux in the data plane during switchover.
            //

            /*
            //
            // Build a batch operation to withdraw all intents from this
            // application.
            //
            log.debug("SDN-IP Intent Synchronizer shutdown: " +
                      "withdrawing all intents...");
            IntentOperations.Builder builder = IntentOperations.builder(appId);
            for (Intent intent : intentService.getIntents()) {
                // Skip the intents from other applications
                if (!intent.appId().equals(appId)) {
                    continue;
                }

                // Skip the intents that are already withdrawn
                IntentState intentState =
                    intentService.getIntentState(intent.id());
                if ((intentState == null) ||
                    intentState.equals(IntentState.WITHDRAWING) ||
                    intentState.equals(IntentState.WITHDRAWN)) {
                    continue;
                }

                log.trace("SDN-IP Intent Synchronizer withdrawing intent: {}",
                          intent);
                builder.addWithdrawOperation(intent.id());
            }
            IntentOperations intentOperations = builder.build();
            intentService.execute(intentOperations);
            leaderChanged(false);

            peerIntents.clear();
            routeIntents.clear();
            log.debug("SDN-IP Intent Synchronizer shutdown completed");
            */
        }
    }

    @Override
    public void submit(Intent intent) {
        synchronized (this) {
            intents.put(intent.key(), intent);
            if (isElectedLeader && isActivatedLeader) {
                log.trace("SDN-IP Submitting intent: {}", intent);
                intentService.submit(intent);
            }
        }
    }

    @Override
    public void withdraw(Intent intent) {
        synchronized (this) {
            intents.remove(intent.key(), intent);
            if (isElectedLeader && isActivatedLeader) {
                log.trace("SDN-IP Withdrawing intent: {}", intent);
                intentService.withdraw(intent);
            }
        }
    }

    /**
     * Signals the synchronizer that the SDN-IP leadership has changed.
     *
     * @param isLeader true if this instance is now the leader, otherwise false
     */
    public void leaderChanged(boolean isLeader) {
        log.debug("SDN-IP Leader changed: {}", isLeader);

        if (!isLeader) {
            this.isElectedLeader = false;
            this.isActivatedLeader = false;
            return;                     // Nothing to do
        }
        this.isActivatedLeader = false;
        this.isElectedLeader = true;

        // Run the synchronization method off-thread
        bgpIntentsSynchronizerExecutor.execute(this::synchronizeIntents);
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
                if (!IntentUtils.equals(serviceIntent, localIntent) || state == null ||
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

        log.debug("SDN-IP Intent Synchronizer: submitting {}, withdrawing {}",
                intentsToAdd.size(), intentsToRemove.size());

        // Withdraw Intents
        for (Intent intent : intentsToRemove) {
            intentService.withdraw(intent);
            log.trace("SDN-IP Intent Synchronizer: withdrawing intent: {}",
                    intent);
        }
        if (!isElectedLeader) {
            log.debug("SDN-IP Intent Synchronizer: cannot withdraw intents: " +
                    "not elected leader anymore");
            isActivatedLeader = false;
            return;
        }

        // Add Intents
        for (Intent intent : intentsToAdd) {
            intentService.submit(intent);
            log.trace("SDN-IP Intent Synchronizer: submitting intent: {}",
                    intent);
        }
        if (!isElectedLeader) {
            log.debug("SDN-IP Intent Synchronizer: cannot submit intents: " +
                    "not elected leader anymore");
            isActivatedLeader = false;
            return;
        }

        if (isElectedLeader) {
            isActivatedLeader = true;       // Allow push of Intents
        } else {
            isActivatedLeader = false;
        }
        log.debug("SDN-IP intent synchronization completed");
    }

}
