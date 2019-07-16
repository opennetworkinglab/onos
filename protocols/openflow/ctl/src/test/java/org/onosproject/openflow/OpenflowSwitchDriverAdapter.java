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
package org.onosproject.openflow;

import org.onosproject.net.Device;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowSession;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.OpenFlowAgent;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;
import org.onosproject.openflow.controller.driver.RoleHandler;
import org.onosproject.openflow.controller.driver.SwitchStateException;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.projectfloodlight.openflow.protocol.OFMeterFeaturesStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;

import java.util.List;
import java.util.Set;

/**
 * Testing adapter for the OpenFlow switch driver class.
 */
public class OpenflowSwitchDriverAdapter implements OpenFlowSwitchDriver {

    RoleState role = RoleState.MASTER;
    // state of the connection
    private Set<Dpid> connected;
    private Dpid myDpid;
    private boolean complete;

    public OpenflowSwitchDriverAdapter() {
    }

    public OpenflowSwitchDriverAdapter(Set<Dpid> connected, Dpid myDpid, boolean complete) {
        this.connected = connected;
        this.myDpid = myDpid;
        this.complete = complete;
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
    public boolean connectSwitch() {
        return !connected.contains(myDpid);
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
    public void setPortDescReplies(List<OFPortDescStatsReply> portDescReplies) {

    }

    @Override
    public void setFeaturesReply(OFFeaturesReply featuresReply) {

    }

    @Override
    public void setMeterFeaturesReply(OFMeterFeaturesStatsReply meterFeaturesReply) {

    }

    @Override
    public void setSwitchDescription(OFDescStatsReply desc) {

    }

    @Override
    public int getNextTransactionId() {
        return 0;
    }

    @Override
    public void setOFVersion(OFVersion ofV) {

    }

    @Override
    public void setTableFull(boolean full) {

    }

    @Override
    public void setChannel(OpenFlowSession channel) {

    }

    @Override
    public void setConnected(boolean connected) {

    }

    @Override
    public void init(Dpid dpid, OFDescStatsReply desc, OFVersion ofv) {

    }

    @Override
    public Boolean supportNxRole() {
        return true;
    }

    @Override
    public void startDriverHandshake() {

    }

    @Override
    public boolean isDriverHandshakeComplete() {
        return complete;
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        complete = true;
    }

    @Override
    public void sendRoleRequest(OFMessage message) {

    }

    @Override
    public void sendHandshakeMessage(OFMessage message) {

    }

    @Override
    public DriverHandler handler() {
        return null;
    }

    @Override
    public void setHandler(DriverHandler handler) {

    }

    @Override
    public DriverData data() {
        return null;
    }

    @Override
    public void setData(DriverData data) {

    }

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
        this.role = role;
    }

    @Override
    public RoleState getRole() {
        return role;
    }

    @Override
    public List<OFPortDesc> getPorts() {
        return null;
    }

    @Override
    public OFMeterFeatures getMeterFeatures() {
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
    public boolean isConnected() {
        return false;
    }

    @Override
    public void disconnectSwitch() {

    }

    @Override
    public void returnRoleReply(RoleState requested, RoleState response) {

    }

    @Override
    public Device.Type deviceType() {
        return Device.Type.SWITCH;
    }

    @Override
    public String channelId() {
        return null;
    }
}
