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

import org.onosproject.drivers.server.devices.cpu.CpuCacheHierarchyDevice;
import org.onosproject.drivers.server.devices.cpu.CpuDevice;
import org.onosproject.drivers.server.devices.memory.MemoryHierarchyDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.RestServerSBDevice;

import org.onosproject.protocol.rest.DefaultRestSBDevice;
import org.onosproject.protocol.rest.RestSBDevice.AuthenticationScheme;
import org.onlab.packet.IpAddress;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_HIERARCHY_NULL;
import static org.onosproject.drivers.server.Constants.MSG_CPU_LIST_NULL;
import static org.onosproject.drivers.server.Constants.MSG_MEM_HIERARCHY_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_LIST_NULL;

/**
 * Default implementation for REST server devices.
 */
public class DefaultRestServerSBDevice
        extends DefaultRestSBDevice implements RestServerSBDevice {

    private Collection<CpuDevice> cpus = null;
    private CpuCacheHierarchyDevice caches = null;
    private MemoryHierarchyDevice memory = null;
    private Collection<NicDevice> nics = null;

    /**
     * Indicates an invalid port.
     */
    private static final long INVALID_PORT = (long) -1;
    private static final String INVALID_PORT_STR = "";

    public DefaultRestServerSBDevice(
            IpAddress ip, int port, String name, String password,
            String protocol, String url, boolean isActive,
            Collection<CpuDevice> cpus,
            CpuCacheHierarchyDevice caches,
            MemoryHierarchyDevice memory,
            Collection<NicDevice> nics) {
        this(ip, port, name, password, protocol, url, isActive,
            "", "", "", "", cpus, caches, memory, nics);
    }

    public DefaultRestServerSBDevice(
            IpAddress ip, int port, String name, String password,
            String protocol, String url, boolean isActive, String testUrl,
            String manufacturer, String hwVersion, String swVersion,
            Collection<CpuDevice> cpus, CpuCacheHierarchyDevice caches,
            MemoryHierarchyDevice memory, Collection<NicDevice> nics) {
        super(ip, port, name, password, protocol, url, isActive,
              testUrl, manufacturer, hwVersion, swVersion,
              AuthenticationScheme.BASIC, "");

        checkNotNull(cpus, MSG_CPU_LIST_NULL);
        checkNotNull(caches, MSG_CPU_CACHE_HIERARCHY_NULL);
        checkNotNull(memory, MSG_MEM_HIERARCHY_NULL);
        checkNotNull(nics, MSG_NIC_LIST_NULL);

        this.cpus = cpus;
        this.caches = caches;
        this.memory = memory;
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
    public CpuCacheHierarchyDevice caches() {
        return this.caches;
    }

    @Override
    public int numberOfCaches() {
        return this.caches.levels();
    }

    @Override
    public long cacheCapacity() {
        return this.caches.totalCapacity();
    }

    @Override
    public MemoryHierarchyDevice memory() {
        return this.memory;
    }

    @Override
    public long memoryCapacity() {
        return this.memory.totalCapacity();
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
    public long portNumberFromName(String portName) {
        if (Strings.isNullOrEmpty(portName)) {
            return INVALID_PORT;
        }

        for (NicDevice nic : this.nics) {
            if (nic.name().equals(portName)) {
                return nic.portNumber();
            }
        }

        return INVALID_PORT;
    }

    @Override
    public String portNameFromNumber(long portNumber) {
        if (portNumber < 0) {
            return INVALID_PORT_STR;
        }

        for (NicDevice nic : this.nics) {
            if (nic.portNumber() == portNumber) {
                return nic.name();
            }
        }

        return INVALID_PORT_STR;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("url", url())
                .add("protocol", protocol())
                .add("username", username())
                .add("ip", ip())
                .add("port", port())
                .add("testUrl", testUrl().orElse(null))
                .add("manufacturer", manufacturer().orElse(null))
                .add("hwVersion", hwVersion().orElse(null))
                .add("swVersion", swVersion().orElse(null))
                .add("cpus", cpus())
                .add("cpuCaches", caches())
                .add("memory", memory())
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
        return  this.protocol().equals(device.protocol()) &&
                this.username().equals(device.username()) &&
                this.ip().equals(device.ip()) &&
                this.port() == device.port() &&
                this.cpus() == device.cpus() &&
                this.caches() == device.caches() &&
                this.memory() == device.memory() &&
                this.nics() == device.nics();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip(), port(), cpus(), caches(), memory(), nics());
    }

}
