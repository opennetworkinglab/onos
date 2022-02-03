/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation class of kubevirt floating IP.
 */
public final class DefaultKubevirtFloatingIp implements KubevirtFloatingIp {

    private static final String NOT_NULL_MSG = "Floating IP % cannot be null";

    private final String id;
    private final String routerName;
    private final String podName;
    private final String vmName;
    private final String networkName;
    private final IpAddress floatingIp;
    private final IpAddress fixedIp;

    /**
     * A default constructor.
     *
     * @param id            floating IP identifier
     * @param routerName    router name
     * @param podName       POD name
     * @param vmName        VM name
     * @param networkName   network name
     * @param floatingIp    floating IP address
     * @param fixedIp       fixed IP address
     */
    public DefaultKubevirtFloatingIp(String id, String routerName, String podName, String vmName,
                                     String networkName, IpAddress floatingIp, IpAddress fixedIp) {
        this.id = id;
        this.routerName = routerName;
        this.podName = podName;
        this.vmName = vmName;
        this.networkName = networkName;
        this.floatingIp = floatingIp;
        this.fixedIp = fixedIp;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String routerName() {
        return routerName;
    }

    @Override
    public String networkName() {
        return networkName;
    }

    @Override
    public IpAddress floatingIp() {
        return floatingIp;
    }

    @Override
    public IpAddress fixedIp() {
        return fixedIp;
    }

    @Override
    public String vmName() {
        return vmName;
    }

    @Override
    public String podName() {
        return podName;
    }

    @Override
    public KubevirtFloatingIp updateFixedIp(IpAddress ip) {
        return DefaultKubevirtFloatingIp.builder()
                .id(id)
                .networkName(networkName)
                .routerName(routerName)
                .floatingIp(floatingIp)
                .fixedIp(ip)
                .vmName(vmName)
                .podName(podName)
                .build();
    }

    @Override
    public KubevirtFloatingIp updatePodName(String name) {
        return DefaultKubevirtFloatingIp.builder()
                .id(id)
                .networkName(networkName)
                .routerName(routerName)
                .floatingIp(floatingIp)
                .fixedIp(fixedIp)
                .vmName(vmName)
                .podName(name)
                .build();
    }

    @Override
    public KubevirtFloatingIp updateVmName(String name) {
        return DefaultKubevirtFloatingIp.builder()
                .id(id)
                .networkName(networkName)
                .routerName(routerName)
                .floatingIp(floatingIp)
                .fixedIp(fixedIp)
                .vmName(name)
                .podName(podName)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtFloatingIp that = (DefaultKubevirtFloatingIp) o;
        return id.equals(that.id) && routerName.equals(that.routerName) &&
                Objects.equals(podName, that.podName) &&
                networkName.equals(that.networkName) &&
                floatingIp.equals(that.floatingIp) &&
                Objects.equals(fixedIp, that.fixedIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, routerName, podName, floatingIp, fixedIp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("routerName", routerName)
                .add("podName", podName)
                .add("networkName", networkName)
                .add("floatingIp", floatingIp)
                .add("fixedIp", fixedIp)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt floating IP builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements KubevirtFloatingIp.Builder {

        private String id;
        private String routerName;
        private String podName;
        private String vmName;
        private String networkName;
        private IpAddress floatingIp;
        private IpAddress fixedIp;

        @Override
        public KubevirtFloatingIp build() {
            checkArgument(id != null, NOT_NULL_MSG, "id");
            checkArgument(networkName != null, NOT_NULL_MSG, "networkName");
            checkArgument(routerName != null, NOT_NULL_MSG, "routerName");
            checkArgument(floatingIp != null, NOT_NULL_MSG, "floatingIp");

            return new DefaultKubevirtFloatingIp(id, routerName, podName, vmName,
                    networkName, floatingIp, fixedIp);
        }

        @Override
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder routerName(String name) {
            this.routerName = name;
            return this;
        }

        @Override
        public Builder networkName(String name) {
            this.networkName = name;
            return this;
        }

        @Override
        public Builder floatingIp(IpAddress ip) {
            this.floatingIp = ip;
            return this;
        }

        @Override
        public Builder fixedIp(IpAddress ip) {
            this.fixedIp = ip;
            return this;
        }

        @Override
        public Builder vmName(String name) {
            this.vmName = name;
            return this;
        }

        @Override
        public Builder podName(String name) {
            this.podName = name;
            return this;
        }
    }
}
