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
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.onos.sdnip.config.BgpPeer;
import org.onlab.onos.sdnip.config.Interface;
import org.onlab.onos.sdnip.config.SdnIpConfigService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
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
 *
 * TODO: Make it thread-safe.
 */
public class Router implements RouteListener {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    // Store all route updates in a radix tree.
    // The key in this tree is the binary string of prefix of the route.
    private InvertedRadixTree<RouteEntry> bgpRoutes;

    // Stores all incoming route updates in a queue.
    private BlockingQueue<RouteUpdate> routeUpdates;

    // The Ip4Address is the next hop address of each route update.
    private SetMultimap<IpAddress, RouteEntry> routesWaitingOnArp;
    private ConcurrentHashMap<IpPrefix, MultiPointToSinglePointIntent> pushedRouteIntents;

    private IntentService intentService;
    //private IProxyArpService proxyArp;
    private HostService hostService;
    private SdnIpConfigService configInfoService;
    private InterfaceService interfaceService;

    private ExecutorService bgpUpdatesExecutor;
    private ExecutorService bgpIntentsSynchronizerExecutor;

    // TODO temporary
    private int intentId = Integer.MAX_VALUE / 2;

    //
    // State to deal with SDN-IP Leader election and pushing Intents
    //
    private Semaphore intentsSynchronizerSemaphore = new Semaphore(0);
    private volatile boolean isElectedLeader = false;
    private volatile boolean isActivatedLeader = false;

    // For routes announced by local BGP daemon in SDN network,
    // the next hop will be 0.0.0.0.
    public static final IpAddress LOCAL_NEXT_HOP = IpAddress.valueOf("0.0.0.0");

