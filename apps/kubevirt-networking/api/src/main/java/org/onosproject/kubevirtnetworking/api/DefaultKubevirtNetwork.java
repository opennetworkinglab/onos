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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_TO_TUNNEL_PREFIX;
import static org.onosproject.kubevirtnetworking.api.Constants.TUNNEL_TO_TENANT_PREFIX;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.FLAT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GENEVE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GRE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.STT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VXLAN;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Default implementation class of kubevirt network.
 */
public final class DefaultKubevirtNetwork implements KubevirtNetwork {

    private static final String NOT_NULL_MSG = "Network % cannot be null";
    private static final String TENANT_BRIDGE_PREFIX = "br-int-";
    private static final String OF_PREFIX = "of:";

    private final String networkId;
    private final Type type;
    private final String name;
    private final Integer mtu;
    private final String segmentId;
    private final IpAddress gatewayIp;
    private final boolean defaultRoute;
    private final String cidr;
    private final Set<KubevirtHostRoute> hostRoutes;
    private final KubevirtIpPool ipPool;
    private final Set<IpAddress> dnses;

    /**
     * Default constructor.
     *
     * @param networkId         network identifier
     * @param type              type of network
     * @param name              network name
     * @param mtu               network MTU
     * @param segmentId         segment identifier
     * @param gatewayIp         gateway IP address
     * @param defaultRoute      default route
     * @param cidr              CIDR of network
     * @param hostRoutes        a set of host routes
     * @param ipPool            IP pool
     * @param dnses             a set of DNSes
     */
    public DefaultKubevirtNetwork(String networkId, Type type, String name,
                                  Integer mtu, String segmentId, IpAddress gatewayIp,
                                  boolean defaultRoute, String cidr,
                                  Set<KubevirtHostRoute> hostRoutes,
                                  KubevirtIpPool ipPool, Set<IpAddress> dnses) {
        this.networkId = networkId;
        this.type = type;
        this.name = name;
        this.mtu = mtu;
        this.segmentId = segmentId;
        this.gatewayIp = gatewayIp;
        this.defaultRoute = defaultRoute;
        this.cidr = cidr;
        this.hostRoutes = hostRoutes;
        this.ipPool = ipPool;
        this.dnses = dnses;
    }

    @Override
    public String networkId() {
        return networkId;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Integer mtu() {
        return mtu;
    }

    @Override
    public String segmentId() {
        return segmentId;
    }

    @Override
    public IpAddress gatewayIp() {
        return gatewayIp;
    }

    @Override
    public String cidr() {
        return cidr;
    }

    @Override
    public Set<KubevirtHostRoute> hostRoutes() {
        if (hostRoutes == null || hostRoutes.size() == 0) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(hostRoutes);
        }
    }

    @Override
    public boolean defaultRoute() {
        return defaultRoute;
    }

    @Override
    public KubevirtIpPool ipPool() {
        return ipPool;
    }

