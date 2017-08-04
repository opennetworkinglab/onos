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
package org.onosproject.net.link;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of infrastructure links; not intended for direct use.
 */
public interface LinkStore extends Store<LinkEvent, LinkStoreDelegate> {

    /**
     * Returns the number of links in the store.
     *
     * @return number of links
     */
    int getLinkCount();

    /**
     * Returns an iterable collection of all links in the inventory.
     *
     * @return collection of all links
     */
    Iterable<Link> getLinks();

    /**
     * Returns all links egressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device links
     */
    Set<Link> getDeviceEgressLinks(DeviceId deviceId);

    /**
     * Returns all links ingressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device links
     */
    Set<Link> getDeviceIngressLinks(DeviceId deviceId);

    /**
     * Returns the link between the two end-points.
     *
     * @param src source connection point
     * @param dst destination connection point
     * @return link or null if one not found between the end-points
     */
    Link getLink(ConnectPoint src, ConnectPoint dst);

    /**
     * Returns all links egressing from the specified connection point.
     *
     * @param src source connection point
     * @return set of connection point links
     */
    Set<Link> getEgressLinks(ConnectPoint src);

    /**
     * Returns all links ingressing to the specified connection point.
     *
     * @param dst destination connection point
     * @return set of connection point links
     */
    Set<Link> getIngressLinks(ConnectPoint dst);

    /**
     * Creates a new link, or updates an existing one, based on the given
     * information.
     *
     * @param providerId      provider identity
     * @param linkDescription link description
     * @return create or update link event, or null if no change resulted
     */
    LinkEvent createOrUpdateLink(ProviderId providerId,
                                        LinkDescription linkDescription);

    /**
     * Removes the link, or marks it as inactive if the link is durable,
     * based on the specified information.
     *
     * @param src link source
     * @param dst link destination
     * @return remove or update link event, or null if no change resulted
     */
    LinkEvent removeOrDownLink(ConnectPoint src, ConnectPoint dst);

    /**
     * Removes the link based on the specified information.
     *
     * @param src link source
     * @param dst link destination
     * @return remove link event, or null if no change resulted
     */
    LinkEvent removeLink(ConnectPoint src, ConnectPoint dst);


}
