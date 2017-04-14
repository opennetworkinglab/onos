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

package org.onosproject.provider.tl1.device.impl;

import com.google.common.annotations.Beta;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;

/**
 * Configuration to push devices to the TL1 provider.
 */
@Beta
public class Tl1DeviceConfig extends Config<DeviceId> {

    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    @Override
    public boolean isValid() {
        return hasOnlyFields(IP, PORT, USERNAME, PASSWORD) &&
                ip() != null;
    }

    /**
     * Gets the Ip of the TL1 device.
     *
     * @return ip
     */
    public IpAddress ip() {
        return IpAddress.valueOf(get(IP, extractIpPort().getKey()));
    }

    /**
     * Gets the port of the TL1 device.
     *
     * @return port
     */
    public int port() {
        return get(PORT, extractIpPort().getValue());
    }

    /**
     * Gets the username of the TL1 device.
     *
     * @return username
     */
    public String username() {
        return get(USERNAME, "");
    }

    /**
     * Gets the password of the TL1 device.
     *
     * @return password
     */
    public String password() {
        return get(PASSWORD, "");
    }


    private Pair<String, Integer> extractIpPort() {
        String info = subject.toString();
        if (info.startsWith(Tl1DeviceProvider.TL1)) {
            //+1 is due to length of colon separator
            String ip = info.substring(info.indexOf(":") + 1, info.lastIndexOf(":"));
            int port = Integer.parseInt(info.substring(info.lastIndexOf(":") + 1));
            return Pair.of(ip, port);
        }
        return null;
    }
}