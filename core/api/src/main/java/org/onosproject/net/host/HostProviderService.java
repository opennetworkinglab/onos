/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.host;

import org.onosproject.net.HostId;
import org.onosproject.net.provider.ProviderService;

/**
 * Means of conveying host information to the core.
 */
public interface HostProviderService extends ProviderService<HostProvider> {

    /**
     * Notifies the core when a host has been detected on a network along with
     * information that identifies the host location.
     *
     * @param hostId          id of the host that been detected
     * @param hostDescription description of host and its location
     */
    void hostDetected(HostId hostId, HostDescription hostDescription);

    /**
     * Notifies the core when a host is no longer detected on a network.
     *
     * @param hostId id of the host that vanished
     */
    void hostVanished(HostId hostId);

}
