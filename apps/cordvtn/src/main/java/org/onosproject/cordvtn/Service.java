/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Objects;

public final class Service {

    enum ServiceType {
        PRIVATE,
        PRIVATE_DIRECT,
        PRIVATE_INDIRECT,
        PUBLIC_DIRECT,
        PUBLIC_INDIRECT
    }

    private final ServiceId serviceId;
    private final String networkId;
    private final ServiceType serviceType;
    private final IpPrefix serviceIpRange;
    private final IpAddress serviceIp;

    /**
     * Default constructor.
     *
     * @param serviceId service id
     * @param networkId OpenStack Neutron network id
     * @param serviceType service type
     * @param serviceIpRange service ip range
     * @param serviceIp service ip
     */
    public Service(ServiceId serviceId, String networkId, ServiceType serviceType,
                   IpPrefix serviceIpRange, IpAddress serviceIp) {
        this.serviceId = serviceId;
        this.networkId = networkId;
        this.serviceType = serviceType;
        this.serviceIpRange = serviceIpRange;
        this.serviceIp = serviceIp;
    }

    /**
     * Returns service ID.
     *
     * @return service id
     */
    public ServiceId serviceId() {
        return serviceId;
    }

    /**
     * Returns OpenStack Neutron network ID of this service.
     *
     * @return network id
     */
    public String networkId() {
        return networkId;
    }

    /**
     * Returns service type.
     *
     * @return service type
     */
    public ServiceType serviceType() {
        return serviceType;
    }

    /**
     * Returns service IP range.
     *
     * @return CIDR
     */
    public IpPrefix serviceIpRange() {
        return serviceIpRange;
    }

    /**
     * Returns service IP address.
     *
     * @return ip address
     */
    public IpAddress serviceIp() {
        return serviceIp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Service)) {
            return false;
        }
        final Service other = (Service) obj;
        return Objects.equals(this.serviceId, other.serviceId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("serviceId", serviceId)
                .add("networkId", networkId)
                .add("serviceType", serviceType)
                .add("serviceIpRange", serviceIpRange)
                .add("serviceIp", serviceIp)
                .toString();
    }
}
