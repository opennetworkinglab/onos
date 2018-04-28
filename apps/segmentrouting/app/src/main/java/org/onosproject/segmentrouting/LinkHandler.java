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

package org.onosproject.segmentrouting;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.link.LinkService;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;


public class LinkHandler {
    private static final Logger log = LoggerFactory.getLogger(LinkHandler.class);
    protected final SegmentRoutingManager srManager;

    // Local store for all links seen and their present status, used for
    // optimized routing. The existence of the link in the keys is enough to know
    // if the link has been "seen-before" by this instance of the controller.
    // The boolean value indicates if the link is currently up or not.
    // Currently the optimized routing logic depends on "forgetting" a link
    // when a switch goes down, but "remembering" it when only the link goes down.
    private Map<Link, Boolean> seenLinks = new ConcurrentHashMap<>();
    private Set<Link> seenBefore = Sets.newConcurrentHashSet();
    private EventuallyConsistentMap<DeviceId, Set<PortNumber>> downedPortStore = null;

    /**
     * Constructs the LinkHandler.
     *
     * @param srManager Segment Routing manager
     */
    LinkHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        log.debug("Creating EC map downedportstore");
        EventuallyConsistentMapBuilder<DeviceId, Set<PortNumber>> downedPortsMapBuilder
                = srManager.storageService.eventuallyConsistentMapBuilder();
        downedPortStore = downedPortsMapBuilder.withName("downedportstore")
                .withSerializer(srManager.createSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.trace("Current size {}", downedPortStore.size());
    }

    /**
     * Constructs the LinkHandler for unit-testing.
     *
     * @param srManager SegmentRoutingManager
     * @param linkService LinkService
     */
    LinkHandler(SegmentRoutingManager srManager, LinkService linkService) {
        this.srManager = srManager;
    }

    /**
     * Initialize LinkHandler.
     */
    void init() {
        log.info("Loading stored links");
        srManager.linkService.getActiveLinks().forEach(this::processLinkAdded);
    }

    /**
     * Preprocessing of added link before being sent for route-path handling.
     * Also performs post processing of link.
     *
     * @param link the link to be processed
     */
    void processLinkAdded(Link link) {
        log.info("** LINK ADDED {}", link.toString());
        if (!isLinkValid(link)) {
            return;
        }
        // Irrespective of whether the local is a MASTER or not for this device,
        // create group handler instance and push default TTP flow rules if needed,
        // as in a multi-instance setup, instances can initiate groups for any
        // device. Also update local groupHandler stores.
        DefaultGroupHandler groupHandler = srManager.groupHandlerMap
                                                .get(link.src().deviceId());
        if (groupHandler == null) {
            Device device = srManager.deviceService.getDevice(link.src().deviceId());
            if (device != null) {
                log.warn("processLinkAdded: Link Added notification without "
                        + "Device Added event, still handling it");
                srManager.processDeviceAdded(device);
            }
        }

        if (isSeenLink(link)) {
            // temp store retains previous state, just before the state is updated in
            // seenLinks; previous state is necessary when processing the
            // linkupdate in defaultRoutingHandler
            seenBefore.add(link);
        }
        updateSeenLink(link, true);

        if (srManager.deviceConfiguration == null ||
                !srManager.deviceConfiguration.isConfigured(link.src().deviceId())) {
            log.warn("Source device of this link is not configured.. "
                    + "not processing further");
            return;
        }

        // process link only if it is bidirectional
        if (!isBidirectionalLinkUp(link)) {
            log.debug("Link not bidirectional.. waiting for other direction " +
                    "src {} --> dst {} ", link.dst(), link.src());
            return;
        }

        log.info("processing bidi links {} <--> {} UP", link.src(), link.dst());
        // update groupHandler internal port state for both directions
        List<Link> ulinks = getBidiComponentLinks(link);
        for (Link ulink : ulinks) {
            DefaultGroupHandler gh = srManager.groupHandlerMap
                                         .get(ulink.src().deviceId());
            if (gh != null) {
                gh.portUpForLink(ulink);
            }
        }

        for (Link ulink : ulinks) {
            log.info("-- Starting optimized route-path processing for component "
                    + "unidirectional link {} --> {} UP", ulink.src(), ulink.dst());
            srManager.defaultRoutingHandler
                    .populateRoutingRulesForLinkStatusChange(null, ulink, null,
                                                             seenBefore.contains(ulink));

            if (srManager.mastershipService.isLocalMaster(ulink.src().deviceId())) {
                // handle edge-ports for dual-homed hosts
                updateHostPorts(ulink, true);

                // It's possible that linkUp causes no route-path change as ECMP graph does
                // not change if the link is a parallel link (same src-dst as
                // another link). However we still need to update ECMP hash
                // groups to include new buckets for the link that has come up.
                DefaultGroupHandler gh = srManager.groupHandlerMap
                                            .get(ulink.src().deviceId());
                if (gh != null) {
                    if (!seenBefore.contains(ulink) && isParallelLink(ulink)) {
                        // if link seen first time, we need to ensure hash-groups have
                        // all ports
                        log.debug("Attempting retryHash for paralled first-time link {}",
                                  ulink);
                        gh.retryHash(ulink, false, true);
                    } else {
                        // seen before-link
                        if (isParallelLink(ulink)) {
                            log.debug("Attempting retryHash for paralled seen-before "
                                    + "link {}", ulink);
                            gh.retryHash(ulink, false, false);
                        }
                    }
                }
            }
            //clean up temp state
            seenBefore.remove(ulink);
        }

    }

