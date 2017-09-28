/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onlab.packet.dhcp;

import com.google.common.collect.Maps;
import org.onlab.packet.Deserializer;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Representation of DHCP relay agent option (option 82).
 */
public class DhcpRelayAgentOption extends DhcpOption {
    private static final int SUB_OPT_DEFAULT_LEN = 2;
    private final Logger log = getLogger(getClass());
    private final Map<Byte, DhcpOption> subOptions = Maps.newHashMap();

    // Sub-option codes for option 82
    public enum RelayAgentInfoOptions {
        CIRCUIT_ID((byte) 1),
        REMOTE_ID((byte) 2),
        DOCSIS((byte) 4),
        LINK_SELECTION((byte) 5),
        SUBSCRIBER_ID((byte) 6),
        RADIUS((byte) 7),
        AUTH((byte) 8),
        VENDOR_SPECIFIC((byte) 9),
        RELAY_AGENT_FLAGS((byte) 10),
        SERVER_ID_OVERRIDE((byte) 11),
        VIRTUAL_SUBNET_SELECTION((byte) 151),
        VIRTUAL_SUBNET_SELECTION_CTRL((byte) 152);

        private byte value;
        public byte getValue() {
            return value;
        }
        RelayAgentInfoOptions(byte value) {
            this.value = value;
        }
    }

    @Override
    public byte[] serialize() {
        int totalLen = 0;
        totalLen += subOptions.size() * SUB_OPT_DEFAULT_LEN;
        totalLen += subOptions.values().stream().mapToInt(DhcpOption::getLength).sum();
        totalLen += DEFAULT_LEN;
        ByteBuffer byteBuffer = ByteBuffer.allocate(totalLen);
        byteBuffer.put(code);
        byteBuffer.put(length);
        subOptions.values().forEach(subOpt -> {
            byteBuffer.put(subOpt.code);
            byteBuffer.put(subOpt.length);
            byteBuffer.put(subOpt.data);
        });
        return byteBuffer.array();
    }



    /**
     * Deserializer function for DHCP relay agent option.
     *
     * @return deserializer function
     */
    public static Deserializer<DhcpOption> deserializer() {
        return (data, offset, length) -> {
            DhcpRelayAgentOption relayOption = new DhcpRelayAgentOption();
            ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, length);
            relayOption.code = byteBuffer.get();
            relayOption.length = byteBuffer.get();

            while (byteBuffer.remaining() >= DEFAULT_LEN) {
                byte subOptCode = byteBuffer.get();
                byte subOptLen = byteBuffer.get();
                int subOptLenInt = UNSIGNED_BYTE_MASK & subOptLen;
                byte[] subOptData = new byte[subOptLenInt];
                byteBuffer.get(subOptData);

                DhcpOption subOption = new DhcpOption();
                subOption.code = subOptCode;
                subOption.length = subOptLen;
                subOption.data = subOptData;
                relayOption.subOptions.put(subOptCode, subOption);
            }

            return relayOption;
        };
    }

    /**
     * Gets sub-option from this option by given option code.
     *
     * @param code the option code
     * @return sub-option of given code; null if there is no sub-option for given
     * code
     */
    public DhcpOption getSubOption(byte code) {
        return subOptions.get(code);
    }

    /**
     * Adds a sub-option for this option.
     *
     * @param subOption the sub-option
     */
    public void addSubOption(DhcpOption subOption) {
        this.length += SUB_OPT_DEFAULT_LEN + subOption.length;
        this.subOptions.put(subOption.getCode(), subOption);
    }

    /**
     * Removes a sub-option by given sub-option code.
     *
     * @param code the code for sub-option
     * @return sub-option removed; null of sub-option not exists
     */
    public DhcpOption removeSubOption(byte code) {
        DhcpOption subOption = subOptions.remove(code);
        if (subOption != null) {
            this.length -= SUB_OPT_DEFAULT_LEN + subOption.length;
        }
        return subOption;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DhcpRelayAgentOption)) {
            return false;
        }
        DhcpRelayAgentOption that = (DhcpRelayAgentOption) obj;
        return Objects.equals(this.subOptions, that.subOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subOptions);
    }
}
