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
package org.onosproject.sdnip.bgp;

import java.util.Collection;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;

/**
 * Class for handling the remote BGP Peer session.
 */
class TestBgpPeerChannelHandler extends SimpleChannelHandler {
    static final long PEER_AS = 65001;
    static final int PEER_HOLDTIME = 120;       // 120 seconds
    final Ip4Address bgpId;                     // The BGP ID
    ChannelHandlerContext savedCtx;

    /**
     * Constructor for given BGP ID.
     *
     * @param bgpId the BGP ID to use
     */
    TestBgpPeerChannelHandler(Ip4Address bgpId) {
        this.bgpId = bgpId;
    }

    /**
     * Closes the channel.
     */
    void closeChannel() {
        savedCtx.getChannel().close();
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx,
                                 ChannelStateEvent channelEvent) {
        this.savedCtx = ctx;
        // Prepare and transmit BGP OPEN message
        ChannelBuffer message = prepareBgpOpen();
        ctx.getChannel().write(message);

        // Prepare and transmit BGP KEEPALIVE message
        message = prepareBgpKeepalive();
        ctx.getChannel().write(message);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx,
                                    ChannelStateEvent channelEvent) {
        // Nothing to do
    }

    /**
     * Prepares BGP OPEN message.
     *
     * @return the message to transmit (BGP header included)
     */
    ChannelBuffer prepareBgpOpen() {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);
        message.writeByte(BgpConstants.BGP_VERSION);
        message.writeShort((int) PEER_AS);
        message.writeShort(PEER_HOLDTIME);
        message.writeInt(bgpId.toInt());
        message.writeByte(0);                   // No Optional Parameters
        return prepareBgpMessage(BgpConstants.BGP_TYPE_OPEN, message);
    }

    /**
     * Prepares BGP UPDATE message.
     *
     * @param nextHopRouter the next-hop router address for the routes to add
     * @param localPref the local preference for the routes to use
     * @param multiExitDisc the MED value
     * @param asPath the AS path for the routes to add
     * @param addedRoutes the routes to add
     * @param withdrawnRoutes the routes to withdraw
     * @return the message to transmit (BGP header included)
     */
    ChannelBuffer prepareBgpUpdate(Ip4Address nextHopRouter,
                                   long localPref,
                                   long multiExitDisc,
                                   BgpRouteEntry.AsPath asPath,
                                   Collection<Ip4Prefix> addedRoutes,
                                   Collection<Ip4Prefix> withdrawnRoutes) {
        int attrFlags;
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);
        ChannelBuffer pathAttributes =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        // Encode the Withdrawn Routes
        ChannelBuffer encodedPrefixes = encodePackedPrefixes(withdrawnRoutes);
        message.writeShort(encodedPrefixes.readableBytes());
        message.writeBytes(encodedPrefixes);

        // Encode the Path Attributes
        // ORIGIN: IGP
        attrFlags = 0x40;                               // Transitive flag
        pathAttributes.writeByte(attrFlags);
        pathAttributes.writeByte(BgpConstants.Update.Origin.TYPE);
        pathAttributes.writeByte(1);                    // Data length
        pathAttributes.writeByte(BgpConstants.Update.Origin.IGP);

        // AS_PATH: asPath
        attrFlags = 0x40;                               // Transitive flag
        pathAttributes.writeByte(attrFlags);
        pathAttributes.writeByte(BgpConstants.Update.AsPath.TYPE);
        ChannelBuffer encodedAsPath = encodeAsPath(asPath);
        pathAttributes.writeByte(encodedAsPath.readableBytes()); // Data length
        pathAttributes.writeBytes(encodedAsPath);
        // NEXT_HOP: nextHopRouter
        attrFlags = 0x40;                               // Transitive flag
        pathAttributes.writeByte(attrFlags);
        pathAttributes.writeByte(BgpConstants.Update.NextHop.TYPE);
        pathAttributes.writeByte(4);                    // Data length
        pathAttributes.writeInt(nextHopRouter.toInt()); // Next-hop router
        // LOCAL_PREF: localPref
        attrFlags = 0x40;                               // Transitive flag
        pathAttributes.writeByte(attrFlags);
        pathAttributes.writeByte(BgpConstants.Update.LocalPref.TYPE);
        pathAttributes.writeByte(4);                    // Data length
        pathAttributes.writeInt((int) localPref);       // Preference value
        // MULTI_EXIT_DISC: multiExitDisc
        attrFlags = 0x80;                               // Optional
                                                        // Non-Transitive flag
        pathAttributes.writeByte(attrFlags);
        pathAttributes.writeByte(BgpConstants.Update.MultiExitDisc.TYPE);
        pathAttributes.writeByte(4);                    // Data length
        pathAttributes.writeInt((int) multiExitDisc);   // Preference value
        // The NLRI prefixes
        encodedPrefixes = encodePackedPrefixes(addedRoutes);

        // Write the Path Attributes, beginning with its length
        message.writeShort(pathAttributes.readableBytes());
        message.writeBytes(pathAttributes);
        message.writeBytes(encodedPrefixes);

        return prepareBgpMessage(BgpConstants.BGP_TYPE_UPDATE, message);
    }

    /**
     * Encodes a collection of IPv4 network prefixes in a packed format.
     * <p>
     * The IPv4 prefixes are encoded in the form:
     * <Length, Prefix> where Length is the length in bits of the IPv4 prefix,
     * and Prefix is the IPv4 prefix (padded with trailing bits to the end
     * of an octet).
     *
     * @param prefixes the prefixes to encode
     * @return the buffer with the encoded prefixes
     */
    private ChannelBuffer encodePackedPrefixes(Collection<Ip4Prefix> prefixes) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        // Write each of the prefixes
        for (Ip4Prefix prefix : prefixes) {
            int prefixBitlen = prefix.prefixLength();
            int prefixBytelen = (prefixBitlen + 7) / 8;         // Round-up
            message.writeByte(prefixBitlen);

            Ip4Address address = prefix.address();
            long value = address.toInt() & 0xffffffffL;
            for (int i = 0; i < Ip4Address.BYTE_LENGTH; i++) {
                if (prefixBytelen-- == 0) {
                    break;
                }
                long nextByte =
                    (value >> ((Ip4Address.BYTE_LENGTH - i - 1) * 8)) & 0xff;
                message.writeByte((int) nextByte);
            }
        }

        return message;
    }

    /**
     * Encodes an AS path.
     *
     * @param asPath the AS path to encode
     * @return the buffer with the encoded AS path
     */
    private ChannelBuffer encodeAsPath(BgpRouteEntry.AsPath asPath) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        for (BgpRouteEntry.PathSegment pathSegment : asPath.getPathSegments()) {
            message.writeByte(pathSegment.getType());
            message.writeByte(pathSegment.getSegmentAsNumbers().size());
            for (Long asNumber : pathSegment.getSegmentAsNumbers()) {
                message.writeShort(asNumber.intValue());
            }
        }

        return message;
    }

    /**
     * Prepares BGP KEEPALIVE message.
     *
     * @return the message to transmit (BGP header included)
     */
    ChannelBuffer prepareBgpKeepalive() {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);
        return prepareBgpMessage(BgpConstants.BGP_TYPE_KEEPALIVE, message);
    }

    /**
     * Prepares BGP NOTIFICATION message.
     *
     * @param errorCode the BGP NOTIFICATION Error Code
     * @param errorSubcode the BGP NOTIFICATION Error Subcode if applicable,
     * otherwise BgpConstants.Notifications.ERROR_SUBCODE_UNSPECIFIC
     * @param payload the BGP NOTIFICATION Data if applicable, otherwise null
     * @return the message to transmit (BGP header included)
     */
    ChannelBuffer prepareBgpNotification(int errorCode, int errorSubcode,
                                         ChannelBuffer data) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);
        // Prepare the NOTIFICATION message payload
        message.writeByte(errorCode);
        message.writeByte(errorSubcode);
        if (data != null) {
            message.writeBytes(data);
        }
        return prepareBgpMessage(BgpConstants.BGP_TYPE_NOTIFICATION, message);
    }

    /**
     * Prepares BGP message.
     *
     * @param type the BGP message type
     * @param payload the message payload to transmit (BGP header excluded)
     * @return the message to transmit (BGP header included)
     */
    private ChannelBuffer prepareBgpMessage(int type, ChannelBuffer payload) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_HEADER_LENGTH +
                                  payload.readableBytes());

        // Write the marker
        for (int i = 0; i < BgpConstants.BGP_HEADER_MARKER_LENGTH; i++) {
            message.writeByte(0xff);
        }

        // Write the rest of the BGP header
        message.writeShort(BgpConstants.BGP_HEADER_LENGTH +
                           payload.readableBytes());
        message.writeByte(type);

        // Write the payload
        message.writeBytes(payload);
        return message;
    }
}
