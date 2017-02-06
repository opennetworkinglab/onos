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

import io.netty.buffer.ByteBuf;
import org.onlab.packet.DeserializationException;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of LispReferral.
 */
public final class DefaultLispReferral extends DefaultLispGenericLocator
                                                        implements LispReferral {

    static final DefaultLispReferral.ReferralWriter WRITER;
    static {
        WRITER = new ReferralWriter();
    }

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param priority          uni-cast priority
     * @param weight            uni-cast weight
     * @param multicastPriority multi-cast priority
     * @param multicastWeight   multi-cast weight
     * @param localLocator      local locator flag
     * @param rlocProbed        RLOC probed flag
     * @param routed            routed flag
     * @param locatorAfi        locator AFI
     */
    private DefaultLispReferral(byte priority, byte weight, byte multicastPriority,
                                byte multicastWeight, boolean localLocator,
                                boolean rlocProbed, boolean routed,
                                LispAfiAddress locatorAfi) {
        super(priority, weight, multicastPriority, multicastWeight,
                                localLocator, rlocProbed, routed, locatorAfi);
    }

    @Override
    public void writeTo(ByteBuf byteBuf) throws LispWriterException {
        WRITER.writeTo(byteBuf, this);
    }

    public static final class DefaultReferralBuilder
                                extends AbstractGenericLocatorBuilder<ReferralBuilder>
                                        implements ReferralBuilder {

        @Override
        public LispReferral build() {

            checkNotNull(locatorAfi, "Must specify a locator address");

            return new DefaultLispReferral(priority, weight, multicastPriority,
                    multicastWeight, localLocator, rlocProbed, routed, locatorAfi);
        }
    }

    /**
     * A LISP message reader for Referral portion.
     */
    public static final class ReferralReader implements LispMessageReader<LispReferral> {

        @Override
        public LispReferral readFrom(ByteBuf byteBuf) throws LispParseError,
                                                             LispReaderException,
                                                             DeserializationException {
            LispGenericLocator gl = deserialize(byteBuf);

            return new DefaultReferralBuilder()
                            .withPriority(gl.getPriority())
                            .withWeight(gl.getWeight())
                            .withMulticastPriority(gl.getMulticastPriority())
                            .withMulticastWeight(gl.getMulticastWeight())
                            .withLocalLocator(gl.isLocalLocator())
                            .withRouted(gl.isRouted())
                            .withLocatorAfi(gl.getLocatorAfi()).build();
        }
    }

    /**
     * A LISP message writer for Referral portion.
     */
    public static final class ReferralWriter implements LispMessageWriter<LispReferral> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispReferral message)
                                                throws LispWriterException {

            serialize(byteBuf, message);
        }
    }
}
