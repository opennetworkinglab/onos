/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.sdnip;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.flow.criteria.Criteria.IPCriterion;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.packet.Ip4Prefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class IntentSynchronizer {
    private static final Logger log =
        LoggerFactory.getLogger(IntentSynchronizer.class);

    private final ApplicationId appId;
    private final IntentService intentService;
    private final Map<Ip4Prefix, MultiPointToSinglePointIntent> pushedRouteIntents;

    //
    // State to deal with SDN-IP Leader election and pushing Intents
    //
    private final ExecutorService bgpIntentsSynchronizerExecutor;
    private final Semaphore intentsSynchronizerSemaphore = new Semaphore(0);
    private volatile boolean isElectedLeader = false;
    private volatile boolean isActivatedLeader = false;

    /**
     * Class constructor.
     *
     * @param appId the Application ID
     * @param intentService the intent service
     */
    IntentSynchronizer(ApplicationId appId, IntentService intentService) {
        this.appId = appId;
        this.intentService = intentService;
        pushedRouteIntents = new ConcurrentHashMap<>();

        bgpIntentsSynchronizerExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                .setNameFormat("sdnip-intents-synchronizer-%d").build());
    }

    /**
     * Starts the synchronizer.
     */
    public void start() {
        bgpIntentsSynchronizerExecutor.execute(new Runnable() {
            @Override
            public void run() {
                doIntentSynchronizationThread();
            }
        });
    }

    /**
     * Stops the synchronizer.
     */
    public void stop() {
        // Stop the thread(s)
        bgpIntentsSynchronizerExecutor.shutdownNow();

        //
        // Withdraw all SDN-IP intents
        //
        if (!isElectedLeader) {
            return;         // Nothing to do: not the leader anymore
        }
        log.debug("Withdrawing all SDN-IP Route Intents...");
        for (Intent intent : intentService.getIntents()) {
            if (!(intent instanceof MultiPointToSinglePointIntent)
                || !intent.appId().equals(appId)) {
                continue;
            }
            intentService.withdraw(intent);
        }

        pushedRouteIntents.clear();
    }

    //@Override TODO hook this up to something
    public void leaderChanged(boolean isLeader) {
        log.debug("Leader changed: {}", isLeader);

        if (!isLeader) {
            this.isElectedLeader = false;
            this.isActivatedLeader = false;
            return;                     // Nothing to do
        }
        this.isActivatedLeader = false;
        this.isElectedLeader = true;

        //
        // Tell the Intents Synchronizer thread to start the synchronization
        //
        intentsSynchronizerSemaphore.release();
    }

    /**
     * Gets the pushed route intents.
     *
     * @return the pushed route intents
     */
    public Collection<MultiPointToSinglePointIntent> getPushedRouteIntents() {
        List<MultiPointToSinglePointIntent> pushedIntents = new LinkedList<>();

        for (Map.Entry<Ip4Prefix, MultiPointToSinglePointIntent> entry :
            pushedRouteIntents.entrySet()) {
            pushedIntents.add(entry.getValue());
        }
        return pushedIntents;
    }

    /**
     * Thread for Intent Synchronization.
     */
    private void doIntentSynchronizationThread() {
        boolean interrupted = false;
        try {
            while (!interrupted) {
                try {
                    intentsSynchronizerSemaphore.acquire();
                    //
                    // Drain all permits, because a single synchronization is
                    // sufficient.
                    //
                    intentsSynchronizerSemaphore.drainPermits();
                } catch (InterruptedException e) {
                    log.debug("Interrupted while waiting to become " +
                                      "Intent Synchronization leader");
                    interrupted = true;
                    break;
                }
                syncIntents();
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Submits a multi-point-to-single-point intent.
     *
     * @param prefix the IPv4 matching prefix for the intent to submit
     * @param intent the intent to submit
     */
    void submitRouteIntent(Ip4Prefix prefix,
                           MultiPointToSinglePointIntent intent) {
        synchronized (this) {
            if (isElectedLeader && isActivatedLeader) {
                log.debug("Intent installation: adding Intent for prefix: {}",
                          prefix);
                intentService.submit(intent);
            }

            // Maintain the Intent
            pushedRouteIntents.put(prefix, intent);
        }
    }

    /**
     * Withdraws a multi-point-to-single-point intent.
     *
     * @param prefix the IPv4 matching prefix for the intent to withdraw.
     */
    void withdrawRouteIntent(Ip4Prefix prefix) {
        synchronized (this) {
            MultiPointToSinglePointIntent intent =
                pushedRouteIntents.remove(prefix);

            if (intent == null) {
                log.debug("There is no intent in pushedRouteIntents to delete " +
                          "for prefix: {}", prefix);
                return;
            }

            if (isElectedLeader && isActivatedLeader) {
                log.debug("Intent installation: deleting Intent for prefix: {}",
                          prefix);
                intentService.withdraw(intent);
            }
        }
    }

    /**
     * Performs Intents Synchronization between the internally stored Route
     * Intents and the installed Route Intents.
     */
    private void syncIntents() {
        synchronized (this) {
            if (!isElectedLeader) {
                return;         // Nothing to do: not the leader anymore
            }
            log.debug("Syncing SDN-IP Route Intents...");

            Map<Ip4Prefix, MultiPointToSinglePointIntent> fetchedIntents =
                    new HashMap<>();

            //
            // Fetch all intents, and classify the Multi-Point-to-Point Intents
            // based on the matching prefix.
            //
            for (Intent intent : intentService.getIntents()) {

                if (!(intent instanceof MultiPointToSinglePointIntent)
                        || !intent.appId().equals(appId)) {
                    continue;
                }
                MultiPointToSinglePointIntent mp2pIntent =
                        (MultiPointToSinglePointIntent) intent;

                Criterion c =
                    mp2pIntent.selector().getCriterion(Criterion.Type.IPV4_DST);
                if (c != null && c instanceof IPCriterion) {
                    IPCriterion ipCriterion = (IPCriterion) c;
                    Ip4Prefix ip4Prefix = ipCriterion.ip().getIp4Prefix();
                    if (ip4Prefix == null) {
                        // TODO: For now we support only IPv4
                        continue;
                    }
                    fetchedIntents.put(ip4Prefix, mp2pIntent);
                } else {
                    log.warn("No IPV4_DST criterion found for intent {}",
                            mp2pIntent.id());
                }

            }

            //
            // Compare for each prefix the local IN-MEMORY Intents with the
            // FETCHED Intents:
            //  - If the IN-MEMORY Intent is same as the FETCHED Intent, store
            //    the FETCHED Intent in the local memory (i.e., override the
            //    IN-MEMORY Intent) to preserve the original Intent ID
            //  - if the IN-MEMORY Intent is not same as the FETCHED Intent,
            //    delete the FETCHED Intent, and push/install the IN-MEMORY
            //    Intent.
            //  - If there is an IN-MEMORY Intent for a prefix, but no FETCHED
            //    Intent for same prefix, then push/install the IN-MEMORY
            //    Intent.
            //  - If there is a FETCHED Intent for a prefix, but no IN-MEMORY
            //    Intent for same prefix, then delete/withdraw the FETCHED
            //    Intent.
            //
            Collection<Pair<Ip4Prefix, MultiPointToSinglePointIntent>>
                    storeInMemoryIntents = new LinkedList<>();
            Collection<Pair<Ip4Prefix, MultiPointToSinglePointIntent>>
                    addIntents = new LinkedList<>();
            Collection<Pair<Ip4Prefix, MultiPointToSinglePointIntent>>
                    deleteIntents = new LinkedList<>();
            for (Map.Entry<Ip4Prefix, MultiPointToSinglePointIntent> entry :
                    pushedRouteIntents.entrySet()) {
                Ip4Prefix prefix = entry.getKey();
                MultiPointToSinglePointIntent inMemoryIntent =
                        entry.getValue();
                MultiPointToSinglePointIntent fetchedIntent =
                        fetchedIntents.get(prefix);

                if (fetchedIntent == null) {
                    //
                    // No FETCHED Intent for same prefix: push the IN-MEMORY
                    // Intent.
                    //
                    addIntents.add(Pair.of(prefix, inMemoryIntent));
                    continue;
                }

                IntentState state = intentService.getIntentState(fetchedIntent.id());
                if (state == IntentState.WITHDRAWING ||
                        state == IntentState.WITHDRAWN) {
                    // The intent has been withdrawn but according to our route
                    // table it should be installed. We'll reinstall it.
                    addIntents.add(Pair.of(prefix, inMemoryIntent));
                }

                //
                // If IN-MEMORY Intent is same as the FETCHED Intent,
                // store the FETCHED Intent in the local memory.
                //
                if (compareMultiPointToSinglePointIntents(inMemoryIntent,
                                                          fetchedIntent)) {
                    storeInMemoryIntents.add(Pair.of(prefix, fetchedIntent));
                } else {
                    //
                    // The IN-MEMORY Intent is not same as the FETCHED Intent,
                    // hence delete the FETCHED Intent, and install the
                    // IN-MEMORY Intent.
                    //
                    deleteIntents.add(Pair.of(prefix, fetchedIntent));
                    addIntents.add(Pair.of(prefix, inMemoryIntent));
                }
                fetchedIntents.remove(prefix);
            }

            //
            // Any remaining FETCHED Intents have to be deleted/withdrawn
            //
            for (Map.Entry<Ip4Prefix, MultiPointToSinglePointIntent> entry :
                    fetchedIntents.entrySet()) {
                Ip4Prefix prefix = entry.getKey();
                MultiPointToSinglePointIntent fetchedIntent = entry.getValue();
                deleteIntents.add(Pair.of(prefix, fetchedIntent));
            }

            //
            // Perform the actions:
            // 1. Store in memory fetched intents that are same. Can be done
            //    even if we are not the leader anymore
            // 2. Delete intents: check if the leader before each operation
            // 3. Add intents: check if the leader before each operation
            //
            for (Pair<Ip4Prefix, MultiPointToSinglePointIntent> pair :
                    storeInMemoryIntents) {
                Ip4Prefix prefix = pair.getLeft();
                MultiPointToSinglePointIntent intent = pair.getRight();
                log.debug("Intent synchronization: updating in-memory " +
                                  "Intent for prefix: {}", prefix);
                pushedRouteIntents.put(prefix, intent);
            }
            //
            isActivatedLeader = true;           // Allow push of Intents
            for (Pair<Ip4Prefix, MultiPointToSinglePointIntent> pair :
                    deleteIntents) {
                Ip4Prefix prefix = pair.getLeft();
                MultiPointToSinglePointIntent intent = pair.getRight();
                if (!isElectedLeader) {
                    isActivatedLeader = false;
                    return;
                }
                log.debug("Intent synchronization: deleting Intent for " +
                                  "prefix: {}", prefix);
                intentService.withdraw(intent);
            }
            //
            for (Pair<Ip4Prefix, MultiPointToSinglePointIntent> pair :
                    addIntents) {
                Ip4Prefix prefix = pair.getLeft();
                MultiPointToSinglePointIntent intent = pair.getRight();
                if (!isElectedLeader) {
                    isActivatedLeader = false;
                    return;
                }
                log.debug("Intent synchronization: adding Intent for " +
                                  "prefix: {}", prefix);
                intentService.submit(intent);
            }
            if (!isElectedLeader) {
                isActivatedLeader = false;
            }
            log.debug("Syncing SDN-IP routes completed.");
        }
    }

    /**
     * Compares two Multi-point to Single Point Intents whether they represent
     * same logical intention.
     *
     * @param intent1 the first Intent to compare
     * @param intent2 the second Intent to compare
     * @return true if both Intents represent same logical intention, otherwise
     * false
     */
    private boolean compareMultiPointToSinglePointIntents(
            MultiPointToSinglePointIntent intent1,
            MultiPointToSinglePointIntent intent2) {

        return Objects.equal(intent1.appId(), intent2.appId()) &&
                Objects.equal(intent1.selector(), intent2.selector()) &&
                Objects.equal(intent1.treatment(), intent2.treatment()) &&
                Objects.equal(intent1.ingressPoints(), intent2.ingressPoints()) &&
                Objects.equal(intent1.egressPoint(), intent2.egressPoint());
    }
}
