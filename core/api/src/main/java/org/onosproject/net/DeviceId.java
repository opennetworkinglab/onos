/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net;

import java.net.URI;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Immutable representation of a device identity.
 */
public final class DeviceId extends ElementId {

    /**
     * Represents either no device, or an unspecified device.
     */
    public static final DeviceId NONE = deviceId("none:none");

    private static final int DEVICE_ID_MAX_LENGTH = 1024;

    private final URI uri;
    private final String str;

    // Public construction is prohibited
    private DeviceId(URI uri) {
        this.uri = uri;
        this.str = uri.toString().toLowerCase();
    }


    // Default constructor for serialization
    protected DeviceId() {
        this.uri = null;
        this.str = null;
    }

    /**
     * Creates a device id using the supplied URI.
     *
     * @param uri device URI
     * @return DeviceId
     */
    public static DeviceId deviceId(URI uri) {
        return new DeviceId(uri);
    }

    /**
     * Creates a device id using the supplied URI string.
     *
     * @param string device URI string
     * @return DeviceId
     */
    public static DeviceId deviceId(String string) {
        checkArgument(string.length() <= DEVICE_ID_MAX_LENGTH,
                "deviceId exceeds maximum length " + DEVICE_ID_MAX_LENGTH);
        return deviceId(URI.create(string));
    }

    /**
     * Returns the backing URI.
     *
     * @return backing URI
     */
    public URI uri() {
        return uri;
    }

    @Override
    public int hashCode() {
        return str.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DeviceId) {
            final DeviceId that = (DeviceId) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.str, that.str);
        }
        return false;
    }

    @Override
    public String toString() {
        return str;
    }

}