    /**
     * Preprocessing of removed link before being sent for route-path handling.
     * Also performs post processing of link.
     *
     * @param link the link to be processed
     */
    void processLinkRemoved(Link link) {
        log.info("** LINK REMOVED {}", link.toString());
        if (!isLinkValid(link)) {
            return;
        }
        // when removing links, update seen links first, before doing route-path
        // changes
        updateSeenLink(link, false);
        // handle edge-ports for dual-homed hosts
        if (srManager.mastershipService.isLocalMaster(link.src().deviceId())) {
            updateHostPorts(link, false);
        }

        // device availability check helps to ensure that multiple link-removed
        // events are actually treated as a single switch removed event.
        // purgeSeenLink is necessary so we do rerouting (instead of rehashing)
        // when switch comes back.
        if (link.src().elementId() instanceof DeviceId
                && !srManager.deviceService.isAvailable(link.src().deviceId())) {
            purgeSeenLink(link);
            return;
        }
        if (link.dst().elementId() instanceof DeviceId
                && !srManager.deviceService.isAvailable(link.dst().deviceId())) {
            purgeSeenLink(link);
            return;
        }

        // process link only if it is bidirectional
        if (!isBidirectionalLinkDown(link)) {
            log.debug("Link not bidirectional.. waiting for other direction " +
                    "src {} --> dst {} ", link.dst(), link.src());
            return;
        }
        log.info("processing bidi links {} <--> {} DOWN", link.src(), link.dst());

        for (Link ulink : getBidiComponentLinks(link)) {
            log.info("-- Starting optimized route-path processing for component "
                    + "unidirectional link {} --> {} DOWN", ulink.src(), ulink.dst());
            srManager.defaultRoutingHandler
                .populateRoutingRulesForLinkStatusChange(ulink, null, null, true);

            // attempt rehashing for parallel links
            DefaultGroupHandler groupHandler = srManager.groupHandlerMap
                    .get(ulink.src().deviceId());
            if (groupHandler != null) {
                if (srManager.mastershipService.isLocalMaster(ulink.src().deviceId())
                        && isParallelLink(ulink)) {
                    log.debug("* retrying hash for parallel link removed:{}", ulink);
                    groupHandler.retryHash(ulink, true, false);
                } else {
                    log.debug("Not attempting retry-hash for link removed: {} .. {}",
                              ulink,
                              (srManager.mastershipService.isLocalMaster(ulink
                                      .src().deviceId())) ? "not parallel"
                                                          : "not master");
                }
                // ensure local stores are updated after all rerouting or rehashing
                groupHandler.portDownForLink(ulink);
            } else {
                log.warn("group handler not found for dev:{} when removing link: {}",
                         ulink.src().deviceId(), ulink);
            }
        }
    }

