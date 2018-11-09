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

package org.onosproject.incubator.protobuf.models.cfg;

import org.onosproject.cfg.ConfigProperty;
import org.onosproject.grpc.cfg.models.ConfigPropertyProtoOuterClass.ConfigPropertyProto;

/**
 * gRPC ConfigPropertyProto message to equivalent ONOS ConfigProperty conversion related utilities.
 */
public final class ConfigPropertyProtoTranslator {

    /**
     * Translates gRPC ConfigProperty message to {@link ConfigProperty}.
     *
     * @param configPropertyProto gRPC message
     * @return {@link ConfigProperty}
     */
    public static ConfigProperty translate(ConfigPropertyProto configPropertyProto) {

        ConfigProperty configProperty = ConfigProperty.defineProperty(configPropertyProto.getName(),
                                                                      ConfigPropertyEnumsProtoTranslator
                                                                              .translate(configPropertyProto
                                                                                                 .getType()),
                                                                      configPropertyProto.getDefaultValue(),
                                                                      configPropertyProto.getDescriptionBytes()
                                                                              .toString());
        return ConfigProperty.setProperty(configProperty, configPropertyProto.getValue());
    }

    /**
     * Translates {@link ConfigProperty} to gRPC ConfigProperty message.
     *
     * @param configProperty config property
     * @return gRPC ConfigProperty message
     */
    public static ConfigPropertyProto translate(ConfigProperty configProperty) {

        if (configProperty != null) {
            return ConfigPropertyProto.newBuilder()
                    .setName(configProperty.name())
                    .setType(ConfigPropertyEnumsProtoTranslator.translate(configProperty.type()))
                    .setDefaultValue(configProperty.defaultValue())
                    .setDescription(configProperty.description())
                    .setValue(configProperty.value())
                    .build();
        }

        return ConfigPropertyProto.getDefaultInstance();
    }

    // Utility class not intended for instantiation.
    private ConfigPropertyProtoTranslator() {}
}
