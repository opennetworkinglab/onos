package net.onrc.onos.of.ctl;

import org.projectfloodlight.openflow.protocol.OFControllerRole;

/**
 * The role of the controller as it pertains to a particular switch.
 * Note that this definition of the role enum is different from the
 * OF1.3 definition. It is maintained here to be backward compatible to
 * earlier versions of the controller code. This enum is translated
 * to the OF1.3 enum, before role messages are sent to the switch.
 * See sendRoleRequestMessage method in OFSwitchImpl
 */
public enum Role {
    EQUAL(OFControllerRole.ROLE_EQUAL),
    MASTER(OFControllerRole.ROLE_MASTER),
    SLAVE(OFControllerRole.ROLE_SLAVE);

    private Role(OFControllerRole nxRole) {
        nxRole.ordinal();
    }
    /*
    private static Map<Integer,Role> nxRoleToEnum
            = new HashMap<Integer,Role>();
    static {
        for(Role r: Role.values())
            nxRoleToEnum.put(r.toNxRole(), r);
    }
    public int toNxRole() {
        return nxRole;
    }
    // Return the enum representing the given nxRole or null if no
    // such role exists
    public static Role fromNxRole(int nxRole) {
        return nxRoleToEnum.get(nxRole);
    }*/
}
