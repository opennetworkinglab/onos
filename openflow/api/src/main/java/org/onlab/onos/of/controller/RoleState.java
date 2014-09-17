package org.onlab.onos.of.controller;

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



