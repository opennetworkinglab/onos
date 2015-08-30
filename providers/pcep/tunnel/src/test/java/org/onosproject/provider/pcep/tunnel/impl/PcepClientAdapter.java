package org.onosproject.provider.pcep.tunnel.impl;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.netty.channel.Channel;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepFactory;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepVersion;

public class PcepClientAdapter implements PcepClient {

    private Channel channel;
    protected String channelId;

    private boolean connected;
    private PccId pccId;

    private PcepVersion pcepVersion;

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
    };

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
    public final boolean isSyncComplete() {
        return false;
    }

    @Override
    public final void setIsSyncComplete(boolean value) {
    }
}
