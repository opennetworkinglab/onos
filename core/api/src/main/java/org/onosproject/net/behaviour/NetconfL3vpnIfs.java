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
 * Represent the object for the xml element of l3vpnIfs.
 */
public class NetconfL3vpnIfs {
    private final List<NetconfL3vpnIf> l3vpnIfs;

    /**
     * NetconfL3vpnIfs constructor.
     * 
     * @param l3vpnIfs List of NetconfL3vpnIf
     */
    public NetconfL3vpnIfs(List<NetconfL3vpnIf> l3vpnIfs) {
        checkNotNull(l3vpnIfs, "l3vpnIfs cannot be null");
        this.l3vpnIfs = l3vpnIfs;
    }

    /**
     * Returns l3vpnIfs.
     * 
     * @return l3vpnIfs
     */
    public List<NetconfL3vpnIf> l3vpnIfs() {
        return l3vpnIfs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(l3vpnIfs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfL3vpnIfs) {
            final NetconfL3vpnIfs other = (NetconfL3vpnIfs) obj;
            return Objects.equals(this.l3vpnIfs, other.l3vpnIfs);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("l3vpnIfs", l3vpnIfs).toString();
    }
}
