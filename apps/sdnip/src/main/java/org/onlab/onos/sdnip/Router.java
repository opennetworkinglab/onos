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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.criteria.Criteria.IPCriterion;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.criteria.Criterion.Type;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.onos.sdnip.config.BgpPeer;
import org.onlab.onos.sdnip.config.Interface;
import org.onlab.onos.sdnip.config.SdnIpConfigService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

/**
 * This class processes BGP route update, translates each update into a intent
 * and submits the intent.
 */
public class Router implements RouteListener {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    // Store all route updates in a radix tree.
    // The key in this tree is the binary string of prefix of the route.
    private InvertedRadixTree<RouteEntry> bgpRoutes;

    // Stores all incoming route updates in a queue.
    private BlockingQueue<RouteUpdate> routeUpdates;

    // The Ip4Address is the next hop address of each route update.
    private SetMultimap<Ip4Address, RouteEntry> routesWaitingOnArp;
    private ConcurrentHashMap<Ip4Prefix, MultiPointToSinglePointIntent> pushedRouteIntents;

    private IntentService intentService;
    private HostService hostService;
    private SdnIpConfigService configService;
    private InterfaceService interfaceService;

    private ExecutorService bgpUpdatesExecutor;
    private ExecutorService bgpIntentsSynchronizerExecutor;

    private final ApplicationId appId;

    //
    // State to deal with SDN-IP Leader election and pushing Intents
    //
    private Semaphore intentsSynchronizerSemaphore = new Semaphore(0);
    private volatile boolean isElectedLeader = false;
    private volatile boolean isActivatedLeader = false;

    // For routes announced by local BGP daemon in SDN network,
    // the next hop will be 0.0.0.0.
    public static final Ip4Address LOCAL_NEXT_HOP =
        Ip4Address.valueOf("0.0.0.0");

