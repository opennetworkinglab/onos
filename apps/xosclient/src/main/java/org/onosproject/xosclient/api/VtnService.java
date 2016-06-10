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
package org.onosproject.xosclient.api;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.xosclient.api.VtnServiceApi.NetworkType;
import org.onosproject.xosclient.api.VtnServiceApi.ServiceType;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of CORD VTN controlled network service.
 */
public final class VtnService {

    private final VtnServiceId id;
    private final String name;
    private final ServiceType serviceType;
    private final NetworkType networkType;
    private final long vni;
    private final IpPrefix subnet;
    private final IpAddress serviceIp;
    private final Set<VtnServiceId> providerServices;
    private final Set<VtnServiceId> tenantServices;

    /**
     * Creates a new VTN service with the specified entities.
     *
     * @param id service id
     * @param name user friendly name
     * @param serviceType service type
     * @param networkType network type
     * @param vni vni of this service network
     * @param subnet service network subnet range
     * @param serviceIp service ip for indirect service access
     * @param providerServices provider services
     * @param tenantServices tenant services
     */
    public VtnService(VtnServiceId id,
                      String name,
                      ServiceType serviceType,
                      NetworkType networkType,
                      long vni,
                      IpPrefix subnet,
                      IpAddress serviceIp,
                      Set<VtnServiceId> providerServices,
                      Set<VtnServiceId> tenantServices) {
        this.id = checkNotNull(id);
        this.name = name;
        this.serviceType = serviceType;
        this.networkType = networkType;
        this.vni = vni;
        this.subnet = checkNotNull(subnet);
        this.serviceIp = checkNotNull(serviceIp);
        this.providerServices = providerServices == null ? Sets.newHashSet() : providerServices;
        this.tenantServices = tenantServices == null ? Sets.newHashSet() : tenantServices;
    }

    /**
     * Returns service ID.
     *
     * @return service id
     */
    public VtnServiceId id() {
        return id;
    }

    /**
     * Returns service name.
     *
     * @return name
     */
    public String name() {
        return name;
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
     * Returns segmentation ID of this service.
     *
     * @return segmentation id
     */
    public long vni() {
        return vni;
    }

    /**
     * Returns network type.
     *
     * @return network type
     */
    public NetworkType networkType() {
        return networkType;
    }

    /**
     * Returns service IP range.
     *
     * @return subnet cidr
     */
    public IpPrefix subnet() {
        return subnet;
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
     * Returns provider service IDs.
     *
     * @return list of provider service id
     */
    public Set<VtnServiceId> providerServices() {
        return providerServices;
    }

    /**
     * Returns tenant service IDs.
     *
     * @return list of tenant service id
     */
    public Set<VtnServiceId> tenantServices() {
        return tenantServices;
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
        if (!(obj instanceof VtnService)) {
            return false;
        }
        final VtnService other = (VtnService) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("serviceType", serviceType)
                .add("networkType", networkType)
                .add("vni", vni)
                .add("subnet", subnet)
                .add("serviceIp", serviceIp)
                .add("providerServices", providerServices)
                .add("tenantServices", tenantServices)
                .toString();
    }
}
