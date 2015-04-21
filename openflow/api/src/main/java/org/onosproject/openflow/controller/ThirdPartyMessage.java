package org.onosproject.openflow.controller;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;

import com.google.common.hash.PrimitiveSink;
/**
 * Used to support for the third party privacy flow rule.
 * it implements OFMessage interface to use exist adapter API.
 */
public class ThirdPartyMessage implements OFMessage {

    private final byte[] payLoad; //privacy flow rule

    public ThirdPartyMessage(byte[] payLoad) {
        this.payLoad = payLoad;
    }

    public byte[] payLoad() {
        return payLoad;
    }

    @Override
    public void putTo(PrimitiveSink sink) {
     // Do nothing here for now.
    }

    @Override
    public OFVersion getVersion() {
     // Do nothing here for now.
        return null;
    }

    @Override
    public OFType getType() {
     // Do nothing here for now.
        return null;
    }

    @Override
    public long getXid() {
     // Do nothing here for now.
        return 0;
    }

    @Override
    public void writeTo(ChannelBuffer channelBuffer) {
     // Do nothing here for now.
    }

    @Override
    public Builder createBuilder() {
     // Do nothing here for now.
        return null;
    }

}
