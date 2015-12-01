/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.bgpio.protocol;

import java.util.List;

import org.onlab.packet.IpPrefix;
import org.onosproject.bgpio.protocol.ver4.BgpPathAttributes;

/**
 * Abstraction of an entity providing BGP Update Message.
 */
public interface BgpUpdateMsg extends BgpMessage {
    /**
     * Returns path attributes in BGP Update Message.
     *
     * @return path attributes in BGP Update Message
     */
    BgpPathAttributes bgpPathAttributes();

    /**
     * Returns withdrawn Routes.
     *
     * @return withdrawn Routes
     */
    List<IpPrefix> withdrawnRoutes();

    /**
     * Returns NLRI list of prefix.
     *
     * @return NLRI list of prefix
     */
    List<IpPrefix> nlri();
}
