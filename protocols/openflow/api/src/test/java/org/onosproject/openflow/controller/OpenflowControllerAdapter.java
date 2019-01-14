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
package org.onosproject.openflow.controller;

import org.projectfloodlight.openflow.protocol.OFMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Test adapter for the OpenFlow controller interface.
 */
public class OpenflowControllerAdapter implements OpenFlowController {
    @Override
    public Iterable<OpenFlowSwitch> getSwitches() {
        return null;
    }

    @Override
    public Iterable<OpenFlowSwitch> getMasterSwitches() {
        return null;
    }

    @Override
    public Iterable<OpenFlowSwitch> getEqualSwitches() {
        return null;
    }

    @Override
    public OpenFlowSwitch getSwitch(Dpid dpid) {
        return null;
    }

    @Override
    public OpenFlowSwitch getMasterSwitch(Dpid dpid) {
        return null;
    }

    @Override
    public OpenFlowSwitch getEqualSwitch(Dpid dpid) {
        return null;
    }

    @Override
    public void addListener(OpenFlowSwitchListener listener) {
    }

    @Override
    public void removeListener(OpenFlowSwitchListener listener) {
    }

    @Override
    public void addMessageListener(OpenFlowMessageListener listener) {

    }

    @Override
    public void removeMessageListener(OpenFlowMessageListener listener) {

    }

    @Override
    public void addPacketListener(int priority, PacketListener listener) {
    }

    @Override
    public void removePacketListener(PacketListener listener) {
    }

    @Override
    public void write(Dpid dpid, OFMessage msg) {
    }

    @Override
    public CompletableFuture<OFMessage> writeResponse(Dpid dpid, OFMessage msg) {
        return null;
    }

    @Override
    public void processPacket(Dpid dpid, OFMessage msg) {
    }

    @Override
    public void setRole(Dpid dpid, RoleState role) {
    }

    @Override
    public void addEventListener(OpenFlowEventListener listener) {
    }

    @Override
    public void removeEventListener(OpenFlowEventListener listener) {
    }

    @Override
    public void removeClassifierListener(OpenFlowClassifierListener listener) {
    }

    @Override
    public void addClassifierListener(OpenFlowClassifierListener listener) {
    }
}
