/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.bgp;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.onosproject.bgpio.protocol.ver4.BgpKeepaliveMsgVer4;
import org.onosproject.bgpio.protocol.ver4.BgpOpenMsgVer4;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.bgpio.types.BgpValueType;

public class BgpPeerChannelHandlerTest extends SimpleChannelHandler {
    public static final int OPEN_MSG_MINIMUM_LENGTH = 29;
    public static final byte[] MARKER = new byte[] {(byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff};
    public static final BgpHeader DEFAULT_OPEN_HEADER = new BgpHeader(MARKER,
            (short) OPEN_MSG_MINIMUM_LENGTH, (byte) 0X01);
    LinkedList<BgpValueType> capabilityTlv = new LinkedList<>();
    public byte version;
    public short asNumber;
    public short holdTime;
    public int bgpId;
    public boolean isLargeAsCapabilitySet;

    final BgpOpenMsgVer4 openMessage = new BgpOpenMsgVer4();
    ChannelHandlerContext savedCtx;

    /**
     * Constructor to initialize all variables of BGP Open message.
     *
     * @param version BGP version in open message
     * @param asNumber AS number in open message
     * @param holdTime hold time in open message
     * @param bgpId BGP identifier in open message
     * @param capabilityTlv capabilities in open message
     */
    public BgpPeerChannelHandlerTest(byte version,
            short asNumber,
            short holdTime,
            int bgpId,
            boolean isLargeAsCapabilitySet,
            LinkedList<BgpValueType> capabilityTlv) {
        this.version = version;
        this.asNumber = asNumber;
        this.holdTime = holdTime;
        this.bgpId = bgpId;
        this.isLargeAsCapabilitySet = isLargeAsCapabilitySet;
        this.capabilityTlv = capabilityTlv;
    }

    /**
     * closes the channel.
     */
    void closeChannel() {
        savedCtx.getChannel().close();
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx,
                                 ChannelStateEvent channelEvent) throws InterruptedException {
        this.savedCtx = ctx;

        BgpOpenMsgVer4 openMsg = new BgpOpenMsgVer4(DEFAULT_OPEN_HEADER,
                this.version,
                this.asNumber,
                this.holdTime,
                this.bgpId,
                this.capabilityTlv);
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        openMsg.writeTo(buffer);
        ctx.getChannel().write(buffer);

        TimeUnit.MILLISECONDS.sleep(100);

        BgpKeepaliveMsgVer4 keepaliveMsg = new BgpKeepaliveMsgVer4();
        ChannelBuffer buffer1 = ChannelBuffers.dynamicBuffer();
        keepaliveMsg.writeTo(buffer1);
        ctx.getChannel().write(buffer1);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx,
                                    ChannelStateEvent channelEvent) {
        //Do Nothing
    }
}
