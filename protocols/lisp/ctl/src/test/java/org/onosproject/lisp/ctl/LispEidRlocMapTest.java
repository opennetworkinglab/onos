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
package org.onosproject.lisp.ctl;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.protocols.DefaultLispLocatorRecord.DefaultLocatorRecordBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispLocatorRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord.MapRecordBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for LISP EID RLOC Map class.
 */
public class LispEidRlocMapTest {

    final LispEidRlocMap eidRlocMap = LispEidRlocMap.getInstance();

    @Before
    public void setup() {

        byte cidr1 = (byte) 32;
        LispIpv4Address eid1 = new LispIpv4Address(IpAddress.valueOf("10.1.1.1"));
        LispEidRecord eidRecord1 = new LispEidRecord(cidr1, eid1);

        LispIpv4Address locator11 = new LispIpv4Address(IpAddress.valueOf("123.1.1.1"));
        LispIpv4Address locator12 = new LispIpv4Address(IpAddress.valueOf("123.1.1.2"));
        LispIpv4Address locator13 = new LispIpv4Address(IpAddress.valueOf("123.1.1.3"));

        LispLocatorRecord locatorRecord11 = new DefaultLocatorRecordBuilder()
                                            .withLocatorAfi(locator11).build();
        LispLocatorRecord locatorRecord12 = new DefaultLocatorRecordBuilder()
                                            .withLocatorAfi(locator12).build();
        LispLocatorRecord locatorRecord13 = new DefaultLocatorRecordBuilder()
                                            .withLocatorAfi(locator13).build();
        List<LispLocatorRecord> locatorRecords1 =
                ImmutableList.of(locatorRecord11, locatorRecord12, locatorRecord13);

        byte cidr2 = (byte) 24;
        LispIpv4Address eid2 = new LispIpv4Address(IpAddress.valueOf("10.1.2.0"));
        LispEidRecord eidRecord2 = new LispEidRecord(cidr2, eid2);

        LispIpv4Address locator21 = new LispIpv4Address(IpAddress.valueOf("123.2.1.1"));
        LispIpv4Address locator22 = new LispIpv4Address(IpAddress.valueOf("123.2.1.2"));

        LispLocatorRecord locatorRecord21 = new DefaultLocatorRecordBuilder()
                                            .withLocatorAfi(locator21).build();
        LispLocatorRecord locatorRecord22 = new DefaultLocatorRecordBuilder()
                                            .withLocatorAfi(locator22).build();

        List<LispLocatorRecord> locatorRecords2 =
                    ImmutableList.of(locatorRecord21, locatorRecord22);

        byte cidr3 = (byte) 16;
        LispIpv4Address eid3 = new LispIpv4Address(IpAddress.valueOf("10.2.0.0"));
        LispEidRecord eidRecord3 = new LispEidRecord(cidr3, eid3);

        LispIpv4Address locator31 = new LispIpv4Address(IpAddress.valueOf("123.3.1.1"));

        LispLocatorRecord locatorRecord31 = new DefaultLocatorRecordBuilder()
                                            .withLocatorAfi(locator31).build();

        List<LispLocatorRecord> locatorRecords3 = ImmutableList.of(locatorRecord31);

        MapRecordBuilder builder1 = new DefaultMapRecordBuilder();
        builder1.withMaskLength(cidr1);
        builder1.withEidPrefixAfi(eid1);
        builder1.withLocators(locatorRecords1);
        LispMapRecord mapRecord1 = builder1.build();

        MapRecordBuilder builder2 = new DefaultMapRecordBuilder();
        builder2.withMaskLength(cidr2);
        builder2.withEidPrefixAfi(eid2);
        builder2.withLocators(locatorRecords2);
        LispMapRecord mapRecord2 = builder2.build();

        MapRecordBuilder builder3 = new DefaultMapRecordBuilder();
        builder3.withMaskLength(cidr3);
        builder3.withEidPrefixAfi(eid3);
        builder3.withLocators(locatorRecords3);
        LispMapRecord mapRecord3 = builder3.build();

        eidRlocMap.insertMapRecord(eidRecord1, mapRecord1);
        eidRlocMap.insertMapRecord(eidRecord2, mapRecord2);
        eidRlocMap.insertMapRecord(eidRecord3, mapRecord3);
    }

    @Test
    public void test32MaskRange() {
        byte cidr32 = (byte) 32;
        LispIpv4Address eid = new LispIpv4Address(IpAddress.valueOf("10.1.1.1"));
        LispEidRecord record = new LispEidRecord(cidr32, eid);
        LispMapRecord mapRecord = eidRlocMap.getMapRecordByEidRecord(record);

        assertThat("Failed to fetch the RLOCs with /32 EID record",
                    mapRecord.getLocatorCount(), is(3));
    }

    @Test
    public void test24MaskRange() {
        byte cidr32 = (byte) 32;
        LispIpv4Address eid = new LispIpv4Address(IpAddress.valueOf("10.1.2.1"));
        LispEidRecord record32 = new LispEidRecord(cidr32, eid);
        LispMapRecord mapRecord32 = eidRlocMap.getMapRecordByEidRecord(record32);

        byte cidr24 = (byte) 24;
        LispIpv4Address eid24 = new LispIpv4Address(IpAddress.valueOf("10.1.2.0"));
        LispEidRecord record24 = new LispEidRecord(cidr24, eid24);
        LispMapRecord mapRecord24 = eidRlocMap.getMapRecordByEidRecord(record24);

        assertThat("Failed to fetch the RLOCs with /32 EID record",
                    mapRecord32.getLocatorCount(), is(2));
        assertThat("Failed to fetch the RLOCs with /24 EID record",
                    mapRecord24.getLocatorCount(), is(2));
    }

    @Test
    public void test16MaskRange() {
        byte cidr32 = (byte) 32;
        LispIpv4Address eid = new LispIpv4Address(IpAddress.valueOf("10.2.1.1"));
        LispEidRecord record32 = new LispEidRecord(cidr32, eid);
        LispMapRecord mapRecord32 = eidRlocMap.getMapRecordByEidRecord(record32);

        byte cidr24 = (byte) 24;
        LispIpv4Address eid24 = new LispIpv4Address(IpAddress.valueOf("10.2.1.0"));
        LispEidRecord record24 = new LispEidRecord(cidr24, eid24);
        LispMapRecord mapRecord24 = eidRlocMap.getMapRecordByEidRecord(record24);

        byte cidr16 = (byte) 16;
        LispIpv4Address eid16 = new LispIpv4Address(IpAddress.valueOf("10.2.0.0"));
        LispEidRecord record16 = new LispEidRecord(cidr16, eid16);
        LispMapRecord mapRecord16 = eidRlocMap.getMapRecordByEidRecord(record16);

        assertThat("Failed to fetch the RLOCs with /32 EID record",
                    mapRecord32.getLocatorCount(), is(1));
        assertThat("Failed to fetch the RLOCs with /24 EID record",
                    mapRecord24.getLocatorCount(), is(1));
        assertThat("Failed to fetch the RLOCs with /16 EID record",
                    mapRecord16.getLocatorCount(), is(1));
    }
}
