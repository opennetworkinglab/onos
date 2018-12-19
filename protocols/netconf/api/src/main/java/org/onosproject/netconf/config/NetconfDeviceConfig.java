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

package org.onosproject.netconf.config;

import com.google.common.annotations.Beta;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.netconf.NetconfDeviceInfo.extractIpPortPath;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configuration for Netconf provider.
 *
 * The URI for a netconf device is of the format
 *
 * {@code netconf:<ip>[:<port>][/<path>]}
 *
 * The {@code ip} and {@code port} are used to create a netconf connection
 * to the device. The {@code path} is an optional component that is not used
 * by the default netconf driver, but is leveragable by custom drivers.
 */
@Beta
public class NetconfDeviceConfig extends Config<DeviceId> {

    private final Logger log = getLogger(getClass());

    /**
     * netcfg ConfigKey.
     */
    public static final String CONFIG_KEY = "netconf";

    public static final String IP = "ip";
    public static final String PORT = "port";
    public static final String PATH = "path";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SSHKEY = "sshkey";
    public static final String SSHCLIENT = "ssh-client";
    public static final String CONNECT_TIMEOUT = "connect-timeout";
    public static final String REPLY_TIMEOUT = "reply-timeout";
    public static final String IDLE_TIMEOUT = "idle-timeout";

    @Override
    public boolean isValid() {
        return hasOnlyFields(IP, PORT, PATH, USERNAME, PASSWORD, SSHKEY, SSHCLIENT,
                CONNECT_TIMEOUT, REPLY_TIMEOUT, IDLE_TIMEOUT) && ip() != null;
    }

    /**
     * Gets the Ip of the NETCONF device.
     *
     * @return ip
     */
    public IpAddress ip() {
        return IpAddress.valueOf(get(IP, checkNotNull(extractIpPortPath(subject)).getLeft()));
    }

    /**
     * Gets the port of the NETCONF device.
     *
     * @return port
     */
    public int port() {
        return get(PORT, checkNotNull(extractIpPortPath(subject)).getMiddle());
    }

    /**
     * Gets the path of the NETCONF device.
     *
     * @return path
     */
    public Optional<String> path() {
        String val = get(PATH, "");
        if (val.isEmpty()) {
            return extractIpPortPath(subject).getRight();
        }
        return Optional.ofNullable(val);
    }

    /**
     * Gets the username of the NETCONF device.
     *
     * @return username
     */
    public String username() {
        return get(USERNAME, "");
    }

    /**
     * Gets the password of the NETCONF device.
     *
     * @return password
     */
    public String password() {
        return get(PASSWORD, "");
    }

    /**
     * Gets the sshKey of the NETCONF device.
     *
     * @return sshkey
     */
    public String sshKey() {
        return get(SSHKEY, "");
    }

    /**
     * Gets the NETCONF SSH Client implementation.
     * Expecting "apache-mina"
     *
     * @return sshClient
     */
    public Optional<String> sshClient() {
        String sshClient = get(SSHCLIENT, "");
        return (sshClient.isEmpty() ? Optional.empty() : Optional.ofNullable(sshClient));
    }

    /**
     * Gets the connect timeout of the SSH connection.
     *
     * @return connectTimeout
     */
    public OptionalInt connectTimeout() {
        int connectTimeout = get(CONNECT_TIMEOUT, 0);
        return (connectTimeout == 0) ? OptionalInt.empty() : OptionalInt.of(connectTimeout);
    }

    /**
     * Gets the reply timeout of the SSH connection.
     *
     * @return replyTimeout
     */
    public OptionalInt replyTimeout() {
        int replyTimeout = get(REPLY_TIMEOUT, 0);
        return (replyTimeout == 0) ? OptionalInt.empty() : OptionalInt.of(replyTimeout);
    }

    /**
     * Gets the idle timeout of the SSH connection.
     *
     * @return idleTimeout
     */
    public OptionalInt idleTimeout() {
        int idleTimeout = get(IDLE_TIMEOUT, 0);
        return (idleTimeout == 0) ? OptionalInt.empty() : OptionalInt.of(idleTimeout);
    }

    /**
     * Sets the Ip for the Device.
     *
     * @param ip the ip
     * @return instance for chaining
     */
    public NetconfDeviceConfig setIp(String ip) {
        return (NetconfDeviceConfig) setOrClear(IP, ip);
    }

    /**
     * Sets the Port for the Device.
     *
     * @param port the port
     * @return instance for chaining
     */
    public NetconfDeviceConfig setPort(int port) {
        return (NetconfDeviceConfig) setOrClear(PORT, port);
    }

    /**
     * Sets the path for the device.
     *
     * @param path the path
     * @return instance for chaining
     */
    public NetconfDeviceConfig setPath(String path) {
        return (NetconfDeviceConfig) setOrClear(PATH, path);
    }

    /**
     * Sets the username for the Device.
     *
     * @param username username
     * @return instance for chaining
     */
    public NetconfDeviceConfig setUsername(String username) {
        return (NetconfDeviceConfig) setOrClear(USERNAME, username);
    }

    /**
     * Sets the password for the Device.
     *
     * @param password password
     * @return instance for chaining
     */
    public NetconfDeviceConfig setPassword(String password) {
        return (NetconfDeviceConfig) setOrClear(PASSWORD, password);
    }

    /**
     * Sets the SshKey for the Device.
     *
     * @param sshKey sshKey as string
     * @return instance for chaining
     */
    public NetconfDeviceConfig setSshKey(String sshKey) {
        return (NetconfDeviceConfig) setOrClear(SSHKEY, sshKey);
    }

    /**
     * Sets the NETCONF Ssh client implementation for the Device.
     * Must be 'apache-mina'
     * When specified, overrides NetconfControllerImpl.sshLibrary for this device
     *
     * @param sshimpl sshimpl as string
     * @return instance for chaining
     */
    public NetconfDeviceConfig setSshImpl(String sshimpl) {
        return (NetconfDeviceConfig) setOrClear(SSHCLIENT, sshimpl);
    }

    /**
     * Sets the NETCONF Connect Timeout for the Device.
     * This is the amount of time in seconds allowed for the SSH handshake to take place
     * Minimum 1 second
     * When specified, overrides NetconfControllerImpl.netconfConnectTimeout for this device
     *
     * @param connectTimeout connectTimeout as int
     * @return instance for chaining
     */
    public NetconfDeviceConfig setConnectTimeout(Integer connectTimeout) {
        return (NetconfDeviceConfig) setOrClear(CONNECT_TIMEOUT, connectTimeout);
    }

    /**
     * Sets the NETCONF Reply Timeout for the Device.
     * This is the amount of time in seconds allowed for the NETCONF Reply to a command
     * Minimum 1 second
     * When specified, overrides NetconfControllerImpl.netconfReplyTimeout for this device
     *
     * @param replyTimeout replyTimeout as int
     * @return instance for chaining
     */
    public NetconfDeviceConfig setReplyTimeout(Integer replyTimeout) {
        return (NetconfDeviceConfig) setOrClear(REPLY_TIMEOUT, replyTimeout);
    }

    /**
     * Sets the NETCONF Idle Timeout for the Device.
     * This is the amount of time in seconds after which the SSH connection will
     * close if no traffic is detected
     * Minimum 10 second
     * When specified, overrides NetconfControllerImpl.netconfIdleTimeout for this device
     *
     * @param idleTimeout idleTimeout as int
     * @return instance for chaining
     */
    public NetconfDeviceConfig setIdleTimeout(Integer idleTimeout) {
        return (NetconfDeviceConfig) setOrClear(IDLE_TIMEOUT, idleTimeout);
    }
}
