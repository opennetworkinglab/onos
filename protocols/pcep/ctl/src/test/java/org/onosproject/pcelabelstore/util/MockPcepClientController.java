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
package org.onosproject.pcelabelstore.util;



import org.onosproject.incubator.net.tunnel.DefaultLabelStack;
import org.onosproject.incubator.net.tunnel.LabelStack;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.net.Path;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepClientListener;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcep.controller.PcepNodeListener;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.types.PcepValueType;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

public class MockPcepClientController implements PcepClientController {

    Map<PccId, PcepClient> clientMap = new HashMap<>();

    @Override
    public Collection<PcepClient> getClients() {
        // TODO Auto-generated method stub
        return null;
    }

    public void addClient(PccId pccId, PcepClient pc) {
        clientMap.put(pccId, pc);
        return;
    }

    @Override
    public PcepClient getClient(PccId pccId) {
        return clientMap.get(pccId);
    }

    @Override
    public void addListener(PcepClientListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(PcepClientListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addEventListener(PcepEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeEventListener(PcepEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addNodeListener(PcepNodeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeNodeListener(PcepNodeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeMessage(PccId pccId, PcepMessage msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processClientMessage(PccId pccId, PcepMessage msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void closeConnectedClients() {
        // TODO Auto-generated method stub

    }

    @Override
    public LabelStack computeLabelStack(Path path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LinkedList<PcepValueType> createPcepLabelStack(DefaultLabelStack labelStack, Path path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, List<String>> getPcepExceptions() {
        return null;
    }

    @Override
    public Map<Integer, Integer> getPcepErrorMsg() {
        return null;
    }

    @Override
    public Map<String, String> getPcepSessionMap() {
        return null;
    }

    @Override
    public Map<String, Byte> getPcepSessionIdMap() {
        return null;
    }

    @Override
    public void peerErrorMsg(String peerId, Integer errorType, Integer errValue) {

    }

    @Override
    public boolean allocateLocalLabel(Tunnel tunnel) {
        // TODO Auto-generated method stub
        return false;
    }

}
