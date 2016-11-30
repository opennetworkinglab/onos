/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.onosproject.net.Device;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.projectfloodlight.openflow.protocol.OFPortDesc;

import java.util.List;

/**
 * Test adapter for the OpenFlow switch interface.
 */
public class OpenFlowSwitchAdapter implements OpenFlowSwitch {
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

    }

    @Override
    public RoleState getRole() {
        return null;
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
        return null;
    }

    @Override
    public String getStringId() {
        return null;
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
        return null;
    }

    @Override
    public String channelId() {
        return null;
    }
}