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
package org.onosproject.iptopology.api.device;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onosproject.iptopology.api.InterfaceIdentifier;
import org.onosproject.net.Description;


/**
 * Information about an interface.
 */
public interface InterfaceDescription extends Description {

    /**
     * Returns the IPv4 Address of an interface.
     *
     * @return ipv4 address
     */
    Ip4Address ipv4Address();

    /**
     * Returns the IPv6 Address of an interface.
     *
     * @return ipv6 address
     */
    Ip6Address ipv6Address();


    /**
     * Returns the interface id of the interface.
     *
     * @return interface identifier
     */
    InterfaceIdentifier intfId();

}
