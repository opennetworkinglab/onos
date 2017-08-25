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
package org.onosproject.incubator.protobuf.models.security;

import org.onosproject.grpc.security.models.PermissionProtoOuterClass.PermissionProto;
import org.onosproject.security.Permission;

import static org.onosproject.grpc.security.models.PermissionProtoOuterClass.PermissionProto.getDefaultInstance;

/**
 * gRPC Permission message to equivalent ONOS Permission conversion related utilities.
 */
public final class PermissionProtoTranslator {

    /**
     * Translate {@link Permission} to gRPC permission message.
     *
     * @param permission {@link Permission}
     * @return gRPC message
     */
    public static PermissionProto translate(Permission permission) {

        if (permission != null) {
            return PermissionProto.newBuilder()
                    .setActions(permission.getActions())
                    .setClassname(permission.getClassName())
                    .setName(permission.getName())
                    .build();
        }

        return getDefaultInstance();
    }

    /**
     * Translate gRPC permission message to {@link Permission}.
     *
     * @param permission gRPC message
     * @return {@link Permission}
     */
    public static Permission translate(PermissionProto permission) {

        return new Permission(permission.getClassname(),
                permission.getName(), permission.getActions());
    }

    // Utility class not intended for instantiation.
    private PermissionProtoTranslator() {}
}
