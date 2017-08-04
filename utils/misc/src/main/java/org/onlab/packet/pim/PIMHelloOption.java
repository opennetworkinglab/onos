/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onlab.packet.DeserializationException;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Arrays;

import static org.onlab.packet.PacketUtils.checkBufferLength;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * PIM HELLO option.
 */
public class PIMHelloOption {

    /*
     * PIM Option types.
     */
    public static final short OPT_HOLDTIME = 1;
    public static final short HOLDTIME_LENGTH = 2;
    public static final short DEFAULT_HOLDTIME = 105;

    public static final short OPT_PRUNEDELAY = 2;
    public static final short PRUNEDELAY_LENGTH = 4;
    public static final short DEFAULT_PRUNEDELAY = 500; // 500 ms
    public static final short DEFAULT_OVERRIDEINTERVAL = 2500; // 2500 ms

    public static final short OPT_PRIORITY = 19;
    public static final short PRIORITY_LENGTH = 4;
    public static final int DEFAULT_PRIORITY = 1;

    public static final short OPT_GENID = 20;
    public static final short GENID_LENGTH = 4;
    public static final int DEFAULT_GENID = 0;

    public static final short OPT_ADDRLIST = 24;

    public static final int MINIMUM_OPTION_LEN_BYTES = 4;

    // Values for this particular hello option.
    private short optType = 0;
    private short optLength = 0;
    private byte[] optValue;

    /**
     * Constructs a new hello option with no fields set.
     */
    public PIMHelloOption() {
    }

    /**
     * Set a PIM Hello option by type. The length and default value of the
     * type will be auto filled in by default.
     *
     * @param type hello option type
     */
    public PIMHelloOption(short type) {
        this.optType = type;
        switch (type) {
            case OPT_HOLDTIME:
                this.optLength = HOLDTIME_LENGTH;
                this.optValue = new byte[optLength];
                ByteBuffer.wrap(this.optValue).putShort(PIMHelloOption.DEFAULT_HOLDTIME);
                break;

            case OPT_PRUNEDELAY:
                this.optLength = PRUNEDELAY_LENGTH;
                this.optValue = new byte[this.optLength];
                ByteBuffer.wrap(this.optValue).putInt(PIMHelloOption.DEFAULT_PRUNEDELAY);
                break;

            case OPT_PRIORITY:
                this.optLength = PRIORITY_LENGTH;
                this.optValue = new byte[this.optLength];
                ByteBuffer.wrap(this.optValue).putInt(PIMHelloOption.DEFAULT_PRIORITY);
                break;

            case OPT_GENID:
                this.optLength = GENID_LENGTH;
                this.optValue = new byte[this.optLength];
                ByteBuffer.wrap(this.optValue).putInt(PIMHelloOption.DEFAULT_GENID);
                break;

            case OPT_ADDRLIST:
                this.optLength = 0;   // We don't know what the length will be yet.
                this.optValue = null;
                break;

            default:
                //log.error("Unkown option type: " + type + "\n" );
                return;
        }
    }

    public void setOptType(short type) {
        this.optType = type;
    }

    public short getOptType() {
        return this.optType;
    }

    public void setOptLength(short len) {
        this.optLength = len;
    }

    public short getOptLength() {
        return this.optLength;
    }

    public void setValue(ByteBuffer bb) {
        this.optValue = new byte[this.optLength];
        bb.get(this.optValue, 0, this.optLength);
    }

    public void setValue(byte[] value) {
        this.optValue = value;
    }

    public byte[] getValue() {
        return this.optValue;
    }

    /**
     * Creates a new PIM Hello option with the specified values.
     *
     * @param type hello option type
     * @param length option length
     * @param value option value
     * @return new PIM Hello option
     */
    public static PIMHelloOption create(short type, short length, ByteBuffer value) {
        PIMHelloOption option = new PIMHelloOption();
        option.setOptType(type);
        option.setOptLength(length);
        value.rewind();
        option.setValue(value);
        return option;
    }

    /**
     * Creates a new priority option.
     *
     * @param priority priority
     * @return priority option
     */
    public static PIMHelloOption createPriority(int priority) {
        return create(OPT_PRIORITY, PRIORITY_LENGTH,
                ByteBuffer.allocate(PRIORITY_LENGTH).putInt(priority));
    }

    /**
     * Creates a new hold time option.
     *
     * @param holdTime hold time
     * @return hold time option
     */
    public static PIMHelloOption createHoldTime(short holdTime) {
        return create(OPT_HOLDTIME, HOLDTIME_LENGTH,
                ByteBuffer.allocate(HOLDTIME_LENGTH).putShort(holdTime));
    }

    /**
     * Creates a new generation ID option with a particular generation ID.
     *
     * @param genId generation ID value
     * @return generation ID option
     */
    public static PIMHelloOption createGenID(int genId) {
        return create(OPT_GENID, GENID_LENGTH,
                ByteBuffer.allocate(GENID_LENGTH).putInt(genId));
    }

    /**
     * Creates a new LAN Prune Delay option.
     *
     * @param propagationDelay prune delay
     * @param overrideInterval override interval
     * @return prune delay option
     */
    public static PIMHelloOption createPruneDelay(short propagationDelay, short overrideInterval) {
        return create(OPT_PRUNEDELAY, PRUNEDELAY_LENGTH,
                ByteBuffer.allocate(PRUNEDELAY_LENGTH)
                        .putShort(propagationDelay)
                        .putShort(overrideInterval));
    }

    public static PIMHelloOption deserialize(ByteBuffer bb) throws DeserializationException {
        checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), MINIMUM_OPTION_LEN_BYTES);

        PIMHelloOption opt = new PIMHelloOption();
        opt.setOptType(bb.getShort());
        opt.setOptLength(bb.getShort());

        checkBufferLength(bb.limit(), bb.position(), opt.getOptLength());
        opt.setValue(bb);

        return opt;
    }

    public byte[] serialize() {
        int len = MINIMUM_OPTION_LEN_BYTES + this.optLength;
        ByteBuffer bb = ByteBuffer.allocate(len);
        bb.putShort(this.optType);
        bb.putShort(this.optLength);
        bb.put(this.optValue);
        return bb.array();
    }

    public String toString() {
        return MessageFormat.format("Type: {0}, len: {1} value: {2}", this.optType, this.optLength,
                Arrays.toString(this.optValue));
    }

}
