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
 * Unit tests for DefaultLispMapRecord class.
 */
public final class DefaultLispMapRecordTest {

    private LispMapRecord record1;
    private LispMapRecord sameAsRecord1;
    private LispMapRecord record2;

    @Before
    public void setup() {

        LispMapRecord.MapRecordBuilder builder1 =
                        new DefaultLispMapRecord.DefaultMapRecordBuilder();

        record1 = builder1
                        .withRecordTtl(100)
                        .withAuthoritative(true)
                        .withLocatorCount(100)
                        .withMapVersionNumber((short) 1)
                        .withMaskLength((byte) 0x01)
                        .build();

        LispMapRecord.MapRecordBuilder builder2 =
                        new DefaultLispMapRecord.DefaultMapRecordBuilder();

        sameAsRecord1 = builder2
                        .withRecordTtl(100)
                        .withAuthoritative(true)
                        .withLocatorCount(100)
                        .withMapVersionNumber((short) 1)
                        .withMaskLength((byte) 0x01)
                        .build();

        LispMapRecord.MapRecordBuilder builder3 =
                        new DefaultLispMapRecord.DefaultMapRecordBuilder();

        record2 = builder3
                        .withRecordTtl(200)
                        .withAuthoritative(false)
                        .withLocatorCount(200)
                        .withMapVersionNumber((short) 2)
                        .withMaskLength((byte) 0x02)
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
        DefaultLispMapRecord record = (DefaultLispMapRecord) record1;

        assertThat(record.getRecordTtl(), is(100));
        assertThat(record.isAuthoritative(), is(true));
        assertThat(record.getLocatorCount(), is(100));
        assertThat(record.getMapVersionNumber(), is((short) 1));
        assertThat(record.getMaskLength(), is((byte) 0x01));
    }
}
