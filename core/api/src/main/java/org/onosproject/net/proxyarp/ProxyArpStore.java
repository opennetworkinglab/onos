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
package org.onosproject.net.proxyarp;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;

import java.nio.ByteBuffer;

/**
 * State distribution mechanism for the proxy ARP service.
 *
 * @deprecated in Hummingbird release. This is no longer necessary as there are
 * other solutions for the problem this was solving.
 */
@Deprecated
public interface ProxyArpStore {

    /**
     * Forwards an ARP or neighbor solicitation request to its destination.
     * Floods at the edg the request if the destination is not known.
     *
     * @param outPort the port the request was received on
     * @param subject subject host
     * @param packet  an ethernet frame containing an ARP or neighbor
     *                solicitation request
     */
    void forward(ConnectPoint outPort, Host subject, ByteBuffer packet);

    /**
     * Associates the specified delegate with the store.
     *
     * @param delegate store delegate
     */
    void setDelegate(ProxyArpStoreDelegate delegate);
}
