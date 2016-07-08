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
package org.onosproject.ne;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * VpnAc is vpn access.
 */
public final class VpnAc {
    private final String netVpnId;
    private final String acId;
    private final String acName;
    private final String ipAddress;
    private final Integer subNetMask;

    /**
     * VpnAc constructor.
     * 
     * @param columnName the column name
     * @param obj the data of the column
     */
    public VpnAc(String netVpnId, String acId, String acName, String ipAddress,
                 Integer subNetMask) {
        checkNotNull(netVpnId, "netVpnId cannot be null");
        checkNotNull(acId, "acId cannot be null");
        checkNotNull(acName, "acName cannot be null");
        checkNotNull(ipAddress, "ipAddress cannot be null");
        checkNotNull(subNetMask, "subNetMask cannot be null");
        this.netVpnId = netVpnId;
        this.acId = acId;
        this.acName = acName;
        this.ipAddress = ipAddress;
        this.subNetMask = subNetMask;
    }

    /**
     * Returns netVpnId.
     * 
     * @return netVpnId
     */
    public String netVpnId() {
        return netVpnId;
    }

    /**
     * Returns acId.
     * 
     * @return acId
     */
    public String acId() {
        return acId;
    }

    /**
     * Returns acName.
     * 
     * @return acName
     */
    public String acName() {
        return acName;
    }

    /**
     * Returns ipAddress.
     * 
     * @return ipAddress
     */
    public String ipAddress() {
        return ipAddress;
    }

    /**
     * Returns subNetMask.
     * 
     * @return subNetMask
     */
    public Integer subNetMask() {
        return subNetMask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(netVpnId, acId, acName, ipAddress, subNetMask);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VpnAc) {
            final VpnAc other = (VpnAc) obj;
            return Objects.equals(this.netVpnId, other.netVpnId)
                    && Objects.equals(this.acId, other.acId)
                    && Objects.equals(this.acName, other.acName)
                    && Objects.equals(this.ipAddress, other.ipAddress)
                    && Objects.equals(this.subNetMask, other.subNetMask);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("netVpnId", netVpnId).add("acId", acId)
                .add("acName", acName).add("ipAddress", ipAddress)
                .add("subNetMask", subNetMask).toString();
    }
}
