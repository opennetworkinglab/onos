/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.netconf;

import org.apache.commons.lang3.tuple.Triple;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.config.NetconfDeviceConfig;
import org.onosproject.netconf.config.NetconfSshClientLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a Netconf device information.
 */
public class NetconfDeviceInfo {

    public static final Logger log = LoggerFactory
            .getLogger(NetconfDeviceInfo.class);

    private String name;
    private String password;
    private IpAddress ipAddress;
    private int port;
    private Optional<String> path;
    private char[] key;
    private Optional<NetconfSshClientLib> sshClientLib;
    private OptionalInt connectTimeoutSec;
    private OptionalInt replyTimeoutSec;
    private OptionalInt idleTimeoutSec;
    private DeviceId deviceId;

    /**
     * Information for contacting the controller.
     *
     * @param name      the connection type
     * @param password  the password for the device
     * @param ipAddress the ip address
     * @param port      the tcp port
     * @param path      the path part
     */
    public NetconfDeviceInfo(String name, String password, IpAddress ipAddress,
                             int port, String path) {
        checkArgument(!name.equals(""), "Empty device username");
        checkArgument(port > 0, "Negative port");
        checkNotNull(ipAddress, "Null ip address");
        this.name = name;
        this.password = password;
        this.ipAddress = ipAddress;
        this.port = port;
        if (path == null || path.isEmpty()) {
            this.path = Optional.empty();
        } else {
            this.path = Optional.of(path);
        }
        this.sshClientLib = Optional.empty();
        this.connectTimeoutSec = OptionalInt.empty();
        this.replyTimeoutSec = OptionalInt.empty();
        this.idleTimeoutSec = OptionalInt.empty();
    }

    /**
     * Information for contacting the controller.
     *
     * @param name      the connection type
     * @param password  the password for the device
     * @param ipAddress the ip address
     * @param port      the tcp port
     */
    public NetconfDeviceInfo(String name, String password, IpAddress ipAddress,
                             int port) {
        this(name, password, ipAddress, port, null);
    }

    /**
     * Information for contacting the controller.
     *
     * @param name      the connection type
     * @param password  the password for the device
     * @param ipAddress the ip address
     * @param port      the tcp port
     * @param path      the path part
     * @param keyString the string containing a DSA or RSA private key
     *                  of the user in OpenSSH key format
     */
    public NetconfDeviceInfo(String name, String password, IpAddress ipAddress,
                             int port, String path, String keyString) {
        checkArgument(!name.equals(""), "Empty device name");
        checkArgument(port > 0, "Negative port");
        checkNotNull(ipAddress, "Null ip address");
        this.name = name;
        this.password = password;
        this.ipAddress = ipAddress;
        this.port = port;
        this.path = Optional.ofNullable(path);
        this.key = keyString.toCharArray();
        this.sshClientLib = Optional.empty();
        this.connectTimeoutSec = OptionalInt.empty();
        this.replyTimeoutSec = OptionalInt.empty();
        this.idleTimeoutSec = OptionalInt.empty();
    }

    /**
     * Convenieince constructor that converts all known fields from NetCfg data.
     * @param netconfConfig NetCf configuration
     * @param deviceId deviceId as per netcfg
     */
    public NetconfDeviceInfo(NetconfDeviceConfig netconfConfig, DeviceId deviceId) {
        checkArgument(!netconfConfig.username().isEmpty(), "Empty device name");
        checkArgument(netconfConfig.port() > 0, "Negative port");
        checkNotNull(netconfConfig.ip(), "Null ip address");

        this.name = netconfConfig.username();
        this.password = netconfConfig.password();
        this.ipAddress = netconfConfig.ip();
        this.deviceId = deviceId;
        this.port = netconfConfig.port();
        this.path = netconfConfig.path();
        if (netconfConfig.sshKey() != null && !netconfConfig.sshKey().isEmpty()) {
            this.key = netconfConfig.sshKey().toCharArray();
        }
        if (netconfConfig.sshClient().isPresent()) {
            this.sshClientLib = Optional.of(NetconfSshClientLib.getEnum(netconfConfig.sshClient().get()));
        } else {
            this.sshClientLib = Optional.empty();
        }
        this.connectTimeoutSec = netconfConfig.connectTimeout();
        this.replyTimeoutSec = netconfConfig.replyTimeout();
        this.idleTimeoutSec = netconfConfig.idleTimeout();
    }

    /**
     * Allows the NETCONF SSH Client library to be set.
     *
     * @param sshClientLib An enumerated value
     */
    public void setSshClientLib(Optional<NetconfSshClientLib> sshClientLib) {
        this.sshClientLib = sshClientLib;
    }

    /**
     * Allows the NETCONF SSH session initial connect timeout to be set.
     *
     * @param connectTimeoutSec value in seconds
     */
    public void setConnectTimeoutSec(OptionalInt connectTimeoutSec) {
        this.connectTimeoutSec = connectTimeoutSec;
    }

