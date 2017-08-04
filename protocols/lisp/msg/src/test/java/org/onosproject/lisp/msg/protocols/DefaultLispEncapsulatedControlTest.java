/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.lisp.msg.protocols;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.IP;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.UDP;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispEncapsulatedControl.DefaultEcmBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispEncapsulatedControl.EcmReader;
import org.onosproject.lisp.msg.protocols.DefaultLispEncapsulatedControl.EcmWriter;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRegister.DefaultRegisterBuilder;
import org.onosproject.lisp.msg.protocols.LispEncapsulatedControl.EcmBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRegister.RegisterBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.protocols.LispMapRecord.MapRecordBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


/**
 * Unit tests for DefaultLispEncapsulatedControl class.
 */
public final class DefaultLispEncapsulatedControlTest {

    private static final String ECM1_SRC_IP = "192.168.1.1";
    private static final String ECM1_DST_IP = "192.168.1.2";
    private static final String ECM2_SRC_IP = "192.168.2.1";
    private static final String ECM2_DST_IP = "192.168.2.2";
    private static final String RECORD_EID = "1.1.1.1";

    private static final String AUTH_KEY = "onos";

    private LispEncapsulatedControl ecm1;
    private LispEncapsulatedControl sameAsEcm1;
    private LispEncapsulatedControl ecm2;

    @Before
    public void setup() {

        //Creates ecm1
        EcmBuilder builder1 = new DefaultEcmBuilder();

        IP innerIp1 = new IPv4().setSourceAddress(ECM1_SRC_IP)
                .setDestinationAddress(ECM1_DST_IP)
                .setProtocol(IPv4.PROTOCOL_UDP).setVersion((byte) (4 & 0xf));
        UDP innerUdp1 = new UDP().setSourcePort(1)
                .setDestinationPort(2);

        RegisterBuilder msgBuilder = new DefaultRegisterBuilder();

        List<LispMapRecord> records1 = ImmutableList.of(getMapRecord(),
                                                        getMapRecord());

        LispMapRegister innerMsg1 = msgBuilder.withIsProxyMapReply(true)
                                    .withIsWantMapNotify(false)
                                    .withKeyId((short) 1)
                                    .withAuthKey(AUTH_KEY)
                                    .withNonce(1L)
                                    .withMapRecords(records1)
                                    .build();

        ecm1 = builder1.isSecurity(false)
                .innerIpHeader(innerIp1)
                .innerUdpHeader(innerUdp1)
                .innerLispMessage(innerMsg1)
                .build();

        //Creates sameAsEcm1
        EcmBuilder builder2 = new DefaultEcmBuilder();

        IP innerIp2 = new IPv4().setSourceAddress(ECM1_SRC_IP)
                .setDestinationAddress(ECM1_DST_IP)
                .setProtocol(IPv4.PROTOCOL_UDP);
        UDP innerUdp2 = new UDP().setSourcePort(1)
                .setDestinationPort(2);

        RegisterBuilder msgBuilder2 = new DefaultRegisterBuilder();

        List<LispMapRecord> records2 = ImmutableList.of(getMapRecord(),
                                                        getMapRecord());

        LispMapRegister innerMsg2 = msgBuilder2.withIsProxyMapReply(true)
                .withIsWantMapNotify(false)
                .withKeyId((short) 1)
                .withAuthKey(AUTH_KEY)
                .withNonce(1L)
                .withMapRecords(records2)
                .build();

        sameAsEcm1 = builder2.isSecurity(false)
                .innerIpHeader(innerIp2)
                .innerUdpHeader(innerUdp2)
                .innerLispMessage(innerMsg2)
                .build();

        //Creates ecm2
        EcmBuilder builder3 = new DefaultEcmBuilder();

        IP innerIp3 = new IPv4().setSourceAddress(ECM2_SRC_IP)
                .setDestinationAddress(ECM2_DST_IP)
                .setProtocol(IPv4.PROTOCOL_UDP);
        UDP innerUdp3 = new UDP().setSourcePort(10)
                .setDestinationPort(20);

        RegisterBuilder msgBuilder3 = new DefaultRegisterBuilder();

        List<LispMapRecord> records3 = ImmutableList.of(getMapRecord(),
                                                        getMapRecord());

        LispMapRegister innerMsg3 = msgBuilder3.withIsProxyMapReply(true)
                .withIsWantMapNotify(false)
                .withKeyId((short) 2)
                .withAuthKey(AUTH_KEY)
                .withNonce(1L)
                .withMapRecords(records3)
                .build();

        ecm2 = builder3.isSecurity(false)
                .innerIpHeader(innerIp3)
                .innerUdpHeader(innerUdp3)
                .innerLispMessage(innerMsg3)
                .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(ecm1, sameAsEcm1)
                .addEqualityGroup(ecm2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultLispEncapsulatedControl ecm = (DefaultLispEncapsulatedControl) ecm1;

        assertThat("Inner Ip versions are not match",
                   ecm.innerIpHeader().getVersion(), is((byte) 4));
        assertThat("Inner Ip protocols are not match",
                   ((IPv4) ecm.innerIpHeader()).getProtocol(),
                   is(IPv4.PROTOCOL_UDP));
        assertThat("Inner IP source addresses are not match",
                   ((IPv4) ecm.innerIpHeader()).getSourceAddress(),
                   is(IPv4.toIPv4Address(ECM1_SRC_IP)));
        assertThat("Inner IP destination addresses are not match",
                   ((IPv4) ecm.innerIpHeader()).getDestinationAddress(),
                   is(IPv4.toIPv4Address(ECM1_DST_IP)));
        assertThat("Inner UDP source ports are not match",
                   ecm.innerUdp().getSourcePort(), is(1));
        assertThat("Inner UDP destination ports are not match",
                   ecm.innerUdp().getDestinationPort(), is(2));
        assertThat("Inner LISP control messages are not match",
                   ecm.getControlMessage().getType(),
                   is(LispType.LISP_MAP_REGISTER));
    }

    @Test
    public void testSerialization() throws LispReaderException,
            LispWriterException, LispParseError, DeserializationException {

        ByteBuf byteBuf = Unpooled.buffer();
        EcmWriter writer = new EcmWriter();
        writer.writeTo(byteBuf, ecm1);

        EcmReader reader = new EcmReader();

        LispEncapsulatedControl deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(ecm1, deserialized).testEquals();
    }

    private LispMapRecord getMapRecord() {
        MapRecordBuilder builder1 = new DefaultMapRecordBuilder();

        LispIpv4Address ipv4Locator1 =
                new LispIpv4Address(IpAddress.valueOf(RECORD_EID));

        return builder1
                .withRecordTtl(100)
                .withIsAuthoritative(true)
                .withMapVersionNumber((short) 1)
                .withMaskLength((byte) 0x01)
                .withAction(LispMapReplyAction.NativelyForward)
                .withEidPrefixAfi(ipv4Locator1)
                .build();
    }
}
