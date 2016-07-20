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
package org.onosproject.iptopology.api.link;

import org.onosproject.iptopology.api.IpLink;
import org.onosproject.iptopology.api.TerminationPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of ip links; not intended for direct use.
 */
public interface IpLinkStore extends Store<IpLinkEvent, IpLinkStoreDelegate> {

    /**
     * Returns the number of ip links in the store.
     *
     * @return number of ip links
     */
    int getIpLinkCount();

    /**
     * Returns an iterable collection of all ip links in the inventory.
     *
     * @return collection of all ip links
     */
    Iterable<IpLink> getIpLinks();

    /**
     * Returns all ip links egressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of ip device links
     */
    Set<IpLink> getIpDeviceEgressLinks(DeviceId deviceId);

    /**
     * Returns all ip links ingressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of ip device links
     */
    Set<IpLink> getIpDeviceIngressLinks(DeviceId deviceId);

    /**
     * Returns the ip link between the two termination points.
     *
     * @param src source termination point
     * @param dst destination termination point
     * @return ip link or null if one not found between the termination points
     */
    IpLink getIpLink(TerminationPoint src, TerminationPoint dst);

    /**
     * Returns all ip links egressing from the specified termination point.
     *
     * @param src source termination point
     * @return set of termination point ip links
     */
    Set<IpLink> getEgressIpLinks(TerminationPoint src);

    /**
     * Returns all ip links ingressing to the specified termination point.
     *
     * @param dst destination termination point
     * @return set of termination point ip links
     */
    Set<IpLink> getIngressIpLinks(TerminationPoint dst);

    /**
     * Creates a new ip link, or updates an existing one, based on the given
     * information.
     *
     * @param providerId      provider identity
     * @param linkDescription ip link description
     * @return create or update ip link event, or null if no change resulted
     */
    IpLinkEvent createOrUpdateIpLink(ProviderId providerId,
                                        IpLinkDescription linkDescription);

    /**
     * Removes ip link, based on the specified information.
     *
     * @param src ip link source
     * @param dst ip link destination
     * @return remove or update ip link event, or null if no change resulted
     */
    IpLinkEvent removeOrDownIpLink(TerminationPoint src, TerminationPoint dst);

    /**
     * Removes ip link based on the specified information.
     *
     * @param src ip link source
     * @param dst ip link destination
     * @return remove ip link event, or null if no change resulted
     */
    IpLinkEvent removeIpLink(TerminationPoint src, TerminationPoint dst);

}
