/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onlab.packet.DeserializationException;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispSignature.DefaultSignatureBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispSignature.SignatureReader;
import org.onosproject.lisp.msg.protocols.DefaultLispSignature.SignatureWriter;
import org.onosproject.lisp.msg.protocols.LispSignature.SignatureBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispSignature class.
 */
public final class DefaultLispSignatureTest {

    private static final int SIG_UNIQUE_VALUE_1 = 100;
    private static final int SIG_UNIQUE_VALUE_2 = 200;

    private LispSignature signature1;
    private LispSignature sameAsSignature1;
    private LispSignature signature2;

    @Before
    public void setup() {

        SignatureBuilder builder1 = new DefaultSignatureBuilder();

        signature1 = builder1
                .withRecordTtl(SIG_UNIQUE_VALUE_1)
                .withSigExpiration(SIG_UNIQUE_VALUE_1)
                .withSigInception(SIG_UNIQUE_VALUE_1)
                .withKeyTag((short) SIG_UNIQUE_VALUE_1)
                .withSigLength((short) SIG_UNIQUE_VALUE_1)
                .withSigAlgorithm((byte) 1)
                .withSignature(SIG_UNIQUE_VALUE_1)
                .build();

        SignatureBuilder builder2 = new DefaultSignatureBuilder();

        sameAsSignature1 = builder2
                .withRecordTtl(SIG_UNIQUE_VALUE_1)
                .withSigExpiration(SIG_UNIQUE_VALUE_1)
                .withSigInception(SIG_UNIQUE_VALUE_1)
                .withKeyTag((short) SIG_UNIQUE_VALUE_1)
                .withSigLength((short) SIG_UNIQUE_VALUE_1)
                .withSigAlgorithm((byte) 1)
                .withSignature(SIG_UNIQUE_VALUE_1)
                .build();

        SignatureBuilder builder3 = new DefaultSignatureBuilder();

        signature2 = builder3
                .withRecordTtl(SIG_UNIQUE_VALUE_2)
                .withSigExpiration(SIG_UNIQUE_VALUE_2)
                .withSigInception(SIG_UNIQUE_VALUE_2)
                .withKeyTag((short) SIG_UNIQUE_VALUE_2)
                .withSigLength((short) SIG_UNIQUE_VALUE_2)
                .withSigAlgorithm((byte) 2)
                .withSignature(SIG_UNIQUE_VALUE_2)
                .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(signature1, sameAsSignature1)
                .addEqualityGroup(signature2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispSignature signature = signature1;

        assertThat(signature.getRecordTtl(), is(SIG_UNIQUE_VALUE_1));
        assertThat(signature.getSigExpiration(), is(SIG_UNIQUE_VALUE_1));
        assertThat(signature.getSigInception(), is(SIG_UNIQUE_VALUE_1));
        assertThat(signature.getKeyTag(), is((short) SIG_UNIQUE_VALUE_1));
        assertThat(signature.getSigLength(), is((short) SIG_UNIQUE_VALUE_1));
        assertThat(signature.getSigAlgorithm(), is((byte) 1));
        assertThat(signature.getSignature(), is(SIG_UNIQUE_VALUE_1));
    }

    @Test
    public void testSerialization() throws LispReaderException, DeserializationException,
                                           LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();

        SignatureWriter writer = new SignatureWriter();
        writer.writeTo(byteBuf, signature1);

        SignatureReader reader = new SignatureReader();
        LispSignature deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(signature1, deserialized).testEquals();
    }
}