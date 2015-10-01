/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.aaa;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.BasicElementConfig;

/**
 * Network config for the AAA app.
 */
public class AAAConfig extends Config<ApplicationId> {

    private static final String RADIUS_IP = "radiusIp";
    private static final String RADIUS_MAC = "radiusMac";
    private static final String NAS_IP = "nasIp";
    private static final String NAS_MAC = "nasMac";
    private static final String RADIUS_SECRET = "radiusSecret";
    private static final String RADIUS_SWITCH = "radiusSwitch";
    private static final String RADIUS_PORT = "radiusPort";

    // RADIUS server IP address
    protected static final String DEFAULT_RADIUS_IP = "192.168.1.10";

    // RADIUS MAC address
    protected static final String DEFAULT_RADIUS_MAC = "00:00:00:00:01:10";

    // NAS IP address
    protected static final String DEFAULT_NAS_IP = "192.168.1.11";

    // NAS MAC address
    protected static final String DEFAULT_NAS_MAC = "00:00:00:00:10:01";

    // RADIUS uplink port
    protected static final int DEFAULT_RADIUS_UPLINK = 2;

    // RADIUS server shared secret
    protected static final String DEFAULT_RADIUS_SECRET = "ONOSecret";

    // Radius Switch Id
    protected static final String DEFAULT_RADIUS_SWITCH = "of:90e2ba82f97791e9";

    // Radius Port Number
    protected static final String DEFAULT_RADIUS_PORT = "129";

    /**
     * Returns the NAS ip.
     *
     * @return ip address or null if not set
     */
    public InetAddress nasIp() {
        try {
            if (object == null) {
                return InetAddress.getByName(DEFAULT_NAS_IP);
            }
            return InetAddress.getByName(get(NAS_IP, DEFAULT_NAS_IP));
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Sets the NAS ip.
     *
     * @param ip new ip address; null to clear
     * @return self
     */
    public BasicElementConfig nasIp(String ip) {
        return (BasicElementConfig) setOrClear(NAS_IP, ip);
    }

    /**
     * Returns the RADIUS server ip.
     *
     * @return ip address or null if not set
     */
    public InetAddress radiusIp() {
        try {
            if (object == null) {
                return InetAddress.getByName(DEFAULT_RADIUS_IP);
            }
            return InetAddress.getByName(get(RADIUS_IP, DEFAULT_RADIUS_IP));
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Sets the RADIUS server ip.
     *
     * @param ip new ip address; null to clear
     * @return self
     */
    public BasicElementConfig radiusIp(String ip) {
        return (BasicElementConfig) setOrClear(RADIUS_IP, ip);
    }

    /**
     * Returns the RADIUS MAC address.
     *
     * @return mac address or null if not set
     */
    public String radiusMac() {
        if (object == null) {
            return DEFAULT_RADIUS_MAC;
        }
        return get(RADIUS_MAC, DEFAULT_RADIUS_MAC);
    }

    /**
     * Sets the RADIUS MAC address.
     *
     * @param mac new MAC address; null to clear
     * @return self
     */
    public BasicElementConfig radiusMac(String mac) {
        return (BasicElementConfig) setOrClear(RADIUS_MAC, mac);
    }

    /**
     * Returns the RADIUS MAC address.
     *
     * @return mac address or null if not set
     */
    public String nasMac() {
        if (object == null) {
            return DEFAULT_NAS_MAC;
        }
        return get(NAS_MAC, DEFAULT_NAS_MAC);
    }

    /**
     * Sets the RADIUS MAC address.
     *
     * @param mac new MAC address; null to clear
     * @return self
     */
    public BasicElementConfig nasMac(String mac) {
        return (BasicElementConfig) setOrClear(NAS_MAC, mac);
    }

    /**
     * Returns the RADIUS secret.
     *
     * @return radius secret or null if not set
     */
    public String radiusSecret() {
        if (object == null) {
            return DEFAULT_RADIUS_SECRET;
        }
        return get(RADIUS_SECRET, DEFAULT_RADIUS_SECRET);
    }

    /**
     * Sets the RADIUS secret.
     *
     * @param secret new MAC address; null to clear
     * @return self
     */
    public BasicElementConfig radiusSecret(String secret) {
        return (BasicElementConfig) setOrClear(RADIUS_SECRET, secret);
    }

    /**
     * Returns the ID of the RADIUS switch.
     *
     * @return radius switch ID or null if not set
     */
    public String radiusSwitch() {
        if (object == null) {
            return DEFAULT_RADIUS_SWITCH;
        }
        return get(RADIUS_SWITCH, DEFAULT_RADIUS_SWITCH);
    }

    /**
     * Sets the ID of the RADIUS switch.
     *
     * @param switchId new RADIUS switch ID; null to clear
     * @return self
     */
    public BasicElementConfig radiusSwitch(String switchId) {
        return (BasicElementConfig) setOrClear(RADIUS_SWITCH, switchId);
    }

    /**
     * Returns the RADIUS port.
     *
     * @return radius port or null if not set
     */
    public long radiusPort() {
        if (object == null) {
            return Integer.parseInt(DEFAULT_RADIUS_PORT);
        }
        return Integer.parseInt(get(RADIUS_PORT, "-1"));
    }

    /**
     * Sets the RADIUS port.
     *
     * @param port new RADIUS port; null to clear
     * @return self
     */
    public BasicElementConfig radiusPort(long port) {
        return (BasicElementConfig) setOrClear(RADIUS_PORT, port);
    }

}
