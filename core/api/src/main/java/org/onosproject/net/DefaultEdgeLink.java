/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.provider.ProviderId;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default edge link model implementation.
 */
public class DefaultEdgeLink extends DefaultLink implements EdgeLink {

    private final HostId hostId;
    private final HostLocation hostLocation;

    /**
     * Creates an edge link using the supplied information.
     *
     * @param providerId   provider identity
     * @param hostPoint    host-side connection point
     * @param hostLocation location where host attaches to the network
     * @param isIngress    true to indicate host-to-network direction; false
     *                     for network-to-host direction
     * @param annotations  optional key/value annotations
     */
    public DefaultEdgeLink(ProviderId providerId, ConnectPoint hostPoint,
                           HostLocation hostLocation, boolean isIngress,
                           Annotations... annotations) {
        super(providerId, isIngress ? hostPoint : hostLocation,
              isIngress ? hostLocation : hostPoint, Type.EDGE, State.ACTIVE, annotations);
        checkArgument(hostPoint.elementId() instanceof HostId,
                      "Host point does not refer to a host ID");
        this.hostId = (HostId) hostPoint.elementId();
        this.hostLocation = hostLocation;
    }

    @Override
    public HostId hostId() {
        return hostId;
    }

    @Override
    public HostLocation hostLocation() {
        return hostLocation;
    }

    /**
     * Creates a phantom edge link, to an unspecified end-station. This link
     * does not represent any actually discovered link stored in the system.
     *
     * @param edgePort  network edge port
     * @param isIngress true to indicate host-to-network direction; false
     *                  for network-to-host direction
     * @return new phantom edge link
     */
    public static DefaultEdgeLink createEdgeLink(ConnectPoint edgePort,
                                                 boolean isIngress) {
        checkNotNull(edgePort, "Edge port cannot be null");
        HostLocation location = (edgePort instanceof HostLocation) ?
                (HostLocation) edgePort : new HostLocation(edgePort, 0);
        return new DefaultEdgeLink(ProviderId.NONE,
                                   new ConnectPoint(HostId.NONE, PortNumber.P0),
                                   location, isIngress);
    }

    /**
     * Creates a an edge link, to the specified end-station.
     *
     * The edge link inherits the target host annotations.
     *
     * @param host      host
     * @param isIngress true to indicate host-to-network direction; false
     *                  for network-to-host direction
     * @return new phantom edge link
     */
    public static DefaultEdgeLink createEdgeLink(Host host, boolean isIngress) {
        checkNotNull(host, "Host cannot be null");
        return new DefaultEdgeLink(ProviderId.NONE,
                                   new ConnectPoint(host.id(), PortNumber.P0),
                                   host.location(), isIngress, host.annotations());
    }

    /**
     * Creates edge links, to the specified end-station.
     *
     * The edge link inherits the target host annotations.
     *
     * @param host      host
     * @param isIngress true to indicate host-to-network direction; false
     *                  for network-to-host direction
     * @return new phantom edge link
     */
    public static Set<DefaultEdgeLink> createEdgeLinks(Host host, boolean isIngress) {
        checkNotNull(host, "Host cannot be null");
        ImmutableSet.Builder<DefaultEdgeLink> edgeLinksBuilder = ImmutableSet.builder();
        host.locations().forEach(
                location -> edgeLinksBuilder.add(new DefaultEdgeLink(ProviderId.NONE,
                                                 new ConnectPoint(host.id(), PortNumber.P0),
                                                 location, isIngress, host.annotations()))
        );
        return edgeLinksBuilder.build();
    }

}
