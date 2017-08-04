/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.newoptical;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onlab.util.Bandwidth;
import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entity to store optical connectivity request and related information.
 */
@Beta
public class OpticalConnectivity {

    private static final Logger log = LoggerFactory.getLogger(OpticalConnectivity.class);

    private final OpticalConnectivityId id;
    private final List<Link> links;
    private final Bandwidth requestBandwidth;
    private final Duration requestLatency;

    /**
     * Set of packet link that is not yet established.
     * Packet links in this set are expected to be discovered after underlying (optical) path has been provisioned.
     */
    private final ImmutableSet<PacketLinkRealizedByOptical> unestablishedLinks;

    /**
     * Set of packet link that is already established.
     */
    private final ImmutableSet<PacketLinkRealizedByOptical> establishedLinks;

    public OpticalConnectivity(OpticalConnectivityId id,
                               List<Link> links,
                               Bandwidth requestBandwidth,
                               Duration requestLatency,
                               Set<PacketLinkRealizedByOptical> unestablishedLinks,
                               Set<PacketLinkRealizedByOptical> establishedLinks) {
        this.id = id;
        this.links = ImmutableList.copyOf(links);
        this.requestBandwidth = requestBandwidth;
        this.requestLatency = requestLatency;
        this.unestablishedLinks = ImmutableSet.copyOf(unestablishedLinks);
        this.establishedLinks = ImmutableSet.copyOf(establishedLinks);
    }

    private OpticalConnectivity(OpticalConnectivity connectivity) {
        this.id = connectivity.id;
        this.links = ImmutableList.copyOf(connectivity.links);
        this.requestBandwidth = connectivity.requestBandwidth;
        this.requestLatency = connectivity.requestLatency;
        this.unestablishedLinks = ImmutableSet.copyOf(connectivity.unestablishedLinks);
        this.establishedLinks = ImmutableSet.copyOf(connectivity.establishedLinks);
    }

    public boolean isAllRealizingLinkEstablished() {
        // Check if all links are established
        return unestablishedLinks.isEmpty();
    }

    public boolean isAllRealizingLinkNotEstablished() {
        // Check if any link is not established
        return establishedLinks.isEmpty();
    }

    public OpticalConnectivityId id() {
        return id;
    }

    public List<Link> links() {
        return links;
    }

    public Bandwidth bandwidth() {
        return requestBandwidth;
    }

    public Duration latency() {
        return requestLatency;
    }

    public Set<PacketLinkRealizedByOptical> getEstablishedLinks() {
        return establishedLinks;
    }

    public Set<PacketLinkRealizedByOptical> getUnestablishedLinks() {
        return unestablishedLinks;
    }

    public OpticalConnectivity setLinkEstablished(ConnectPoint src,
                                                  ConnectPoint dst,
                                                  boolean established) {
        Set<PacketLinkRealizedByOptical> newEstablishedLinks;
        Set<PacketLinkRealizedByOptical> newUnestablishedLinks;

        if (established) {
            // move PacketLink from unestablished set to established set
            Optional<PacketLinkRealizedByOptical> link = this.unestablishedLinks.stream()
                    .filter(l -> l.isBetween(src, dst)).findAny();

            if (link.isPresent()) {

                newUnestablishedLinks = this.unestablishedLinks.stream()
                        .filter(l -> !l.isBetween(src, dst))
                        .collect(Collectors.toSet());
                newEstablishedLinks = ImmutableSet.<PacketLinkRealizedByOptical>builder()
                        .addAll(this.establishedLinks)
                        .add(link.get())
                        .build();
            } else {
                // no-op:
                newEstablishedLinks = ImmutableSet.copyOf(establishedLinks);
                newUnestablishedLinks = ImmutableSet.copyOf(unestablishedLinks);

                // sanity check
                boolean alreadyThere = establishedLinks.stream()
                    .filter(l -> l.isBetween(src, dst))
                    .findAny().isPresent();
                if (!alreadyThere) {
                    log.warn("Attempted to change {}-{} to established, "
                            + "which is not part of {}", src, dst, this);
                }
            }
        } else {
            // move PacketLink from established set to unestablished set
            Optional<PacketLinkRealizedByOptical> link = this.establishedLinks.stream()
                    .filter(l -> l.isBetween(src, dst)).findAny();

            if (link.isPresent()) {
                newEstablishedLinks = this.establishedLinks.stream()
                        .filter(l -> !l.isBetween(src, dst))
                        .collect(Collectors.toSet());
                newUnestablishedLinks = ImmutableSet.<PacketLinkRealizedByOptical>builder()
                        .addAll(this.unestablishedLinks)
                        .add(link.get())
                        .build();
            } else {
                // no-op:
                newEstablishedLinks = ImmutableSet.copyOf(establishedLinks);
                newUnestablishedLinks = ImmutableSet.copyOf(unestablishedLinks);

                // sanity check
                boolean alreadyThere = unestablishedLinks.stream()
                        .filter(l -> l.isBetween(src, dst))
                        .findAny().isPresent();
                if (!alreadyThere) {
                    log.warn("Attempted to change {}-{} to unestablished, "
                            + "which is not part of {}", src, dst, this);
                }
            }
        }

        return new OpticalConnectivity(this.id,
                this.links,
                this.requestBandwidth,
                this.requestLatency,
                newUnestablishedLinks,
                newEstablishedLinks);
    }

    public Set<PacketLinkRealizedByOptical> getRealizingLinks() {
        return ImmutableSet.<PacketLinkRealizedByOptical>builder()
                .addAll(unestablishedLinks)
                .addAll(establishedLinks)
                .build();
    }

    public static OpticalConnectivity copyOf(OpticalConnectivity connectivity) {
        return new OpticalConnectivity(connectivity);
    }
}
