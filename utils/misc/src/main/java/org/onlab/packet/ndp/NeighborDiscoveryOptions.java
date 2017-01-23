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
package org.onlab.packet.ndp;

import org.onlab.packet.BasePacket;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Neighbor Discovery Protocol packet options.
 */
public class NeighborDiscoveryOptions extends BasePacket {
    public static final byte TYPE_SOURCE_LL_ADDRESS = 1;
    public static final byte TYPE_TARGET_LL_ADDRESS = 2;
    public static final byte TYPE_PREFIX_INFORMATION = 3;
    public static final byte TYPE_REDIRECTED_HEADER = 4;
    public static final byte TYPE_MTU = 5;

    public static final byte INITIAL_HEADER_REQUIRED = 2;

    private static final String BUFFER_UNDERFLOW_ERROR =
            "Not enough bytes in buffer to read option";

    private final List<Option> options = new ArrayList<>();

    /**
     * Packet option.
     */
    public final class Option {
        private final byte type;
        private final byte[] data;

        /**
         * Constructor.
         *
         * @param type the option type
         * @param data the option data
         */
        private Option(byte type, byte[] data) {
            this.type = type;
            this.data = Arrays.copyOfRange(data, 0, data.length);
        }

        /**
         * Gets the option type.
         *
         * @return the option type
         */
        public byte type() {
            return this.type;
        }

        /**
         * Gets the option data.
         *
         * @return the option data
         */
        public byte[] data() {
            return this.data;
        }

        /**
         * Gets the option data length (in number of octets).
         *
         * @return the option data length (in number of octets)
         */
        public int dataLength() {
            return data.length;
        }

        /**
         * Gets the option length (in number of octets), including the type and
         * length fields (one octet each).
         *
         * @return the option length (in number of octets), including the type
         * and length fields
         */
        private int optionLength() {
            return 2 + dataLength();
        }

        /**
         * Gets the option length field value (in units of 8 octets).
         *
         * @return the option length field value (in units of 8 octets)
         */
        private byte optionLengthField() {
            return (byte) ((optionLength() + 7) / 8);
        }

        /**
         * Gets the option length on the wire (in number of octets).
         *
         * @return the option length on the wire (in number of octets)
         */
        private int optionWireLength() {
            return 8 * optionLengthField();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("type", type).
                    add("data", data).toString();
        }
    }

    /**
     * Adds a Neighbor Discovery Protocol packet option.
     *
     * @param type the option type
     * @param data the option data
     * @return this
     */
    public NeighborDiscoveryOptions addOption(byte type, byte[] data) {
        options.add(new Option(type, data));
        return this;
    }

    /**
     * Gets the Neighbor Discovery Protocol packet options.
     *
     * @return the Neighbor Discovery Protocol packet options
     */
    public List<NeighborDiscoveryOptions.Option> options() {
        return this.options;
    }

    /**
     * Checks whether any options are included.
     *
     * @return true if options are included, otherwise false
     */
    public boolean hasOptions() {
        return !this.options.isEmpty();
    }

    @Override
    public byte[] serialize() {
        // Compute first the total length on the wire for all options

        int wireLength = 0;

        for (Option option : this.options) {
            wireLength += option.optionWireLength();
        }

        final byte[] data = new byte[wireLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        //
        // Serialize all options
        //
        for (Option option : this.options) {
            bb.put(option.type());
            bb.put(option.optionLengthField());
            bb.put(option.data());
            // Add the padding
            int paddingLength =
                option.optionWireLength() - option.optionLength();
            for (int i = 0; i < paddingLength; i++) {
                bb.put((byte) 0);
            }
        }

        return data;
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

        options.clear();

        //
        // Deserialize all options
        //
        while (bb.hasRemaining()) {
            byte type = bb.get();
            if (!bb.hasRemaining()) {
                break;
            }
            byte lengthField = bb.get();
            int dataLength = lengthField * 8;   // The data length field is in
            // unit of 8 octets

            // Exclude the type and length fields
            if (dataLength < 2) {
                break;
            }
            dataLength -= 2;

            if (bb.remaining() < dataLength) {
                break;
            }
            byte[] optionData = new byte[dataLength];
            bb.get(optionData, 0, optionData.length);
            addOption(type, optionData);
        }

        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.options.toArray());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NeighborDiscoveryOptions) {
            NeighborDiscoveryOptions other = (NeighborDiscoveryOptions) obj;
            return Objects.equal(this.options, other.options);
        }
        return false;
    }

    public static Deserializer<NeighborDiscoveryOptions> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, INITIAL_HEADER_REQUIRED);

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            NeighborDiscoveryOptions ndo = new NeighborDiscoveryOptions();

            ndo.options.clear();

            //
            // Deserialize all options
            //
            while (bb.hasRemaining()) {
                byte type = bb.get();
                if (!bb.hasRemaining()) {
                    throw new DeserializationException(BUFFER_UNDERFLOW_ERROR);
                }
                byte lengthField = bb.get();
                int dataLength = lengthField * 8;   // The data length field is in
                // unit of 8 octets

                // Exclude the type and length fields
                if (dataLength < 2) {
                    break;
                }
                dataLength -= 2;

                if (bb.remaining() < dataLength) {
                    throw new DeserializationException(BUFFER_UNDERFLOW_ERROR);
                }
                byte[] optionData = new byte[dataLength];
                bb.get(optionData, 0, optionData.length);
                ndo.addOption(type, optionData);
            }

            return ndo;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .toString();
        // TODO: need to handle options
    }
}
