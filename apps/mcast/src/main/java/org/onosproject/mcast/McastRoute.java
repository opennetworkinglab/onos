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
package org.onosproject.mcast;

import org.onlab.packet.IpPrefix;

/**
 * An entity representing a multicast route consisting of a source
 * and a multicast group address.
 */
public class McastRoute {

    public final IpPrefix source;
    public final IpPrefix group;

    public McastRoute(IpPrefix source, IpPrefix group) {
        this.source = source;
        this.group = group;
    }

    /**
     * Fetches the source address of this route.
     *
     * @return an ip address
     */
    public IpPrefix source() {
        return source;
    }

    /**
     * Fetches the group address of this route.
     *
     * @return an ip address
     */
    public IpPrefix group() {
        return group;
    }

}
