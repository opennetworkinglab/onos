/*
 * Copyright 2014 Open Networking Laboratory
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
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.OpenFlowAgent;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;
import org.onosproject.openflow.controller.driver.RoleHandler;
import org.onosproject.openflow.controller.driver.RoleRecvStatus;
import org.onosproject.openflow.controller.driver.RoleReplyInfo;
import org.onosproject.openflow.controller.driver.SwitchStateException;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.U64;

import static org.junit.Assert.assertEquals;
import static org.onosproject.openflow.controller.RoleState.*;
import static org.onosproject.openflow.controller.driver.RoleRecvStatus.*;

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

    private class TestSwitchDriver implements OpenFlowSwitchDriver {

        RoleState failed = null;
        RoleState current = null;

        @Override
        public void sendMsg(OFMessage msg) {
        }

        @Override
        public void sendMsg(List<OFMessage> msgs) {
        }

        @Override
        public void handleMessage(OFMessage fromSwitch) {
        }

        @Override
        public void setRole(RoleState role) {
            current = role;
        }

        @Override
        public RoleState getRole() {
            return current;
        }

        @Override
        public List<OFPortDesc> getPorts() {
            return null;
        }

        @Override
        public OFFactory factory() {
            // return what-ever triggers requestPending = true
            return OFFactories.getFactory(OFVersion.OF_10);
        }

        @Override
        public String getStringId() {
            return "100";
        }

        @Override
        public long getId() {
            return 0;
        }

        @Override
        public String manufacturerDescription() {
            return null;
        }

        @Override
        public String datapathDescription() {
            return null;
        }

        @Override
        public String hardwareDescription() {
            return null;
        }

        @Override
        public String softwareDescription() {
            return null;
        }

        @Override
        public String serialNumber() {
            return null;
        }

        @Override
        public void disconnectSwitch() {
        }

        @Override
        public boolean isOptical() {
            return false;
        }

        @Override
        public void setAgent(OpenFlowAgent agent) {
        }

        @Override
        public void setRoleHandler(RoleHandler roleHandler) {
        }

        @Override
        public void reassertRole() {
        }

        @Override
        public boolean handleRoleError(OFErrorMsg error) {
            return false;
        }

        @Override
        public void handleNiciraRole(OFMessage m) throws SwitchStateException {
        }

        @Override
        public void handleRole(OFMessage m) throws SwitchStateException {
        }

        @Override
        public void startDriverHandshake() {
        }

        @Override
        public boolean isDriverHandshakeComplete() {
            return false;
        }

        @Override
        public void processDriverHandshakeMessage(OFMessage m) {
        }

        @Override
        public boolean connectSwitch() {
            return false;
        }

        @Override
        public boolean activateMasterSwitch() {
            return false;
        }

        @Override
        public boolean activateEqualSwitch() {
            return false;
        }

        @Override
        public void transitionToEqualSwitch() {
        }

        @Override
        public void transitionToMasterSwitch() {
        }

        @Override
        public void removeConnectedSwitch() {
        }

        @Override
        public void setPortDescReply(OFPortDescStatsReply portDescReply) {
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
        public Boolean supportNxRole() {
            return true;
        }

        @Override
        public void setOFVersion(OFVersion ofV) {
        }

        @Override
        public void setTableFull(boolean full) {
        }

        @Override
        public void setChannel(Channel channel) {
        }

        @Override
        public void setConnected(boolean connected) {
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public void write(OFMessage msg) {
        }

        @Override
        public void write(List<OFMessage> msgs) {
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
