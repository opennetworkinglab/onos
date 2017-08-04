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

import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoRequest.DefaultInfoRequestBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoRequest.InfoRequestReader;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoRequest.InfoRequestWriter;
import org.onosproject.lisp.msg.protocols.LispInfoRequest.InfoRequestBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispInfoRequest class.
 */
public final class DefaultLispInfoRequestTest {

    private LispInfoRequest request1;
    private LispInfoRequest sameAsRequest1;
    private LispInfoRequest request2;

    private static final String AUTH_KEY = "onos";

    @Before
    public void setup() {

        InfoRequestBuilder builder1 = new DefaultInfoRequestBuilder();

        LispIpv4Address address1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        request1 = builder1
                        .withNonce(1L)
                        .withKeyId((short) 1)
                        .withAuthKey(AUTH_KEY)
                        .withIsInfoReply(false)
                        .withMaskLength((byte) 1)
                        .withEidPrefix(address1).build();

        InfoRequestBuilder builder2 = new DefaultInfoRequestBuilder();

        sameAsRequest1 = builder2
                            .withNonce(1L)
                            .withKeyId((short) 1)
                            .withAuthKey(AUTH_KEY)
                            .withIsInfoReply(false)
                            .withMaskLength((byte) 1)
                            .withEidPrefix(address1).build();

        InfoRequestBuilder builder3 = new DefaultInfoRequestBuilder();

        LispIpv4Address address2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));

        request2 = builder3
                        .withNonce(2L)
                        .withKeyId((short) 2)
                        .withAuthKey(AUTH_KEY)
                        .withIsInfoReply(true)
                        .withMaskLength((byte) 1)
                        .withEidPrefix(address2).build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(request1, sameAsRequest1)
                .addEqualityGroup(request2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultLispInfoRequest request = (DefaultLispInfoRequest) request1;

        LispIpv4Address address = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        assertThat(request.isInfoReply(), is(false));
        assertThat(request.getNonce(), is(1L));
        assertThat(request.getKeyId(), is((short) 1));
        assertThat(request.getMaskLength(), is((byte) 1));
        assertThat(request.getPrefix(), is(address));
    }


    @Test
    public void testSerialization() throws LispReaderException, LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();

        InfoRequestWriter writer = new InfoRequestWriter();
        writer.writeTo(byteBuf, request1);

        InfoRequestReader reader = new InfoRequestReader();
        LispInfoRequest deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(request1, deserialized).testEquals();
    }
}
