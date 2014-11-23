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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.host.HostService;
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
    // For routes announced by local BGP daemon in SDN network,
    // the next hop will be 0.0.0.0.
    private static final Ip4Address LOCAL_NEXT_HOP =
        Ip4Address.valueOf("0.0.0.0");

    // Store all route updates in a radix tree.
    // The key in this tree is the binary string of prefix of the route.
    private InvertedRadixTree<RouteEntry> bgpRoutes;

    // Stores all incoming route updates in a queue.
    private final BlockingQueue<RouteUpdate> routeUpdates;

    // The Ip4Address is the next hop address of each route update.
    private final SetMultimap<Ip4Address, RouteEntry> routesWaitingOnArp;

    private final ApplicationId appId;
    private final IntentSynchronizer intentSynchronizer;
    private final HostService hostService;
    private final SdnIpConfigService configService;
    private final InterfaceService interfaceService;
    private final ExecutorService bgpUpdatesExecutor;
    private final HostListener hostListener;

    /**
     * Class constructor.
     *
     * @param appId             the application ID
     * @param intentSynchronizer the intent synchronizer
     * @param configService     the configuration service
     * @param interfaceService  the interface service
     * @param hostService       the host service
     */
    public Router(ApplicationId appId, IntentSynchronizer intentSynchronizer,
                  SdnIpConfigService configService,
                  InterfaceService interfaceService,
                  HostService hostService) {
        this.appId = appId;
        this.intentSynchronizer = intentSynchronizer;
        this.configService = configService;
        this.interfaceService = interfaceService;
        this.hostService = hostService;

        this.hostListener = new InternalHostListener();

        bgpRoutes = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
        routeUpdates = new LinkedBlockingQueue<>();
        routesWaitingOnArp = Multimaps.synchronizedSetMultimap(
                HashMultimap.<Ip4Address, RouteEntry>create());

        bgpUpdatesExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                .setNameFormat("sdnip-bgp-updates-%d").build());
    }

    /**
     * Starts the router.
     */
    public void start() {
        this.hostService.addListener(hostListener);

        bgpUpdatesExecutor.execute(new Runnable() {
            @Override
            public void run() {
                doUpdatesThread();
            }
        });
    }

    /**
     * Stops the router.
     */
    public void stop() {
        this.hostService.removeListener(hostListener);

        // Stop the thread(s)
        bgpUpdatesExecutor.shutdownNow();

        synchronized (this) {
            // Cleanup all local state
            bgpRoutes = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
            routeUpdates.clear();
            routesWaitingOnArp.clear();
        }
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
    void processRouteAdd(RouteEntry routeEntry) {
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

        Set<ConnectPoint> ingressPorts = new HashSet<>();
        ConnectPoint egressPort = egressInterface.connectPoint();

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

        intentSynchronizer.submitRouteIntent(prefix, intent);
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
    void processRouteDelete(RouteEntry routeEntry) {
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

        intentSynchronizer.withdrawRouteIntent(routeEntry.prefix());
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
