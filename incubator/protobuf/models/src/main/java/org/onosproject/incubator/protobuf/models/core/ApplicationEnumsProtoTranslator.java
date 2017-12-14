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

import org.onosproject.app.ApplicationState;
import org.onosproject.core.ApplicationRole;
import org.onosproject.grpc.app.models.ApplicationEnumsProto.ApplicationRoleProto;
import org.onosproject.grpc.app.models.ApplicationEnumsProto.ApplicationStateProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * gRPC ApplicationEnumsProto message to equivalent ONOS Application Enums conversion related utilities.
 */
public final class ApplicationEnumsProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEnumsProtoTranslator.class);

    /**
     * Translates {@link ApplicationRole} to gRPC ApplicationRole.
     *
     * @param role {@link ApplicationRole}
     * @return gRPC message
     */
    public static ApplicationRoleProto translate(ApplicationRole role) {

        switch (role) {
            case USER:
                return ApplicationRoleProto.USER;
            case ADMIN:
                return ApplicationRoleProto.ADMIN;
            case UNSPECIFIED:
                return ApplicationRoleProto.UNSPECIFIED;

            default:
                log.warn("Unexpected application role: {}", role);
                return ApplicationRoleProto.UNSPECIFIED;
        }
    }

    /**
     * Translates gRPC ApplicationRole to {@link ApplicationRole}.
     *
     * @param roleProto gRPC message
     * @return {@link ApplicationRole}
     */
    public static Optional<ApplicationRole> translate(ApplicationRoleProto roleProto) {

        switch (roleProto) {
            case USER:
                return Optional.of(ApplicationRole.USER);
            case ADMIN:
                return Optional.of(ApplicationRole.ADMIN);
            case UNSPECIFIED:
                return Optional.of(ApplicationRole.UNSPECIFIED);

            default:
                log.warn("Unexpected application role proto: {}", roleProto);
                return Optional.empty();
        }
    }

    /**
     * Translate {@link ApplicationState} to gRPC ApplicationState.
     *
     * @param state {@link ApplicationState}
     * @return gRPC message
     */
    public static ApplicationStateProto translate(ApplicationState state) {

        switch (state) {
            case ACTIVE:
                return ApplicationStateProto.ACTIVE;
            case INSTALLED:
                return ApplicationStateProto.INSTALLED;

            default:
                log.warn("Unexpected application state: {}", state);
                return ApplicationStateProto.INSTALLED;
        }
    }

    /**
     * Translate gRPC ApplicationState to {@link ApplicationState}.
     *
     * @param stateProto gRPC message
     * @return {@link ApplicationState}
     */
    public static Optional<ApplicationState> translate(ApplicationStateProto stateProto) {

        switch (stateProto) {
            case ACTIVE:
                return Optional.of(ApplicationState.ACTIVE);
            case INSTALLED:
                return Optional.of(ApplicationState.INSTALLED);

            default:
                log.warn("Unexpected application state proto: {}", stateProto);
                return Optional.empty();
        }
    }

    // Utility class not intended for instantiation.
    private ApplicationEnumsProtoTranslator() {}
}