    @Override
    public Set<IpAddress> dnses() {
        if (dnses == null || dnses.size() == 0) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(dnses);
        }
    }

    @Override
    public String tenantBridgeName() {
        if (type == VXLAN || type == GRE || type == GENEVE || type == STT) {
            return TENANT_BRIDGE_PREFIX + segmentIdHex(segmentId);
        }
        return null;
    }

    @Override
    public DeviceId tenantDeviceId(String hostname) {
        if (type == VXLAN || type == GRE || type == GENEVE || type == STT) {
            String dpid = genDpidFromName(tenantBridgeName() + "-" + hostname);
            return DeviceId.deviceId(dpid);
        }
        return null;
    }

    @Override
    public PortNumber tunnelToTenantPort(DeviceId deviceId) {
        String portName = TUNNEL_TO_TENANT_PREFIX + segmentIdHex(segmentId);
        Port port = port(deviceId, portName);
        if (port == null) {
            return null;
        } else {
            return port.number();
        }
    }

    @Override
    public PortNumber tenantToTunnelPort(DeviceId deviceId) {
        String portName = TENANT_TO_TUNNEL_PREFIX + segmentIdHex(segmentId);
        Port port = port(deviceId, portName);
        if (port == null) {
            return null;
        } else {
            return port.number();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtNetwork that = (DefaultKubevirtNetwork) o;
        return networkId.equals(that.networkId) && type == that.type &&
                name.equals(that.name) && mtu.equals(that.mtu) &&
                gatewayIp.equals(that.gatewayIp) && defaultRoute == that.defaultRoute &&
                cidr.equals(that.cidr) && hostRoutes.equals(that.hostRoutes) &&
                ipPool.equals(that.ipPool) &&
                dnses.equals(that.dnses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, type, name, mtu, segmentId, gatewayIp,
                defaultRoute, cidr, hostRoutes, ipPool, dnses);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", networkId)
                .add("type", type)
                .add("name", name)
                .add("mtu", mtu)
                .add("segmentId", segmentId)
                .add("gatewayIp", gatewayIp)
                .add("defaultRoute", defaultRoute)
                .add("cidr", cidr)
                .add("hostRouts", hostRoutes)
                .add("ipPool", ipPool)
                .add("dnses", dnses)
                .toString();
    }

    private Port port(DeviceId deviceId, String portName) {
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        return deviceService.getPorts(deviceId).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), portName))
                .findAny().orElse(null);
    }

    private String segmentIdHex(String segIdStr) {
        int segId = Integer.parseInt(segIdStr);
        return String.format("%06x", segId).toLowerCase();
    }

    private String genDpidFromName(String name) {
        if (name != null) {
            String hexString = Integer.toHexString(name.hashCode());
            return OF_PREFIX + Strings.padStart(hexString, 16, '0');
        }

        return null;
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt port builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements KubevirtNetwork.Builder {

        private String networkId;
        private Type type;
        private String name;
        private Integer mtu;
        private String segmentId;
        private IpAddress gatewayIp;
        private boolean defaultRoute;
        private String cidr;
        private Set<KubevirtHostRoute> hostRouts;
        private KubevirtIpPool ipPool;
        private Set<IpAddress> dnses;

        @Override
        public KubevirtNetwork build() {
            checkArgument(networkId != null, NOT_NULL_MSG, "networkId");
            checkArgument(type != null, NOT_NULL_MSG, "type");
            checkArgument(name != null, NOT_NULL_MSG, "name");
            checkArgument(mtu != null, NOT_NULL_MSG, "mtu");
            checkArgument(cidr != null, NOT_NULL_MSG, "cidr");
            checkArgument(ipPool != null, NOT_NULL_MSG, "ipPool");

            if (type != FLAT) {
                checkArgument(segmentId != null, NOT_NULL_MSG, "segmentId");
            }

            if (dnses == null) {
                dnses = new HashSet<>();
            }

            return new DefaultKubevirtNetwork(networkId, type, name, mtu, segmentId,
                    gatewayIp, defaultRoute, cidr, hostRouts, ipPool, dnses);
        }

        @Override
        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder mtu(Integer mtu) {
            this.mtu = mtu;
            return this;
        }

        @Override
        public Builder segmentId(String segmentId) {
            this.segmentId = segmentId;
            return this;
        }

        @Override
        public Builder gatewayIp(IpAddress ipAddress) {
            this.gatewayIp = ipAddress;
            return this;
        }

        @Override
        public KubevirtNetwork.Builder defaultRoute(boolean flag) {
            this.defaultRoute = flag;
            return this;
        }

        @Override
        public Builder cidr(String cidr) {
            this.cidr = cidr;
            return this;
        }

        @Override
        public Builder ipPool(KubevirtIpPool ipPool) {
            this.ipPool = ipPool;
            return this;
        }

        @Override
        public Builder hostRoutes(Set<KubevirtHostRoute> hostRoutes) {
            this.hostRouts = hostRoutes;
            return this;
        }

        @Override
        public Builder dnses(Set<IpAddress> dnses) {
            this.dnses = dnses;
            return this;
        }
    }
}
