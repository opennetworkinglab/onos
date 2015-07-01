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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.IntentRequestListener;
import org.onosproject.routing.config.BgpPeer;
import org.onosproject.routing.config.Interface;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Synchronizes intents between the in-memory intent store and the
 * IntentService.
 */
public class IntentSynchronizer implements FibListener, IntentRequestListener {
    private static final int PRIORITY_OFFSET = 100;
    private static final int PRIORITY_MULTIPLIER = 5;

    private static final Logger log =
        LoggerFactory.getLogger(IntentSynchronizer.class);

    private final ApplicationId appId;
    private final IntentService intentService;
    private final HostService hostService;
    private final Map<IntentKey, PointToPointIntent> peerIntents;
    private final Map<IpPrefix, MultiPointToSinglePointIntent> routeIntents;

    //
    // State to deal with SDN-IP Leader election and pushing Intents
    //
    private final ExecutorService bgpIntentsSynchronizerExecutor;
    private final Semaphore intentsSynchronizerSemaphore = new Semaphore(0);
    private volatile boolean isElectedLeader = false;
    private volatile boolean isActivatedLeader = false;

    private final RoutingConfigurationService configService;

    /**
     * Class constructor.
     *
     * @param appId the Application ID
     * @param intentService the intent service
     * @param hostService the host service
     * @param configService the SDN-IP configuration service
     */
    IntentSynchronizer(ApplicationId appId, IntentService intentService,
                       HostService hostService,
                       RoutingConfigurationService configService) {
        this.appId = appId;
        this.intentService = intentService;
        this.hostService = hostService;
        peerIntents = new ConcurrentHashMap<>();
        routeIntents = new ConcurrentHashMap<>();

        this.configService = configService;

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

        //
        // Tell the Intents Synchronizer thread to start the synchronization
        //
        intentsSynchronizerSemaphore.release();
    }

    /**
     * Gets the route intents.
     *
     * @return the route intents
     */
    public Collection<MultiPointToSinglePointIntent> getRouteIntents() {
        List<MultiPointToSinglePointIntent> result = new LinkedList<>();

        for (Map.Entry<IpPrefix, MultiPointToSinglePointIntent> entry :
            routeIntents.entrySet()) {
            result.add(entry.getValue());
        }
        return result;
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
                    interrupted = true;
                    break;
                }
                synchronizeIntents();
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Submits a collection of point-to-point intents.
     *
     * @param intents the intents to submit
     */
    void submitPeerIntents(Collection<PointToPointIntent> intents) {
        synchronized (this) {
            // Store the intents in memory
            for (PointToPointIntent intent : intents) {
                peerIntents.put(new IntentKey(intent), intent);
            }

            // Push the intents
            if (isElectedLeader && isActivatedLeader) {
                log.debug("SDN-IP Submitting all Peer Intents...");
                for (Intent intent : intents) {
                    log.trace("SDN-IP Submitting intents: {}", intent);
                    intentService.submit(intent);
                }
            }
        }
    }

    /**
     * Submits a MultiPointToSinglePointIntent for reactive routing.
     *
     * @param ipPrefix the IP prefix to match in a MultiPointToSinglePointIntent
     * @param intent the intent to submit
     */
    void submitReactiveIntent(IpPrefix ipPrefix, MultiPointToSinglePointIntent intent) {
        synchronized (this) {
            // Store the intent in memory
            routeIntents.put(ipPrefix, intent);

            // Push the intent
            if (isElectedLeader && isActivatedLeader) {
                log.trace("SDN-IP submitting reactive routing intent: {}", intent);
                intentService.submit(intent);
            }
        }
    }

