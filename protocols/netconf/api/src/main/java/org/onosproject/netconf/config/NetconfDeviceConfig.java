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

package org.onosproject.netconf.config;

import com.google.common.annotations.Beta;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration for Netconf provider.
 */
@Beta
public class NetconfDeviceConfig extends Config<DeviceId> {

    /**
     * netcfg ConfigKey.
     */
    public static final String CONFIG_KEY = "netconf";

    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String SSHKEY = "sshkey";

    @Override
    public boolean isValid() {
        return hasOnlyFields(IP, PORT, USERNAME, PASSWORD, SSHKEY) &&
                ip() != null;
    }

    /**
     * Gets the Ip of the NETCONF device.
     *
     * @return ip
     */
    public IpAddress ip() {
        return IpAddress.valueOf(get(IP, checkNotNull(extractIpPort()).getKey()));
    }

    /**
     * Gets the port of the NETCONF device.
     *
     * @return port
     */
    public int port() {
        return get(PORT, checkNotNull(extractIpPort()).getValue());
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

    private Pair<String, Integer> extractIpPort() {
        // Assuming one of
        //  - netconf:ip:port
        //  - netconf:ip

        // foo:schemespecifcpart
        String info = subject.uri().getSchemeSpecificPart();
        int portSeparator = info.lastIndexOf(':');
        if (portSeparator == -1) {
            // assume default port
            return Pair.of(info, 830);
        }
        String ip = info.substring(0, portSeparator);
        int port = Integer.parseInt(info.substring(portSeparator + 1));
        return Pair.of(ip, port);
    }
}
