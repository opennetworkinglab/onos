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
package org.onosproject.openflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;

/**
 * Test harness for a switch listener.
 */
public class OpenFlowSwitchListenerAdapter implements OpenFlowSwitchListener {
    final List<Dpid> removedDpids = new ArrayList<>();
    final List<Dpid> addedDpids = new ArrayList<>();
    final List<Dpid> changedDpids = new ArrayList<>();
    final Map<Dpid, OFPortStatus> portChangedDpids = new HashMap<>();

    @Override
    public void switchAdded(Dpid dpid) {
        addedDpids.add(dpid);
    }

    @Override
    public void switchRemoved(Dpid dpid) {
        removedDpids.add(dpid);
    }

    @Override
    public void switchChanged(Dpid dpid) {
        changedDpids.add(dpid);
    }

    @Override
    public void portChanged(Dpid dpid, OFPortStatus status) {
        portChangedDpids.put(dpid, status);
    }

    @Override
    public void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response) {
        // Stub
    }

    public List<Dpid> removedDpids() {
        return removedDpids;
    }

    public List<Dpid> addedDpids() {
        return addedDpids;
    }

    public List<Dpid> changedDpids() {
        return changedDpids;
    }

    public Map<Dpid, OFPortStatus> portChangedDpids() {
        return portChangedDpids;
    }
}
