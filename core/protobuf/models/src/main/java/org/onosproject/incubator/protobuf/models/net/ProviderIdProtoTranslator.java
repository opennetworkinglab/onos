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
package org.onosproject.incubator.protobuf.models.net;

import org.onosproject.grpc.net.models.ProviderIdProtoOuterClass;
import org.onosproject.net.provider.ProviderId;

/**
 * gRPC ProviderId message to org.onosproject.net.provider.ProviderId conversion related utilities.
 */
public final class ProviderIdProtoTranslator {

    /**
     * Translates gRPC ProviderId message to {@link org.onosproject.net.provider.ProviderId}.
     *
     * @param providerId gRPC ProviderId message
     * @return {@link org.onosproject.net.provider.ProviderId} or null if providerId is a default instance
     */
    public static ProviderId translate(ProviderIdProtoOuterClass.ProviderIdProto providerId) {
        if (providerId.equals(ProviderIdProtoOuterClass.ProviderIdProto.getDefaultInstance())) {
            return null;
        }
        return new ProviderId(providerId.getScheme(), providerId.getId(), providerId.getAncillary());
    }

    /**
     * Translates {@link org.onosproject.net.provider.ProviderId} to gRPC ProviderId message.
     *
     * @param providerId {@link org.onosproject.net.provider.ProviderId}
     * @return gRPC ProviderId message
     */
    public static ProviderIdProtoOuterClass.ProviderIdProto translate(ProviderId providerId) {
        if (providerId == null) {
            return ProviderIdProtoOuterClass.ProviderIdProto.getDefaultInstance();
        }
        return ProviderIdProtoOuterClass.ProviderIdProto.newBuilder()
                .setScheme(providerId.scheme())
                .setId(providerId.id())
                .setAncillary(providerId.isAncillary())
                .build();
    }


    // Utility class not intended for instantiation.
    private ProviderIdProtoTranslator() {}

}