    /**
     * Checks validity of link. Examples of invalid links include
     * indirect-links, links between ports on the same switch, and more.
     *
     * @param link the link to be processed
     * @return true if valid link
     */
     boolean isLinkValid(Link link) {
        if (link.type() != Link.Type.DIRECT) {
            // NOTE: A DIRECT link might be transiently marked as INDIRECT
            // if BDDP is received before LLDP. We can safely ignore that
            // until the LLDP is received and the link is marked as DIRECT.
            log.info("Ignore link {}->{}. Link type is {} instead of DIRECT.",
                     link.src(), link.dst(), link.type());
            return false;
        }
        DeviceId srcId = link.src().deviceId();
        DeviceId dstId = link.dst().deviceId();
        if (srcId.equals(dstId)) {
            log.warn("Links between ports on the same switch are not "
                    + "allowed .. ignoring link {}", link);
            return false;
        }
        DeviceConfiguration devConfig = srManager.deviceConfiguration;
        if (devConfig == null) {
            log.warn("Cannot check validity of link without device config");
            return true;
        }
        try {
            /*if (!devConfig.isEdgeDevice(srcId)
                    && !devConfig.isEdgeDevice(dstId)) {
                // ignore links between spines
                // XXX revisit when handling multi-stage fabrics
                log.warn("Links between spines not allowed...ignoring "
                        + "link {}", link);
                return false;
            }*/
            if (devConfig.isEdgeDevice(srcId)
                    && devConfig.isEdgeDevice(dstId)) {
                // ignore links between leaves if they are not pair-links
                // XXX revisit if removing pair-link config or allowing more than
                // one pair-link
                if (devConfig.getPairDeviceId(srcId).equals(dstId)
                        && devConfig.getPairLocalPort(srcId)
                                .equals(link.src().port())
                        && devConfig.getPairLocalPort(dstId)
                                .equals(link.dst().port())) {
                    // found pair link - allow it
                    return true;
                } else {
                    log.warn("Links between leaves other than pair-links are "
                            + "not allowed...ignoring link {}", link);
                    return false;
                }
            }
        } catch (DeviceConfigNotFoundException e) {
            // We still want to count the links in seenLinks even though there
            // is no config. So we let it return true
            log.warn("Could not check validity of link {} as subtending devices "
                    + "are not yet configured", link);
        }
        return true;
    }

