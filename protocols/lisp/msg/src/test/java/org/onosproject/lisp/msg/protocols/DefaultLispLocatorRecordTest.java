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
package org.onosproject.lisp.msg.protocols;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispLocatorRecord class.
 */
public final class DefaultLispLocatorRecordTest {

    private LispLocatorRecord record1;
    private LispLocatorRecord sameAsRecord1;
    private LispLocatorRecord record2;

    @Before
    public void setup() {

        LispLocatorRecord.LocatorRecordBuilder builder1 =
                    new DefaultLispLocatorRecord.DefaultLocatorRecordBuilder();

        record1 = builder1
                        .withPriority((byte) 0x01)
                        .withWeight((byte) 0x01)
                        .withMulticastPriority((byte) 0x01)
                        .withMulticastWeight((byte) 0x01)
                        .withLocalLocator(true)
                        .withRlocProbed(false)
                        .withRouted(true)
                        .build();

        LispLocatorRecord.LocatorRecordBuilder builder2 =
                    new DefaultLispLocatorRecord.DefaultLocatorRecordBuilder();

        sameAsRecord1 = builder2
                        .withPriority((byte) 0x01)
                        .withWeight((byte) 0x01)
                        .withMulticastPriority((byte) 0x01)
                        .withMulticastWeight((byte) 0x01)
                        .withLocalLocator(true)
                        .withRlocProbed(false)
                        .withRouted(true)
                        .build();

        LispLocatorRecord.LocatorRecordBuilder builder3 =
                    new DefaultLispLocatorRecord.DefaultLocatorRecordBuilder();

        record2 = builder3
                        .withPriority((byte) 0x02)
                        .withWeight((byte) 0x02)
                        .withMulticastPriority((byte) 0x02)
                        .withMulticastWeight((byte) 0x02)
                        .withLocalLocator(false)
                        .withRlocProbed(true)
                        .withRouted(false)
                        .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(record1, sameAsRecord1)
                .addEqualityGroup(record2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultLispLocatorRecord record = (DefaultLispLocatorRecord) record1;

        assertThat(record.getPriority(), is((byte) 0x01));
        assertThat(record.getWeight(), is((byte) 0x01));
        assertThat(record.getMulticastPriority(), is((byte) 0x01));
        assertThat(record.getMulticastWeight(), is((byte) 0x01));
        assertThat(record.isLocalLocator(), is(true));
        assertThat(record.isRlocProbed(), is(false));
        assertThat(record.isRouted(), is(true));
    }
}
