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

import java.util.Objects;

/**
 * Represent the object for the xml element of l3vpnIf.
 */
public class NetconfL3vpnIf {
    private final String operation;
    private final String ifName;
    private final String ipv4Addr;
    private final String subnetMask;

    /**
     * NetconfL3vpnIf constructor.
     *
     * @param operation operation
     * @param ifName interface name
     * @param ipv4Addr ipv4Addr
     * @param subnetMask subnetMask
     */
    public NetconfL3vpnIf(String operation, String ifName, String ipv4Addr,
                          String subnetMask) {
        checkNotNull(operation, "operation cannot be null");
        checkNotNull(ifName, "ifName cannot be null");
        checkNotNull(ipv4Addr, "ipv4Addr cannot be null");
        checkNotNull(subnetMask, "subnetMask cannot be null");
        this.operation = operation;
        this.ifName = ifName;
        this.ipv4Addr = ipv4Addr;
        this.subnetMask = subnetMask;
    }

    /**
     * Returns operation.
     *
     * @return operation
     */
    public String operation() {
        return operation;
    }

    /**
     * Returns ifName.
     *
     * @return ifName
     */
    public String ifName() {
        return ifName;
    }

    /**
     * Returns ipv4Addr.
     *
     * @return ipv4Addr
     */
    public String ipv4Addr() {
        return ipv4Addr;
    }

    /**
     * Returns subnetMask.
     *
     * @return subnetMask
     */
    public String subnetMask() {
        return subnetMask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, ifName, ipv4Addr, subnetMask);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfL3vpnIf) {
            final NetconfL3vpnIf other = (NetconfL3vpnIf) obj;
            return Objects.equals(this.operation, other.operation)
                    && Objects.equals(this.ifName, other.ifName)
                    && Objects.equals(this.ipv4Addr, other.ipv4Addr)
                    && Objects.equals(this.subnetMask, other.subnetMask);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("operation", operation)
                .add("ifName", ifName).add("ipv4Addr", ipv4Addr)
                .add("subnetMask", subnetMask).toString();
    }
}
