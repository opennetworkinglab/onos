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
package org.onosproject.lisp.msg.types.lcaf;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.lcaf.LispTeLcafAddress.TeAddressBuilder;
import org.onosproject.lisp.msg.types.lcaf.LispTeLcafAddress.TeLcafAddressReader;
import org.onosproject.lisp.msg.types.lcaf.LispTeLcafAddress.TeLcafAddressWriter;
import org.onosproject.lisp.msg.types.lcaf.LispTeRecord.TeRecordBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispTeLcafAddress class.
 */
public class LispTeLcafAddressTest {

    private LispTeLcafAddress address1;
    private LispTeLcafAddress sameAsAddress1;
    private LispTeLcafAddress address2;

    @Before
    public void setup() {

        TeAddressBuilder builder1 = new TeAddressBuilder();

        TeRecordBuilder recordBuilder1 = new TeRecordBuilder();
        LispIpv4Address rtrRloc1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        recordBuilder1.withIsLookup(false);
        recordBuilder1.withIsRlocProbe(false);
        recordBuilder1.withIsStrict(false);
        recordBuilder1.withRtrRlocAddress(rtrRloc1);
        LispTeRecord record1 = recordBuilder1.build();

        TeRecordBuilder recordBuilder2 = new TeRecordBuilder();
        LispIpv4Address rtrRloc2 = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));

        recordBuilder2.withIsLookup(false);
        recordBuilder2.withIsRlocProbe(true);
        recordBuilder2.withIsStrict(false);
        recordBuilder2.withRtrRlocAddress(rtrRloc2);
        LispTeRecord record2 = recordBuilder2.build();

        builder1.withTeRecords(ImmutableList.of(record1, record2));

        address1 = builder1.build();

        TeAddressBuilder builder2 = new TeAddressBuilder();

        TeRecordBuilder recordBuilder3 = new TeRecordBuilder();
        recordBuilder3.withIsLookup(false);
        recordBuilder3.withIsRlocProbe(false);
        recordBuilder3.withIsStrict(false);
        recordBuilder3.withRtrRlocAddress(rtrRloc1);
        LispTeRecord record3 = recordBuilder3.build();

        TeRecordBuilder recordBuilder4 = new TeRecordBuilder();
        recordBuilder4.withIsLookup(false);
        recordBuilder4.withIsRlocProbe(true);
        recordBuilder4.withIsStrict(false);
        recordBuilder4.withRtrRlocAddress(rtrRloc2);
        LispTeRecord record4 = recordBuilder4.build();

        builder2.withTeRecords(ImmutableList.of(record3, record4));

        sameAsAddress1 = builder2.build();

        TeAddressBuilder builder3 = new TeAddressBuilder();

        TeRecordBuilder recordBuilder5 = new TeRecordBuilder();
        LispIpv4Address rtrRloc3 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));

        recordBuilder5.withIsLookup(true);
        recordBuilder5.withIsRlocProbe(false);
        recordBuilder5.withIsStrict(true);
        recordBuilder5.withRtrRlocAddress(rtrRloc3);
        LispTeRecord record5 = recordBuilder5.build();

        TeRecordBuilder recordBuilder6 = new TeRecordBuilder();
        LispIpv4Address rtrRloc4 = new LispIpv4Address(IpAddress.valueOf("192.168.2.2"));

        recordBuilder6.withIsLookup(true);
        recordBuilder6.withIsRlocProbe(true);
        recordBuilder6.withIsStrict(true);
        recordBuilder6.withRtrRlocAddress(rtrRloc4);
        LispTeRecord record6 = recordBuilder6.build();

        builder3.withTeRecords(ImmutableList.of(record5, record6));

        address2 = builder3.build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(address1, sameAsAddress1)
                .addEqualityGroup(address2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispTeLcafAddress teLcafAddress = address1;
        LispIpv4Address rtrRloc1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        LispIpv4Address rtrRloc2 = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));

        assertThat("lookup flag value in TeRecord is not correct",
                    teLcafAddress.getTeRecords().get(0).isLookup(), is(false));
        assertThat("RLOC probe flag value in TeRecord is not correct",
                    teLcafAddress.getTeRecords().get(0).isRlocProbe(), is(false));
        assertThat("strict flag value in TeRecord is not correct",
                    teLcafAddress.getTeRecords().get(0).isStrict(), is(false));
        assertThat("RTR RLOC address in TeRecord is not correct",
                    teLcafAddress.getTeRecords().get(0).getRtrRlocAddress(), is(rtrRloc1));

        assertThat("lookup flag value in TeRecord in not correct",
                    teLcafAddress.getTeRecords().get(1).isLookup(), is(false));
        assertThat("RLOC probe flag value in TeRecord is not correct",
                    teLcafAddress.getTeRecords().get(1).isRlocProbe(), is(true));
        assertThat("strict flag value in TeRecord is not correct",
                    teLcafAddress.getTeRecords().get(1).isStrict(), is(false));
        assertThat("RTR RLOC address in TeRecord is not correct",
                    teLcafAddress.getTeRecords().get(1).getRtrRlocAddress(), is(rtrRloc2));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError, LispReaderException {
        ByteBuf byteBuf = Unpooled.buffer();

        TeLcafAddressWriter writer = new TeLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        TeLcafAddressReader reader = new TeLcafAddressReader();
        LispTeLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
