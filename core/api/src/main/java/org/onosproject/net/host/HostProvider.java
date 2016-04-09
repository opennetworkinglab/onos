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
package org.onosproject.net.host;

import org.onosproject.net.Host;
import org.onosproject.net.provider.Provider;

/**
 * Provider of information about hosts and their location on the network.
 */
public interface HostProvider extends Provider {

    /**
     * Triggers an asynchronous probe of the specified host, intended to
     * determine whether the host is present or not. An indirect result of this
     * should be invocation of {@link org.onosproject.net.host.HostProviderService#hostDetected}
     * or {@link org.onosproject.net.host.HostProviderService#hostVanished}
     * at some later point in time.
     *
     * @param host host to probe
     */
    void triggerProbe(Host host);

}
