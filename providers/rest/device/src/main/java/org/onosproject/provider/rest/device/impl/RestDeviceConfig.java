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

package org.onosproject.provider.rest.device.impl;

import com.google.common.annotations.Beta;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.protocol.rest.RestSBDevice.AuthenticationScheme;

/**
 * Configuration to push devices to the REST provider.
 */
@Beta
public class RestDeviceConfig extends Config<DeviceId> {

    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String PROTOCOL = "protocol";
    private static final String URL = "url";
    private static final String TESTURL = "testUrl";
    private static final String MANUFACTURER = "manufacturer";
    private static final String HWVERSION = "hwVersion";
    private static final String SWVERSION = "swVersion";
    private static final String PROXY = "isProxy";
    private static final String AUTHENTICATION_SCHEME = "authenticationScheme";
    private static final String TOKEN = "token";

    @Override
    public boolean isValid() {
        return hasOnlyFields(IP, PORT, USERNAME, PASSWORD, PROTOCOL, URL,
                TESTURL, MANUFACTURER, HWVERSION, SWVERSION, PROXY,
                AUTHENTICATION_SCHEME, TOKEN) &&
                ip() != null;
    }

    /**
     * Gets the Ip of the REST device.
     *
     * @return ip
     */
    public IpAddress ip() {
        return IpAddress.valueOf(get(IP, extractIpPort().getKey()));
    }

    /**
     * Gets the port of the REST device.
     *
     * @return port
     */
    public int port() {
        return get(PORT, extractIpPort().getValue());
    }

    /**
     * Gets the protocol of the REST device.
     *
     * @return protocol
     */
    public String protocol() {
        return get(PROTOCOL, "http");
    }

    /**
     * Gets the username of the REST device.
     *
     * @return username
     */
    public String username() {
        return get(USERNAME, "");
    }

    /**
     * Gets the password of the REST device.
     *
     * @return password
     */
    public String password() {
        return get(PASSWORD, "");
    }

    /**
     * Gets the base url of the REST device.
     *
     * @return base url for the device config tree
     */
    public String url() {
        return get(URL, "");
    }

    /**
     * Gets the testUrl of the REST device.
     *
     * @return testUrl to test the device connection
     */
    public String testUrl() {
        return get(TESTURL, "");
    }

    /**
     * Gets the manufacturer of the REST device.
     *
     * @return manufacturer
     */
    public String manufacturer() {
        return get(MANUFACTURER, "");
    }

    /**
     * Gets the hwversion of the REST device.
     *
     * @return hwversion
     */
    public String hwVersion() {
        return get(HWVERSION, "");
    }

    /**
     * Gets the swversion of the REST device.
     *
     * @return swversion
     */
    public String swVersion() {
        return get(SWVERSION, "");
    }

    /**
     * Gets whether the REST device is a proxy or not.
     *
     * @return proxy
     */
    public boolean isProxy() {
        if (!hasField(PROXY)) {
            return false;
        }
        return get(PROXY, false);
    }

    /**
     * Gets the authentication type of the REST device.
     * Default is 'basic' if username is defined, else default is no_authentication.
     *
     * @return authentication
     */
    public AuthenticationScheme authenticationScheme() {
        // hack for backward compatibility
        if (!hasField(AUTHENTICATION_SCHEME)) {
            if (hasField(USERNAME)) {
                return AuthenticationScheme.BASIC;
            }
        }
        return AuthenticationScheme.valueOf(get(AUTHENTICATION_SCHEME, "NO_AUTHENTICATION").toUpperCase());
    }

    /**
     * Gets the token of the REST device.
     *
     * @return token
     */
    public String token() {
        return get(TOKEN, "");
    }

    private Pair<String, Integer> extractIpPort() {
        String info = subject.toString();
        if (info.startsWith(RestDeviceProvider.REST)) {
            //+1 is due to length of colon separator
            String ip = info.substring(info.indexOf(":") + 1, info.lastIndexOf(":"));
            int port = Integer.parseInt(info.substring(info.lastIndexOf(":") + 1));
            return Pair.of(ip, port);
        }
        return null;
    }

}