/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vtnrsc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onosproject.net.PortNumber;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for FiveTuple class.
 */
public class DefaultFiveTupleTest {


    final FiveTuple fiveTuple1 = DefaultFiveTuple.builder().setIpSrc(IpAddress.valueOf("1.1.1.1"))
            .setIpDst(IpAddress.valueOf("2.2.2.2"))
            .setPortSrc(PortNumber.portNumber(500))
            .setPortDst(PortNumber.portNumber(1000))
            .setProtocol(IPv4.PROTOCOL_TCP)
            .setTenantId(TenantId.tenantId("aaa"))
            .build();

    final FiveTuple sameAsFiveTuple1 = DefaultFiveTuple.builder().setIpSrc(IpAddress.valueOf("1.1.1.1"))
            .setIpDst(IpAddress.valueOf("2.2.2.2"))
            .setPortSrc(PortNumber.portNumber(500))
            .setPortDst(PortNumber.portNumber(1000))
            .setProtocol(IPv4.PROTOCOL_TCP)
            .setTenantId(TenantId.tenantId("aaa"))
            .build();

    final FiveTuple fiveTuple2 =  DefaultFiveTuple.builder().setIpSrc(IpAddress.valueOf("3.3.3.3"))
            .setIpDst(IpAddress.valueOf("4.4.4.4"))
            .setPortSrc(PortNumber.portNumber(1500))
            .setPortDst(PortNumber.portNumber(2000))
            .setProtocol(IPv4.PROTOCOL_UDP)
            .setTenantId(TenantId.tenantId("bbb"))
            .build();

    /**
     * Checks that the FiveTuple class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultFiveTuple.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(fiveTuple1, sameAsFiveTuple1).addEqualityGroup(fiveTuple2)
        .testEquals();
    }

    /**
     * Checks the construction of a FiveTuple object.
     */
    @Test
    public void testConstruction() {
        final FiveTuple fiveTuple1 = DefaultFiveTuple.builder().setIpSrc(IpAddress.valueOf("1.1.1.1"))
                .setIpDst(IpAddress.valueOf("2.2.2.2"))
                .setPortSrc(PortNumber.portNumber(500))
                .setPortDst(PortNumber.portNumber(1000))
                .setProtocol(IPv4.PROTOCOL_TCP)
                .setTenantId(TenantId.tenantId("aaa"))
                .build();

        assertThat(fiveTuple1, is(notNullValue()));
        assertThat(fiveTuple1.protocol(), is(IPv4.PROTOCOL_TCP));
        assertThat(fiveTuple1.ipSrc(), is(IpAddress.valueOf("1.1.1.1")));
        assertThat(fiveTuple1.ipDst(), is(IpAddress.valueOf("2.2.2.2")));
        assertThat(fiveTuple1.portSrc(), is(PortNumber.portNumber(500)));
        assertThat(fiveTuple1.portDst(), is(PortNumber.portNumber(1000)));
        assertThat(fiveTuple1.tenantId(), is(TenantId.tenantId("aaa")));
    }
}
