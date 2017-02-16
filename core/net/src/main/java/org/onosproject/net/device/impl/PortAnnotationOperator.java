/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.PortConfigOperator;
import org.onosproject.net.config.basics.PortAnnotationConfig;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;

/**
 * Implementations of {@link PortConfigOperator} to weave
 * annotations added via {@link PortAnnotationConfig}.
 */
public class PortAnnotationOperator implements PortConfigOperator {

    private NetworkConfigService networkConfigService;

    /**
     * Creates {@link PortAnnotationOperator} instance.
     */
    public PortAnnotationOperator() {
    }

    PortAnnotationOperator(NetworkConfigService networkConfigService) {
        bindService(networkConfigService);
    }

    @Override
    public void bindService(NetworkConfigService networkConfigService) {
        this.networkConfigService = networkConfigService;
    }

    private PortAnnotationConfig lookupConfig(ConnectPoint cp) {
        if (networkConfigService == null) {
            return null;
        }
        return networkConfigService.getConfig(cp, PortAnnotationConfig.class);
    }

    @Override
    public PortDescription combine(ConnectPoint cp, PortDescription descr) {
        PortAnnotationConfig cfg = lookupConfig(cp);
        if (cfg == null) {
            return descr;
        }
        Map<String, String> annotations = cfg.annotations();
        if (annotations.isEmpty()) {
            return descr;
        }

        Builder builder = DefaultAnnotations.builder();
        builder.putAll(descr.annotations());
        builder.putAll(annotations);

        return DefaultPortDescription.copyReplacingAnnotation(descr, builder.build());
    }

}
