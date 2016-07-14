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

package org.onosproject.netconf;

import com.google.common.base.Preconditions;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

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
    private File keyFile;
    private DeviceId deviceId;


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
        Preconditions.checkArgument(!name.equals(""), "Empty device name");
        Preconditions.checkNotNull(port > 0, "Negative port");
        Preconditions.checkNotNull(ipAddress, "Null ip address");
        this.name = name;
        this.password = password;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Information for contacting the controller.
     *
     * @param name      the connection type
     * @param password  the password for the device
     * @param ipAddress the ip address
     * @param port      the tcp port
     * @param keyString the string containing the key.
     */
    public NetconfDeviceInfo(String name, String password, IpAddress ipAddress,
                             int port, String keyString) {
        Preconditions.checkArgument(!name.equals(""), "Empty device name");
        Preconditions.checkNotNull(port > 0, "Negative port");
        Preconditions.checkNotNull(ipAddress, "Null ip address");
        this.name = name;
        this.password = password;
        this.ipAddress = ipAddress;
        this.port = port;
        this.keyFile = new File(keyString);
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
     * @return int port address
     */
    public int port() {
        return port;
    }

    /**
     * Exposes the keyFile of the controller.
     *
     * @return int port address
     */
    public File getKeyFile() {
        return keyFile;
    }

    /**
     * Return the info about the device in a string.
     * String format: "netconf:name@ip:port"
     *
     * @return String device info
     */
    public String toString() {
        return "netconf:" + name + "@" + ipAddress + ":" + port;
    }

    /**
     * Return the DeviceId about the device containing the URI.
     *
     * @return DeviceId
     */
    public DeviceId getDeviceId() {
        if (deviceId == null) {
            try {
                deviceId = DeviceId.deviceId(new URI("netconf", ipAddress.toString() + ":" + port, null));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Unable to build deviceID for device " + toString(), e);
            }
        }
        return deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, port, name);
    }

    @Override
    public boolean equals(Object toBeCompared) {
        if (toBeCompared instanceof NetconfDeviceInfo) {
            NetconfDeviceInfo netconfDeviceInfo = (NetconfDeviceInfo) toBeCompared;
            if (netconfDeviceInfo.name().equals(name)
                    && netconfDeviceInfo.ip().equals(ipAddress)
                    && netconfDeviceInfo.port() == port
                    && netconfDeviceInfo.password().equals(password)) {
                return true;
            }
        }
        return false;
    }
}
