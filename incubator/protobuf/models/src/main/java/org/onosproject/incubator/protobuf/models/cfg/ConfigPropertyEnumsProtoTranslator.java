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

import org.onosproject.cfg.ConfigProperty.Type;
import org.onosproject.grpc.cfg.models.ConfigPropertyEnumsProto.ConfigPropertyTypeProto;

/**
 * gRPC ConfigProperty.Type message to equivalent ONOS enum conversion related utilities.
 */
public final class ConfigPropertyEnumsProtoTranslator {

    /**
     * Translates gRPC ConfigProperty type to {@link Type}.
     *
     * @param configPropertyTypeProto config Property proto type
     * @return {@link Type}
     */
    public static Type translate(ConfigPropertyTypeProto configPropertyTypeProto) {

        return Type.valueOf(configPropertyTypeProto.name());
    }

    /**
     * Translates {@link Type} to gRPC ConfigProperty type.
     *
     * @param type config Property type
     * @return gRPC ConfigProperty type
     */
    public static ConfigPropertyTypeProto translate(Type type) {

        return ConfigPropertyTypeProto.valueOf(type.name());
    }

    // Utility class not intended for instantiation.
    private ConfigPropertyEnumsProtoTranslator() {}
}
