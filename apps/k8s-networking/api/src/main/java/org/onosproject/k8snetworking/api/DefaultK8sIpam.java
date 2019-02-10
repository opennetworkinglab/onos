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
package org.onosproject.k8snetworking.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.IpAddress;

/**
 * Default implementation of kubernetes IPAM instance.
 */
public final class DefaultK8sIpam implements K8sIpam {

    private final String ipamId;
    private final IpAddress ipAddress;
    private final String networkId;

    // private constructor not intended for external invocation
    public DefaultK8sIpam(String ipamId, IpAddress ipAddress, String networkId) {
        this.ipamId = ipamId;
        this.ipAddress = ipAddress;
        this.networkId = networkId;
    }

    @Override
    public String ipamId() {
        return ipamId;
    }

    @Override
    public IpAddress ipAddress() {
        return ipAddress;
    }

    @Override
    public String networkId() {
        return networkId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultK8sIpam that = (DefaultK8sIpam) o;
        return Objects.equal(ipamId, that.ipamId) &&
                Objects.equal(ipAddress, that.ipAddress) &&
                Objects.equal(networkId, that.networkId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ipamId, ipAddress, networkId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("ipamId", ipamId)
                .add("ipAddress", ipAddress)
                .add("networkId", networkId)
                .toString();
    }
}
