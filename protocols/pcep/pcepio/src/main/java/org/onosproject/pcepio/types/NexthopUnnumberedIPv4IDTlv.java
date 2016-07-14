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

package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * NexthopUnnumberedIPv4IDTlv provides the next node's ID and Interface ID.
 */
public class NexthopUnnumberedIPv4IDTlv implements PcepValueType {

    /*
        Reference : draft-zhao-pce-pcep-extension-for-pce-controller-01.

        0 1 2 3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       | Type=TBD                      | Length = 12                   |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          Node-ID                              |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          Interface ID                         |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                      NEXTHOP-UNNUMBERED-IPV4-ID TLV

     */
    protected static final Logger log = LoggerFactory.getLogger(NexthopUnnumberedIPv4IDTlv.class);

    public static final short TYPE = 1; //to be defined
    //Length is header + value
    public static final short LENGTH = 12;

    private final int nodeID;
    private final int interfaceID;

    /**
     * constructor to initialize nodeID and interfaceID.
     *
     * @param nodeID node ID
     * @param interfaceID interface ID
     */
    public NexthopUnnumberedIPv4IDTlv(int nodeID, int interfaceID) {
        this.nodeID = nodeID;
        this.interfaceID = interfaceID;
    }

    /**
     * Returns new object of NexthopUnnumberedIPv4IDTlv.
     *
     * @param nodeID node ID
     * @param interfaceID interface ID
     * @return NexthopUnnumberedIPv4IDTlv
     */
    public static NexthopUnnumberedIPv4IDTlv of(int nodeID, int interfaceID) {
        return new NexthopUnnumberedIPv4IDTlv(nodeID, interfaceID);
    }

    /**
     * Returns Node Id.
     *
     * @return node ID
     */
    public int getNodeID() {
        return nodeID;
    }

    /**
     * Returns Interface Id.
     *
     * @return interface ID
     */
    public int getInterfaceID() {
        return interfaceID;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
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
        return Objects.hash(nodeID, interfaceID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NexthopUnnumberedIPv4IDTlv) {
            NexthopUnnumberedIPv4IDTlv other = (NexthopUnnumberedIPv4IDTlv) obj;
            return Objects.equals(this.nodeID, other.nodeID) && Objects.equals(this.interfaceID, other.interfaceID);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);

        c.writeInt(nodeID);
        c.writeInt(interfaceID);

        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of NexthopUnnumberedIPv4IDTlv.
     *
     * @param cb type of channel buffer
     * @return object of NexthopUnnumberedIPv4IDTlv
     */
    public static NexthopUnnumberedIPv4IDTlv read(ChannelBuffer cb) {
        int nodeID = cb.readInt();
        int interfaceID = cb.readInt();
        return new NexthopUnnumberedIPv4IDTlv(nodeID, interfaceID);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("NodeId", nodeID)
                .add("InterfaceId", interfaceID)
                .toString();
    }
}