    /**
     * Class constructor.
     *
     * @param appId             the application ID
     * @param intentService     the intent service
     * @param hostService       the host service
     * @param configService     the configuration service
     * @param interfaceService  the interface service
     */
    public Router(ApplicationId appId, IntentService intentService,
                  HostService hostService, SdnIpConfigService configService,
                  InterfaceService interfaceService) {
        this.appId = appId;
        this.intentService = intentService;
        this.hostService = hostService;
        this.configService = configService;
        this.interfaceService = interfaceService;

        bgpRoutes = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
        routeUpdates = new LinkedBlockingQueue<>();
        routesWaitingOnArp = Multimaps.synchronizedSetMultimap(
                HashMultimap.<Ip4Address, RouteEntry>create());
        pushedRouteIntents = new ConcurrentHashMap<>();

        bgpUpdatesExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("bgp-updates-%d").build());
        bgpIntentsSynchronizerExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("bgp-intents-synchronizer-%d").build());

        this.hostService.addListener(new InternalHostListener());
    }

    /**
     * Starts the router.
     */
    public void start() {
        bgpUpdatesExecutor.execute(new Runnable() {
            @Override
            public void run() {
                doUpdatesThread();
            }
        });

        bgpIntentsSynchronizerExecutor.execute(new Runnable() {
            @Override
            public void run() {
                doIntentSynchronizationThread();
            }
        });
    }

    /**
     * Shuts the router down.
     */
    public void shutdown() {
        bgpUpdatesExecutor.shutdownNow();
        bgpIntentsSynchronizerExecutor.shutdownNow();
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

    @Override
    public void update(RouteUpdate routeUpdate) {
        log.debug("Received new route update: {}", routeUpdate);

        try {
            routeUpdates.put(routeUpdate);
        } catch (InterruptedException e) {
            log.debug("Interrupted while putting on routeUpdates queue", e);
            Thread.currentThread().interrupt();
        }
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
     * Thread for handling route updates.
     */
    private void doUpdatesThread() {
        boolean interrupted = false;
        try {
            while (!interrupted) {
                try {
                    RouteUpdate update = routeUpdates.take();
                    switch (update.type()) {
                        case UPDATE:
                            processRouteAdd(update.routeEntry());
                            break;
                        case DELETE:
                            processRouteDelete(update.routeEntry());
                            break;
                        default:
                            log.error("Unknown update Type: {}", update.type());
                            break;
                    }
                } catch (InterruptedException e) {
                    log.debug("Interrupted while taking from updates queue", e);
                    interrupted = true;
                } catch (Exception e) {
                    log.debug("exception", e);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
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

                Criterion c = mp2pIntent.selector().getCriterion(Type.IPV4_DST);
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

    /**
     * Processes adding a route entry.
     * <p>
     * Put new route entry into the radix tree. If there was an existing
     * next hop for this prefix, but the next hop was different, then execute
     * deleting old route entry. If the next hop is the SDN domain, we do not
     * handle it at the moment. Otherwise, execute adding a route.
     * </p>
     *
     * @param routeEntry the route entry to add
     */
    protected void processRouteAdd(RouteEntry routeEntry) {
        synchronized (this) {
            log.debug("Processing route add: {}", routeEntry);

            Ip4Prefix prefix = routeEntry.prefix();
            Ip4Address nextHop = null;
            RouteEntry foundRouteEntry =
                    bgpRoutes.put(RouteEntry.createBinaryString(prefix),
                                  routeEntry);
            if (foundRouteEntry != null) {
                nextHop = foundRouteEntry.nextHop();
            }

            if (nextHop != null && !nextHop.equals(routeEntry.nextHop())) {
                // There was an existing nexthop for this prefix. This update
                // supersedes that, so we need to remove the old flows for this
                // prefix from the switches
                executeRouteDelete(routeEntry);
            }
            if (nextHop != null && nextHop.equals(routeEntry.nextHop())) {
                return;
            }

            if (routeEntry.nextHop().equals(LOCAL_NEXT_HOP)) {
                // Route originated by SDN domain
                // We don't handle these at the moment
                log.debug("Own route {} to {}",
                          routeEntry.prefix(), routeEntry.nextHop());
                return;
            }

            executeRouteAdd(routeEntry);
        }
    }

    /**
     * Executes adding a route entry.
     * <p>
     * Find out the egress Interface and MAC address of next hop router for
     * this route entry. If the MAC address can not be found in ARP cache,
     * then this prefix will be put in routesWaitingOnArp queue. Otherwise,
     * new route intent will be created and installed.
     * </p>
     *
     * @param routeEntry the route entry to add
     */
    private void executeRouteAdd(RouteEntry routeEntry) {
        log.debug("Executing route add: {}", routeEntry);

        // Monitor the IP address so we'll get notified of updates to the MAC
        // address.
        hostService.startMonitoringIp(routeEntry.nextHop());

        // See if we know the MAC address of the next hop
        MacAddress nextHopMacAddress = null;
        Set<Host> hosts = hostService.getHostsByIp(routeEntry.nextHop());
        if (!hosts.isEmpty()) {
            // TODO how to handle if multiple hosts are returned?
            nextHopMacAddress = hosts.iterator().next().mac();
        }

        if (nextHopMacAddress == null) {
            routesWaitingOnArp.put(routeEntry.nextHop(), routeEntry);
            return;
        }

        addRouteIntentToNextHop(routeEntry.prefix(),
                                routeEntry.nextHop(),
                                nextHopMacAddress);
    }

    /**
     * Adds a route intent given a prefix and a next hop IP address. This
     * method will find the egress interface for the intent.
     *
     * @param prefix            IP prefix of the route to add
     * @param nextHopIpAddress  IP address of the next hop
     * @param nextHopMacAddress MAC address of the next hop
     */
    private void addRouteIntentToNextHop(Ip4Prefix prefix,
                                         Ip4Address nextHopIpAddress,
                                         MacAddress nextHopMacAddress) {

        // Find the attachment point (egress interface) of the next hop
        Interface egressInterface;
        if (configService.getBgpPeers().containsKey(nextHopIpAddress)) {
            // Route to a peer
            log.debug("Route to peer {}", nextHopIpAddress);
            BgpPeer peer =
                    configService.getBgpPeers().get(nextHopIpAddress);
            egressInterface =
                    interfaceService.getInterface(peer.connectPoint());
        } else {
            // Route to non-peer
            log.debug("Route to non-peer {}", nextHopIpAddress);
            egressInterface =
                    interfaceService.getMatchingInterface(nextHopIpAddress);
            if (egressInterface == null) {
                log.warn("No outgoing interface found for {}",
                         nextHopIpAddress);
                return;
            }
        }

        doAddRouteIntent(prefix, egressInterface, nextHopMacAddress);
    }

    /**
     * Installs a route intent for a prefix.
     * <p/>
     * Intent will match dst IP prefix and rewrite dst MAC address at all other
     * border switches, then forward packets according to dst MAC address.
     *
     * @param prefix            IP prefix from route
     * @param egressInterface   egress Interface connected to next hop router
     * @param nextHopMacAddress MAC address of next hop router
     */
    private void doAddRouteIntent(Ip4Prefix prefix, Interface egressInterface,
                                  MacAddress nextHopMacAddress) {
        log.debug("Adding intent for prefix {}, next hop mac {}",
                  prefix, nextHopMacAddress);

        MultiPointToSinglePointIntent pushedIntent =
                pushedRouteIntents.get(prefix);

        // Just for testing.
        if (pushedIntent != null) {
            log.error("There should not be a pushed intent: {}", pushedIntent);
        }

        ConnectPoint egressPort = egressInterface.connectPoint();

        Set<ConnectPoint> ingressPorts = new HashSet<>();

        for (Interface intf : interfaceService.getInterfaces()) {
            if (!intf.connectPoint().equals(egressInterface.connectPoint())) {
                ConnectPoint srcPort = intf.connectPoint();
                ingressPorts.add(srcPort);
            }
        }

        // Match the destination IP prefix at the first hop
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(prefix)
                .build();

        // Rewrite the destination MAC address
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(nextHopMacAddress)
                .build();

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(appId, selector, treatment,
                                                  ingressPorts, egressPort);

        if (isElectedLeader && isActivatedLeader) {
            log.debug("Intent installation: adding Intent for prefix: {}",
                      prefix);
            intentService.submit(intent);
        }

        // Maintain the Intent
        pushedRouteIntents.put(prefix, intent);
    }

    /**
     * Executes deleting a route entry.
     * <p>
     * Removes prefix from radix tree, and if successful, then try to delete
     * the related intent.
     * </p>
     *
     * @param routeEntry the route entry to delete
     */
    protected void processRouteDelete(RouteEntry routeEntry) {
        synchronized (this) {
            log.debug("Processing route delete: {}", routeEntry);
            Ip4Prefix prefix = routeEntry.prefix();

            if (bgpRoutes.remove(RouteEntry.createBinaryString(prefix))) {
                //
                // Only delete flows if an entry was actually removed from the
                // tree. If no entry was removed, the <prefix, nexthop> wasn't
                // there so it's probably already been removed and we don't
                // need to do anything.
                //
                executeRouteDelete(routeEntry);
            }

            routesWaitingOnArp.remove(routeEntry.nextHop(), routeEntry);
            // TODO cancel the request in the ARP manager as well
        }
    }

    /**
     * Executed deleting a route entry.
     *
     * @param routeEntry the route entry to delete
     */
    private void executeRouteDelete(RouteEntry routeEntry) {
        log.debug("Executing route delete: {}", routeEntry);

        Ip4Prefix prefix = routeEntry.prefix();

        MultiPointToSinglePointIntent intent =
                pushedRouteIntents.remove(prefix);

        if (intent == null) {
            log.debug("There is no intent in pushedRouteIntents to delete " +
                              "for prefix: {}", prefix);
        } else {
            if (isElectedLeader && isActivatedLeader) {
                log.debug("Intent installation: deleting Intent for prefix: {}",
                          prefix);
                intentService.withdraw(intent);
            }
        }
    }

    /**
     * Signals the Router that the MAC to IP mapping has potentially been
     * updated. This has the effect of updating the MAC address for any
     * installed prefixes if it has changed, as well as installing any pending
     * prefixes that were waiting for MAC resolution.
     *
     * @param ipAddress the IP address that an event was received for
     * @param macAddress the most recently known MAC address for the IP address
     */
    private void updateMac(Ip4Address ipAddress, MacAddress macAddress) {
        log.debug("Received updated MAC info: {} => {}", ipAddress, macAddress);

        // TODO here we should check whether the next hop for any of our
        // installed prefixes has changed, not just prefixes pending
        // installation.

        // We synchronize on this to prevent changes to the radix tree
        // while we're pushing intents. If the tree changes, the
        // tree and intents could get out of sync.
        synchronized (this) {

            Set<RouteEntry> routesToPush =
                    routesWaitingOnArp.removeAll(ipAddress);

            for (RouteEntry routeEntry : routesToPush) {
                // These will always be adds
                Ip4Prefix prefix = routeEntry.prefix();
                String binaryString = RouteEntry.createBinaryString(prefix);
                RouteEntry foundRouteEntry =
                        bgpRoutes.getValueForExactKey(binaryString);
                if (foundRouteEntry != null &&
                        foundRouteEntry.nextHop().equals(routeEntry.nextHop())) {
                    // We only push prefix flows if the prefix is still in the
                    // radix tree and the next hop is the same as our
                    // update.
                    // The prefix could have been removed while we were waiting
                    // for the ARP, or the next hop could have changed.
                    addRouteIntentToNextHop(prefix, ipAddress, macAddress);
                } else {
                    log.debug("{} has been revoked before the MAC was resolved",
                            routeEntry);
                }
            }
        }
    }

    /**
     * Gets the SDN-IP routes.
     *
     * @return the SDN-IP routes
     */
    public Collection<RouteEntry> getRoutes() {
        Iterator<KeyValuePair<RouteEntry>> it =
                bgpRoutes.getKeyValuePairsForKeysStartingWith("").iterator();

        List<RouteEntry> routes = new LinkedList<>();

        while (it.hasNext()) {
            KeyValuePair<RouteEntry> entry = it.next();
            routes.add(entry.getValue());
        }

        return routes;
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
     * Listener for host events.
     */
    class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            if (event.type() == HostEvent.Type.HOST_ADDED ||
                    event.type() == HostEvent.Type.HOST_UPDATED) {
                Host host = event.subject();
                for (IpAddress ip : host.ipAddresses()) {
                    Ip4Address ip4Address = ip.getIp4Address();
                    if (ip4Address == null) {
                        // TODO: For now we support only IPv4
                        continue;
                    }
                    updateMac(ip4Address, host.mac());
                }
            }
        }
    }
}
