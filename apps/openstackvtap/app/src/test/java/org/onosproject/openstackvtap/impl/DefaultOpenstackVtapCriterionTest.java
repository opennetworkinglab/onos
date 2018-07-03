/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.impl;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultOpenstackVtapCriterion.
 */
public class DefaultOpenstackVtapCriterionTest {

    private static final IpPrefix SRC_IP_PREFIX_1 =
            IpPrefix.valueOf(IpAddress.valueOf("10.10.10.1"), 32);
    private static final IpPrefix SRC_IP_PREFIX_2 =
            IpPrefix.valueOf(IpAddress.valueOf("20.20.20.1"), 32);
    private static final IpPrefix DST_IP_PREFIX_1 =
            IpPrefix.valueOf(IpAddress.valueOf("10.10.10.2"), 32);
    private static final IpPrefix DST_IP_PREFIX_2 =
            IpPrefix.valueOf(IpAddress.valueOf("20.20.20.2"), 32);

    private static final TpPort SRC_PORT_1 = TpPort.tpPort(10);
    private static final TpPort SRC_PORT_2 = TpPort.tpPort(20);
    private static final TpPort DST_PORT_1 = TpPort.tpPort(30);
    private static final TpPort DST_PORT_2 = TpPort.tpPort(40);

    private static final byte IP_PROTOCOL_1 = (byte) 1;
    private static final byte IP_PROTOCOL_2 = (byte) 2;

    private OpenstackVtapCriterion criterion1;
    private OpenstackVtapCriterion sameAsCriterion1;
    private OpenstackVtapCriterion criterion2;

    @Before
    public void setup() {

        OpenstackVtapCriterion.Builder builder1 = DefaultOpenstackVtapCriterion.builder();

        criterion1 = builder1
                        .srcIpPrefix(SRC_IP_PREFIX_1)
                        .dstIpPrefix(DST_IP_PREFIX_1)
                        .srcTpPort(SRC_PORT_1)
                        .dstTpPort(DST_PORT_1)
                        .ipProtocol(IP_PROTOCOL_1)
                        .build();

        OpenstackVtapCriterion.Builder builder2 = DefaultOpenstackVtapCriterion.builder();

        sameAsCriterion1 = builder2
                        .srcIpPrefix(SRC_IP_PREFIX_1)
                        .dstIpPrefix(DST_IP_PREFIX_1)
                        .srcTpPort(SRC_PORT_1)
                        .dstTpPort(DST_PORT_1)
                        .ipProtocol(IP_PROTOCOL_1)
                        .build();

        OpenstackVtapCriterion.Builder builder3 = DefaultOpenstackVtapCriterion.builder();

        criterion2 = builder3
                        .srcIpPrefix(SRC_IP_PREFIX_2)
                        .dstIpPrefix(DST_IP_PREFIX_2)
                        .srcTpPort(SRC_PORT_2)
                        .dstTpPort(DST_PORT_2)
                        .ipProtocol(IP_PROTOCOL_2)
                        .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(criterion1, sameAsCriterion1)
                .addEqualityGroup(criterion2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultOpenstackVtapCriterion criterion =
                                      (DefaultOpenstackVtapCriterion) criterion1;

        assertThat(criterion.srcIpPrefix(), is(SRC_IP_PREFIX_1));
        assertThat(criterion.dstIpPrefix(), is(DST_IP_PREFIX_1));
        assertThat(criterion.srcTpPort(), is(SRC_PORT_1));
        assertThat(criterion.dstTpPort(), is(DST_PORT_1));
        assertThat(criterion.ipProtocol(), is(IP_PROTOCOL_1));
    }
}
