/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

/**
 * Represent the object for the xml element of l3vpnInstances.
 */
public class NetconfL3vpnInstances {
    private final List<NetconfL3vpnInstance> l3vpninstances;

    /**
     * NetconfL3vpninstances constructor.
     *
     * @param l3vpninstances List of NetconfL3vpnInstance
     */
    public NetconfL3vpnInstances(List<NetconfL3vpnInstance> l3vpninstances) {
        checkNotNull(l3vpninstances, "l3vpninstances cannot be null");
        this.l3vpninstances = l3vpninstances;
    }

    /**
     * Returns l3vpninstances.
     *
     * @return l3vpninstances
     */
    public List<NetconfL3vpnInstance> l3vpninstances() {
        return l3vpninstances;
    }

    @Override
    public int hashCode() {
        return Objects.hash(l3vpninstances);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfL3vpnInstances) {
            final NetconfL3vpnInstances other = (NetconfL3vpnInstances) obj;
            return Objects.equals(this.l3vpninstances, other.l3vpninstances);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("l3vpninstances", l3vpninstances)
                .toString();
    }
}
