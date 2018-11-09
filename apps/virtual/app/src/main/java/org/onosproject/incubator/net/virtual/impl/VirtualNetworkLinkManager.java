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

package org.onosproject.incubator.net.virtual.impl;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.event.AbstractVirtualListenerManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Link service implementation built on the virtual network service.
 */
public class VirtualNetworkLinkManager
        extends AbstractVirtualListenerManager<LinkEvent, LinkListener>
        implements LinkService {

    private static final String DEVICE_NULL = "Device cannot be null";
    private static final String CONNECT_POINT_NULL = "Connect point cannot be null";

    /**
     * Creates a new VirtualNetworkLinkService object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param networkId a virtual networkIdentifier
     */
    public VirtualNetworkLinkManager(VirtualNetworkService virtualNetworkManager,
                                     NetworkId networkId) {
        super(virtualNetworkManager, networkId, LinkEvent.class);
    }

    @Override
    public int getLinkCount() {
        return manager.getVirtualLinks(this.networkId()).size();
    }

    @Override
    public Iterable<Link> getLinks() {
        return manager.getVirtualLinks(this.networkId())
                .stream().collect(Collectors.toSet());
    }

    @Override
    public Iterable<Link> getActiveLinks() {

        return manager.getVirtualLinks(this.networkId())
                .stream()
                .filter(link -> (link.state().equals(Link.State.ACTIVE)))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Link> getDeviceLinks(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_NULL);
        return manager.getVirtualLinks(this.networkId())
                .stream()
                .filter(link -> (deviceId.equals(link.src().elementId()) ||
                        deviceId.equals(link.dst().elementId())))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_NULL);
        return manager.getVirtualLinks(this.networkId())
                .stream()
                .filter(link -> (deviceId.equals(link.dst().elementId())))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_NULL);
        return manager.getVirtualLinks(this.networkId())
                .stream()
                .filter(link -> (deviceId.equals(link.src().elementId())))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Link> getLinks(ConnectPoint connectPoint) {
        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return manager.getVirtualLinks(this.networkId())
                .stream()
                .filter(link -> (connectPoint.equals(link.src()) ||
                        connectPoint.equals(link.dst())))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint connectPoint) {
        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return manager.getVirtualLinks(this.networkId())
                .stream()
                .filter(link -> (connectPoint.equals(link.dst())))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint connectPoint) {
        checkNotNull(connectPoint, CONNECT_POINT_NULL);
        return manager.getVirtualLinks(this.networkId())
                .stream()
                .filter(link -> (connectPoint.equals(link.src())))
                .collect(Collectors.toSet());
    }

    @Override
    public Link getLink(ConnectPoint src, ConnectPoint dst) {
        checkNotNull(src, CONNECT_POINT_NULL);
        checkNotNull(dst, CONNECT_POINT_NULL);
        Optional<VirtualLink> foundLink =  manager.getVirtualLinks(this.networkId())
                .stream()
                .filter(link -> (src.equals(link.src()) &&
                        dst.equals(link.dst())))
                .findFirst();

        if (foundLink.isPresent()) {
            return foundLink.get();
        }
        return null;
    }
}
