/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import java.util.Set;

import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.Port;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class SuppressionRules {

    public static final String ANY_VALUE = "(any)";

    private final Set<DeviceId> suppressedDevice;
    private final Set<Device.Type> suppressedDeviceType;
    private final Map<String, String> suppressedAnnotation;

    public SuppressionRules(Set<DeviceId> suppressedDevice,
                     Set<Device.Type> suppressedType,
                     Map<String, String> suppressedAnnotation) {

        this.suppressedDevice = ImmutableSet.copyOf(suppressedDevice);
        this.suppressedDeviceType = ImmutableSet.copyOf(suppressedType);
        this.suppressedAnnotation = ImmutableMap.copyOf(suppressedAnnotation);
    }

    public boolean isSuppressed(Device device) {
        if (suppressedDevice.contains(device.id())) {
            return true;
        }
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

    Set<DeviceId> getSuppressedDevice() {
        return suppressedDevice;
    }

    Set<Device.Type> getSuppressedDeviceType() {
        return suppressedDeviceType;
    }

    Map<String, String> getSuppressedAnnotation() {
        return suppressedAnnotation;
    }
}
