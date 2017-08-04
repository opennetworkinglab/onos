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

package org.onosproject.provider.lldp.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.Element;
import org.onosproject.net.Port;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.base.MoreObjects;

public class SuppressionRules {

    public static final String ANY_VALUE = "(any)";

    private final Set<Device.Type> suppressedDeviceType;
    private final Map<String, String> suppressedAnnotation;

    public SuppressionRules(Set<Device.Type> suppressedType,
                            Map<String, String> suppressedAnnotation) {

        this.suppressedDeviceType = ImmutableSet.copyOf(suppressedType);
        this.suppressedAnnotation = ImmutableMap.copyOf(suppressedAnnotation);
    }

    public boolean isSuppressed(Device device) {
        if (suppressedDeviceType.contains(device.type())) {
            return true;
        }
        final Annotations annotations = device.annotations();
        if (containsSuppressionAnnotation(annotations)) {
            return true;
        }
        return false;
    }

    public boolean isSuppressed(Port port) {
        Element parent = port.element();
        if (parent instanceof Device) {
            if (isSuppressed((Device) parent)) {
                return true;
            }
        }

        final Annotations annotations = port.annotations();
        if (containsSuppressionAnnotation(annotations)) {
            return true;
        }
        return false;
    }

    private boolean containsSuppressionAnnotation(final Annotations annotations) {
        for (Entry<String, String> entry : suppressedAnnotation.entrySet()) {
            final String suppValue = entry.getValue();
            final String suppKey = entry.getKey();
            if (suppValue == ANY_VALUE) {
                if (annotations.keys().contains(suppKey)) {
                    return true;
                }
            } else {
                if (suppValue.equals(annotations.value(suppKey))) {
                    return true;
                }
            }
        }
        return false;
    }

    Set<Device.Type> getSuppressedDeviceType() {
        return suppressedDeviceType;
    }

    Map<String, String> getSuppressedAnnotation() {
        return suppressedAnnotation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suppressedDeviceType,
                            suppressedAnnotation);
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && getClass() == object.getClass()) {
            SuppressionRules that = (SuppressionRules) object;
            return Objects.equals(this.suppressedDeviceType,
                                      that.suppressedDeviceType)
                    && Objects.equals(this.suppressedAnnotation,
                                      that.suppressedAnnotation);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("suppressedDeviceType", suppressedDeviceType)
                .add("suppressedAnnotation", suppressedAnnotation)
                .toString();
    }
}
