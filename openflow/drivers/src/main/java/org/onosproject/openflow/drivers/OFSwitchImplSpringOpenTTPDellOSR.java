/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.openflow.drivers;

import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.types.TableId;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Dell Make (Hardware
 * Desc.) : OpenFlow 1.3 Reference Userspace Switch Model (Datapath Desc.) :
 * None Software : Serial : None.
 */
//TODO: Knock-off this class as we don't need any switch/app specific
//drivers in the south bound layers.
public class OFSwitchImplSpringOpenTTPDellOSR extends OFSwitchImplSpringOpenTTP {

    /* Table IDs to be used for Dell Open Segment Routers*/
    private static final int DELL_TABLE_VLAN = 17;
    private static final int DELL_TABLE_TMAC = 18;
    private static final int DELL_TABLE_IPV4_UNICAST = 30;
    private static final int DELL_TABLE_MPLS = 25;
    private static final int DELL_TABLE_ACL = 40;

    public OFSwitchImplSpringOpenTTPDellOSR(Dpid dpid, OFDescStatsReply desc) {
        super(dpid, desc);
        vlanTableId = DELL_TABLE_VLAN;
        tmacTableId = DELL_TABLE_TMAC;
        ipv4UnicastTableId = DELL_TABLE_IPV4_UNICAST;
        mplsTableId = DELL_TABLE_MPLS;
        aclTableId = DELL_TABLE_ACL;
    }

    @Override
    public TableType getTableType(TableId tid) {
        switch (tid.getValue()) {
            case DELL_TABLE_IPV4_UNICAST:
                return TableType.IP;
            case DELL_TABLE_MPLS:
                return TableType.MPLS;
            case DELL_TABLE_ACL:
                return TableType.ACL;
            case DELL_TABLE_VLAN:
                return TableType.VLAN;
            case DELL_TABLE_TMAC:
                return TableType.ETHER;
            default:
                log.error("Table type for Table id {} is not supported in the driver", tid);
                return TableType.NONE;
        }
    }
}