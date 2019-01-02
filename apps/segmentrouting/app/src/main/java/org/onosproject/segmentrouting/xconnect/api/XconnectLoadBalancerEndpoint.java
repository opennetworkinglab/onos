/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.segmentrouting.xconnect.api;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a cross connect endpoint specified by load balancer.
 */
public final class XconnectLoadBalancerEndpoint extends XconnectEndpoint {
    private final int key;

    private XconnectLoadBalancerEndpoint(int key) {
        this.key = key;
    }

    /**
     * Returns load balancer key.
     *
     * @return load balancer key.
     */
    public int key() {
        return key;
    }

    /**
     * Returns an instance of XconnectLoadBalancerEndpoint with given load balancer key.
     *
     * @param key load balancer key
     * @return an instance of XconnectLoadBalancerEndpoint
     */
    public static XconnectLoadBalancerEndpoint of(int key) {
        return new XconnectLoadBalancerEndpoint(key);
    }

    /**
     * Gets XconnectLoadBalancerEndpoint from string.
     *
     * @param s string
     * @return XconnectLoadBalancerEndpoint
     */
    public static XconnectLoadBalancerEndpoint fromString(String s) {
        checkArgument(s.matches(LOAD_BALANCER_PATTERN), "String {} does not match {} format", s, LOAD_BALANCER_PATTERN);
        return new XconnectLoadBalancerEndpoint(Integer.valueOf(s.replaceFirst(LB_KEYWORD, "")));
    }

    @Override
    public Type type() {
        return Type.LOAD_BALANCER;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof XconnectLoadBalancerEndpoint) {
            final XconnectLoadBalancerEndpoint other = (XconnectLoadBalancerEndpoint) obj;
            return Objects.equals(this.key, other.key);
        }
        return false;
    }

    @Override
    public String toString() {
        return LB_KEYWORD + String.valueOf(key);
    }
}
