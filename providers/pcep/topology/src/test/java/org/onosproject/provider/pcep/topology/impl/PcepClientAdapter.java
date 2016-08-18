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
package org.onosproject.provider.pcep.topology.impl;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.netty.channel.Channel;
import org.onosproject.pcep.controller.ClientCapability;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.LspKey;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepSyncStatus;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepFactory;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepStateReport;
import org.onosproject.pcepio.protocol.PcepVersion;

/**
 * Representation of PCEP client adapter.
 */
public class PcepClientAdapter implements PcepClient {

    private Channel channel;
    protected String channelId;

    private boolean connected;
    private PccId pccId;
    private ClientCapability capability;

    private PcepVersion pcepVersion;
    private PcepSyncStatus lspDbSyncStatus;
    private PcepSyncStatus labelDbSyncStatus;
    private Map<LspKey, Boolean> lspDelegationInfo = new HashMap<>();

    /**
     * Initialize instance with specified parameters.
     *
     * @param pccId PCC id
     * @param pcepVersion PCEP message version
     */
    public void init(PccId pccId, PcepVersion pcepVersion) {
        this.pccId = pccId;
        this.pcepVersion = pcepVersion;
    }

    @Override
    public final void disconnectClient() {
        this.channel.close();
    }

    @Override
    public final void sendMessage(PcepMessage m) {
    }

    @Override
    public final void sendMessage(List<PcepMessage> msgs) {
        try {
            PcepMessage pcepMsg = msgs.get(0);
            assertNotNull("PCEP MSG should be created.", pcepMsg);
        } catch (RejectedExecutionException e) {
            throw e;
        }
    }

    @Override
    public final boolean isConnected() {
        return this.connected;
    }

    @Override
    public String channelId() {
        return channelId;
    }

    @Override
    public final PccId getPccId() {
        return this.pccId;
    }

    @Override
    public final String getStringId() {
        return this.pccId.toString();
    }

    @Override
    public final void handleMessage(PcepMessage m) {
    }

    @Override
    public boolean isOptical() {
        return false;
    }

    @Override
    public PcepFactory factory() {
        return PcepFactories.getFactory(pcepVersion);
    }

    @Override
    public void setLspDbSyncStatus(PcepSyncStatus syncStatus) {
        this.lspDbSyncStatus = syncStatus;
    }

    @Override
    public PcepSyncStatus lspDbSyncStatus() {
        return lspDbSyncStatus;
    }

    @Override
    public void setLabelDbSyncStatus(PcepSyncStatus syncStatus) {
        this.labelDbSyncStatus = syncStatus;
    }

    @Override
    public PcepSyncStatus labelDbSyncStatus() {
        return labelDbSyncStatus;
    }

    @Override
    public void setCapability(ClientCapability capability) {
        this.capability = capability;
    }

    @Override
    public ClientCapability capability() {
        return capability;
    }

    @Override
    public void addNode(PcepClient pc) {
    }

    @Override
    public void deleteNode(PccId pccId) {
    }

    @Override
    public void setLspAndDelegationInfo(LspKey lspKey, boolean dFlag) {
        lspDelegationInfo.put(lspKey, dFlag);
    }

    @Override
    public Boolean delegationInfo(LspKey lspKey) {
        return lspDelegationInfo.get(lspKey);
    }

    @Override
    public void initializeSyncMsgList(PccId pccId) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<PcepStateReport> getSyncMsgList(PccId pccId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeSyncMsgList(PccId pccId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addSyncMsgToList(PccId pccId, PcepStateReport rptMsg) {
        // TODO Auto-generated method stub

    }
}
