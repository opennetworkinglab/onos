package org.onlab.onos.of.controller.driver;

import org.onlab.onos.of.controller.RoleState;
import org.projectfloodlight.openflow.types.U64;

/**
 * Helper class returns role reply information in the format understood
 * by the controller.
 */
public class RoleReplyInfo {
    private final RoleState role;
    private final U64 genId;
    private final long xid;

    public RoleReplyInfo(RoleState role, U64 genId, long xid) {
        this.role = role;
        this.genId = genId;
        this.xid = xid;
    }
    public RoleState getRole() { return role; }
    public U64 getGenId() { return genId; }
    public long getXid() { return xid; }
    @Override
    public String toString() {
        return "[Role:" + role + " GenId:" + genId + " Xid:" + xid + "]";
    }
}
