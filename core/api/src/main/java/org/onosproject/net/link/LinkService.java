/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.util.Set;

import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;

/**
 * Service for interacting with the inventory of infrastructure links.
 */
public interface LinkService
    extends ListenerService<LinkEvent, LinkListener> {

    /**
     * Returns the count of all known infrastructure links.
     *
     * @return number of infrastructure links
     */
    int getLinkCount();

    /**
     * Returns a collection of all known infrastructure links.
     *
     * @return all infrastructure links
     */
    Iterable<Link> getLinks();

    /**
     * Returns a collection of all active infrastructure links.
     *
     * @return all infrastructure links
     */
    Iterable<Link> getActiveLinks();

    /**
     * Returns set of all infrastructure links leading to and from the
     * specified device.
     *
     * @param deviceId device identifier
     * @return set of device links
     */
    Set<Link> getDeviceLinks(DeviceId deviceId);

    /**
     * Returns set of all infrastructure links leading from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device egress links
     */
    Set<Link> getDeviceEgressLinks(DeviceId deviceId);

    /**
     * Returns set of all infrastructure links leading to the specified device.
     *
     * @param deviceId device identifier
     * @return set of device ingress links
     */
    Set<Link> getDeviceIngressLinks(DeviceId deviceId);

    /**
     * Returns set of all infrastructure links leading to and from the
     * specified connection point.
     *
     * @param connectPoint connection point
     * @return set of links
     */
    Set<Link> getLinks(ConnectPoint connectPoint);

    /**
     * Returns set of all infrastructure links leading from the specified
     * connection point.
     *
     * @param connectPoint connection point
     * @return set of device egress links
     */
    Set<Link> getEgressLinks(ConnectPoint connectPoint);

    /**
     * Returns set of all infrastructure links leading to the specified
     * connection point.
     *
     * @param connectPoint connection point
     * @return set of device ingress links
     */
    Set<Link> getIngressLinks(ConnectPoint connectPoint);

    // FIXME: I don't think this makes sense; discuss and remove or adjust return
    // to be a Set<Link> or add Link.Type parameter
    /**
     * Returns the infrastructure links between the specified source
     * and destination connection points.
     *
     * @param src source connection point
     * @param dst destination connection point
     * @return link from source to destination; null if none found
     */
    Link getLink(ConnectPoint src, ConnectPoint dst);

}
