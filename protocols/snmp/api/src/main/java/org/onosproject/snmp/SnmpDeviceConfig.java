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

package org.onosproject.snmp;

import com.google.common.annotations.Beta;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;

/**
 * Configuration to push devices to the SNMP provider.
 */
@Beta
public class SnmpDeviceConfig extends Config<DeviceId> {

    private static final String PROTOCOL = "protocol";
    private static final String NOTIFICATION_PROTOCOL = "notificationProtocol";
    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final int DEFAULT_PORT = 161;
    private static final String NOTIFICATION_PORT = "notificationPort";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    @Override
    public boolean isValid() {
        return hasOnlyFields(PROTOCOL, NOTIFICATION_PROTOCOL, IP, PORT,
                             NOTIFICATION_PORT, USERNAME, PASSWORD) &&
                ip() != null;
    }

    /**
     * Gets the protocol of the SNMP device.
     *
     * @return protocol
     */
    public String protocol() {
        return get(PROTOCOL, "udp");
    }

    /**
     * Gets the notification protocol of the SNMP device.
     *
     * @return notification protocol
     */
    public String notificationProtocol() {
        return get(NOTIFICATION_PROTOCOL, "udp");
    }

    /**
     * Gets the Ip of the SNMP device.
     *
     * @return ip
     */
    public IpAddress ip() {
        return IpAddress.valueOf(get(IP, extractIpPort().getKey()));
    }

    /**
     * Gets the port of the SNMP device.
     *
     * @return port
     */
    public int port() {
        return get(PORT, extractIpPort().getValue());
    }

    /**
     * Gets the notification port of the SNMP device.
     *
     * @return notification port
     */
    public int notificationPort() {
        return get(NOTIFICATION_PORT, 0);
    }

    /**
     * Gets the username of the SNMP device.
     *
     * @return username
     */
    public String username() {
        return get(USERNAME, "");
    }

    /**
     * Gets the password of the SNMP device.
     *
     * @return password
     */
    public String password() {
        return get(PASSWORD, "");
    }


    private Pair<String, Integer> extractIpPort() {
        String info = subject.uri().getSchemeSpecificPart();
        int portSeparator = info.lastIndexOf(':');
        if (portSeparator == -1) {
            return Pair.of(info, DEFAULT_PORT);
        }

        String ip = info.substring(0, portSeparator);
        int port = Integer.parseInt(info.substring(portSeparator + 1));
        return Pair.of(ip, port);
    }
}
