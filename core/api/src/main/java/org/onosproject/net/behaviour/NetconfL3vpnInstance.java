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
 * Represent the object for the xml element of l3vpnInstance.
 */
public class NetconfL3vpnInstance {
    private final String operation;
    private final String vrfName;
    private final NetconfVpnInstAFs vpnInstAFs;
    private final NetconfL3vpnIfs l3vpnIfs;

    /**
     * NetconfL3vpnInstance constructor.
     *
     * @param operation operation
     * @param vrfName vrf name
     * @param vpnInstAFs NetconfVpnInstAFs
     * @param l3vpnIfs NetconfL3vpnIfs
     */
    public NetconfL3vpnInstance(String operation, String vrfName,
                                NetconfVpnInstAFs vpnInstAFs, NetconfL3vpnIfs l3vpnIfs) {
        checkNotNull(operation, "operation cannot be null");
        checkNotNull(vrfName, "vrfName cannot be null");
        checkNotNull(vpnInstAFs, "vpnInstAFs cannot be null");
        checkNotNull(l3vpnIfs, "l3vpnIfs cannot be null");
        this.operation = operation;
        this.vrfName = vrfName;
        this.vpnInstAFs = vpnInstAFs;
        this.l3vpnIfs = l3vpnIfs;
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
     * Returns vrfName.
     *
     * @return vrfName
     */
    public String vrfName() {
        return vrfName;
    }

    /**
     * Returns vpnInstAFs.
     *
     * @return vpnInstAFs
     */
    public NetconfVpnInstAFs vpnInstAFs() {
        return vpnInstAFs;
    }

    /**
     * Returns l3vpnIfs.
     *
     * @return l3vpnIfs
     */
    public NetconfL3vpnIfs l3vpnIfs() {
        return l3vpnIfs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, vrfName, vpnInstAFs, l3vpnIfs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfL3vpnInstance) {
            final NetconfL3vpnInstance other = (NetconfL3vpnInstance) obj;
            return Objects.equals(this.operation, other.operation)
                    && Objects.equals(this.vrfName, other.vrfName)
                    && Objects.equals(this.vpnInstAFs, other.vpnInstAFs)
                    && Objects.equals(this.l3vpnIfs, other.l3vpnIfs);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("operation", operation)
                .add("vrfName", vrfName).add("vpnInstAFs", vpnInstAFs)
                .add("l3vpnIfs", l3vpnIfs).toString();
    }
}
