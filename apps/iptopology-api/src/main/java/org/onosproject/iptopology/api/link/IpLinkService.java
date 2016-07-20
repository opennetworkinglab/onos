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

import java.util.Set;

import org.onosproject.event.ListenerService;
import org.onosproject.iptopology.api.IpLink;
import org.onosproject.iptopology.api.TerminationPoint;
import org.onosproject.net.DeviceId;

/**
 * Service for interacting with the inventory of infrastructure links.
 */
public interface IpLinkService
    extends ListenerService<IpLinkEvent, IpLinkListener> {

    /**
     * Returns the count of all known ip links.
     *
     * @return number of ip links
     */
    int getIpLinkCount();

    /**
     * Returns a collection of all ip links.
     *
     * @return all ip links
     */
    Iterable<IpLink> getIpLinks();


    /**
     * Returns set of all ip links leading to and from the
     * specified ip device.
     *
     * @param deviceId device identifier
     * @return set of ip device links
     */
    Set<IpLink> getIpDeviceLinks(DeviceId deviceId);

    /**
     * Returns set of all ip links leading from the specified ip device.
     *
     * @param deviceId device identifier
     * @return set of ip device egress links
     */
    Set<IpLink> getIpDeviceEgressLinks(DeviceId deviceId);

    /**
     * Returns set of all ip links leading to the specified ip device.
     *
     * @param deviceId device identifier
     * @return set of ip device ingress links
     */
    Set<IpLink> getIpDeviceIngressLinks(DeviceId deviceId);

    /**
     * Returns set of all ip links leading to and from the
     * specified termination point.
     *
     * @param terminationPoint termination point
     * @return set of ip links
     */
    Set<IpLink> getIpLinks(TerminationPoint terminationPoint);

    /**
     * Returns set of all ip links leading from the specified
     * termination point.
     *
     * @param terminationPoint termination point
     * @return set of ip device egress links
     */
    Set<IpLink> getEgressIpLinks(TerminationPoint terminationPoint);

    /**
     * Returns set of all ip links leading to the specified
     * termination point.
     *
     * @param terminationPoint termination point
     * @return set of ip device ingress links
     */
    Set<IpLink> getIngressIpLinks(TerminationPoint terminationPoint);

    /**
     * Returns the ip links between the specified source
     * and destination termination points.
     *
     * @param src source termination point
     * @param dst destination termination point
     * @return ip link from source to destination; null if none found
     */
    IpLink getIpLink(TerminationPoint src, TerminationPoint dst);

}
