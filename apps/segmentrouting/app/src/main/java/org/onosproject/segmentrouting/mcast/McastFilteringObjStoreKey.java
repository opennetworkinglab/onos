/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.segmentrouting.mcast;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Key of multicast filtering objective store.
 */
public class McastFilteringObjStoreKey {

    private final ConnectPoint ingressCP;
    private final VlanId vlanId;
    private final boolean isIpv4;

    /**
     * Constructs the key of multicast filtering objective store.
     *
     * @param ingressCP ingress ConnectPoint
     * @param vlanId vlan id
     * @param isIpv4 is Ipv4
     */
    public McastFilteringObjStoreKey(ConnectPoint ingressCP, VlanId vlanId, boolean isIpv4) {
        checkNotNull(ingressCP, "connectpoint cannot be null");
        checkNotNull(vlanId, "vlanid cannot be null");
        this.ingressCP = ingressCP;
        this.vlanId = vlanId;
        this.isIpv4 = isIpv4;
    }

    // Constructor for serialization
    private McastFilteringObjStoreKey() {
        this.ingressCP = null;
        this.vlanId = null;
        this.isIpv4 = false;
    }


    /**
     * Returns the connect point.
     *
     * @return ingress connectpoint
     */
    public ConnectPoint ingressCP() {
        return ingressCP;
    }

    /**
     * Returns whether the filtering is for ipv4 mcast.
     *
     * @return isIpv4
     */
    public boolean isIpv4() {
        return isIpv4;
    }

    /**
     * Returns the vlan ID of this key.
     *
     * @return vlan ID
     */
    public VlanId vlanId() {
        return vlanId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof McastFilteringObjStoreKey)) {
            return false;
        }
        McastFilteringObjStoreKey that =
                (McastFilteringObjStoreKey) o;
        return (Objects.equals(this.ingressCP, that.ingressCP) &&
                Objects.equals(this.isIpv4, that.isIpv4) &&
                Objects.equals(this.vlanId, that.vlanId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingressCP, vlanId, isIpv4);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("ingressCP", ingressCP)
                .add("isIpv4", isIpv4)
                .add("vlanId", vlanId)
                .toString();
    }
}
