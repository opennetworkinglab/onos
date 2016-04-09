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
package org.onosproject.iptopology.api;

import org.onosproject.net.AbstractElement;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default ip device model implementation.
 */
public class DefaultIpDevice extends AbstractElement implements IpDevice {

    private final Type type;
    private final IpDeviceIdentifier deviceIdentifier;
    private final DeviceTed deviceTed;


    /**
     * For Serialization.
     */
    private DefaultIpDevice() {
        this.type = null;
        this.deviceIdentifier = null;
        this.deviceTed = null;
    }

    /**
     * Creates a network element attributed to the specified provider.
     *
     * @param providerId   identity of the provider
     * @param id           device identifier
     * @param type         device type
     * @param deviceIdentifier provides device identifier details
     * @param deviceTed device traffic engineering parameters
     * @param annotations  optional key/value annotations
     */
    public DefaultIpDevice(ProviderId providerId, DeviceId id, Type type,
                         IpDeviceIdentifier deviceIdentifier, DeviceTed deviceTed,
                         Annotations... annotations) {
        super(providerId, id, annotations);
        this.type = type;
        this.deviceIdentifier = deviceIdentifier;
        this.deviceTed = deviceTed;
    }

    @Override
    public DeviceId id() {
        return (DeviceId) id;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public IpDeviceIdentifier deviceIdentifier() {
        return deviceIdentifier;
    }

    @Override
    public DeviceTed deviceTed() {
        return deviceTed; }

    @Override
    public int hashCode() {
        return Objects.hash(type, deviceIdentifier, deviceTed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultIpDevice) {
            final DefaultIpDevice other = (DefaultIpDevice) obj;
            return Objects.equals(this.id, other.id) &&
                    Objects.equals(this.type, other.type) &&
                    Objects.equals(this.deviceIdentifier, other.deviceIdentifier) &&
                    Objects.equals(this.deviceTed, other.deviceTed);
        }
        return false;
    }
    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("deviceIdentifier", deviceIdentifier)
                .add("deviceTed", deviceTed)
                .toString();
    }
}