    /**
     * Allows the NETCONF SSH session replies timeout to be set.
     *
     * @param replyTimeoutSec value in seconds
     */
    public void setReplyTimeoutSec(OptionalInt replyTimeoutSec) {
        this.replyTimeoutSec = replyTimeoutSec;
    }

    /**
     * Allows the NETCONF SSH session idle timeout to be set.
     *
     * @param idleTimeoutSec value in seconds
     */
    public void setIdleTimeoutSec(OptionalInt idleTimeoutSec) {
        this.idleTimeoutSec = idleTimeoutSec;
    }

    /**
     * Allows the path aspect of the device URI to be set.
     *
     * @param path path aspect value
     */
    public void setPath(Optional<String> path) {
        this.path = path;
    }

    /**
     * Exposes the name of the controller.
     *
     * @return String name
     */
    public String name() {
        return name;
    }

    /**
     * Exposes the password of the controller.
     *
     * @return String password
     */
    public String password() {
        return password;
    }

    /**
     * Exposes the ip address of the controller.
     *
     * @return IpAddress ip address
     */
    public IpAddress ip() {
        return ipAddress;
    }

    /**
     * Exposes the port of the controller.
     *
     * @return port number
     */
    public int port() {
        return port;
    }

    /*
     * Exposes the path of the aspect.
     *
     * @return path aspect
     */
    public Optional<String> path() {
        return path;
    }

    /**
     * Exposes the key of the controller.
     *
     * @return {@code char[]} containing a DSA or RSA private key of the user
     *         in OpenSSH key format
     *         or null if device is not configured to use public key authentication
     */
    public char[] getKey() {
        return key;
    }

    /**
     * Exposes the Client library implementation.
     *
     * @return Enumerated value
     */
    public Optional<NetconfSshClientLib> sshClientLib() {
        return sshClientLib;
    }

    /**
     * Exposes the device specific connect timeout.
     *
     * @return The timeout value in seconds
     */
    public OptionalInt getConnectTimeoutSec() {
        return connectTimeoutSec;
    }

    /**
     * Exposes the device specific reply timeout.
     *
     * @return The timeout value in seconds
     */
    public OptionalInt getReplyTimeoutSec() {
        return replyTimeoutSec;
    }

    /**
     * Exposes the device specific idle timeout.
     *
     * @return The timeout value in seconds
     */
    public OptionalInt getIdleTimeoutSec() {
        return idleTimeoutSec;
    }

    /**
     * Return the info about the device in a string.
     * String format: "netconf:name@ip:port"
     *
     * @return String device info
     */
    @Override
    public String toString() {
        return "netconf:" + name + "@" + ipAddress + ":" + port +
            (path.isPresent() ? '/' + path.get() : "");
    }

    /**
     * Return the DeviceId about the device containing the URI.
     *
     * @return DeviceId
     */
    public DeviceId getDeviceId() {
        if (deviceId == null) {
            try {
                deviceId = DeviceId.deviceId(new URI("netconf", ipAddress.toString() + ":" + port +
                            (path.isPresent() ? "/" + path.get() : ""), null));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Unable to build deviceID for device " + toString(), e);
            }
        }
        return deviceId;
    }

    @Override
    public int hashCode() {
        if (path.isPresent()) {
            return Objects.hash(ipAddress, port, path.get(), name);
        } else {
            return Objects.hash(ipAddress, port, name);
        }
    }

    @Override
    public boolean equals(Object toBeCompared) {
        if (toBeCompared instanceof NetconfDeviceInfo) {
            NetconfDeviceInfo netconfDeviceInfo = (NetconfDeviceInfo) toBeCompared;
            if (netconfDeviceInfo.name().equals(name)
                    && netconfDeviceInfo.ip().equals(ipAddress)
                    && netconfDeviceInfo.port() == port
                    && netconfDeviceInfo.path().equals(path)
                    && netconfDeviceInfo.password().equals(password)) {
                return true;
            }
        }
        return false;
    }

    public static Triple<String, Integer, Optional<String>> extractIpPortPath(DeviceId deviceId) {
        /*
         * We can expect the following formats:
         *
         * netconf:ip:port/path
         * netconf:ip:port
         */
        String string = deviceId.toString();

        /*
         * The first ':' is the separation between the scheme and the IP.
         *
         * The last ':' will represent the separator between the IP and the port.
         */
        int first = string.indexOf(':');
        int last = string.lastIndexOf(':');
        String ip = string.substring(first + 1, last);
        String port = string.substring(last + 1);
        String path = null;
        int pathSep = port.indexOf('/');
        if (pathSep != -1) {
            path = port.substring(pathSep + 1);
            port = port.substring(0, pathSep);
        }

        return Triple.of(ip, new Integer(port),
                (path == null || path.isEmpty() ? Optional.empty() : Optional.of(path)));
    }
}
