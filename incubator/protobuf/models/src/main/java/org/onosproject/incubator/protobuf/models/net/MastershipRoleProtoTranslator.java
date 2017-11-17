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

import org.onosproject.grpc.net.models.MastershipRoleProtoOuterClass;
import org.onosproject.net.MastershipRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * gRPC MastershipRoleProto message to equivalent ONOS MastershipRole conversion related utilities.
 */
public final class MastershipRoleProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(MastershipRoleProtoTranslator.class);

    /**
     * Translates {@link MastershipRole} to gRPC MastershipRole.
     *
     * @param mastershipRole {@link MastershipRole}
     * @return gRPC message
     */
    public static MastershipRoleProtoOuterClass.MastershipRoleProto translate(MastershipRole mastershipRole) {

        switch (mastershipRole) {
            case MASTER:
                return MastershipRoleProtoOuterClass.MastershipRoleProto.MASTER;
            case STANDBY:
                return MastershipRoleProtoOuterClass.MastershipRoleProto.STANDBY;
            case NONE:
                return MastershipRoleProtoOuterClass.MastershipRoleProto.NONE;

            default:
                log.warn("Unexpected mastership role: {}", mastershipRole);
                return MastershipRoleProtoOuterClass.MastershipRoleProto.NONE;
        }
    }

    /**
     * Translate gRPC MastershipRole to {@link MastershipRole}.
     *
     * @param mastershipRole gRPC message
     * @return {@link MastershipRole}
     */
    public static Optional<Object> translate(MastershipRoleProtoOuterClass.MastershipRoleProto mastershipRole) {

        switch (mastershipRole) {
            case MASTER:
                return Optional.of(MastershipRole.MASTER);
            case STANDBY:
                return Optional.of(MastershipRole.STANDBY);
            case UNRECOGNIZED:
                return Optional.of(MastershipRole.NONE);

            default:
                log.warn("Unexpected mastership role: {}", mastershipRole);
                return Optional.empty();
        }
    }

    // Utility class not intended for instantiation.
    private MastershipRoleProtoTranslator() {}
}
