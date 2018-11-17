/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.net.host.impl;

import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.HostId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.HostConfigOperator;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.HostAnnotationConfig;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;

import java.util.Map;
import java.util.Optional;

/**
 * Implementations of {@link HostConfigOperator} to weave
 * annotations added via {@link HostAnnotationConfig}.
 */
public class HostAnnotationOperator implements HostConfigOperator {

    private NetworkConfigService networkConfigService;

    /**
     * Creates {@link HostAnnotationOperator} instance.
     */
    public HostAnnotationOperator() {
    }

    HostAnnotationOperator(NetworkConfigService networkConfigService) {
        bindService(networkConfigService);
    }

    @Override
    public void bindService(NetworkConfigService networkConfigService) {
        this.networkConfigService = networkConfigService;
    }

    private HostAnnotationConfig lookupConfig(HostId hostId) {
        if (networkConfigService == null) {
            return null;
        }
        return networkConfigService.getConfig(hostId, HostAnnotationConfig.class);
    }

    @Override
    public HostDescription combine(HostId hostId, HostDescription descr,
                                   Optional<Config> prevConfig) {
        HostAnnotationConfig cfg = lookupConfig(hostId);
        if (cfg == null) {
            return descr;
        }
        Map<String, String> annotations = cfg.annotations();

        Builder builder = DefaultAnnotations.builder();
        builder.putAll(descr.annotations());
        if (prevConfig.isPresent()) {
            HostAnnotationConfig prevHostAnnotationConfig = (HostAnnotationConfig) prevConfig.get();
            for (String key : prevHostAnnotationConfig.annotations().keySet()) {
                if (!annotations.containsKey(key)) {
                    builder.remove(key);
                }
            }
        }
        builder.putAll(annotations);

        return DefaultHostDescription.copyReplacingAnnotation(descr, builder.build());
    }

}
