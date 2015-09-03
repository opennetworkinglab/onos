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
package org.onosproject.routing.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.routing.BgpService;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.RouteEntry;
import org.onosproject.routing.RouteListener;
import org.onosproject.routing.RouteUpdate;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.routing.RouteEntry.createBinaryString;

/**
 * This class processes route updates and maintains a Routing Information Base
 * (RIB). After route updates have been processed and next hops have been
 * resolved, FIB updates are sent to any listening FIB components.
 */
@Component(immediate = true)
@Service
public class Router implements RoutingService {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    // Route entries are stored in a radix tree.
    // The key in this tree is the binary string of prefix of the route.
    private InvertedRadixTree<RouteEntry> ribTable4;
    private InvertedRadixTree<RouteEntry> ribTable6;

    // Stores all incoming route updates in a queue.
    private final BlockingQueue<Collection<RouteUpdate>> routeUpdatesQueue =
            new LinkedBlockingQueue<>();

    // Next-hop IP address to route entry mapping for next hops pending MAC
    // resolution
    private SetMultimap<IpAddress, RouteEntry> routesWaitingOnArp;

    // The IPv4 address to MAC address mapping
    private final Map<IpAddress, MacAddress> ip2Mac = new ConcurrentHashMap<>();

    private FibListener fibComponent;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BgpService bgpService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RoutingConfigurationService routingConfigurationService;

    private ExecutorService bgpUpdatesExecutor;
    private final HostListener hostListener = new InternalHostListener();

    @Activate
    public void activate() {
        ribTable4 = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
        ribTable6 = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());

        routesWaitingOnArp = Multimaps.synchronizedSetMultimap(
                HashMultimap.<IpAddress, RouteEntry>create());

        coreService.registerApplication(ROUTER_APP_ID);

        bgpUpdatesExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                .setNameFormat("sdnip-bgp-updates-%d").build());
    }

    @Deactivate
    public void deactivate() {
        log.debug("Stopped");
    }

    @Override
    public void addFibListener(FibListener fibListener) {
        this.fibComponent = checkNotNull(fibListener);
    }

    @Override
    public void start() {
        this.hostService.addListener(hostListener);

        bgpService.start(new InternalRouteListener());

        bgpUpdatesExecutor.execute(this::doUpdatesThread);
    }

    @Override
    public void stop() {
        bgpService.stop();

        this.hostService.removeListener(hostListener);

        // Stop the thread(s)
        bgpUpdatesExecutor.shutdownNow();

        synchronized (this) {
            // Cleanup all local state
            ribTable4 = new ConcurrentInvertedRadixTree<>(
                    new DefaultByteArrayNodeFactory());
            ribTable6 = new ConcurrentInvertedRadixTree<>(
                    new DefaultByteArrayNodeFactory());
            routeUpdatesQueue.clear();
            routesWaitingOnArp.clear();
            ip2Mac.clear();
        }
    }

    /**
     * Entry point for route updates.
     *
     * @param routeUpdates collection of route updates to process
     */
    private void update(Collection<RouteUpdate> routeUpdates) {
        try {
            routeUpdatesQueue.put(routeUpdates);
        } catch (InterruptedException e) {
            log.error("Interrupted while putting on routeUpdatesQueue", e);
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
                    Collection<RouteUpdate> routeUpdates =
                            routeUpdatesQueue.take();
                    processRouteUpdates(routeUpdates);
                } catch (InterruptedException e) {
                    log.error("Interrupted while taking from updates queue", e);
                    interrupted = true;
                } catch (Exception e) {
                    log.error("exception", e);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Gets all IPv4 routes from the RIB.
     *
     * @return all IPv4 routes from the RIB
     */
    @Override
    public Collection<RouteEntry> getRoutes4() {
        Iterator<KeyValuePair<RouteEntry>> it =
                ribTable4.getKeyValuePairsForKeysStartingWith("").iterator();

        List<RouteEntry> routes = new LinkedList<>();

        while (it.hasNext()) {
            KeyValuePair<RouteEntry> entry = it.next();
            routes.add(entry.getValue());
        }

        return routes;
    }

    /**
     * Gets all IPv6 routes from the RIB.
     *
     * @return all IPv6 routes from the RIB
     */
    @Override
    public Collection<RouteEntry> getRoutes6() {
        Iterator<KeyValuePair<RouteEntry>> it =
                ribTable6.getKeyValuePairsForKeysStartingWith("").iterator();

        List<RouteEntry> routes = new LinkedList<>();

        while (it.hasNext()) {
            KeyValuePair<RouteEntry> entry = it.next();
            routes.add(entry.getValue());
        }

        return routes;
    }

    /**
     * Finds a route in the RIB for a prefix. The prefix can be either IPv4 or
     * IPv6.
     *
     * @param prefix the prefix to use
     * @return the route if found, otherwise null
     */
    RouteEntry findRibRoute(IpPrefix prefix) {
        String binaryString = createBinaryString(prefix);
        if (prefix.isIp4()) {
            // IPv4
            return ribTable4.getValueForExactKey(binaryString);
        }
        // IPv6
        return ribTable6.getValueForExactKey(binaryString);
    }

    /**
     * Adds a route to the RIB. The route can be either IPv4 or IPv6.
     *
     * @param routeEntry the route entry to use
     */
    void addRibRoute(RouteEntry routeEntry) {
        if (routeEntry.isIp4()) {
            // IPv4
            ribTable4.put(createBinaryString(routeEntry.prefix()), routeEntry);
        } else {
            // IPv6
            ribTable6.put(createBinaryString(routeEntry.prefix()), routeEntry);
        }
    }

    /**
     * Removes a route for a prefix from the RIB. The prefix can be either IPv4
     * or IPv6.
     *
     * @param prefix the prefix to use
     * @return true if the route was found and removed, otherwise false
     */
    boolean removeRibRoute(IpPrefix prefix) {
        if (prefix.isIp4()) {
            // IPv4
            return ribTable4.remove(createBinaryString(prefix));
        }
        // IPv6
        return ribTable6.remove(createBinaryString(prefix));
    }

    /**
     * Processes route updates.
     *
     * @param routeUpdates the route updates to process
     */
    void processRouteUpdates(Collection<RouteUpdate> routeUpdates) {
        synchronized (this) {
            Collection<IpPrefix> withdrawPrefixes = new LinkedList<>();
            Collection<FibUpdate> fibUpdates = new LinkedList<>();
            Collection<FibUpdate> fibWithdraws = new LinkedList<>();

            for (RouteUpdate update : routeUpdates) {
                switch (update.type()) {
                case UPDATE:

                    FibEntry fib = processRouteAdd(update.routeEntry(),
                            withdrawPrefixes);
                    if (fib != null) {
                        fibUpdates.add(new FibUpdate(FibUpdate.Type.UPDATE, fib));
                    }

                    break;
                case DELETE:
                    processRouteDelete(update.routeEntry(), withdrawPrefixes);

                    break;
                default:
                    log.error("Unknown update Type: {}", update.type());
                    break;
                }
            }

            withdrawPrefixes.forEach(p -> fibWithdraws.add(new FibUpdate(
                    FibUpdate.Type.DELETE, new FibEntry(p, null, null))));

            if (!fibUpdates.isEmpty() || !fibWithdraws.isEmpty()) {
                fibComponent.update(fibUpdates, fibWithdraws);
            }
        }
    }

    /**
     * Processes adding a route entry.
     * <p>
     * The route entry is added to the radix tree. If there was an existing
     * next hop for this prefix, but the next hop was different, then the
     * old route entry is deleted.
     * </p>
     * <p>
     * NOTE: Currently, we don't handle routes if the next hop is within the
     * SDN domain.
     * </p>
     *
     * @param routeEntry the route entry to add
     * @param withdrawPrefixes the collection of accumulated prefixes whose
     * intents will be withdrawn
     * @return the corresponding FIB entry change, or null
     */
    private FibEntry processRouteAdd(RouteEntry routeEntry,
                                     Collection<IpPrefix> withdrawPrefixes) {
        log.debug("Processing route add: {}", routeEntry);

        // Find the old next-hop if we are updating an old route entry
        IpAddress oldNextHop = null;
        RouteEntry oldRouteEntry = findRibRoute(routeEntry.prefix());
        if (oldRouteEntry != null) {
            oldNextHop = oldRouteEntry.nextHop();
        }

        // Add the new route to the RIB
        addRibRoute(routeEntry);

        if (oldNextHop != null) {
            if (oldNextHop.equals(routeEntry.nextHop())) {
                return null;            // No change
            }
            //
            // Update an existing nexthop for the prefix.
            // We need to remove the old flows for this prefix from the
            // switches before the new flows are added.
            //
            withdrawPrefixes.add(routeEntry.prefix());
        }

        if (routingConfigurationService.isIpPrefixLocal(routeEntry.prefix())) {
            // Route originated by local SDN domain
            // We don't handle these here, reactive routing APP will handle
            // these
            log.debug("Own route {} to {}",
                    routeEntry.prefix(), routeEntry.nextHop());
            return null;
        }

        //
        // Find the MAC address of next hop router for this route entry.
        // If the MAC address can not be found in ARP cache, then this prefix
        // will be put in routesWaitingOnArp queue. Otherwise, generate
        // a new route intent.
        //

        // Monitor the IP address for updates of the MAC address
        hostService.startMonitoringIp(routeEntry.nextHop());

        // Check if we know the MAC address of the next hop
        MacAddress nextHopMacAddress = ip2Mac.get(routeEntry.nextHop());
        if (nextHopMacAddress == null) {
            Set<Host> hosts = hostService.getHostsByIp(routeEntry.nextHop());
            if (!hosts.isEmpty()) {
                nextHopMacAddress = hosts.iterator().next().mac();
            }
            if (nextHopMacAddress != null) {
                ip2Mac.put(routeEntry.nextHop(), nextHopMacAddress);
            }
        }
        if (nextHopMacAddress == null) {
            routesWaitingOnArp.put(routeEntry.nextHop(), routeEntry);
            return null;
        }
        return new FibEntry(routeEntry.prefix(), routeEntry.nextHop(),
                nextHopMacAddress);
    }

    /**
     * Processes the deletion of a route entry.
     * <p>
     * The prefix for the routing entry is removed from radix tree.
     * If the operation is successful, the prefix is added to the collection
     * of prefixes whose intents that will be withdrawn.
     * </p>
     *
     * @param routeEntry the route entry to delete
     * @param withdrawPrefixes the collection of accumulated prefixes whose
     * intents will be withdrawn
     */
    private void processRouteDelete(RouteEntry routeEntry,
                                    Collection<IpPrefix> withdrawPrefixes) {
        log.debug("Processing route delete: {}", routeEntry);
        boolean isRemoved = removeRibRoute(routeEntry.prefix());

        if (isRemoved) {
            //
            // Only withdraw intents if an entry was actually removed from the
            // tree. If no entry was removed, the <prefix, nexthop> wasn't
            // there so it's probably already been removed and we don't
            // need to do anything.
            //
            withdrawPrefixes.add(routeEntry.prefix());
        }

        routesWaitingOnArp.remove(routeEntry.nextHop(), routeEntry);
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
    private void updateMac(IpAddress ipAddress, MacAddress macAddress) {
        log.debug("Received updated MAC info: {} => {}", ipAddress,
                macAddress);

        //
        // We synchronize on "this" to prevent changes to the Radix tree
        // while we're pushing intents. If the tree changes, the
        // tree and the intents could get out of sync.
        //
        synchronized (this) {
            Collection<FibUpdate> submitFibEntries = new LinkedList<>();

            Set<RouteEntry> routesToPush =
                    routesWaitingOnArp.removeAll(ipAddress);

            for (RouteEntry routeEntry : routesToPush) {
                // These will always be adds
                RouteEntry foundRouteEntry = findRibRoute(routeEntry.prefix());
                if (foundRouteEntry != null &&
                        foundRouteEntry.nextHop().equals(routeEntry.nextHop())) {
                    // We only push FIB updates if the prefix is still in the
                    // radix tree and the next hop is the same as our entry.
                    // The prefix could have been removed while we were waiting
                    // for the ARP, or the next hop could have changed.
                    submitFibEntries.add(new FibUpdate(FibUpdate.Type.UPDATE,
                            new FibEntry(routeEntry.prefix(),
                                    ipAddress, macAddress)));
                } else {
                    log.debug("{} has been revoked before the MAC was resolved",
                            routeEntry);
                }
            }

            if (!submitFibEntries.isEmpty()) {
                fibComponent.update(submitFibEntries, Collections.emptyList());
            }

            ip2Mac.put(ipAddress, macAddress);
        }
    }

    /**
     * Listener for host events.
     */
    class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            log.debug("Received HostEvent {}", event);

            Host host = event.subject();
            switch (event.type()) {
            case HOST_ADDED:
                // FALLTHROUGH
            case HOST_UPDATED:
                for (IpAddress ipAddress : host.ipAddresses()) {
                    updateMac(ipAddress, host.mac());
                }
                break;
            case HOST_REMOVED:
                for (IpAddress ipAddress : host.ipAddresses()) {
                    ip2Mac.remove(ipAddress);
                }
                break;
            default:
                break;
            }
        }
    }

    /**
     * Listener for route events.
     */
    private class InternalRouteListener implements RouteListener {
        @Override
        public void update(Collection<RouteUpdate> routeUpdates) {
            Router.this.update(routeUpdates);
        }
    }

    @Override
    public RouteEntry getLongestMatchableRouteEntry(IpAddress ipAddress) {
        RouteEntry routeEntry = null;
        Iterable<RouteEntry> routeEntries;

        if (ipAddress.isIp4()) {
            routeEntries = ribTable4.getValuesForKeysPrefixing(
                    createBinaryString(
                    IpPrefix.valueOf(ipAddress, Ip4Address.BIT_LENGTH)));
        } else {
            routeEntries = ribTable6.getValuesForKeysPrefixing(
                    createBinaryString(
                    IpPrefix.valueOf(ipAddress, Ip6Address.BIT_LENGTH)));
        }
        if (routeEntries == null) {
            return null;
        }
        Iterator<RouteEntry> it = routeEntries.iterator();
        while (it.hasNext()) {
            routeEntry = it.next();
        }
        return routeEntry;
    }

}
