/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.openflow.controller.impl;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openflow.OpenflowSwitchDriverAdapter;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;
import org.onosproject.openflow.controller.driver.RoleRecvStatus;
import org.onosproject.openflow.controller.driver.RoleReplyInfo;
import org.onosproject.openflow.controller.driver.SwitchStateException;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.types.U64;

import static org.junit.Assert.assertEquals;
import static org.onosproject.openflow.controller.RoleState.MASTER;
import static org.onosproject.openflow.controller.RoleState.SLAVE;
import static org.onosproject.openflow.controller.driver.RoleRecvStatus.MATCHED_CURRENT_ROLE;
import static org.onosproject.openflow.controller.driver.RoleRecvStatus.OTHER_EXPECTATION;

public class RoleManagerTest {

    private static final U64 GID = U64.of(10L);
    private static final long XID = 1L;

    private OpenFlowSwitchDriver sw;
    private RoleManager manager;

    @Before
    public void setUp() {
        sw = new TestSwitchDriver();
        manager = new RoleManager(sw);
    }

    @After
    public void tearDown() {
        manager = null;
        sw = null;
    }

    @Test
    public void deliverRoleReply() {
        RoleRecvStatus status;

        RoleReplyInfo asserted = new RoleReplyInfo(MASTER, GID, XID);
        RoleReplyInfo unasserted = new RoleReplyInfo(SLAVE, GID, XID);

        try {
            //call without sendRoleReq() for requestPending = false
            //first, sw.role == null
            status = manager.deliverRoleReply(asserted);
            assertEquals("expectation wrong", OTHER_EXPECTATION, status);

            sw.setRole(MASTER);
            assertEquals("expectation wrong", OTHER_EXPECTATION, status);
            sw.setRole(SLAVE);

            //match to pendingRole = MASTER, requestPending = true
            manager.sendRoleRequest(MASTER, MATCHED_CURRENT_ROLE);
            status = manager.deliverRoleReply(asserted);
            assertEquals("expectation wrong", MATCHED_CURRENT_ROLE, status);

            //requestPending never gets reset -- this might be a bug.
            status = manager.deliverRoleReply(unasserted);
            assertEquals("expectation wrong", OTHER_EXPECTATION, status);
            assertEquals("pending role mismatch", MASTER, ((TestSwitchDriver) sw).failed);

        } catch (IOException | SwitchStateException e) {
            assertEquals("unexpected error thrown",
                    SwitchStateException.class, e.getClass());
        }
    }

    private class TestSwitchDriver extends OpenflowSwitchDriverAdapter {

        RoleState failed = null;
        RoleState current = null;

        @Override
        public void setRole(RoleState role) {
            current = role;
        }

        @Override
        public RoleState getRole() {
            return current;
        }

        @Override
        public void setFeaturesReply(OFFeaturesReply featuresReply) {
        }

        @Override
        public void setSwitchDescription(OFDescStatsReply desc) {
        }

        @Override
        public int getNextTransactionId() {
            return (int) XID;
        }

        @Override
        public void returnRoleReply(RoleState requested, RoleState response) {
            failed = requested;
        }

        @Override
        public String channelId() {
            return "1.2.3.4:1";
        }
    }
}
