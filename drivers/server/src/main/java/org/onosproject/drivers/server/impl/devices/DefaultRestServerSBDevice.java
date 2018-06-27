/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.server.impl.devices;

import org.onosproject.drivers.server.devices.CpuDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.RestServerSBDevice;

import org.onosproject.protocol.rest.DefaultRestSBDevice;
import org.onosproject.protocol.rest.RestSBDevice.AuthenticationScheme;
import org.onlab.packet.IpAddress;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import java.util.Objects;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation for REST server devices.
 */
public class DefaultRestServerSBDevice
        extends DefaultRestSBDevice implements RestServerSBDevice {

    private Collection<CpuDevice> cpus = Lists.newArrayList();
    private Collection<NicDevice> nics = Lists.newArrayList();

    public DefaultRestServerSBDevice(
            IpAddress ip, int port, String name, String password,
            String protocol, String url, boolean isActive,
            Collection<CpuDevice> cpus, Collection<NicDevice> nics) {
        this(
            ip, port, name, password, protocol, url, isActive,
            "", "", "", "", AuthenticationScheme.BASIC, "", cpus, nics
        );
    }

    public DefaultRestServerSBDevice(
            IpAddress ip, int port, String name, String password,
            String protocol, String url, boolean isActive, String testUrl,
            String manufacturer, String hwVersion, String swVersion,
            AuthenticationScheme authenticationScheme, String token,
            Collection<CpuDevice> cpus, Collection<NicDevice> nics) {
        super(
            ip, port, name, password, protocol, url, isActive,
            testUrl, manufacturer, hwVersion, swVersion,
            authenticationScheme, token
        );

        checkNotNull(cpus, "Device's set of CPUs cannot be null");
        checkNotNull(nics, "Device's set of NICs cannot be null");

        this.cpus = cpus;
        this.nics = nics;
    }

    @Override
    public Collection<CpuDevice> cpus() {
        return this.cpus;
    }

    @Override
    public int numberOfCpus() {
        return this.cpus.size();
    }

    @Override
    public Collection<NicDevice> nics() {
        return this.nics;
    }

    @Override
    public int numberOfNics() {
        return this.nics.size();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("url", url())
                .add("testUrl", testUrl())
                .add("protocol", protocol())
                .add("username", username())
                .add("port", port())
                .add("ip", ip())
                .add("manufacturer", manufacturer().orElse(null))
                .add("hwVersion", hwVersion().orElse(null))
                .add("swVersion", swVersion().orElse(null))
                .add("cpus", cpus())
                .add("nics", nics())
                .toString();

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RestServerSBDevice)) {
            return false;
        }
        RestServerSBDevice device = (RestServerSBDevice) obj;

        return  this.username().equals(device.username()) &&
                this.ip().equals(device.ip()) &&
                this.port() == device.port();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip(), port(), cpus(), nics());
    }

}
