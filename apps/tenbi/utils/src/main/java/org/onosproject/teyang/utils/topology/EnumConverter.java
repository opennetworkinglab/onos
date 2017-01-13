/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.teyang.utils.topology;

import org.onosproject.tetopology.management.api.TeStatus;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeAdminStatus;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeOperStatus;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.teadminstatus.TeAdminStatusEnum;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.teoperstatus.TeOperStatusEnum;

/**
 * The Enum conversion functions.
 */
public final class EnumConverter {

    // no instantiation
    private EnumConverter() {
    }

    /**
     * Converts YANG Operation Status Enum to TE Topology TeStatus Enum.
     *
     * @param opStatus YANG Operation Status
     * @return the equivalent Enum from TE Topology TeStatus or null if not
     *         found
     */
    public static TeStatus yang2TeSubsystemOpStatus(TeOperStatus opStatus) {
        if (opStatus == null) {
            return null;
        }

        switch (opStatus.enumeration()) {
        case DOWN:
            return TeStatus.DOWN;
        case UP:
            return TeStatus.UP;
        case MAINTENANCE:
            return TeStatus.MAINTENANCE;
        case PREPARING_MAINTENANCE:
            return TeStatus.PREPARING_MAINTENANCE;
        case TESTING:
            return TeStatus.TESTING;
        case UNKNOWN:
            return TeStatus.UNKNOWN;
        default:
            return null;
        }
    }

    /**
     * Converts YANG TeAdminStatus Enum to TE Topology TeStatus Enum.
     *
     * @param adminStatus YANG Admin Status
     * @return the equivalent Enum from TE Topology TeStatus or null if not
     *         found
     */
    public static TeStatus yang2TeSubsystemAdminStatus(TeAdminStatus adminStatus) {
        if (adminStatus == null) {
            return TeStatus.UNKNOWN;
        }

        switch (adminStatus.enumeration()) {
        case DOWN:
            return TeStatus.DOWN;
        case UP:
            return TeStatus.UP;
        case TESTING:
            return TeStatus.TESTING;
        case MAINTENANCE:
            return TeStatus.MAINTENANCE;
        case PREPARING_MAINTENANCE:
            return TeStatus.PREPARING_MAINTENANCE;
        default:
            return TeStatus.UNKNOWN;
        }
    }

    /**
     * Converts TE Topology TeStatus Enum to YANG TeAdminStatus Enum.
     *
     * @param adminStatus TE Topology admin status
     * @return the equivalent Enum from YANG TeAdminStatus or null if not found
     */
    public static TeAdminStatus teSubsystem2YangAdminStatus(TeStatus adminStatus) {
        if (adminStatus == null) {
            return null;
        }

        switch (adminStatus) {
        case DOWN:
            return TeAdminStatus.of(TeAdminStatusEnum.DOWN);
        case UP:
            return TeAdminStatus.of(TeAdminStatusEnum.UP);
        case TESTING:
            return TeAdminStatus.of(TeAdminStatusEnum.TESTING);
        case MAINTENANCE:
            return TeAdminStatus.of(TeAdminStatusEnum.MAINTENANCE);
        case PREPARING_MAINTENANCE:
            return TeAdminStatus.of(TeAdminStatusEnum.PREPARING_MAINTENANCE);
        case UNKNOWN:
            return null;
        default:
            return null;
        }
    }

    /**
     * Converts TE Topology TeStatus Enum to YANG TeOperStatus Enum.
     *
     * @param opStatus TE Topology operation status
     * @return the equivalent Enum from YANG TeOperStatus or null if not found
     */
    public static TeOperStatus teSubsystem2YangOperStatus(TeStatus opStatus) {
        if (opStatus == null) {
            return null;
        }

        switch (opStatus) {
        case DOWN:
            return TeOperStatus.of(TeOperStatusEnum.DOWN);
        case UP:
            return TeOperStatus.of(TeOperStatusEnum.UP);
        case TESTING:
            return TeOperStatus.of(TeOperStatusEnum.TESTING);
        case MAINTENANCE:
            return TeOperStatus.of(TeOperStatusEnum.MAINTENANCE);
        case PREPARING_MAINTENANCE:
            return TeOperStatus.of(TeOperStatusEnum.PREPARING_MAINTENANCE);
        case UNKNOWN:
            return TeOperStatus.of(TeOperStatusEnum.UNKNOWN);
        default:
            return null;
        }
    }

}