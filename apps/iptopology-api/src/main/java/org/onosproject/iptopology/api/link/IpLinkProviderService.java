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


import org.onosproject.iptopology.api.TerminationPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderService;

/**
 * Means for injecting ip link information into the core.
 */
public interface IpLinkProviderService extends ProviderService<IpLinkProvider> {

    /**
     * Signals that an ip link is added or updated with IP topology information.
     *
     * @param linkDescription ip link information
     */
    void addOrUpdateIpLink(IpLinkDescription linkDescription);

    /**
     * Signals that an ip link has disappeared.
     *
     * @param linkDescription ip link information
     */
    void removeIpLink(IpLinkDescription linkDescription);

    /**
     * Signals that ip links associated with the specified
     * termination point have vanished.
     *
     * @param terminationPoint termination point
     */
    void removeIpLink(TerminationPoint terminationPoint);

    /**
     * Signals that ip links associated with the specified
     * device have vanished.
     *
     * @param deviceId device identifier
     */
    void removeIpLink(DeviceId deviceId);
}
