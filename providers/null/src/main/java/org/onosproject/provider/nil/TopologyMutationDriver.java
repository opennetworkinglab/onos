/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.nil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.provider.nil.TopologySimulator.description;

/**
 * Drives topology mutations at a specified rate of events per second.
 */
class TopologyMutationDriver implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int WAIT_DELAY = 2_000;
    private static final int MAX_DOWN_LINKS = 5;

    private final Random random = new Random();

    private volatile boolean stopped = true;

    private double mutationRate;
    private int millis, nanos;

    private LinkService linkService;
    private DeviceService deviceService;
    private LinkProviderService linkProviderService;
    private DeviceProviderService deviceProviderService;
    private TopologySimulator simulator;

    private List<LinkDescription> activeLinks;
    private List<LinkDescription> inactiveLinks;

    private final ExecutorService executor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/null", "topo-mutator", log));

    private Map<DeviceId, Set<Link>> savedLinks = Maps.newConcurrentMap();

    /**
     * Starts the mutation process.
     *
     * @param mutationRate          link events per second
     * @param linkService           link service
     * @param deviceService         device service
     * @param linkProviderService   link provider service
     * @param deviceProviderService device provider service
     * @param simulator             topology simulator
     */
    void start(double mutationRate,
               LinkService linkService, DeviceService deviceService,
               LinkProviderService linkProviderService,
               DeviceProviderService deviceProviderService,
               TopologySimulator simulator) {
        savedLinks.clear();
        stopped = false;
        this.linkService = linkService;
        this.deviceService = deviceService;
        this.linkProviderService = linkProviderService;
        this.deviceProviderService = deviceProviderService;
        this.simulator = simulator;
        activeLinks = reduceLinks();
        inactiveLinks = Lists.newArrayList();
        adjustRate(mutationRate);
        executor.execute(this);
    }

    /**
     * Adjusts the topology mutation rate.
     *
     * @param mutationRate new topology mutation rate
     */
    void adjustRate(double mutationRate) {
        this.mutationRate = mutationRate;
        if (mutationRate > 0) {
            this.millis = (int) (1_000 / mutationRate / 2);
            this.nanos = (int) (1_000_000 / mutationRate / 2) % 1_000_000;
        } else {
            this.millis = 0;
            this.nanos = 0;
        }
        log.info("Settings: millis={}, nanos={}", millis, nanos);
    }

    /**
     * Stops the mutation process.
     */
    void stop() {
        stopped = true;
    }

    /**
     * Severs the link between the specified end-points in both directions.
     *
     * @param one link endpoint
     * @param two link endpoint
     */
    void severLink(ConnectPoint one, ConnectPoint two) {
        LinkDescription link = new DefaultLinkDescription(one, two, DIRECT);
        linkProviderService.linkVanished(link);
        linkProviderService.linkVanished(reverse(link));

    }

    /**
     * Repairs the link between the specified end-points in both directions.
     *
     * @param one link endpoint
     * @param two link endpoint
     */
    void repairLink(ConnectPoint one, ConnectPoint two) {
        LinkDescription link = new DefaultLinkDescription(one, two, DIRECT);
        linkProviderService.linkDetected(link);
        linkProviderService.linkDetected(reverse(link));
    }

    /**
     * Fails the specified device.
     *
     * @param deviceId device identifier
     */
    void failDevice(DeviceId deviceId) {
        savedLinks.put(deviceId, linkService.getDeviceLinks(deviceId));
        deviceProviderService.deviceDisconnected(deviceId);
    }

    /**
     * Repairs the specified device.
     *
     * @param deviceId device identifier
     */
    void repairDevice(DeviceId deviceId) {
        // device IDs are expressed in hexadecimal... (use radix 16)
        int chassisId = Integer.parseInt(deviceId.uri().getSchemeSpecificPart(), 16);
        simulator.createDevice(deviceId, chassisId);
        Set<Link> links = savedLinks.remove(deviceId);
        if (links != null) {
            links.forEach(l -> linkProviderService
                    .linkDetected(new DefaultLinkDescription(l.src(), l.dst(), DIRECT)));
        }
    }

    /**
     * Returns whether the given device is considered reachable or not.
     *
     * @param deviceId device identifier
     * @return true if device is reachable
     */
    boolean isReachable(DeviceId deviceId) {
        return !savedLinks.containsKey(deviceId);
    }

    @Override
    public void run() {
        delay(WAIT_DELAY);

        while (!stopped) {
            if (mutationRate > 0 && inactiveLinks.isEmpty()) {
                primeInactiveLinks();
            } else if (mutationRate <= 0 && !inactiveLinks.isEmpty()) {
                repairInactiveLinks();
            } else if (inactiveLinks.isEmpty()) {
                delay(WAIT_DELAY);

            } else {
                activeLinks.add(repairLink());
                pause();
                inactiveLinks.add(severLink());
                pause();
            }
        }
    }

    // Primes the inactive links with a few random links.
    private void primeInactiveLinks() {
        for (int i = 0, n = Math.min(MAX_DOWN_LINKS, activeLinks.size()); i < n; i++) {
            inactiveLinks.add(severLink());
        }
    }

    // Repairs all inactive links.
    private void repairInactiveLinks() {
        while (!inactiveLinks.isEmpty()) {
            repairLink();
        }
    }

    // Picks a random active link and severs it.
    private LinkDescription severLink() {
        LinkDescription link = getRandomLink(activeLinks);
        linkProviderService.linkVanished(link);
        linkProviderService.linkVanished(reverse(link));
        return link;
    }

    // Picks a random inactive link and repairs it.
    private LinkDescription repairLink() {
        LinkDescription link = getRandomLink(inactiveLinks);
        linkProviderService.linkDetected(link);
        linkProviderService.linkDetected(reverse(link));
        return link;
    }

    // Produces a reverse of the specified link.
    private LinkDescription reverse(LinkDescription link) {
        return new DefaultLinkDescription(link.dst(), link.src(), link.type());
    }

    // Returns a random link from the specified list of links.
    private LinkDescription getRandomLink(List<LinkDescription> links) {
        return links.remove(random.nextInt(links.size()));
    }

    // Reduces the given list of links to just a single link in each original pair.
    private List<LinkDescription> reduceLinks() {
        List<LinkDescription> links = Lists.newArrayList();
        linkService.getLinks().forEach(link -> links.add(description(link)));
        return links.stream()
                .filter(this::isOurLink)
                .filter(this::isRightDirection)
                .collect(Collectors.toList());
    }

    // Returns true if the specified link is ours.
    private boolean isOurLink(LinkDescription linkDescription) {
        return deviceService.getRole(linkDescription.src().deviceId()) == MASTER;
    }

    // Returns true if the link source is greater than the link destination.
    private boolean isRightDirection(LinkDescription link) {
        return link.src().deviceId().toString().compareTo(link.dst().deviceId().toString()) > 0;
    }

    // Pauses the current thread for the pre-computed time of millis & nanos.
    private void pause() {
        delay(millis, nanos);
    }

}
