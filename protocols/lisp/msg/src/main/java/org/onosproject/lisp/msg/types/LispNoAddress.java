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
package org.onosproject.lisp.msg.types;

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

/**
 * No address.
 */
public class LispNoAddress extends LispAfiAddress {

    /**
     * Initializes no address.
     */
    public LispNoAddress() {
        super(AddressFamilyIdentifierEnum.NO_ADDRESS);
    }

    /**
     * LISP no address reader class.
     */
    public static class NoAddressReader implements LispAddressReader<LispNoAddress> {

        @Override
        public LispNoAddress readFrom(ByteBuf byteBuf) throws LispParseError {
            return new LispNoAddress();
        }
    }

    /**
     * LISP no address writer class.
     */
    public static class NoAddressWriter implements LispAddressWriter<LispNoAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispNoAddress address)
                                                    throws LispWriterException {
            // since there is nothing to write to channel, we just leave it empty
        }
    }
}
