/*
 * Copyright 2016 Open Networking Laboratory
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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.Path;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Entity to store optical connectivity request and related information.
 */
@Beta
public class OpticalConnectivity {

    private final OpticalConnectivityId id;
    private final List<Link> links;
    private final Bandwidth requestBandwidth;
    private final Duration requestLatency;

    // Bandwidth capacity of optical layer
    private Bandwidth opticalCapacity;

    private final Set<PacketLinkRealizedByOptical> realizingLinks = new HashSet<>();

    private State state = State.CREATED;

    public enum State {
        CREATED,
        INSTALLING,
        INSTALLED,
        WITHDRAWING,
        WITHDRAWN,
        FAILED
    }

    public OpticalConnectivity(OpticalConnectivityId id, Path path, Bandwidth requestBandwidth,
                               Duration requestLatency) {
        this.id = id;
        this.links = ImmutableList.copyOf(path.links());
        this.requestBandwidth = requestBandwidth;
        this.requestLatency = requestLatency;
    }

    public void setLinkEstablished(ConnectPoint src, ConnectPoint dst) {
        realizingLinks.stream().filter(l -> l.isBetween(src, dst))
                .findAny()
                .ifPresent(l -> l.setEstablished(true));
    }

    public void setLinkRemoved(ConnectPoint src, ConnectPoint dst) {
        realizingLinks.stream().filter(l -> l.isBetween(src, dst))
                .findAny()
                .ifPresent(l -> l.setEstablished(false));
    }

    public boolean isAllRealizingLinkEstablished() {
        return realizingLinks.stream().allMatch(PacketLinkRealizedByOptical::isEstablished);
    }

    public boolean isAllRealizingLinkNotEstablished() {
        return !realizingLinks.stream().anyMatch(PacketLinkRealizedByOptical::isEstablished);
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

    public State state() {
        return state;
    }

    public boolean state(State state) {
        boolean valid = true;
        // reject invalid state transition
        switch (this.state) {
            case CREATED:
                valid = (state == State.INSTALLING || state == State.FAILED);
                break;
            case INSTALLING:
                valid = (state == State.INSTALLED || state == State.FAILED);
                break;
            case INSTALLED:
                valid = (state == State.WITHDRAWING || state == State.FAILED);
                break;
            case WITHDRAWING:
                valid = (state == State.WITHDRAWN || state == State.FAILED);
                break;
            case FAILED:
                valid = (state == State.INSTALLING || state == State.WITHDRAWING || state == State.FAILED);
                break;
            default:
                break;
        }

        if (valid) {
            this.state = state;
        }

        return valid;
    }

    public Bandwidth getOpticalCapacity() {
        return opticalCapacity;
    }

    public void setOpticalCapacity(Bandwidth opticalCapacity) {
        this.opticalCapacity = opticalCapacity;
    }

    public void addRealizingLink(PacketLinkRealizedByOptical link) {
        checkNotNull(link);
        realizingLinks.add(link);
    }

    public void removeRealizingLink(PacketLinkRealizedByOptical link) {
        checkNotNull(link);
        realizingLinks.remove(link);
    }

    public Set<PacketLinkRealizedByOptical> getRealizingLinks() {
        return ImmutableSet.copyOf(realizingLinks);
    }
}
