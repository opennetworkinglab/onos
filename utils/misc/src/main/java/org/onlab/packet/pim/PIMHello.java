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
package org.onlab.packet.pim;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import org.onlab.packet.IpAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

public class PIMHello extends BasePacket {

    private IpAddress nbrIpAddress;
    private boolean priorityPresent = false;

    private Map<Short, PIMHelloOption> options = new HashMap<>();

    /**
     * Create a PIM Hello packet with the most common hello options and default
     * values.  The values of any options can be easily changed by modifying the value of
     * the option with the desired change.
     */
    public void createDefaultOptions() {
        options.put(PIMHelloOption.OPT_HOLDTIME, new PIMHelloOption(PIMHelloOption.OPT_HOLDTIME));
        options.put(PIMHelloOption.OPT_PRIORITY, new PIMHelloOption(PIMHelloOption.OPT_PRIORITY));
        options.put(PIMHelloOption.OPT_GENID, new PIMHelloOption(PIMHelloOption.OPT_GENID));
    }

    /**
     * Add a PIM Hello option to this hello message.  Note
     *
     * @param opt the PIM Hello option we are adding
     */
    public void addOption(PIMHelloOption opt) {
        this.options.put(opt.getOptType(), opt);
    }

    public Map<Short, PIMHelloOption> getOptions() {
        return this.options;
    }

    /**
     * Sets all payloads parent packet if applicable, then serializes this
     * packet and all payloads.
     *
     * @return a byte[] containing this packet and payloads
     */
    @Override
    public byte[] serialize() {
        int totalLen = 0;


         // Since we are likely to only have 3-4 options, go head and walk the
         // hashmap twice, once to calculate the space needed to allocate a
         // buffer, the second time serialize the options into the buffer.  This
         // saves us from allocating an over sized buffer the re-allocating and
         // copying.
        for (Short optType : options.keySet()) {
            PIMHelloOption opt = options.get(optType);
            totalLen += PIMHelloOption.MINIMUM_OPTION_LEN_BYTES + opt.getOptLength();
        }

        byte[] data = new byte[totalLen];
        ByteBuffer bb = ByteBuffer.wrap(data);

        // Now serialize the data.
        for (Short optType : options.keySet()) {
            PIMHelloOption opt = options.get(optType);
            bb.put(opt.serialize());
        }
        return data;
    }

    /**
     * XXX: This is deprecated, DO NOT USE, use the deserializer() function instead.
     *
     * @param data bytes to deserialize
     * @param offset offset to start deserializing from
     * @param length length of the data to deserialize
     * @return nothing
     */
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        // TODO: throw an expection?
        return null;
    }

    /**
     * Deserialize this hello message.
     *
     * @return a deserialized hello message
     */
    public static Deserializer<PIMHello> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, PIMHelloOption.MINIMUM_OPTION_LEN_BYTES);
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            PIMHello hello = new PIMHello();
            while (bb.hasRemaining()) {
                PIMHelloOption opt = PIMHelloOption.deserialize(bb);
                hello.addOption(opt);
            }
            return hello;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("nbrIpAddress", nbrIpAddress.toString())
                .add("priorityPresent", Boolean.toString(priorityPresent))
                .toString();
        // TODO: need to handle options
    }
}
