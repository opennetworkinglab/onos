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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.packet.IpPrefix;

/**
 * Provides information of IP address prefix in the IGP topology and a router advertises
 * this to each of its BGP nexthop.
 */
public class IpReachability {
    private final IpPrefix ipPrefix;

    /**
     * Constructor to initialize IP prefix.
     *
     * @param ipPrefix IP address prefix
     */
    public IpReachability(IpPrefix ipPrefix) {
        this.ipPrefix = ipPrefix;
    }

    /**
     * Provides IP Address prefix reachability.
     *
     * @return IP Address prefix
     */
    public IpPrefix ipPrefix() {
        return ipPrefix;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipPrefix);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IpReachability) {
            IpReachability other = (IpReachability) obj;
            return Objects.equals(ipPrefix, other.ipPrefix);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("ipPrefix", ipPrefix)
                .toString();
    }
}