/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman;

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A container class that is used to request control metric of remote node.
 */
public class ControlMetricsRequest {
    private final ControlMetricType type;
    private Optional<DeviceId> deviceId;
    private String resourceName;
    private int duration;
    private TimeUnit unit;

    /**
     * Instantiates a new control metric request of the control metric type and
     * device identifier.
     *
     * @param type     control metric type
     * @param deviceId device identifier
     */
    public ControlMetricsRequest(ControlMetricType type, Optional<DeviceId> deviceId) {
        this.type = type;
        this.deviceId = deviceId;
    }

    /**
     * Instantiates a new control metric request of the control metric type and
     * device identifier with the given projected time range.
     *
     * @param type     control metric type
     * @param duration projected time duration
     * @param unit     projected time unit
     * @param deviceId device dientifer
     */
    public ControlMetricsRequest(ControlMetricType type, int duration, TimeUnit unit,
                                 Optional<DeviceId> deviceId) {
        this.type = type;
        this.deviceId = deviceId;
        this.duration = duration;
        this.unit = unit;
    }

    /**
     * Instantiates a new control metric request of the control metric type and
     * resource name.
     *
     * @param type         control metric type
     * @param resourceName resource name
     */
    public ControlMetricsRequest(ControlMetricType type, String resourceName) {
        this.type = type;
        this.resourceName = resourceName;
    }

    /**
     * Instantiates a new control metric request of the control metric type and
     * resource name with the given projected time range.
     *
     * @param type         control metric type
     * @param duration     projected time duration
     * @param unit         projected time unit
     * @param resourceName resource name
     */
    public ControlMetricsRequest(ControlMetricType type, int duration, TimeUnit unit,
                                 String resourceName) {
        this.type = type;
        this.resourceName = resourceName;
        this.duration = duration;
        this.unit = unit;
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

    /**
     * Obtains projected time duration.
     *
     * @return projected time duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Obtains projected time unit.
     *
     * @return projected time unit
     */
    public TimeUnit getUnit() {
        return unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, deviceId, resourceName, duration, unit.toString());
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
                    Objects.equals(this.resourceName, other.resourceName) &&
                    Objects.equals(this.duration, other.duration) &&
                    Objects.equals(this.unit, other.unit);
        }
        return false;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper;
        helper = toStringHelper(this)
                .add("type", type)
                .add("resourceName", resourceName)
                .add("duration", duration)
                .add("timeUnit", unit);
        if (deviceId != null) {
            helper.add("deviceId", deviceId.get());
        }
        return helper.toString();
    }
}
