/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.openstacknetworking.api.InstancePort;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of instance port based on host subsystem.
 * Basically, HostBasedInstancePort is just a wrapper of a host, which helps
 * mapping between OpenStack port and the OVS port and retrieving information
 * such as IP address, location, and so on.
 */
public final class HostBasedInstancePort implements InstancePort {

    static final String ANNOTATION_NETWORK_ID = "networkId";
    static final String ANNOTATION_PORT_ID = "portId";
    static final String ANNOTATION_CREATE_TIME = "createTime";

    private final Host host;

    /**
     * Default constructor.
     *
     * @param instance host object of this instance
     */
    private HostBasedInstancePort(Host instance) {
        this.host = instance;
    }

    /**
     * Returns new instance.
     *
     * @param host host object of this instance
     * @return instance
     */
    public static HostBasedInstancePort of(Host host) {
        checkNotNull(host);
        checkArgument(!Strings.isNullOrEmpty(host.annotations().value(ANNOTATION_NETWORK_ID)));
        checkArgument(!Strings.isNullOrEmpty(host.annotations().value(ANNOTATION_PORT_ID)));
        checkArgument(!Strings.isNullOrEmpty(host.annotations().value(ANNOTATION_CREATE_TIME)));

        return new HostBasedInstancePort(host);
    }

    @Override
    public String networkId() {
        return host.annotations().value(ANNOTATION_NETWORK_ID);
    }

    @Override
    public String portId() {
        return host.annotations().value(ANNOTATION_PORT_ID);
    }

    @Override
    public MacAddress macAddress() {
        return host.mac();
    }

    @Override
    public IpAddress ipAddress() {
        Optional<IpAddress> ipAddr = host.ipAddresses().stream().findFirst();
        return ipAddr.orElse(null);
    }

    @Override
    public DeviceId deviceId() {
        return host.location().deviceId();
    }

    @Override
    public PortNumber portNumber() {
        return host.location().port();
    }

    @Override
    public String toString() {
        return host.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof HostBasedInstancePort) {
            HostBasedInstancePort that = (HostBasedInstancePort) obj;
            if (Objects.equals(this.portId(), that.portId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(portId());
    }
}
