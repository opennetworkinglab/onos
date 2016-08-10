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
 * Unit tests for DefaultLispMapNotify class.
 */
public final class DefaultLispMapNotifyTest {

    private LispMapNotify notify1;
    private LispMapNotify sameAsNotify1;
    private LispMapNotify notify2;

    @Before
    public void setup() {

        LispMapNotify.NotifyBuilder builder1 =
                        new DefaultLispMapNotify.DefaultNotifyBuilder();

        notify1 = builder1
                        .withKeyId((short) 1)
                        .withNonce(1L)
                        .withRecordCount((byte) 0x01)
                        .build();

        LispMapNotify.NotifyBuilder builder2 =
                        new DefaultLispMapNotify.DefaultNotifyBuilder();

        sameAsNotify1 = builder2
                        .withKeyId((short) 1)
                        .withNonce(1L)
                        .withRecordCount((byte) 0x01)
                        .build();

        LispMapNotify.NotifyBuilder builder3 =
                        new DefaultLispMapNotify.DefaultNotifyBuilder();

        notify2 = builder3
                        .withKeyId((short) 2)
                        .withNonce(2L)
                        .withRecordCount((byte) 0x02)
                        .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(notify1, sameAsNotify1)
                .addEqualityGroup(notify2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultLispMapNotify notify = (DefaultLispMapNotify) notify1;

        assertThat(notify.getKeyId(), is((short) 1));
        assertThat(notify.getNonce(), is(1L));
        assertThat(notify.getRecordCount(), is((byte) 0x01));
    }
}
