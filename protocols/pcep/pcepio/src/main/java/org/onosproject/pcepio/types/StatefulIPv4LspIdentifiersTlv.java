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

package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides StatefulIPv4LspIdentidiersTlv.
 */
public class StatefulIPv4LspIdentifiersTlv implements PcepValueType {

    /*             IPV4-LSP-IDENTIFIERS TLV format
     *
     * Reference :PCEP Extensions for Stateful PCE draft-ietf-pce-stateful-pce-10
     *

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |           Type=18             |           Length=16           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                   IPv4 Tunnel Sender Address                  |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |             LSP ID            |           Tunnel ID           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                        Extended Tunnel ID                     |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |               IPv4 Tunnel Endpoint Address                    |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(StatefulIPv4LspIdentifiersTlv.class);

    public static final short TYPE = 18;
    public static final short LENGTH = 16;
    public static final int VALUE_LENGTH = 16;
    private final int ipv4IngressAddress;
    private final short lspId;
    private final short tunnelId;
    private final int extendedTunnelId;
    private final int ipv4EgressAddress;

    /**
     * Constructor to initialize member variables.
     *
     * @param ipv4IngressAddress ingress ipv4 address
     * @param lspId lsp id
     * @param tunnelId tunnel id
     * @param extendedTunnelId extended tunnel id
     * @param ipv4EgressAddress egress ipv4 address
     */
    public StatefulIPv4LspIdentifiersTlv(int ipv4IngressAddress, short lspId, short tunnelId, int extendedTunnelId,
            int ipv4EgressAddress) {

        this.ipv4IngressAddress = ipv4IngressAddress;
        this.lspId = lspId;
        this.tunnelId = tunnelId;
        this.extendedTunnelId = extendedTunnelId;
        this.ipv4EgressAddress = ipv4EgressAddress;
    }

    /**
     * Creates object of StatefulIPv4LspIdentidiersTlv.
     *
     * @param ipv4IngressAddress ingress ipv4 address
     * @param lspId lsp id
     * @param tunnelId tunnel id
     * @param extendedTunnelId extended tunnel id
     * @param ipv4EgressAddress egress ipv4 address
     * @return object of StatefulIPv4LspIdentidiersTlv
     */
    public static StatefulIPv4LspIdentifiersTlv of(int ipv4IngressAddress, short lspId, short tunnelId,
            int extendedTunnelId, int ipv4EgressAddress) {
        return new StatefulIPv4LspIdentifiersTlv(ipv4IngressAddress, lspId, tunnelId, extendedTunnelId,
                ipv4EgressAddress);
    }

    /**
     * Returns tunnel id.
     *
     * @return tunnelId
     */
    public short getTunnelId() {
        return this.tunnelId;
    }

    /**
     * Returns LSP id.
     *
     * @return lspId
     */
    public short getLspId() {
        return this.lspId;
    }

    /**
     * Returns extendedTunnelId.
     *
     * @return extendedTunnelId
     */
    public int getextendedTunnelId() {
        return this.extendedTunnelId;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    /**
     * Returns ipv4IngressAddress.
     *
     * @return ipv4IngressAddress
     */
    public int getIpv4IngressAddress() {
        return ipv4IngressAddress;
    }

    /**
     * Returns ipv4EgressAddress.
     *
     * @return ipv4EgressAddress
     */
    public int getIpv4EgressAddress() {
        return ipv4EgressAddress;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public short getLength() {
        return LENGTH;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipv4IngressAddress, lspId, tunnelId, extendedTunnelId, ipv4EgressAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof StatefulIPv4LspIdentifiersTlv) {
            StatefulIPv4LspIdentifiersTlv other = (StatefulIPv4LspIdentifiersTlv) obj;
            return Objects.equals(this.ipv4IngressAddress, other.ipv4IngressAddress)
                    && Objects.equals(this.lspId, other.lspId) && Objects.equals(this.tunnelId, other.tunnelId)
                    && Objects.equals(this.extendedTunnelId, other.extendedTunnelId)
                    && Objects.equals(this.ipv4EgressAddress, other.ipv4EgressAddress);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(ipv4IngressAddress);
        c.writeShort(lspId);
        c.writeShort(tunnelId);
        c.writeInt(extendedTunnelId);
        c.writeInt(ipv4EgressAddress);

        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of StatefulIPv4LspIdentidiersTlv.
     *
     * @param c of type channel buffer
     * @return object of StatefulIPv4LspIdentidiersTlv
     */
    public static PcepValueType read(ChannelBuffer c) {
        int ipv4IngressAddress = c.readInt();
        short lspId = c.readShort();
        short tunnelId = c.readShort();
        int extendedTunnelId = c.readInt();
        int ipv4EgressAddress = c.readInt();
        return new StatefulIPv4LspIdentifiersTlv(ipv4IngressAddress, lspId, tunnelId, extendedTunnelId,
                ipv4EgressAddress);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type:", TYPE)
                .add("Length:", LENGTH)
                .add("Ipv4IngressAddress:", ipv4IngressAddress)
                .add("LspId:", lspId).add("TunnelId:", tunnelId)
                .add("ExtendedTunnelId:", extendedTunnelId)
                .add("Ipv4EgressAddress:", ipv4EgressAddress).toString();
    }
}
