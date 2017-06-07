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
package org.onosproject.lisp.ctl.impl;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.protocols.DefaultLispLocator.DefaultLocatorBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispLocator;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord.MapRecordBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for LISP EID RLOC Map class.
 */
public class LispMappingDatabaseTest {

    private static final String LOCATOR_IP_1_1 = "123.1.1.1";
    private static final String LOCATOR_IP_1_2 = "123.1.1.2";
    private static final String LOCATOR_IP_1_3 = "123.1.1.3";

    private static final String LOCATOR_IP_2_1 = "123.2.1.1";
    private static final String LOCATOR_IP_2_2 = "123.2.1.2";

    private static final String LOCATOR_IP_3_1 = "123.3.1.1";

    private static final String EID_IP_1 = "10.1.1.1";
    private static final String EID_IP_2 = "10.1.2.0";
    private static final String EID_IP_3 = "10.2.0.0";

    private static final String EID_IP_PREFIX_1_32 = "10.2.1.1";
    private static final String EID_IP_PREFIX_1_24 = "10.2.1.0";
    private static final String EID_IP_PREFIX_1_16 = "10.2.0.0";

    private static final String EID_IP_PREFIX_2_32 = "10.1.2.1";
    private static final String EID_IP_PREFIX_2_24 = "10.1.2.0";

    private final LispMappingDatabase expireMapDb = LispExpireMapDatabase.getInstance();
    private final LispMappingDatabase radixTreeDb = LispRadixTreeDatabase.getInstance();

    @Before
    public void setup() {

        byte cidr1 = (byte) 32;
        LispIpv4Address eid1 = new LispIpv4Address(IpAddress.valueOf(EID_IP_1));
        LispEidRecord eidRecord1 = new LispEidRecord(cidr1, eid1);

        LispIpv4Address locator11 = new LispIpv4Address(IpAddress.valueOf(LOCATOR_IP_1_1));
        LispIpv4Address locator12 = new LispIpv4Address(IpAddress.valueOf(LOCATOR_IP_1_2));
        LispIpv4Address locator13 = new LispIpv4Address(IpAddress.valueOf(LOCATOR_IP_1_3));

        LispLocator locatorRecord11 = new DefaultLocatorBuilder()
                .withLocatorAfi(locator11).build();
        LispLocator locatorRecord12 = new DefaultLocatorBuilder()
                .withLocatorAfi(locator12).build();
        LispLocator locatorRecord13 = new DefaultLocatorBuilder()
                .withLocatorAfi(locator13).build();
        List<LispLocator> locatorRecords1 =
                ImmutableList.of(locatorRecord11, locatorRecord12, locatorRecord13);

        byte cidr2 = (byte) 24;
        LispIpv4Address eid2 = new LispIpv4Address(IpAddress.valueOf(EID_IP_2));
        LispEidRecord eidRecord2 = new LispEidRecord(cidr2, eid2);

        LispIpv4Address locator21 = new LispIpv4Address(IpAddress.valueOf(LOCATOR_IP_2_1));
        LispIpv4Address locator22 = new LispIpv4Address(IpAddress.valueOf(LOCATOR_IP_2_2));

        LispLocator locatorRecord21 = new DefaultLocatorBuilder()
                .withLocatorAfi(locator21).build();
        LispLocator locatorRecord22 = new DefaultLocatorBuilder()
                .withLocatorAfi(locator22).build();

        List<LispLocator> locatorRecords2 =
                ImmutableList.of(locatorRecord21, locatorRecord22);

        byte cidr3 = (byte) 16;
        LispIpv4Address eid3 = new LispIpv4Address(IpAddress.valueOf(EID_IP_3));
        LispEidRecord eidRecord3 = new LispEidRecord(cidr3, eid3);

        LispIpv4Address locator31 = new LispIpv4Address(IpAddress.valueOf(LOCATOR_IP_3_1));

        LispLocator locatorRecord31 = new DefaultLocatorBuilder()
                .withLocatorAfi(locator31).build();

        List<LispLocator> locatorRecords3 = ImmutableList.of(locatorRecord31);

        MapRecordBuilder builder1 = new DefaultMapRecordBuilder();
        builder1.withMaskLength(cidr1);
        builder1.withEidPrefixAfi(eid1);
        builder1.withLocators(locatorRecords1);
        builder1.withRecordTtl(60);
        LispMapRecord mapRecord1 = builder1.build();

        MapRecordBuilder builder2 = new DefaultMapRecordBuilder();
        builder2.withMaskLength(cidr2);
        builder2.withEidPrefixAfi(eid2);
        builder2.withLocators(locatorRecords2);
        builder2.withRecordTtl(60);
        LispMapRecord mapRecord2 = builder2.build();

        MapRecordBuilder builder3 = new DefaultMapRecordBuilder();
        builder3.withMaskLength(cidr3);
        builder3.withEidPrefixAfi(eid3);
        builder3.withLocators(locatorRecords3);
        builder3.withRecordTtl(60);
        LispMapRecord mapRecord3 = builder3.build();

        expireMapDb.putMapRecord(eidRecord1, mapRecord1, true);
        expireMapDb.putMapRecord(eidRecord2, mapRecord2, true);
        expireMapDb.putMapRecord(eidRecord3, mapRecord3, true);

        radixTreeDb.putMapRecord(eidRecord1, mapRecord1, true);
        radixTreeDb.putMapRecord(eidRecord2, mapRecord2, true);
        radixTreeDb.putMapRecord(eidRecord3, mapRecord3, true);
    }

