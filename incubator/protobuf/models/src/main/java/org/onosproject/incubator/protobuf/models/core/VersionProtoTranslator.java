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
package org.onosproject.incubator.protobuf.models.core;

import org.onosproject.core.Version;
import org.onosproject.grpc.core.models.VersionProtoOuterClass.VersionProto;

import static org.onosproject.grpc.core.models.VersionProtoOuterClass.VersionProto.getDefaultInstance;

/**
 * gRPC Version message to equivalent ONOS Version conversion related utilities.
 */
public final class VersionProtoTranslator {

    /**
     * Translates {@link Version} to gRPC version message.
     *
     * @param version {@link Version}
     * @return gRPC message
     */
    public static VersionProto translate(Version version) {

        if (version != null) {
            return VersionProto.newBuilder()
                    .setMajor(version.major())
                    .setMinor(version.minor())
                    .setPatch(version.patch())
                    .setBuild(version.build())
                    .build();
        }

        return getDefaultInstance();
    }

    /**
     * Translates gRPC version message to {@link Version}.
     *
     * @param version gRPC message
     * @return {@link Version}
     */
    public static Version translate(VersionProto version) {

        return Version.version(version.getMajor(), version.getMinor(),
                version.getPatch(), version.getBuild());
    }

    // Utility class not intended for instantiation.
    private VersionProtoTranslator() {}
}
