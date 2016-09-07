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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.xosclient.api.VtnServiceApi.NetworkType;
import org.onosproject.xosclient.api.VtnServiceApi.ServiceType;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
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

    private VtnService(VtnServiceId id,
                       String name,
                       ServiceType serviceType,
                       NetworkType networkType,
                       long vni,
                       IpPrefix subnet,
                       IpAddress serviceIp,
                       Set<VtnServiceId> providerServices,
                       Set<VtnServiceId> tenantServices) {
        this.id = id;
        this.name = name;
        this.serviceType = serviceType;
        this.networkType = networkType;
        this.vni = vni;
        this.subnet = subnet;
        this.serviceIp = serviceIp;
        this.providerServices = providerServices;
        this.tenantServices = tenantServices;
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

    /**
     * Returns a new builder instance.
     *
     * @return new builder
     */
    public static final Builder build() {
        return new Builder();
    }

    /**
     * Builder of VTN service entities.
     */
    public static final class Builder {

        private VtnServiceId id;
        private String name;
        private ServiceType serviceType;
        private NetworkType networkType;
        private long vni = -1;
        private IpPrefix subnet;
        private IpAddress serviceIp;
        private Set<VtnServiceId> providerServices;
        private Set<VtnServiceId> tenantServices;

        private Builder() {
        }

        /**
         * Builds an immutable VTN service.
         *
         * @return vtn service instance
         */
        public VtnService build() {
            checkNotNull(id, "VTN service ID cannot be null");
            checkArgument(!Strings.isNullOrEmpty(name), "VTN service name cannot be null");
            checkNotNull(serviceType, "VTN service type cannot be null");
            checkNotNull(networkType, "VTN network type cannot be null");
            checkArgument(vni > 0, "VTN network VNI is not set");
            checkNotNull(subnet, "VTN subnet cannot be null");
            checkNotNull(serviceIp, "VTN service IP cannot be null");

            providerServices = providerServices == null ? ImmutableSet.of() : providerServices;
            tenantServices = tenantServices == null ? ImmutableSet.of() : tenantServices;

            return new VtnService(id,
                                  name,
                                  serviceType,
                                  networkType,
                                  vni,
                                  subnet,
                                  serviceIp,
                                  providerServices,
                                  tenantServices);
        }

        /**
         * Returns VTN service builder with the supplied service ID.
         *
         * @param id service identifier
         * @return vtn service builder
         */
        public Builder id(VtnServiceId id) {
            this.id = id;
            return this;
        }

        /**
         * Returns VTN service builder with the supplied service name.
         *
         * @param name service name
         * @return vtn service builder
         */
        public Builder name(String name) {
            if (Strings.isNullOrEmpty(name)) {
                final String msg = "VTN service name cannot be null";
                throw new IllegalArgumentException(msg);
            }
            this.name = name;
            return this;
        }

        /**
         * Returns VTN service builder with the supplied service type.
         *
         * @param serviceType service type
         * @return vtn service builder
         */
        public Builder serviceType(ServiceType serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        /**
         * Returns VTN service builder with the supplied network type.
         *
         * @param networkType network type
         * @return vtn service builder
         */
        public Builder networkType(NetworkType networkType) {
            this.networkType = networkType;
            return this;
        }

        /**
         * Returns VTN service builder with the supplied VNI.
         *
         * @param vni vni of the service network
         * @return vtn service builder
         */
        public Builder vni(long vni) {
            if (vni < 0 || vni > 16777215) {
                final String msg = "VNI " + vni + " is out of range";
                throw new IllegalArgumentException(msg);
            }
            this.vni = vni;
            return this;
        }

        /**
         * Returns VTN service builder with the supplied VNI.
         *
         * @param vni vni of the service network as a string
         * @return vtn service builder
         */
        public Builder vni(String vni) {
            try {
                return vni(Long.parseLong(vni));
            } catch (NumberFormatException | NullPointerException e) {
                final String msg = "Malformed number string " + vni +
                        " for VTN network VNI";
                throw new IllegalArgumentException(msg);
            }
        }

        /**
         * Returns VTN service builder with the supplied subnet.
         *
         * @param subnet subnet of the service network
         * @return vtn service builder
         */
        public Builder subnet(IpPrefix subnet) {
            if (subnet == null) {
                final String msg = "VTN service subnet is null";
                throw new IllegalArgumentException(msg);
            }
            this.subnet = subnet;
            return this;
        }

        /**
         * Returns VTN service builder with the supplied subnet.
         *
         * @param subnet subnet of the service network as a string
         * @return vtn service builder
         */
        public Builder subnet(String subnet) {
            try {
                return subnet(IpPrefix.valueOf(subnet));
            } catch (IllegalArgumentException | NullPointerException e) {
                final String msg = "Malformed IP prefix string " + subnet +
                        " for VTN service subnet";
                throw new IllegalArgumentException(msg);
            }
        }

        /**
         * Returns VTN service builder with the supplied service IP address.
         *
         * @param serviceIp service ip address
         * @return vtn service builder
         */
        public Builder serviceIp(IpAddress serviceIp) {
            if (serviceIp == null) {
                final String msg = "VTN service IP cannot be null";
                throw new IllegalArgumentException(msg);
            }
            this.serviceIp = serviceIp;
            return this;
        }

        /**
         * Returns VTN service builder with the supplied service IP address.
         *
         * @param serviceIp service ip address as a string
         * @return vtn service builder
         */
        public Builder serviceIp(String serviceIp) {
            try {
                return serviceIp(IpAddress.valueOf(serviceIp));
            } catch (IllegalArgumentException | NullPointerException e) {
                final String msg = "Malformed IP address string " + serviceIp +
                        " for VTN service IP address";
                throw new IllegalArgumentException(msg);
            }
        }

        /**
         * Returns VTN service builder with the supplied provider services.
         *
         * @param pServices provider services
         * @return vtn service builder
         */
        public Builder providerServices(Set<VtnServiceId> pServices) {
            if (pServices == null) {
                final String msg = "Provider services cannot be null";
                throw new IllegalArgumentException(msg);
            }
            this.providerServices = pServices;
            return this;
        }

        /**
         * Returns VTN service builder with the supplied tenant services.
         *
         * @param tServices tenant services
         * @return vtn service builder
         */
        public Builder tenantServices(Set<VtnServiceId> tServices) {
            if (tServices == null) {
                final String msg = "Tenant services cannot be null";
                throw new IllegalArgumentException(msg);
            }
            this.tenantServices = tServices;
            return this;
        }
    }
}
