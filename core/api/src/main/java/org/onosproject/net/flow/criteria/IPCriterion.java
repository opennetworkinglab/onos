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
package org.onosproject.net.flow.criteria;

import org.onlab.packet.IpPrefix;

import java.util.Objects;

/**
 * Implementation of IP address criterion.
 */
public final class IPCriterion implements Criterion {
    private final IpPrefix ip;
    private final Type type;

    private final Integer ipv4SuffixLength;

    /**
     * Constructor.
     *
     * @param ip the IP prefix to match. Could be either IPv4 or IPv6
     * @param type the match type. Should be one of the following:
     * Type.IPV4_SRC, Type.IPV4_DST, Type.IPV6_SRC, Type.IPV6_DST
     */
    IPCriterion(IpPrefix ip, Type type) {
        this.ip = ip;
        this.type = type;
        this.ipv4SuffixLength = null;
    }

    public IPCriterion(IpPrefix ip, Type type, int ipv4SuffixLength) {
        this.ip = ip;
        this.type = type;
        this.ipv4SuffixLength = new Integer(ipv4SuffixLength);
    }

    public Integer getIpv4SuffixLength() {
        return ipv4SuffixLength;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the IP prefix to match.
     *
     * @return the IP prefix to match
     */
    public IpPrefix ip() {
        return this.ip;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + ip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), ip);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPCriterion) {
            IPCriterion that = (IPCriterion) obj;
            return Objects.equals(ip, that.ip) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