    /**
     * Generates a route intent for a prefix, the next hop IP address, and
     * the next hop MAC address.
     * <p/>
     * This method will find the egress interface for the intent.
     * Intent will match dst IP prefix and rewrite dst MAC address at all other
     * border switches, then forward packets according to dst MAC address.
     *
     * @param prefix            IP prefix of the route to add
     * @param nextHopIpAddress  IP address of the next hop
     * @param nextHopMacAddress MAC address of the next hop
     * @return the generated intent, or null if no intent should be submitted
     */
    private MultiPointToSinglePointIntent generateRouteIntent(
            IpPrefix prefix,
            IpAddress nextHopIpAddress,
            MacAddress nextHopMacAddress) {

        // Find the attachment point (egress interface) of the next hop
        Interface egressInterface;
        if (configService.getBgpPeers().containsKey(nextHopIpAddress)) {
            // Route to a peer
            log.debug("Route to peer {}", nextHopIpAddress);
            BgpPeer peer =
                    configService.getBgpPeers().get(nextHopIpAddress);
            egressInterface =
                    configService.getInterface(peer.connectPoint());
        } else {
            // Route to non-peer
            log.debug("Route to non-peer {}", nextHopIpAddress);
            egressInterface =
                    configService.getMatchingInterface(nextHopIpAddress);
            if (egressInterface == null) {
                log.warn("No outgoing interface found for {}",
                         nextHopIpAddress);
                return null;
            }
        }

        //
        // Generate the intent itself
        //
        Set<ConnectPoint> ingressPorts = new HashSet<>();
        ConnectPoint egressPort = egressInterface.connectPoint();
        log.debug("Generating intent for prefix {}, next hop mac {}",
                  prefix, nextHopMacAddress);

        for (Interface intf : configService.getInterfaces()) {
            if (!intf.connectPoint().equals(egressInterface.connectPoint())) {
                ConnectPoint srcPort = intf.connectPoint();
                ingressPorts.add(srcPort);
            }
        }

        // Match the destination IP prefix at the first hop
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if (prefix.isIp4()) {
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchIPDst(prefix);
        } else {
            selector.matchEthType(Ethernet.TYPE_IPV6);
            selector.matchIPv6Dst(prefix);
        }

        // Rewrite the destination MAC address
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setEthDst(nextHopMacAddress);
        if (!egressInterface.vlan().equals(VlanId.NONE)) {
            treatment.setVlanId(egressInterface.vlan());
            // If we set VLAN ID, we have to make sure a VLAN tag exists.
            // TODO support no VLAN -> VLAN routing
            selector.matchVlanId(VlanId.ANY);
        }

        int priority =
            prefix.prefixLength() * PRIORITY_MULTIPLIER + PRIORITY_OFFSET;
        Key key = Key.of(prefix.toString(), appId);
        return MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector.build())
                .treatment(treatment.build())
                .ingressPoints(ingressPorts)
                .egressPoint(egressPort)
                .priority(priority)
                .build();
    }

    @Override
    public void setUpConnectivityInternetToHost(IpAddress hostIpAddress) {
        checkNotNull(hostIpAddress);
        Set<ConnectPoint> ingressPoints =
                configService.getBgpPeerConnectPoints();

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        if (hostIpAddress.isIp4()) {
            selector.matchEthType(Ethernet.TYPE_IPV4);
        } else {
            selector.matchEthType(Ethernet.TYPE_IPV6);
        }

        // Match the destination IP prefix at the first hop
        IpPrefix ipPrefix = hostIpAddress.toIpPrefix();
        selector.matchIPDst(ipPrefix);

        // Rewrite the destination MAC address
        MacAddress hostMac = null;
        ConnectPoint egressPoint = null;
        for (Host host : hostService.getHostsByIp(hostIpAddress)) {
            if (host.mac() != null) {
                hostMac = host.mac();
                egressPoint = host.location();
                break;
            }
        }
        if (hostMac == null) {
            hostService.startMonitoringIp(hostIpAddress);
            return;
        }

        TrafficTreatment.Builder treatment =
                DefaultTrafficTreatment.builder().setEthDst(hostMac);
        Key key = Key.of(ipPrefix.toString(), appId);
        int priority = ipPrefix.prefixLength() * PRIORITY_MULTIPLIER
                + PRIORITY_OFFSET;
        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector.build())
                .treatment(treatment.build())
                .ingressPoints(ingressPoints)
                .egressPoint(egressPoint)
                .priority(priority)
                .build();

        log.trace("Generates ConnectivityInternetToHost intent {}", intent);
        submitReactiveIntent(ipPrefix, intent);
    }


    @Override
    public void update(Collection<FibUpdate> updates, Collection<FibUpdate> withdraws) {
        //
        // NOTE: Semantically, we MUST withdraw existing intents before
        // submitting new intents.
        //
        synchronized (this) {
            MultiPointToSinglePointIntent intent;

            log.debug("SDN-IP submitting intents = {} withdrawing = {}",
                     updates.size(), withdraws.size());

            //
            // Prepare the Intent batch operations for the intents to withdraw
            //
            for (FibUpdate withdraw : withdraws) {
                checkArgument(withdraw.type() == FibUpdate.Type.DELETE,
                              "FibUpdate with wrong type in withdraws list");

                IpPrefix prefix = withdraw.entry().prefix();
                intent = routeIntents.remove(prefix);
                if (intent == null) {
                    log.trace("SDN-IP No intent in routeIntents to delete " +
                              "for prefix: {}", prefix);
                    continue;
                }
                if (isElectedLeader && isActivatedLeader) {
                    log.trace("SDN-IP Withdrawing intent: {}", intent);
                    intentService.withdraw(intent);
                }
            }

            //
            // Prepare the Intent batch operations for the intents to submit
            //
            for (FibUpdate update : updates) {
                checkArgument(update.type() == FibUpdate.Type.UPDATE,
                              "FibUpdate with wrong type in updates list");

                IpPrefix prefix = update.entry().prefix();
                intent = generateRouteIntent(prefix, update.entry().nextHopIp(),
                                             update.entry().nextHopMac());

                if (intent == null) {
                    // This preserves the old semantics - if an intent can't be
                    // generated, we don't do anything with that prefix. But
                    // perhaps we should withdraw the old intent anyway?
                    continue;
                }

                MultiPointToSinglePointIntent oldIntent =
                    routeIntents.put(prefix, intent);
                if (isElectedLeader && isActivatedLeader) {
                    if (oldIntent != null) {
                        log.trace("SDN-IP Withdrawing old intent: {}",
                                  oldIntent);
                        intentService.withdraw(oldIntent);
                    }
                    log.trace("SDN-IP Submitting intent: {}", intent);
                    intentService.submit(intent);
                }
            }
        }
    }

    /**
     * Synchronize the in-memory Intents with the Intents in the Intent
     * framework.
     */
    void synchronizeIntents() {
        synchronized (this) {

            Map<IntentKey, Intent> localIntents = new HashMap<>();
            Map<IntentKey, Intent> fetchedIntents = new HashMap<>();
            Collection<Intent> storeInMemoryIntents = new LinkedList<>();
            Collection<Intent> addIntents = new LinkedList<>();
            Collection<Intent> deleteIntents = new LinkedList<>();

            if (!isElectedLeader) {
                return;         // Nothing to do: not the leader anymore
            }
            log.debug("SDN-IP synchronizing all intents...");

            // Prepare the local intents
            for (Intent intent : routeIntents.values()) {
                localIntents.put(new IntentKey(intent), intent);
            }
            for (Intent intent : peerIntents.values()) {
                localIntents.put(new IntentKey(intent), intent);
            }

            // Fetch all intents for this application
            for (Intent intent : intentService.getIntents()) {
                if (!intent.appId().equals(appId)) {
                    continue;
                }
                fetchedIntents.put(new IntentKey(intent), intent);
            }
            if (log.isDebugEnabled()) {
                for (Intent intent: fetchedIntents.values()) {
                    log.trace("SDN-IP Intent Synchronizer: fetched intent: {}",
                              intent);
                }
            }

            computeIntentsDelta(localIntents, fetchedIntents,
                                storeInMemoryIntents, addIntents,
                                deleteIntents);

            //
            // Perform the actions:
            // 1. Store in memory fetched intents that are same. Can be done
            //    even if we are not the leader anymore
            // 2. Delete intents: check if the leader before the operation
            // 3. Add intents: check if the leader before the operation
            //
            for (Intent intent : storeInMemoryIntents) {
                // Store the intent in memory based on its type
                if (intent instanceof MultiPointToSinglePointIntent) {
                    MultiPointToSinglePointIntent mp2pIntent =
                        (MultiPointToSinglePointIntent) intent;
                    // Find the IP prefix
                    Criterion c =
                        mp2pIntent.selector().getCriterion(Criterion.Type.IPV4_DST);
                    if (c == null) {
                        // Try IPv6
                        c =
                            mp2pIntent.selector().getCriterion(Criterion.Type.IPV6_DST);
                    }
                    if (c != null && c instanceof IPCriterion) {
                        IPCriterion ipCriterion = (IPCriterion) c;
                        IpPrefix ipPrefix = ipCriterion.ip();
                        if (ipPrefix == null) {
                            continue;
                        }
                        log.trace("SDN-IP Intent Synchronizer: updating " +
                                  "in-memory Route Intent for prefix {}",
                                  ipPrefix);
                        routeIntents.put(ipPrefix, mp2pIntent);
                    } else {
                        log.warn("SDN-IP no IPV4_DST or IPV6_DST criterion found for Intent {}",
                                 mp2pIntent.id());
                    }
                    continue;
                }
                if (intent instanceof PointToPointIntent) {
                    PointToPointIntent p2pIntent = (PointToPointIntent) intent;
                    log.trace("SDN-IP Intent Synchronizer: updating " +
                              "in-memory Peer Intent {}", p2pIntent);
                    peerIntents.put(new IntentKey(intent), p2pIntent);
                    continue;
                }
            }

            // Withdraw Intents
            for (Intent intent : deleteIntents) {
                intentService.withdraw(intent);
                log.trace("SDN-IP Intent Synchronizer: withdrawing intent: {}",
                      intent);
            }
            if (!isElectedLeader) {
                log.trace("SDN-IP Intent Synchronizer: cannot withdraw intents: " +
                          "not elected leader anymore");
                isActivatedLeader = false;
                return;
            }

            // Add Intents
            for (Intent intent : addIntents) {
                intentService.submit(intent);
                log.trace("SDN-IP Intent Synchronizer: submitting intent: {}",
                          intent);
            }
            if (!isElectedLeader) {
                log.trace("SDN-IP Intent Synchronizer: cannot submit intents: " +
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

    /**
     * Computes the delta in two sets of Intents: local in-memory Intents,
     * and intents fetched from the Intent framework.
     *
     * @param localIntents the local in-memory Intents
     * @param fetchedIntents the Intents fetched from the Intent framework
     * @param storeInMemoryIntents the Intents that should be stored in memory.
     * Note: This Collection must be allocated by the caller, and it will
     * be populated by this method.
     * @param addIntents the Intents that should be added to the Intent
     * framework. Note: This Collection must be allocated by the caller, and
     * it will be populated by this method.
     * @param deleteIntents the Intents that should be deleted from the Intent
     * framework. Note: This Collection must be allocated by the caller, and
     * it will be populated by this method.
     */
    private void computeIntentsDelta(
                                final Map<IntentKey, Intent> localIntents,
                                final Map<IntentKey, Intent> fetchedIntents,
                                Collection<Intent> storeInMemoryIntents,
                                Collection<Intent> addIntents,
                                Collection<Intent> deleteIntents) {

        //
        // Compute the deltas between the LOCAL in-memory Intents and the
        // FETCHED Intents:
        //  - If an Intent is in both the LOCAL and FETCHED sets:
        //    If the FETCHED Intent is WITHDRAWING or WITHDRAWN, then
        //    the LOCAL Intent should be added/installed; otherwise the
        //    FETCHED intent should be stored in the local memory
        //    (i.e., override the LOCAL Intent) to preserve the original
        //    Intent ID.
        //  - if a LOCAL Intent is not in the FETCHED set, then the LOCAL
        //    Intent should be added/installed.
        //  - If a FETCHED Intent is not in the LOCAL set, then the FETCHED
        //    Intent should be deleted/withdrawn.
        //
        for (Map.Entry<IntentKey, Intent> entry : localIntents.entrySet()) {
            IntentKey intentKey = entry.getKey();
            Intent localIntent = entry.getValue();
            Intent fetchedIntent = fetchedIntents.get(intentKey);

            if (fetchedIntent == null) {
                //
                // No FETCHED Intent found: push the LOCAL Intent.
                //
                addIntents.add(localIntent);
                continue;
            }

            IntentState state =
                intentService.getIntentState(fetchedIntent.key());
            if (state == null ||
                state == IntentState.WITHDRAWING ||
                state == IntentState.WITHDRAWN) {
                // The intent has been withdrawn but according to our route
                // table it should be installed. We'll reinstall it.
                addIntents.add(localIntent);
                continue;
            }
            storeInMemoryIntents.add(fetchedIntent);
        }

        for (Map.Entry<IntentKey, Intent> entry : fetchedIntents.entrySet()) {
            IntentKey intentKey = entry.getKey();
            Intent fetchedIntent = entry.getValue();
            Intent localIntent = localIntents.get(intentKey);

            if (localIntent != null) {
                continue;
            }

            IntentState state =
                intentService.getIntentState(fetchedIntent.key());
            if (state == null ||
                state == IntentState.WITHDRAWING ||
                state == IntentState.WITHDRAWN) {
                // Nothing to do. The intent has been already withdrawn.
                continue;
            }
            //
            // No LOCAL Intent found: delete/withdraw the FETCHED Intent.
            //
            deleteIntents.add(fetchedIntent);
        }
    }

    /**
     * Helper class that can be used to compute the key for an Intent by
     * by excluding the Intent ID.
     */
    static final class IntentKey {
        private final Intent intent;

        /**
         * Constructor.
         *
         * @param intent the intent to use
         */
        IntentKey(Intent intent) {
            checkArgument((intent instanceof MultiPointToSinglePointIntent) ||
                          (intent instanceof PointToPointIntent),
                          "Intent type not recognized", intent);
            this.intent = intent;
        }

        /**
         * Compares two Multi-Point to Single-Point Intents whether they
         * represent same logical intention.
         *
         * @param intent1 the first Intent to compare
         * @param intent2 the second Intent to compare
         * @return true if both Intents represent same logical intention,
         * otherwise false
         */
        static boolean equalIntents(MultiPointToSinglePointIntent intent1,
                                    MultiPointToSinglePointIntent intent2) {
            return Objects.equals(intent1.appId(), intent2.appId()) &&
                Objects.equals(intent1.selector(), intent2.selector()) &&
                Objects.equals(intent1.treatment(), intent2.treatment()) &&
                Objects.equals(intent1.ingressPoints(), intent2.ingressPoints()) &&
                Objects.equals(intent1.egressPoint(), intent2.egressPoint());
        }

        /**
         * Compares two Point-to-Point Intents whether they represent
         * same logical intention.
         *
         * @param intent1 the first Intent to compare
         * @param intent2 the second Intent to compare
         * @return true if both Intents represent same logical intention,
         * otherwise false
         */
        static boolean equalIntents(PointToPointIntent intent1,
                                    PointToPointIntent intent2) {
            return Objects.equals(intent1.appId(), intent2.appId()) &&
                Objects.equals(intent1.selector(), intent2.selector()) &&
                Objects.equals(intent1.treatment(), intent2.treatment()) &&
                Objects.equals(intent1.ingressPoint(), intent2.ingressPoint()) &&
                Objects.equals(intent1.egressPoint(), intent2.egressPoint());
        }

        @Override
        public int hashCode() {
            if (intent instanceof PointToPointIntent) {
                PointToPointIntent p2pIntent = (PointToPointIntent) intent;
                return Objects.hash(p2pIntent.appId(),
                                    p2pIntent.resources(),
                                    p2pIntent.selector(),
                                    p2pIntent.treatment(),
                                    p2pIntent.constraints(),
                                    p2pIntent.ingressPoint(),
                                    p2pIntent.egressPoint());
            }
            if (intent instanceof MultiPointToSinglePointIntent) {
                MultiPointToSinglePointIntent m2pIntent =
                    (MultiPointToSinglePointIntent) intent;
                return Objects.hash(m2pIntent.appId(),
                                    m2pIntent.resources(),
                                    m2pIntent.selector(),
                                    m2pIntent.treatment(),
                                    m2pIntent.constraints(),
                                    m2pIntent.ingressPoints(),
                                    m2pIntent.egressPoint());
            }
            checkArgument(false, "Intent type not recognized", intent);
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ((obj == null) || (!(obj instanceof IntentKey))) {
                return false;
            }
            IntentKey other = (IntentKey) obj;

            if (this.intent instanceof PointToPointIntent) {
                if (!(other.intent instanceof PointToPointIntent)) {
                    return false;
                }
                return equalIntents((PointToPointIntent) this.intent,
                                    (PointToPointIntent) other.intent);
            }
            if (this.intent instanceof MultiPointToSinglePointIntent) {
                if (!(other.intent instanceof MultiPointToSinglePointIntent)) {
                    return false;
                }
                return equalIntents(
                                (MultiPointToSinglePointIntent) this.intent,
                                (MultiPointToSinglePointIntent) other.intent);
            }
            checkArgument(false, "Intent type not recognized", intent);
            return false;
        }
    }

    @Override
    public void setUpConnectivityHostToHost(IpAddress dstIpAddress,
                                            IpAddress srcIpAddress,
                                            MacAddress srcMacAddress,
                                            ConnectPoint srcConnectPoint) {
        checkNotNull(dstIpAddress);
        checkNotNull(srcIpAddress);
        checkNotNull(srcMacAddress);
        checkNotNull(srcConnectPoint);

        IpPrefix srcIpPrefix = srcIpAddress.toIpPrefix();
        IpPrefix dstIpPrefix = dstIpAddress.toIpPrefix();
        ConnectPoint dstConnectPoint = null;
        MacAddress dstMacAddress = null;

        for (Host host : hostService.getHostsByIp(dstIpAddress)) {
            if (host.mac() != null) {
                dstMacAddress = host.mac();
                dstConnectPoint = host.location();
                break;
            }
        }
        if (dstMacAddress == null) {
            hostService.startMonitoringIp(dstIpAddress);
            return;
        }

        //
        // Handle intent from source host to destination host
        //
        MultiPointToSinglePointIntent srcToDstIntent =
                hostToHostIntentGenerator(dstIpAddress, dstConnectPoint,
                                    dstMacAddress, srcConnectPoint);
        submitReactiveIntent(dstIpPrefix, srcToDstIntent);

        //
        // Handle intent from destination host to source host
        //

        // Since we proactively handle the intent from destination host to
        // source host, we should check whether there is an exiting intent
        // first.
        if (mp2pIntentExists(srcIpPrefix)) {
            updateExistingMp2pIntent(srcIpPrefix, dstConnectPoint);
            return;
        } else {
            // There is no existing intent, create a new one.
            MultiPointToSinglePointIntent dstToSrcIntent =
                    hostToHostIntentGenerator(srcIpAddress, srcConnectPoint,
                                        srcMacAddress, dstConnectPoint);
            submitReactiveIntent(srcIpPrefix, dstToSrcIntent);
        }
    }

    /**
     * Generates MultiPointToSinglePointIntent for both source host and
     * destination host located in local SDN network.
     *
     * @param dstIpAddress the destination IP address
     * @param dstConnectPoint the destination host connect point
     * @param dstMacAddress the MAC address of destination host
     * @param srcConnectPoint the connect point where packet-in from
     * @return the generated MultiPointToSinglePointIntent
     */
    private MultiPointToSinglePointIntent hostToHostIntentGenerator(
                                       IpAddress dstIpAddress,
                                       ConnectPoint dstConnectPoint,
                                       MacAddress dstMacAddress,
                                       ConnectPoint srcConnectPoint) {
        checkNotNull(dstIpAddress);
        checkNotNull(dstConnectPoint);
        checkNotNull(dstMacAddress);
        checkNotNull(srcConnectPoint);

        Set<ConnectPoint> ingressPoints = new HashSet<ConnectPoint>();
        ingressPoints.add(srcConnectPoint);
        IpPrefix dstIpPrefix = dstIpAddress.toIpPrefix();

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if (dstIpAddress.isIp4()) {
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchIPDst(dstIpPrefix);
        } else {
            selector.matchEthType(Ethernet.TYPE_IPV6);
            selector.matchIPv6Dst(dstIpPrefix);
        }

        // Rewrite the destination MAC address
        TrafficTreatment.Builder treatment =
                DefaultTrafficTreatment.builder().setEthDst(dstMacAddress);

        Key key = Key.of(dstIpPrefix.toString(), appId);
        int priority = dstIpPrefix.prefixLength() * PRIORITY_MULTIPLIER
                + PRIORITY_OFFSET;
        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector.build())
                .treatment(treatment.build())
                .ingressPoints(ingressPoints)
                .egressPoint(dstConnectPoint)
                .priority(priority)
                .build();

        log.trace("Generates ConnectivityHostToHost = {} ", intent);
        return intent;
    }

    @Override
    public void updateExistingMp2pIntent(IpPrefix ipPrefix,
                                         ConnectPoint ingressConnectPoint) {
        checkNotNull(ipPrefix);
        checkNotNull(ingressConnectPoint);

        MultiPointToSinglePointIntent existingIntent =
                getExistingMp2pIntent(ipPrefix);
        if (existingIntent != null) {
            Set<ConnectPoint> ingressPoints = existingIntent.ingressPoints();
            // Add host connect point into ingressPoints of the existing intent
            if (ingressPoints.add(ingressConnectPoint)) {
                MultiPointToSinglePointIntent updatedMp2pIntent =
                        MultiPointToSinglePointIntent.builder()
                        .appId(appId)
                        .key(existingIntent.key())
                        .selector(existingIntent.selector())
                        .treatment(existingIntent.treatment())
                        .ingressPoints(ingressPoints)
                        .egressPoint(existingIntent.egressPoint())
                        .priority(existingIntent.priority())
                        .build();

                log.trace("Update an existing MultiPointToSinglePointIntent "
                        + "to new intent = {} ", updatedMp2pIntent);
                submitReactiveIntent(ipPrefix, updatedMp2pIntent);
            }
            // If adding ingressConnectPoint to ingressPoints failed, it
            // because between the time interval from checking existing intent
            // to generating new intent, onos updated this intent due to other
            // packet-in and the new intent also includes the
            // ingressConnectPoint. This will not affect reactive routing.
        }
    }

    @Override
    public boolean mp2pIntentExists(IpPrefix ipPrefix) {
        checkNotNull(ipPrefix);
        return routeIntents.get(ipPrefix) != null;
    }

    /**
     * Gets the existing MultiPointToSinglePointIntent from memory for a given
     * IP prefix.
     *
     * @param ipPrefix the IP prefix used to find MultiPointToSinglePointIntent
     * @return the MultiPointToSinglePointIntent if found, otherwise null
     */
    private MultiPointToSinglePointIntent getExistingMp2pIntent(IpPrefix
                                                                ipPrefix) {
        checkNotNull(ipPrefix);
        return routeIntents.get(ipPrefix);
    }
}
