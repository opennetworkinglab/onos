/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.openflow.controller;

import org.projectfloodlight.openflow.protocol.OFControllerRole;

/**
 * The role of the controller as it pertains to a particular switch.
 * Note that this definition of the role enum is different from the
 * OF1.3 definition. It is maintained here to be backward compatible to
 * earlier versions of the controller code. This enum is translated
 * to the OF1.3 enum, before role messages are sent to the switch.
 * See sendRoleRequestMessage method in OFSwitchImpl
 */
public enum RoleState {
    EQUAL(OFControllerRole.ROLE_EQUAL),
    MASTER(OFControllerRole.ROLE_MASTER),
    SLAVE(OFControllerRole.ROLE_SLAVE);

    private RoleState(OFControllerRole nxRole) {
        nxRole.ordinal();
    }

}



