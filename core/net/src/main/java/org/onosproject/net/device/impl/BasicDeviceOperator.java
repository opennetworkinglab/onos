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
package org.onosproject.net.device.impl;

import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.ConfigOperator;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.slf4j.Logger;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementations of merge policies for various sources of device configuration
 * information. This includes applications, providers, and network configurations.
 */
public final class BasicDeviceOperator implements ConfigOperator {

    protected static final double DEFAULT_COORD = -1.0;
    private static final Logger log = getLogger(BasicDeviceOperator.class);

    private BasicDeviceOperator() {
    }

    /**
     * Generates a DeviceDescription containing fields from a DeviceDescription and
     * a DeviceConfig.
     *
     * @param bdc   the device config entity from network config
     * @param descr a DeviceDescription
     * @return DeviceDescription based on both sources
     */
    public static DeviceDescription combine(BasicDeviceConfig bdc, DeviceDescription descr) {
        if (bdc == null || descr == null) {
            return descr;
        }

        Device.Type type = descr.type();
        if (bdc.type() != null && bdc.type() != type) {
            type = bdc.type();
        }
        String manufacturer = descr.manufacturer();
        if (bdc.manufacturer() != null && !bdc.manufacturer().equals(manufacturer)) {
            manufacturer = bdc.manufacturer();
        }
        String hwVersion = descr.hwVersion();
        if (bdc.hwVersion() != null && !bdc.hwVersion().equals(hwVersion)) {
            hwVersion = bdc.hwVersion();
        }
        String swVersion = descr.swVersion();
        if (bdc.swVersion() != null && !bdc.swVersion().equals(swVersion)) {
            swVersion = bdc.swVersion();
        }
        String serial = descr.serialNumber();
        if (bdc.serial() != null && !bdc.serial().equals(serial)) {
            serial = bdc.serial();
        }

        SparseAnnotations sa = combine(bdc, descr.annotations());
        return new DefaultDeviceDescription(descr.deviceUri(), type, manufacturer,
                                            hwVersion, swVersion,
                                            serial, descr.chassisId(),
                                            descr.isDefaultAvailable(), sa);
    }

    /**
     * Generates an annotation from an existing annotation and DeviceConfig.
     *
     * @param bdc the device config entity from network config
     * @param an  the annotation
     * @return annotation combining both sources
     */
    public static SparseAnnotations combine(BasicDeviceConfig bdc, SparseAnnotations an) {
        DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();
        if (!Objects.equals(bdc.driver(), an.value(AnnotationKeys.DRIVER))) {
            newBuilder.set(AnnotationKeys.DRIVER, bdc.driver());
        }
        if (bdc.name() != null) {
            newBuilder.set(AnnotationKeys.NAME, bdc.name());
        }
        if (bdc.latitude() != DEFAULT_COORD) {
            newBuilder.set(AnnotationKeys.LATITUDE, Double.toString(bdc.latitude()));
        }
        if (bdc.longitude() != DEFAULT_COORD) {
            newBuilder.set(AnnotationKeys.LONGITUDE, Double.toString(bdc.longitude()));
        }
        if (bdc.rackAddress() != null) {
            newBuilder.set(AnnotationKeys.RACK_ADDRESS, bdc.rackAddress());
        }
        if (bdc.owner() != null) {
            newBuilder.set(AnnotationKeys.OWNER, bdc.owner());
        }
        if (bdc.managementAddress() != null) {
            newBuilder.set(AnnotationKeys.MANAGEMENT_ADDRESS, bdc.managementAddress());
        }
        DefaultAnnotations newAnnotations = newBuilder.build();
        return DefaultAnnotations.union(an, newAnnotations);
    }

    public static DeviceDescription descriptionOf(Device device) {
        checkNotNull(device, "Must supply non-null Device");
        return new DefaultDeviceDescription(device.id().uri(), device.type(),
                                            device.manufacturer(), device.hwVersion(),
                                            device.swVersion(), device.serialNumber(),
                                            device.chassisId(), (SparseAnnotations) device.annotations());
    }
}
