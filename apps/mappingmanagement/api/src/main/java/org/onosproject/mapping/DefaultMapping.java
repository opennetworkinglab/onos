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
package org.onosproject.mapping;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation class for mapping.
 */
public class DefaultMapping implements Mapping {

    private final DeviceId deviceId;
    private final MappingKey key;
    private final MappingValue value;

    private final MappingId id;

    private final Short appId;

    /**
     * Creates a mapping specified with mapping information.
     *
     * @param mapping mapping information
     */
    public DefaultMapping(Mapping mapping) {
        this.deviceId = mapping.deviceId();
        this.key = mapping.key();
        this.value = mapping.value();
        this.appId = mapping.appId();
        this.id = mapping.id();
    }

    /**
     * Creates a mapping specified with several parameters.
     *
     * @param deviceId device identifier
     * @param key      mapping key
     * @param value    mapping value
     * @param id       mapping identifier
     */
    public DefaultMapping(DeviceId deviceId, MappingKey key, MappingValue value,
                          MappingId id) {
        this.deviceId = deviceId;
        this.key = key;
        this.value = value;
        this.appId = (short) (id.value() >>> 48);
        this.id = id;
    }

    @Override
    public MappingId id() {
        return id;
    }

    @Override
    public short appId() {
        return appId;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public MappingKey key() {
        return key;
    }

    @Override
    public MappingValue value() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, key, value, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultMapping) {
            DefaultMapping that = (DefaultMapping) obj;
            return Objects.equals(deviceId, that.deviceId) &&
                    Objects.equals(key, that.key) &&
                    Objects.equals(value, that.value) &&
                    Objects.equals(id, that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", Long.toHexString(id.value()))
                .add("deviceId", deviceId)
                .add("key", key)
                .add("value", value)
                .toString();
    }

    /**
     * Returns a default mapping builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default mapping builder.
     */
    public static final class Builder implements Mapping.Builder {

        private MappingId id;
        private ApplicationId appId;
        private DeviceId deviceId;
        private MappingKey key = DefaultMappingKey.builder().build();
        private MappingValue value = DefaultMappingValue.builder().build();

        @Override
        public Mapping.Builder withId(long id) {
            this.id = MappingId.valueOf(id);
            return this;
        }

        @Override
        public Mapping.Builder fromApp(ApplicationId appId) {
            this.appId = appId;
            return this;
        }

        @Override
        public Mapping.Builder forDevice(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Override
        public Mapping.Builder withKey(MappingKey key) {
            this.key = key;
            return this;
        }

        @Override
        public Mapping.Builder withValue(MappingValue value) {
            this.value = value;
            return this;
        }

        @Override
        public Mapping build() {

            checkArgument((id != null) || (appId != null), "Either an application" +
                    " id or a mapping id must be supplied");
            checkNotNull(key, "Mapping key cannot be null");
            checkNotNull(deviceId, "Must refer to a device");

            return new DefaultMapping(deviceId, key, value, id);
        }
    }
}
