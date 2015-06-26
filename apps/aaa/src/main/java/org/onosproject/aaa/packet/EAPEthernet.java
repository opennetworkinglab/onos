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

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.LLDP;

/**
 * Created by jono on 5/19/15.
 */
public final class EAPEthernet extends Ethernet {

    public static final short TYPE_PAE = (short) 0x888e;

    private EAPEthernet() {

    }

    static {
        Ethernet.ETHER_TYPE_CLASS_MAP.put(org.onlab.packet.Ethernet.TYPE_ARP, ARP.class);
        org.onlab.packet.Ethernet.ETHER_TYPE_CLASS_MAP.put(org.onlab.packet.Ethernet.TYPE_RARP, ARP.class);
        org.onlab.packet.Ethernet.ETHER_TYPE_CLASS_MAP.put(org.onlab.packet.Ethernet.TYPE_IPV4, IPv4.class);
        org.onlab.packet.Ethernet.ETHER_TYPE_CLASS_MAP.put(org.onlab.packet.Ethernet.TYPE_IPV6, IPv6.class);
        org.onlab.packet.Ethernet.ETHER_TYPE_CLASS_MAP.put(org.onlab.packet.Ethernet.TYPE_LLDP, LLDP.class);
        org.onlab.packet.Ethernet.ETHER_TYPE_CLASS_MAP.put(org.onlab.packet.Ethernet.TYPE_BSN, LLDP.class);
        org.onlab.packet.Ethernet.ETHER_TYPE_CLASS_MAP.put(TYPE_PAE, EAPOL.class);
    }
}
