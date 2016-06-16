/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.api.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A BMv2 device.
 */
@Beta
public final class Bmv2Device {

    public static final String N_A = "n/a";

    public static final String SCHEME = "bmv2";
    public static final String PROTOCOL = "bmv2-thrift";
    public static final String MANUFACTURER = "p4.org";
    public static final String HW_VERSION = "bmv2";
    public static final String SW_VERSION = "1.0.0";
    public static final String SERIAL_NUMBER = N_A;

    private final String thriftServerHost;
    private final int thriftServerPort;
    private final int internalDeviceId;

    /**
     * Creates a new BMv2 device object.
     *
     * @param thriftServerHost the hostname or IP address of the Thrift RPC server running on the device
     * @param thriftServerPort the listening port used by the device Thrift RPC server
     * @param internalDeviceId the internal numeric device ID
     */
    public Bmv2Device(String thriftServerHost, int thriftServerPort, int internalDeviceId) {
        this.thriftServerHost = checkNotNull(thriftServerHost, "host cannot be null");
        this.thriftServerPort = checkNotNull(thriftServerPort, "port cannot be null");
        this.internalDeviceId = internalDeviceId;
    }

    /**
     * Returns a Bmv2Device representing the given deviceId.
     *
     * @param deviceId a deviceId
     * @return
     */
    public static Bmv2Device of(DeviceId deviceId) {
        return DeviceIdParser.parse(checkNotNull(deviceId, "deviceId cannot be null"));
    }

    /**
     * Returns the hostname or IP address of the Thrift RPC server running on the device.
     *
     * @return a string value
     */
    public String thriftServerHost() {
        return thriftServerHost;
    }

    /**
     * Returns the listening port of the Thrift RPC server running on the device.
     *
     * @return an integer value
     */
    public int thriftServerPort() {
        return thriftServerPort;
    }

    /**
     * Returns the BMv2-internal device ID, which is an integer arbitrary chosen at device boot.
     * Such an ID must not be confused with the ONOS-internal {@link org.onosproject.net.DeviceId}.
     *
     * @return an integer value
     */
    public int internalDeviceId() {
        return internalDeviceId;
    }

    /**
     * Returns a new ONOS device ID for this device.
     *
     * @return a new device ID
     */
    public DeviceId asDeviceId() {
        try {
            return DeviceId.deviceId(new URI(SCHEME, this.thriftServerHost + ":" + this.thriftServerPort,
                                             String.valueOf(this.internalDeviceId)));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to build deviceID for device " + this.toString(), e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(thriftServerHost, thriftServerPort, internalDeviceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2Device other = (Bmv2Device) obj;
        return Objects.equal(this.thriftServerHost, other.thriftServerHost)
                && Objects.equal(this.thriftServerPort, other.thriftServerPort)
                && Objects.equal(this.internalDeviceId, other.internalDeviceId);
    }

    @Override
    public String toString() {
        return asDeviceId().toString();
    }

    private static class DeviceIdParser {

        private static final Pattern REGEX = Pattern.compile(SCHEME + ":(.+):(\\d+)#(\\d+)");

        public static Bmv2Device parse(DeviceId deviceId) {
            Matcher matcher = REGEX.matcher(deviceId.toString());
            if (matcher.find()) {
                String host = matcher.group(1);
                int port = Integer.valueOf(matcher.group(2));
                int internalDeviceId = Integer.valueOf(matcher.group(3));
                return new Bmv2Device(host, port, internalDeviceId);
            } else {
                throw new RuntimeException("Unable to parse bmv2 device id string: " + deviceId.toString());
            }
        }
    }
}
