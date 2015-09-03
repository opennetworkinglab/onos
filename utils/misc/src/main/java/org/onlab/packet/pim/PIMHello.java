/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onlab.packet.pim;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import org.onlab.packet.IpAddress;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.onlab.packet.PacketUtils.checkInput;

public class PIMHello extends BasePacket {

    private IpAddress nbrIpAddress;

    private int holdtime = 105;
    private int genid = 0;
    private int priority = 1;
    private boolean priorityPresent = false;

    public static final int MINIMUM_OPTION_LEN_BYTES = 4;

    /**
     * PIM Option types.
     */
    public enum Option {
        HOLDTIME  (1, 2),
        PRUNEDELAY(2, 4),
        PRIORITY  (19, 4),
        GENID     (20, 4),
        ADDRLIST  (24, 0);

        private final int optType;
        private final int optLen;

        Option(int ot, int ol) {
            this.optType = ot;
            this.optLen = ol;
        }

        public int optType() {
            return this.optType;
        }

        public int optLen() {
            return this.optLen;
        }
    }

    /**
     * Add the holdtime to the packet.
     *
     * @param holdtime the holdtime in seconds
     */
    public void addHoldtime(int holdtime) {
        this.holdtime = holdtime;
    }

    /**
     * Add the hello priority.
     *
     * @param priority default is 1, the higher the better
     */
    public void addPriority(int priority) {
        this.priority = priority;
        this.priorityPresent = true;
    }

    /**
     * Add a Gen ID.
     *
     * @param genid a random generated number, changes only after reset.
     */
    public void addGenId(int genid) {
        if (genid == 0) {
            this.addGenId();
        } else {
            this.genid = genid;
        }
    }

    /**
     * Add the genid.  Let this function figure out the number.
     */
    public void addGenId() {
        Random rand = new Random();
        this.genid = rand.nextInt();
    }

    /**
     * Sets all payloads parent packet if applicable, then serializes this
     * packet and all payloads.
     *
     * @return a byte[] containing this packet and payloads
     */
    @Override
    public byte[] serialize() {

        // TODO: Figure out a better way to calculate buffer size
        int size = Option.PRIORITY.optLen() + 4 +
                Option.GENID.optLen() + 4 +
                Option.HOLDTIME.optLen() + 4;

        byte[] data = new byte[size];      // Come up with something better
        ByteBuffer bb = ByteBuffer.wrap(data);

        // Add the priority
        bb.putShort((short) Option.PRIORITY.optType);
        bb.putShort((short) Option.PRIORITY.optLen);
        bb.putInt(this.priority);

        // Add the genid
        bb.putShort((short) Option.GENID.optType);
        bb.putShort((short) Option.GENID.optLen);
        bb.putInt(this.genid);

        // Add the holdtime
        bb.putShort((short) Option.HOLDTIME.optType);
        bb.putShort((short) Option.HOLDTIME.optLen);
        bb.putShort((short) this.holdtime);
        return data;
    }

    /**
     * XXX: This is deprecated, DO NOT USE, use the deserializer() function instead.
     */
    // @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        //
        return null;
    }

    /**
     * Deserialize this hello message.
     *
     * @return a deserialized hello message.
     */
    public static Deserializer<PIMHello> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, MINIMUM_OPTION_LEN_BYTES);
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            PIMHello hello = new PIMHello();
            while (bb.hasRemaining()) {
                int optType = bb.getShort();
                int optLen  = bb.getShort();

                // Check that we have enough buffer for the next option.
                checkInput(data, bb.position(), bb.limit() - bb.position(), optLen);
                if (optType == Option.GENID.optType) {
                    hello.addGenId(bb.getInt());
                } else if (optType == Option.PRIORITY.optType) {
                    hello.addPriority(bb.getInt());
                } else if (optType == Option.HOLDTIME.optType) {
                    hello.addHoldtime((int) bb.getShort());
                }
            }

            return hello;
        };
    }
}
