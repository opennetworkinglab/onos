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
package org.onosproject.cordvtn.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.Host;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CordService {

    public enum ServiceType {
        PRIVATE,
        PUBLIC,
        MANAGEMENT
    }

    private final CordServiceId id;
    private final long segmentationId;
    private final ServiceType serviceType;
    private final IpPrefix serviceIpRange;
    private final IpAddress serviceIp;
    private final Map<Host, IpAddress> hosts;
    private final Set<CordServiceId> tenantServices;
    private final Set<CordServiceId> providerServices;

    /**
     * Default constructor.
     *
     * @param osNet OpenStack network
     * @param osSubnet OpenStack subnet
     * @param hosts host and tunnel ip map
     * @param tenantServices list of tenant service ids
     * @param providerServices list of provider service ids
     */
    public CordService(Network osNet, Subnet osSubnet,
                       Map<Host, IpAddress> hosts, Set<CordServiceId> tenantServices,
                       Set<CordServiceId> providerServices) {
        this.id = CordServiceId.of(osNet.getId());
        this.segmentationId = Long.parseLong(osNet.getProviderSegID());
        this.serviceType = getServiceType(osNet.getName());
        this.serviceIpRange = IpPrefix.valueOf(osSubnet.getCidr());
        this.serviceIp = IpAddress.valueOf(osSubnet.getGateway());
        this.hosts = hosts;
        this.tenantServices = tenantServices;
        this.providerServices = providerServices;
    }

    /**
     * Returns service ID.
     *
     * @return service id
     */
    public CordServiceId id() {
        return id;
    }

    /**
     * Returns segmentation ID of this service.
     *
     * @return segmentation id
     */
    public long segmentationId() {
        return segmentationId;
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

    /**
     * Returns hosts associated with this service.
     *
     * @return list of hosts
     */
    public Map<Host, IpAddress> hosts() {
        return hosts;
    }

    /**
     * Returns tenant service IDs.
     *
     * @return list of tenant service id
     */
    public Set<CordServiceId> tenantServices() {
        return tenantServices;
    }

    /**
     * Returns provider service IDs.
     *
     * @return list of provider service id
     */
    public Set<CordServiceId> providerServices() {
        return providerServices;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CordService)) {
            return false;
        }
        final CordService other = (CordService) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("segmentationId", segmentationId)
                .add("serviceType", serviceType)
                .add("serviceIpRange", serviceIpRange)
                .add("serviceIp", serviceIp)
                .add("tenantServices", tenantServices)
                .add("providerServices", providerServices)
                .toString();
    }

    /**
     * Returns network type from network name.
     * It assumes that network name contains network type.
     *
     * @param netName network name
     * @return network type, or PRIVATE if it doesn't match any type
     */
    private ServiceType getServiceType(String netName) {
        checkNotNull(netName);

        String name = netName.toUpperCase();
        if (name.contains(ServiceType.PUBLIC.toString())) {
            return ServiceType.PUBLIC;
        } else if (name.contains(ServiceType.MANAGEMENT.toString())) {
            return ServiceType.MANAGEMENT;
        } else {
            return ServiceType.PRIVATE;
        }
    }
}