    @Test
    public void test32MaskRange() {
        byte cidr32 = (byte) 32;
        LispIpv4Address eid = new LispIpv4Address(IpAddress.valueOf(EID_IP_1));
        LispEidRecord record = new LispEidRecord(cidr32, eid);
        LispMapRecord mapRecord1 = expireMapDb.getMapRecordByEidRecord(record, true);
        LispMapRecord mapRecord2 = radixTreeDb.getMapRecordByEidRecord(record, true);

        assertThat("Failed to fetch the RLOCs with /32 EID record",
                mapRecord1.getLocatorCount(), is(3));

        assertThat("Failed to fetch the RLOCs with /32 EID record",
                mapRecord2.getLocatorCount(), is(3));
    }

    @Test
    public void test24MaskRange() {
        byte cidr32 = (byte) 32;
        LispIpv4Address eid = new LispIpv4Address(IpAddress.valueOf(EID_IP_PREFIX_2_32));
        LispEidRecord record32 = new LispEidRecord(cidr32, eid);
        LispMapRecord mapRecordExpireMap32 = expireMapDb.getMapRecordByEidRecord(record32, true);
        LispMapRecord mapRecordRadixTree32 = radixTreeDb.getMapRecordByEidRecord(record32, true);

        byte cidr24 = (byte) 24;
        LispIpv4Address eid24 = new LispIpv4Address(IpAddress.valueOf(EID_IP_PREFIX_2_24));
        LispEidRecord record24 = new LispEidRecord(cidr24, eid24);
        LispMapRecord mapRecordExpireMap24 = expireMapDb.getMapRecordByEidRecord(record24, true);
        LispMapRecord mapRecordRadixTree24 = radixTreeDb.getMapRecordByEidRecord(record32, true);

        assertThat("Failed to fetch the RLOCs with /32 EID record",
                mapRecordExpireMap32.getLocatorCount(), is(2));
        assertThat("Failed to fetch the RLOCs with /24 EID record",
                mapRecordExpireMap24.getLocatorCount(), is(2));

        assertThat("Failed to fetch the RLOCs with /32 EID record",
                mapRecordRadixTree32.getLocatorCount(), is(2));
        assertThat("Failed to fetch the RLOCs with /24 EID record",
                mapRecordRadixTree24.getLocatorCount(), is(2));
    }

    @Test
    public void test16MaskRange() {
        byte cidr32 = (byte) 32;
        LispIpv4Address eid = new LispIpv4Address(IpAddress.valueOf(EID_IP_PREFIX_1_32));
        LispEidRecord record32 = new LispEidRecord(cidr32, eid);
        LispMapRecord mapRecordExpireMap32 = expireMapDb.getMapRecordByEidRecord(record32, true);
        LispMapRecord mapRecordRadixTree32 = radixTreeDb.getMapRecordByEidRecord(record32, true);

        byte cidr24 = (byte) 24;
        LispIpv4Address eid24 = new LispIpv4Address(IpAddress.valueOf(EID_IP_PREFIX_1_24));
        LispEidRecord record24 = new LispEidRecord(cidr24, eid24);
        LispMapRecord mapRecordExpireMap24 = expireMapDb.getMapRecordByEidRecord(record24, true);
        LispMapRecord mapRecordRadixTree24 = radixTreeDb.getMapRecordByEidRecord(record32, true);

        byte cidr16 = (byte) 16;
        LispIpv4Address eid16 = new LispIpv4Address(IpAddress.valueOf(EID_IP_PREFIX_1_16));
        LispEidRecord record16 = new LispEidRecord(cidr16, eid16);
        LispMapRecord mapRecordExpireMap16 = expireMapDb.getMapRecordByEidRecord(record16, true);
        LispMapRecord mapRecordRadixTree16 = radixTreeDb.getMapRecordByEidRecord(record16, true);

        assertThat("Failed to fetch the RLOCs with /32 EID record",
                mapRecordExpireMap32.getLocatorCount(), is(1));
        assertThat("Failed to fetch the RLOCs with /24 EID record",
                mapRecordExpireMap24.getLocatorCount(), is(1));
        assertThat("Failed to fetch the RLOCs with /16 EID record",
                mapRecordExpireMap16.getLocatorCount(), is(1));

        assertThat("Failed to fetch the RLOCs with /32 EID record",
                mapRecordRadixTree32.getLocatorCount(), is(1));
        assertThat("Failed to fetch the RLOCs with /24 EID record",
                mapRecordRadixTree24.getLocatorCount(), is(1));
        assertThat("Failed to fetch the RLOCs with /16 EID record",
                mapRecordRadixTree16.getLocatorCount(), is(1));
    }
}
