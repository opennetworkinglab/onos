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

import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.grpc.core.models.ApplicationIdProtoOuterClass.ApplicationIdProto;

import static org.onosproject.grpc.core.models.ApplicationIdProtoOuterClass.ApplicationIdProto.getDefaultInstance;

/**
 * gRPC ApplicationIdProto message to equivalent ONOS ApplicationId conversion related utilities.
 */
public final class ApplicationIdProtoTranslator {

    /**
     * Translates gRPC ApplicationId to {@link ApplicationId}.
     *
     * @param applicationId gRPC message
     * @return {@link ApplicationId}
     */
    public static ApplicationId translate(ApplicationIdProto applicationId) {

        return new DefaultApplicationId(applicationId.getId(), applicationId.getName());
    }

    /**
     * Translates {@link ApplicationId} to gRPC ApplicationId message.
     *
     * @param applicationId {@link ApplicationId}
     * @return gRPC ApplicationId message
     */
    public static ApplicationIdProto translate(ApplicationId applicationId) {

        if (applicationId != null) {
            return ApplicationIdProto.newBuilder()
                    .setId(applicationId.id())
                    .setName(applicationId.name())
                    .build();
        }

        return getDefaultInstance();
    }

    // utility class not intended for instantiation.
    private ApplicationIdProtoTranslator() {}
}
