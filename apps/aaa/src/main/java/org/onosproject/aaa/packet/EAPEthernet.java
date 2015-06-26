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

package org.onosproject.aaa.packet;

import org.onlab.packet.Deserializer;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jono on 5/19/15.
 */
public final class EAPEthernet extends Ethernet {

    public static final short TYPE_PAE = (short) 0x888e;

    private static final Map<Short, Deserializer<? extends IPacket>> ETHERTYPE_DESERIALIZER_MAP =
            new HashMap<>();

    private EAPEthernet() {

    }

    static {
        for (EthType.EtherType ethType : EthType.EtherType.values()) {
            if (ethType.deserializer() != null) {
                ETHERTYPE_DESERIALIZER_MAP.put(ethType.ethType().toShort(),
                                               ethType.deserializer());
            }
        }
        ETHERTYPE_DESERIALIZER_MAP.put((short) 0x888e, EAPOL.deserializer());
    }

}
