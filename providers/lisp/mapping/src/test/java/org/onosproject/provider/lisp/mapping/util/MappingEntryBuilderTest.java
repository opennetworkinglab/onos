/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.provider.lisp.mapping.util;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.lisp.msg.protocols.DefaultLispLocator.DefaultLocatorBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.DefaultNotifyBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapReply.DefaultReplyBuilder;
import org.onosproject.lisp.msg.protocols.LispLocator;
import org.onosproject.lisp.msg.protocols.LispLocator.LocatorBuilder;
import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapNotify.NotifyBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord.MapRecordBuilder;
import org.onosproject.lisp.msg.protocols.LispMapReply;
import org.onosproject.lisp.msg.protocols.LispMapReply.ReplyBuilder;
import org.onosproject.lisp.msg.protocols.LispMapReplyAction;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.addresses.IPMappingAddress;
import org.onosproject.net.DeviceId;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Mapping entry builder unit test.
 */
public class MappingEntryBuilderTest {

    private static final String IP_RECORD_ADDRESS = "192.168.1.1";
    private static final String IP_LOCATOR_ADDRESS = "10.1.1.1";
    private static final int IP_RECORD_MASK_LENGTH = 24;
    private static final int IP_LOCATOR_MASK_LENGTH = 32;

    private static final String AUTH_KEY = "onos";

    private static final byte UNIQUE_VALUE = (byte) 0x01;
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("lisp:10.1.1.2");

    private LispMapReply mapReply;
    private LispMapNotify mapNotify;

    @Before
    public void setUp() {
        ReplyBuilder replyBuilder = new DefaultReplyBuilder();

        List<LispMapRecord> records = ImmutableList.of(getMapRecord());

        mapReply = replyBuilder
                        .withIsEtr(true)
                        .withIsProbe(false)
                        .withIsSecurity(true)
                        .withNonce(1L)
                        .withMapRecords(records)
                        .build();

        NotifyBuilder notifyBuilder = new DefaultNotifyBuilder();

        mapNotify = notifyBuilder
                        .withKeyId((short) 1)
                        .withAuthKey(AUTH_KEY)
                        .withNonce(1L)
                        .withMapRecords(records)
                        .build();
    }

    @Test
    public void testMapReplyConversion() {
        List<LispMapRecord> replyRecords = mapReply.getMapRecords();

        assertThat(replyRecords.size(), is(1));

        testMapRecorConversion(replyRecords.get(0));
    }

    @Test
    public void testMapNotifyConversion() {
        List<LispMapRecord> notifyRecords = mapNotify.getMapRecords();

        assertThat(notifyRecords.size(), is(1));

        testMapRecorConversion(notifyRecords.get(0));
    }

    private void testMapRecorConversion(LispMapRecord record) {
        MappingEntry mappingEntry =
                     new MappingEntryBuilder(DEVICE_ID, record).build();
        MappingKey key = mappingEntry.key();
        MappingValue value = mappingEntry.value();

        IPMappingAddress recordAddress = (IPMappingAddress) key.address();

        assertThat(recordAddress.ip(), is(IpPrefix.valueOf(IP_RECORD_ADDRESS + "/" +
                IP_RECORD_MASK_LENGTH)));

        assertThat(value.action().type(), is(MappingAction.Type.NATIVE_FORWARD));

        assertThat(value.treatments().size(), is(1));

        MappingTreatment treatment = value.treatments().get(0);
        IPMappingAddress locatorAddress = (IPMappingAddress) treatment.address();

        assertThat(locatorAddress.ip(), is(IpPrefix.valueOf(IP_LOCATOR_ADDRESS + "/" +
                IP_LOCATOR_MASK_LENGTH)));
    }

    private LispMapRecord getMapRecord() {
        MapRecordBuilder recordBuilder = new DefaultMapRecordBuilder();

        LispIpv4Address recordAddress =
                        new LispIpv4Address(IpAddress.valueOf(IP_RECORD_ADDRESS));

        LocatorBuilder locatorBuilder = new DefaultLocatorBuilder();

        LispIpv4Address locatorAddress =
                        new LispIpv4Address(IpAddress.valueOf(IP_LOCATOR_ADDRESS));

        LispLocator locator1 = locatorBuilder
                                    .withPriority(UNIQUE_VALUE)
                                    .withWeight(UNIQUE_VALUE)
                                    .withMulticastPriority(UNIQUE_VALUE)
                                    .withMulticastWeight(UNIQUE_VALUE)
                                    .withLocalLocator(true)
                                    .withRlocProbed(false)
                                    .withRouted(true)
                                    .withLocatorAfi(locatorAddress)
                                    .build();

        return recordBuilder
                .withRecordTtl(100)
                .withIsAuthoritative(true)
                .withMapVersionNumber((short) 1)
                .withMaskLength((byte) IP_RECORD_MASK_LENGTH)
                .withAction(LispMapReplyAction.NativelyForward)
                .withEidPrefixAfi(recordAddress)
                .withLocators(ImmutableList.of(locator1))
                .build();
    }
}
