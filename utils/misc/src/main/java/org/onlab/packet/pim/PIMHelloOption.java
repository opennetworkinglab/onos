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

import org.onlab.packet.DeserializationException;

import java.nio.ByteBuffer;
import java.text.MessageFormat;

import static org.onlab.packet.PacketUtils.checkBufferLength;
import static org.onlab.packet.PacketUtils.checkInput;

public class PIMHelloOption {

    /**
     * PIM Option types.
     */
    public static final short OPT_HOLDTIME = 1;
    public static final short OPT_PRUNEDELAY = 2;
    public static final short OPT_PRIORITY = 19;
    public static final short OPT_GENID = 20;
    public static final short OPT_ADDRLIST = 24;

    public static final short DEFAULT_HOLDTIME = 105;
    public static final int DEFAULT_PRUNEDELAY = 2000; // 2,000 ms
    public static final int DEFAULT_PRIORITY = 1;
    public static final int DEFAULT_GENID = 0;

    public static final int MINIMUM_OPTION_LEN_BYTES = 4;

    // Values for this particular hello option.
    private short optType;
    private short optLength;
    private byte[] optValue;

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
                this.optLength = 2;
                this.optValue = new byte[optLength];
                ByteBuffer.wrap(this.optValue).putShort(PIMHelloOption.DEFAULT_HOLDTIME);
                break;

            case OPT_PRUNEDELAY:
                this.optLength = 4;
                this.optValue = new byte[this.optLength];
                ByteBuffer.wrap(this.optValue).putInt(PIMHelloOption.DEFAULT_PRUNEDELAY);
                break;

            case OPT_PRIORITY:
                this.optLength = 4;
                this.optValue = new byte[this.optLength];
                ByteBuffer.wrap(this.optValue).putInt(PIMHelloOption.DEFAULT_PRIORITY);
                break;

            case OPT_GENID:
                this.optLength = 4;
                this.optValue = new byte[this.optLength];
                ByteBuffer.wrap(this.optValue).putInt(PIMHelloOption.DEFAULT_GENID);
                break;

            case OPT_ADDRLIST:
                this.optLength = 0;   // We don't know what the length will be yet.
                this.optValue = null;

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

    public void setValue(ByteBuffer bb) throws DeserializationException {
        this.optValue = new byte[this.optLength];
        bb.get(this.optValue, 0, this.optLength);
    }

    public byte[] getValue() {
        return this.optValue;
    }

    public static PIMHelloOption deserialize(ByteBuffer bb) throws DeserializationException {
        checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), 4);

        PIMHelloOption opt = new PIMHelloOption();
        opt.setOptType(bb.getShort());
        opt.setOptLength(bb.getShort());

        checkBufferLength(bb.limit(), bb.position(), opt.getOptLength());
        opt.setValue(bb);

        return opt;
    }

    public byte [] serialize() {
        int len = 4 + this.optLength;
        ByteBuffer bb = ByteBuffer.allocate(len);
        bb.putShort(this.optType);
        bb.putShort(this.optLength);
        bb.put(this.optValue);
        return bb.array();
    }

    public String toString() {
        return MessageFormat.format("Type: {0}, len: {1} value: {2}", this.optType, this.optLength,
                (this.optValue == null) ? "null" : this.optValue.toString());
    }

}
