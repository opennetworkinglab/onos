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
package org.onosproject.cpman.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.net.DeviceId;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A container class that is used to request control metric of remote node.
 */
public class ControlMetricsRequest {
    private final ControlMetricType type;
    private Optional<DeviceId> deviceId;
    private String resourceName;

    /**
     * Instantiates a new control metric request with the given control metric
     * type and device identifier.
     *
     * @param type     control metric type
     * @param deviceId device identifier
     */
    public ControlMetricsRequest(ControlMetricType type, Optional<DeviceId> deviceId) {
        this.type = type;
        this.deviceId = deviceId;
    }

    /**
     * Instantiates a new control metric request with the given control metric
     * type and resource name.
     *
     * @param type         control metric type
     * @param resourceName resource name
     */
    public ControlMetricsRequest(ControlMetricType type, String resourceName) {
        this.type = type;
        this.resourceName = resourceName;
    }

    /**
     * Obtains control metric type.
     *
     * @return control metric type
     */
    public ControlMetricType getType() {
        return type;
    }

    /**
     * Obtains resource name.
     *
     * @return resource name
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Obtains device identifier.
     *
     * @return device identifier
     */
    public Optional<DeviceId> getDeviceId() {
        return deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, deviceId, resourceName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ControlMetricsRequest) {
            final ControlMetricsRequest other = (ControlMetricsRequest) obj;
            return Objects.equals(this.type, other.type) &&
                    Objects.equals(this.deviceId, other.deviceId) &&
                    Objects.equals(this.resourceName, other.resourceName);
        }
        return false;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper;
        helper = toStringHelper(this)
                .add("type", type)
                .add("resourceName", resourceName);
        if (deviceId != null) {
            helper.add("deviceId", deviceId.get());
        }
        return helper.toString();
    }
}
