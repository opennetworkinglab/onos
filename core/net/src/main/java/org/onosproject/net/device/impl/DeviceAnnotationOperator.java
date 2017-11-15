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
package org.onosproject.net.device.impl;

import java.util.Map;
import java.util.Optional;

import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.DeviceConfigOperator;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.DeviceAnnotationConfig;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;

/**
 * Implementations of {@link DeviceConfigOperator} to weave
 * annotations added via {@link DeviceAnnotationConfig}.
 */
public class DeviceAnnotationOperator implements DeviceConfigOperator {

    private NetworkConfigService networkConfigService;

    /**
     * Creates {@link DeviceAnnotationOperator} instance.
     */
    public DeviceAnnotationOperator() {
    }

    DeviceAnnotationOperator(NetworkConfigService networkConfigService) {
        bindService(networkConfigService);
    }

    @Override
    public void bindService(NetworkConfigService networkConfigService) {
        this.networkConfigService = networkConfigService;
    }

    private DeviceAnnotationConfig lookupConfig(DeviceId deviceId) {
        if (networkConfigService == null) {
            return null;
        }
        return networkConfigService.getConfig(deviceId, DeviceAnnotationConfig.class);
    }

    @Override
    public DeviceDescription combine(DeviceId deviceId, DeviceDescription descr,
                                     Optional<Config> prevConfig) {
        DeviceAnnotationConfig cfg = lookupConfig(deviceId);
        if (cfg == null) {
            return descr;
        }
        Map<String, String> annotations = cfg.annotations();

        Builder builder = DefaultAnnotations.builder();
        builder.putAll(descr.annotations());
        if (prevConfig.isPresent()) {
            DeviceAnnotationConfig prevDeviceAnnotationConfig = (DeviceAnnotationConfig) prevConfig.get();
            for (String key : prevDeviceAnnotationConfig.annotations().keySet()) {
                if (!annotations.containsKey(key)) {
                    builder.remove(key);
                }
            }
        }
        builder.putAll(annotations);

        return DefaultDeviceDescription.copyReplacingAnnotation(descr, builder.build());
    }

}