    /**
     * Administratively enables or disables edge ports if the link that was
     * added or removed was the only uplink port from an edge device. Edge ports
     * that belong to dual-homed hosts are always processed. In addition,
     * single-homed host ports are optionally processed depending on the
     * singleHomedDown property.
     *
     * @param link the link to be processed
     * @param added true if link was added, false if link was removed
     */
    private void updateHostPorts(Link link, boolean added) {
        if (added) {
            DeviceConfiguration devConfig = srManager.deviceConfiguration;
            try {
                if (!devConfig.isEdgeDevice(link.src().deviceId())
                        || devConfig.isEdgeDevice(link.dst().deviceId())) {
                    return;
                }
            } catch (DeviceConfigNotFoundException e) {
                log.warn("Unable to determine if link is a valid uplink"
                        + e.getMessage());
            }
            // re-enable previously disabled ports on this edge-device if any
            Set<PortNumber> p = downedPortStore.remove(link.src().deviceId());
            if (p != null) {
                log.warn("Link src {} --> dst {} added is an edge-device uplink, "
                        + "enabling dual homed ports if any: {}", link.src().deviceId(),
                        link.dst().deviceId(), (p.isEmpty()) ? "no ports" : p);
                p.forEach(pnum -> srManager.deviceAdminService
                        .changePortState(link.src().deviceId(), pnum, true));
            }
        } else {
            if (!lastUplink(link)) {
                return;
            }
            // find dual homed hosts on this dev to disable
            DeviceId dev = link.src().deviceId();
            Set<PortNumber> dp = srManager.hostHandler
                    .getDualHomedHostPorts(dev);
            log.warn("Link src {} --> dst {} removed was the last uplink, "
                    + "disabling  dual homed ports:  {}", dev,
                     link.dst().deviceId(), (dp.isEmpty()) ? "no ports" : dp);
            dp.forEach(pnum -> srManager.deviceAdminService
                        .changePortState(dev, pnum, false));
            if (srManager.singleHomedDown) {
                // get all configured ports and down them if they haven't already
                // been downed
                srManager.deviceService.getPorts(dev).stream()
                    .filter(p -> p.isEnabled() && !dp.contains(p.number()))
                    .filter(p -> srManager.interfaceService
                            .isConfigured(new ConnectPoint(dev, p.number())))
                    .filter(p -> !srManager.deviceConfiguration
                            .isPairLocalPort(dev, p.number()))
                    .forEach(p -> {
                        log.warn("Last uplink gone src {} -> dst {} .. removing "
                                + "configured port {}", p.number());
                        srManager.deviceAdminService
                            .changePortState(dev, p.number(), false);
                        dp.add(p.number());
                    });
            }
            if (!dp.isEmpty()) {
                // update global store
                Set<PortNumber> p = downedPortStore.get(dev);
                if (p == null) {
                    p = dp;
                } else {
                    p.addAll(dp);
                }
                downedPortStore.put(link.src().deviceId(), p);
            }
        }
    }

