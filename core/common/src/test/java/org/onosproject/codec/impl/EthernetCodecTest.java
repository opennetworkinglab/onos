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
package org.onosproject.codec.impl;

import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.codec.impl.EthernetJsonMatcher.matchesEthernet;

/**
 * Unit test for Ethernet class codec.
 */
public class EthernetCodecTest {

    /**
     * Unit test for the ethernet object codec.
     */
    @Test
    public void ethernetCodecTest() {
        final CodecContext context = new MockCodecContext();
        final JsonCodec<Ethernet> ethernetCodec = context.codec(Ethernet.class);
        assertThat(ethernetCodec, notNullValue());

        final Ethernet eth1 = new Ethernet();
        eth1.setSourceMACAddress("11:22:33:44:55:01");
        eth1.setDestinationMACAddress("11:22:33:44:55:02");
        eth1.setPad(true);
        eth1.setEtherType(Ethernet.TYPE_ARP);
        eth1.setPriorityCode((byte) 7);
        eth1.setVlanID((short) 33);

        final ObjectNode eth1Json = ethernetCodec.encode(eth1, context);
        assertThat(eth1Json, notNullValue());
        assertThat(eth1Json, matchesEthernet(eth1));
    }
}