    /**
     * Class constructor.
     *
     * @param intentService the intent service
     * @param hostService the host service
     * @param configInfoService the configuration service
     * @param interfaceService the interface service
     */
    public Router(IntentService intentService, HostService hostService,
            SdnIpConfigService configInfoService, InterfaceService interfaceService) {

        this.intentService = intentService;
        this.hostService = hostService;
        this.configInfoService = configInfoService;
        this.interfaceService = interfaceService;

        bgpRoutes = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
        routeUpdates = new LinkedBlockingQueue<>();
        routesWaitingOnArp = Multimaps.synchronizedSetMultimap(
                HashMultimap.<IpAddress, RouteEntry>create());
        pushedRouteIntents = new ConcurrentHashMap<>();

        bgpUpdatesExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("bgp-updates-%d").build());
        bgpIntentsSynchronizerExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                .setNameFormat("bgp-intents-synchronizer-%d").build());

        this.hostService.addListener(new InternalHostListener());
    }

    /**
     * Starts the Router.
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
        log.debug("Received new route Update: {}", routeUpdate);

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

            Map<IpPrefix, MultiPointToSinglePointIntent> fetchedIntents =
                new HashMap<>();

            //
            // Fetch all intents, and classify the Multi-Point-to-Point Intents
            // based on the matching prefix.
            //
            for (Intent intent : intentService.getIntents()) {
                //
                // TODO: Ignore all intents that are not installed by
                // the SDN-IP application.
                //
                if (!(intent instanceof MultiPointToSinglePointIntent)) {
                    continue;
                }
                MultiPointToSinglePointIntent mp2pIntent =
                    (MultiPointToSinglePointIntent) intent;
                /*Match match = mp2pIntent.getMatch();
                if (!(match instanceof PacketMatch)) {
                    continue;
                }
                PacketMatch packetMatch = (PacketMatch) match;
                Ip4Prefix prefix = packetMatch.getDstIpAddress();
                if (prefix == null) {
                    continue;
                }
                fetchedIntents.put(prefix, mp2pIntent);*/
                for (Criterion criterion : mp2pIntent.selector().criteria()) {
                    if (criterion.type() == Type.IPV4_DST) {
                        IPCriterion ipCriterion = (IPCriterion) criterion;
                        fetchedIntents.put(ipCriterion.ip(), mp2pIntent);
                    }
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
            Collection<Pair<IpPrefix, MultiPointToSinglePointIntent>>
                storeInMemoryIntents = new LinkedList<>();
            Collection<Pair<IpPrefix, MultiPointToSinglePointIntent>>
                addIntents = new LinkedList<>();
            Collection<Pair<IpPrefix, MultiPointToSinglePointIntent>>
                deleteIntents = new LinkedList<>();
            for (Map.Entry<IpPrefix, MultiPointToSinglePointIntent> entry :
                     pushedRouteIntents.entrySet()) {
                IpPrefix prefix = entry.getKey();
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
            for (Map.Entry<IpPrefix, MultiPointToSinglePointIntent> entry :
                     fetchedIntents.entrySet()) {
                IpPrefix prefix = entry.getKey();
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
            for (Pair<IpPrefix, MultiPointToSinglePointIntent> pair :
                     storeInMemoryIntents) {
                IpPrefix prefix = pair.getLeft();
                MultiPointToSinglePointIntent intent = pair.getRight();
                log.debug("Intent synchronization: updating in-memory " +
                          "Intent for prefix: {}", prefix);
                pushedRouteIntents.put(prefix, intent);
            }
            //
            isActivatedLeader = true;           // Allow push of Intents
            for (Pair<IpPrefix, MultiPointToSinglePointIntent> pair :
                     deleteIntents) {
                IpPrefix prefix = pair.getLeft();
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
            for (Pair<IpPrefix, MultiPointToSinglePointIntent> pair :
                     addIntents) {
                IpPrefix prefix = pair.getLeft();
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
        /*Match match1 = intent1.getMatch();
        Match match2 = intent2.getMatch();
        Action action1 = intent1.getAction();
        Action action2 = intent2.getAction();
        Set<SwitchPort> ingressPorts1 = intent1.getIngressPorts();
        Set<SwitchPort> ingressPorts2 = intent2.getIngressPorts();
        SwitchPort egressPort1 = intent1.getEgressPort();
        SwitchPort egressPort2 = intent2.getEgressPort();

        return Objects.equal(match1, match2) &&
            Objects.equal(action1, action2) &&
            Objects.equal(egressPort1, egressPort2) &&
            Objects.equal(ingressPorts1, ingressPorts2);*/
        return Objects.equal(intent1.selector(), intent2.selector()) &&
                Objects.equal(intent1.treatment(), intent2.treatment()) &&
                Objects.equal(intent1.ingressPoints(), intent2.ingressPoints()) &&
                Objects.equal(intent1.egressPoint(), intent2.egressPoint());
    }

    /**
     * Processes adding a route entry.
     * <p/>
     * Put new route entry into the radix tree. If there was an existing
     * next hop for this prefix, but the next hop was different, then execute
     * deleting old route entry. If the next hop is the SDN domain, we do not
     * handle it at the moment. Otherwise, execute adding a route.
     *
     * @param routeEntry the route entry to add
     */
    protected void processRouteAdd(RouteEntry routeEntry) {
        synchronized (this) {
            log.debug("Processing route add: {}", routeEntry);

            IpPrefix prefix = routeEntry.prefix();
            IpAddress nextHop = null;
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
     * <p/>
     * Find out the egress Interface and MAC address of next hop router for
     * this route entry. If the MAC address can not be found in ARP cache,
     * then this prefix will be put in routesWaitingOnArp queue. Otherwise,
     * new route intent will be created and installed.
     *
     * @param routeEntry the route entry to add
     */
    private void executeRouteAdd(RouteEntry routeEntry) {
        log.debug("Executing route add: {}", routeEntry);

        // See if we know the MAC address of the next hop
        //MacAddress nextHopMacAddress =
            //proxyArp.getMacAddress(routeEntry.getNextHop());
        MacAddress nextHopMacAddress = null;
        Set<Host> hosts = hostService.getHostsByIp(
                routeEntry.nextHop().toPrefix());
        if (!hosts.isEmpty()) {
            // TODO how to handle if multiple hosts are returned?
            nextHopMacAddress = hosts.iterator().next().mac();
        }

        if (nextHopMacAddress == null) {
            routesWaitingOnArp.put(routeEntry.nextHop(), routeEntry);
            //proxyArp.sendArpRequest(routeEntry.getNextHop(), this, true);
            // TODO maybe just do this for every prefix anyway
            hostService.startMonitoringIp(routeEntry.nextHop());
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
     * @param prefix IP prefix of the route to add
     * @param nextHopIpAddress IP address of the next hop
     * @param nextHopMacAddress MAC address of the next hop
     */
    private void addRouteIntentToNextHop(IpPrefix prefix,
                                         IpAddress nextHopIpAddress,
                                         MacAddress nextHopMacAddress) {

        // Find the attachment point (egress interface) of the next hop
        Interface egressInterface;
        if (configInfoService.getBgpPeers().containsKey(nextHopIpAddress)) {
            // Route to a peer
            log.debug("Route to peer {}", nextHopIpAddress);
            BgpPeer peer =
                configInfoService.getBgpPeers().get(nextHopIpAddress);
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
     * @param prefix IP prefix from route
     * @param egressInterface egress Interface connected to next hop router
     * @param nextHopMacAddress MAC address of next hop router
     */
    private void doAddRouteIntent(IpPrefix prefix, Interface egressInterface,
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
            if (!intf.equals(egressInterface)) {
                ConnectPoint srcPort = intf.connectPoint();
                ingressPorts.add(srcPort);
            }
        }

        // Match the destination IP prefix at the first hop
        //PacketMatchBuilder builder = new PacketMatchBuilder();
        //builder.setEtherType(Ethernet.TYPE_IPV4).setDstIpNet(prefix);
        //PacketMatch packetMatch = builder.build();
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(prefix)
                .build();

        // Rewrite the destination MAC address
        //ModifyDstMacAction modifyDstMacAction =
                //new ModifyDstMacAction(nextHopMacAddress);
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(nextHopMacAddress)
                .build();

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(nextIntentId(),
                        selector, treatment, ingressPorts, egressPort);

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
     * <p/>
     * Removes prefix from radix tree, and if successful, then try to delete
     * the related intent.
     *
     * @param routeEntry the route entry to delete
     */
    protected void processRouteDelete(RouteEntry routeEntry) {
        synchronized (this) {
            log.debug("Processing route delete: {}", routeEntry);
            IpPrefix prefix = routeEntry.prefix();

            // TODO check the change of logic here - remove doesn't check that
            // the route entry was what we expected (and we can't do this
            // concurrently)

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

        IpPrefix prefix = routeEntry.prefix();

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
     * This method handles the prefixes which are waiting for ARP replies for
     * MAC addresses of next hops.
     *
     * @param ipAddress next hop router IP address, for which we sent ARP
     * request out
     * @param macAddress MAC address which is relative to the ipAddress
     */
    //@Override
    // TODO change name
    public void arpResponse(IpAddress ipAddress, MacAddress macAddress) {
        log.debug("Received ARP response: {} => {}", ipAddress, macAddress);

         // We synchronize on this to prevent changes to the radix tree
         // while we're pushing intents. If the tree changes, the
         // tree and intents could get out of sync.
        synchronized (this) {

            Set<RouteEntry> routesToPush =
                routesWaitingOnArp.removeAll(ipAddress);

            for (RouteEntry routeEntry : routesToPush) {
                // These will always be adds
                IpPrefix prefix = routeEntry.prefix();
                String binaryString = RouteEntry.createBinaryString(prefix);
                RouteEntry foundRouteEntry =
                    bgpRoutes.getValueForExactKey(binaryString);
                if (foundRouteEntry != null &&
                    foundRouteEntry.nextHop().equals(routeEntry.nextHop())) {
                    log.debug("Pushing prefix {} next hop {}",
                              routeEntry.prefix(), routeEntry.nextHop());
                    // We only push prefix flows if the prefix is still in the
                    // radix tree and the next hop is the same as our
                    // update.
                    // The prefix could have been removed while we were waiting
                    // for the ARP, or the next hop could have changed.
                    addRouteIntentToNextHop(prefix, ipAddress, macAddress);
                } else {
                    log.debug("Received ARP response, but {}/{} is no longer in"
                            + " the radix tree", routeEntry.prefix(),
                            routeEntry.nextHop());
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
     * Generates a new unique intent ID.
     *
     * @return the new intent ID.
     */
    private IntentId nextIntentId() {
        return new IntentId(intentId++);
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
                for (IpPrefix ip : host.ipAddresses()) {
                    arpResponse(ip.toIpAddress(), host.mac());
                }
            }
        }
    }
}