    /**
     * Returns true if given link was the last active uplink from src-device of
     * link. An uplink is defined as a unidirectional link with src as
     * edgeRouter and dst as non-edgeRouter.
     *
     * @param link
     * @return true if given link was the last uplink from the src device
     */
    private boolean lastUplink(Link link) {
        DeviceConfiguration devConfig = srManager.deviceConfiguration;
        try {
            if (!devConfig.isEdgeDevice(link.src().deviceId())
                    || devConfig.isEdgeDevice(link.dst().deviceId())) {
                return false;
            }
            // note that using linkservice here would cause race conditions as
            // more links can show up while the app is still processing the first one
            Set<Link> devLinks = seenLinks.entrySet().stream()
                    .filter(entry -> entry.getKey().src().deviceId()
                            .equals(link.src().deviceId()))
                    .filter(entry -> entry.getValue())
                    .filter(entry -> !entry.getKey().equals(link))
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toSet());

            for (Link l : devLinks) {
                if (devConfig.isEdgeDevice(l.dst().deviceId())) {
                    continue;
                }
                log.debug("Found another active uplink {}", l);
                return false;
            }
            log.debug("No active uplink found");
            return true;
        } catch (DeviceConfigNotFoundException e) {
            log.warn("Unable to determine if link was the last uplink"
                    + e.getMessage());
        }
        return false;
    }

    /**
     * Returns true if this controller instance has seen this link before. The
     * link may not be currently up, but as long as the link had been seen
     * before this method will return true. The one exception is when the link
     * was indeed seen before, but this controller instance was forced to forget
     * it by a call to purgeSeenLink method.
     *
     * @param link the infrastructure link being queried
     * @return true if this controller instance has seen this link before
     */
    boolean isSeenLink(Link link) {
        return seenLinks.containsKey(link);
    }

    /**
     * Updates the seen link store. Updates can be for links that are currently
     * available or not.
     *
     * @param link the link to update in the seen-link local store
     * @param up the status of the link, true if up, false if down
     */
    void updateSeenLink(Link link, boolean up) {
        seenLinks.put(link, up);
    }

    /**
     * Returns the status of a seen-link (up or down). If the link has not been
     * seen-before, a null object is returned.
     *
     * @param link the infrastructure link being queried
     * @return null if the link was not seen-before; true if the seen-link is
     *         up; false if the seen-link is down
     */
    private Boolean isSeenLinkUp(Link link) {
        return seenLinks.get(link);
    }

    /**
     * Makes this controller instance forget a previously seen before link.
     *
     * @param link the infrastructure link to purge
     */
    private void purgeSeenLink(Link link) {
        seenLinks.remove(link);
        seenBefore.remove(link);
    }

    /**
     * Returns the status of a link as parallel link. A parallel link is defined
     * as a link which has common src and dst switches as another seen-link that
     * is currently enabled. It is not necessary for the link being queried to
     * be a seen-link.
     *
     * @param link the infrastructure link being queried
     * @return true if a seen-link exists that is up, and shares the same src
     *         and dst switches as the link being queried
     */
    private boolean isParallelLink(Link link) {
        for (Entry<Link, Boolean> seen : seenLinks.entrySet()) {
            Link seenLink = seen.getKey();
            if (seenLink.equals(link)) {
                continue;
            }
            if (seenLink.src().deviceId().equals(link.src().deviceId())
                    && seenLink.dst().deviceId().equals(link.dst().deviceId())
                    && seen.getValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the link being queried is a bidirectional link that is
     * up. A bidi-link is defined as a component unidirectional link, whose
     * reverse link - ie. the component unidirectional link in the reverse
     * direction - has been seen-before and is up. It is NOT necessary for the
     * link being queried to be a previously seen-link.
     *
     * @param link the infrastructure (unidirectional) link being queried
     * @return true if another unidirectional link exists in the reverse
     *         direction, has been seen-before and is up
     */
    boolean isBidirectionalLinkUp(Link link) {
        // cannot call linkService as link may be gone
        Link reverseLink = getReverseLink(link);
        if (reverseLink == null) {
            return false;
        }
        Boolean result = isSeenLinkUp(reverseLink);
        if (result == null) {
            return false;
        }
        return result.booleanValue();
    }

    /**
     * Returns true if the link being queried is a bidirectional link that is
     * down. A bidi-link is defined as a component unidirectional link, whose
     * reverse link - i.e the component unidirectional link in the reverse
     * direction - has been seen before and is down. It is necessary for the
     * reverse-link to have been previously seen.
     *
     * @param link the infrastructure (unidirectional) link being queried
     * @return true if another unidirectional link exists in the reverse
     *         direction, has been seen-before and is down
     */
    boolean isBidirectionalLinkDown(Link link) {
        // cannot call linkService as link may be gone
        Link reverseLink = getReverseLink(link);
        if (reverseLink == null) {
            log.warn("Query for bidi-link down but reverse-link not found "
                    + "for link {}", link);
            return false;
        }
        Boolean result = seenLinks.get(reverseLink);
        if (result == null) {
            return false;
        }
        // if reverse link is seen UP (true), then its not bidi yet
        return !result.booleanValue();
    }

    /**
     * Returns the link in the reverse direction from the given link, by
     * consulting the seen-links store.
     *
     * @param link the given link
     * @return the reverse link or null
     */
    Link getReverseLink(Link link) {
        return seenLinks.keySet().stream()
                .filter(l -> l.src().equals(link.dst()) && l.dst().equals(link.src()))
                .findAny()
                .orElse(null);
    }

    /**
     * Returns the component unidirectional links of a declared bidirectional
     * link, by consulting the seen-links store. Caller is responsible for
     * previously verifying bidirectionality. Returned list may be empty if
     * errors are encountered.
     *
     * @param link the declared bidirectional link
     * @return list of component unidirectional links
     */
    List<Link> getBidiComponentLinks(Link link) {
        Link reverseLink = getReverseLink(link);
        List<Link> componentLinks;
        if (reverseLink == null) { // really should not happen if link is bidi
            log.error("cannot find reverse link for given link: {} ... is it "
                    + "bi-directional?", link);
            componentLinks = ImmutableList.of();
        } else {
            componentLinks = ImmutableList.of(reverseLink, link);
        }
        return componentLinks;
    }

    /**
     * Determines if the given link should be avoided in routing calculations by
     * policy or design.
     *
     * @param link the infrastructure link being queried
     * @return true if link should be avoided
     */
    boolean avoidLink(Link link) {
        // XXX currently only avoids all pair-links. In the future can be
        // extended to avoid any generic link
        DeviceId src = link.src().deviceId();
        PortNumber srcPort = link.src().port();
        DeviceConfiguration devConfig = srManager.deviceConfiguration;
        if (devConfig == null || !devConfig.isConfigured(src)) {
            log.warn("Device {} not configured..cannot avoid link {}", src,
                     link);
            return false;
        }
        DeviceId pairDev;
        PortNumber pairLocalPort, pairRemotePort = null;
        try {
            pairDev = devConfig.getPairDeviceId(src);
            pairLocalPort = devConfig.getPairLocalPort(src);
            if (pairDev != null) {
                pairRemotePort = devConfig
                        .getPairLocalPort(pairDev);
            }
        } catch (DeviceConfigNotFoundException e) {
            log.warn("Pair dev for dev {} not configured..cannot avoid link {}",
                     src, link);
            return false;
        }

        return srcPort.equals(pairLocalPort)
                && link.dst().deviceId().equals(pairDev)
                && link.dst().port().equals(pairRemotePort);
    }

    /**
     * Cleans up internal LinkHandler stores.
     *
     * @param device the device that has been removed
     */
    void processDeviceRemoved(Device device) {
        seenLinks.keySet()
                .removeIf(key -> key.src().deviceId().equals(device.id())
                        || key.dst().deviceId().equals(device.id()));
    }

    /**
     * Administratively disables the host location switchport if the edge device
     * has no viable uplinks. The caller needs to determine if such behavior is
     * desired for the single or dual-homed host.
     *
     * @param loc the host location
     */
    void checkUplinksForHost(HostLocation loc) {
        try {
            for (Link l : srManager.linkService.getDeviceLinks(loc.deviceId())) {
                if (srManager.deviceConfiguration.isEdgeDevice(l.dst().deviceId())
                        || l.state() == Link.State.INACTIVE) {
                    continue;
                }
                // found valid uplink - so, nothing to do
                return;
            }
        } catch (DeviceConfigNotFoundException e) {
            log.warn("Could not check for valid uplinks due to missing device"
                    + "config " + e.getMessage());
            return;
        }
        log.warn("Host location {} has no valid uplinks disabling port", loc);
        srManager.deviceAdminService.changePortState(loc.deviceId(), loc.port(),
                                                     false);
        Set<PortNumber> p = downedPortStore.get(loc.deviceId());
        if (p == null) {
            p = Sets.newHashSet(loc.port());
        } else {
            p.add(loc.port());
        }
        downedPortStore.put(loc.deviceId(), p);
    }

    ImmutableMap<Link, Boolean> getSeenLinks() {
        return ImmutableMap.copyOf(seenLinks);
    }

    ImmutableMap<DeviceId, Set<PortNumber>> getDownedPorts() {
        return ImmutableMap.copyOf(downedPortStore.entrySet());
    }

    /**
     * Returns all links that egress from given device that are UP in the
     * seenLinks store. The returned links are also confirmed to be
     * bidirectional.
     *
     * @param deviceId the device identifier
     * @return set of egress links from the device
     */
    Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        return seenLinks.keySet().stream()
                .filter(link -> link.src().deviceId().equals(deviceId))
                .filter(link -> seenLinks.get(link))
                .filter(link -> isBidirectionalLinkUp(link))
                .collect(Collectors.toSet());
    }

}
