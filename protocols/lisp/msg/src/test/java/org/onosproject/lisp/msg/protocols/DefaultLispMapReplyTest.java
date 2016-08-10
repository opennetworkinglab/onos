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
 * Unit tests for DefaultLispMapReply class.
 */
public final class DefaultLispMapReplyTest {

    private LispMapReply reply1;
    private LispMapReply sameAsReply1;
    private LispMapReply reply2;

    @Before
    public void setup() {

        LispMapReply.ReplyBuilder builder1 =
                        new DefaultLispMapReply.DefaultReplyBuilder();

        reply1 = builder1
                        .withIsEtr(true)
                        .withIsProbe(false)
                        .withIsSecurity(true)
                        .withNonce(1L)
                        .withRecordCount((byte) 0x01)
                        .build();

        LispMapReply.ReplyBuilder builder2 =
                        new DefaultLispMapReply.DefaultReplyBuilder();

        sameAsReply1 = builder2
                        .withIsEtr(true)
                        .withIsProbe(false)
                        .withIsSecurity(true)
                        .withNonce(1L)
                        .withRecordCount((byte) 0x01)
                        .build();

        LispMapReply.ReplyBuilder builder3 =
                        new DefaultLispMapReply.DefaultReplyBuilder();
        reply2 = builder3
                        .withIsEtr(false)
                        .withIsProbe(true)
                        .withIsSecurity(false)
                        .withNonce(2L)
                        .withRecordCount((byte) 0x02)
                        .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(reply1, sameAsReply1)
                .addEqualityGroup(reply2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultLispMapReply reply = (DefaultLispMapReply) reply1;

        assertThat(reply.isEtr(), is(true));
        assertThat(reply.isProbe(), is(false));
        assertThat(reply.isSecurity(), is(true));
        assertThat(reply.getNonce(), is(1L));
        assertThat(reply.getRecordCount(), is((byte) 0x01));
    }
}
