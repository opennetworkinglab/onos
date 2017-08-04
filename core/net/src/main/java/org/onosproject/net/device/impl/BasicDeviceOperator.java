/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementations of merge policies for various sources of device configuration
 * information. This includes applications, providers, and network configurations.
 */
public final class BasicDeviceOperator extends BasicElementOperator {

    private BasicDeviceOperator() {
    }

    /**
     * Generates a DeviceDescription containing fields from a DeviceDescription and
     * a DeviceConfig.
     *
     * @param cfg   the device config entity from network config
     * @param descr a DeviceDescription
     * @return DeviceDescription based on both sources
     */
    public static DeviceDescription combine(BasicDeviceConfig cfg, DeviceDescription descr) {
        if (cfg == null || descr == null) {
            return descr;
        }

        Device.Type type = descr.type();
        if (cfg.type() != null && cfg.type() != type) {
            type = cfg.type();
        }
        String manufacturer = descr.manufacturer();
        if (cfg.manufacturer() != null && !cfg.manufacturer().equals(manufacturer)) {
            manufacturer = cfg.manufacturer();
        }
        String hwVersion = descr.hwVersion();
        if (cfg.hwVersion() != null && !cfg.hwVersion().equals(hwVersion)) {
            hwVersion = cfg.hwVersion();
        }
        String swVersion = descr.swVersion();
        if (cfg.swVersion() != null && !cfg.swVersion().equals(swVersion)) {
            swVersion = cfg.swVersion();
        }
        String serial = descr.serialNumber();
        if (cfg.serial() != null && !cfg.serial().equals(serial)) {
            serial = cfg.serial();
        }

        SparseAnnotations sa = combine(cfg, descr.annotations());
        return new DefaultDeviceDescription(descr.deviceUri(), type, manufacturer,
                hwVersion, swVersion,
                serial, descr.chassisId(),
                descr.isDefaultAvailable(), sa);
    }

    /**
     * Generates an annotation from an existing annotation and DeviceConfig.
     *
     * @param cfg the device config entity from network config
     * @param an  the annotation
     * @return annotation combining both sources
     */
    public static SparseAnnotations combine(BasicDeviceConfig cfg, SparseAnnotations an) {
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.putAll(an);
        if (!Objects.equals(cfg.driver(), an.value(AnnotationKeys.DRIVER))) {
            builder.set(AnnotationKeys.DRIVER, cfg.driver());
        }

        combineElementAnnotations(cfg, builder);

        if (cfg.managementAddress() != null) {
            builder.set(AnnotationKeys.MANAGEMENT_ADDRESS, cfg.managementAddress());
        }

        return builder.build();
    }

    /**
     * Returns a description of the given device.
     *
     * @param device the device
     * @return a description of the device
     */
    public static DeviceDescription descriptionOf(Device device) {
        checkNotNull(device, "Must supply non-null Device");
        return new DefaultDeviceDescription(device.id().uri(), device.type(),
                device.manufacturer(), device.hwVersion(),
                device.swVersion(), device.serialNumber(),
                device.chassisId(), (SparseAnnotations) device.annotations());
    }
}
