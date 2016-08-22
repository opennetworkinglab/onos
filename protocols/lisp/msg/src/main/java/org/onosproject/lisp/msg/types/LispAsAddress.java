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
package org.onosproject.lisp.msg.types;

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.Objects;

/**
 * The identifier of Autonomous System (AS).
 */
public class LispAsAddress extends LispAfiAddress {

    private final int asNum;

    /**
     * Initializes AS identifier number.
     *
     * @param num AS number
     */
    public LispAsAddress(int num) {
        super(AddressFamilyIdentifierEnum.AS);
        this.asNum = num;
    }

    /**
     * Obtains AS identifier number.
     *
     * @return AS number
     */
    public int getASNum() {
        return asNum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(asNum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        LispAsAddress other = (LispAsAddress) obj;
        if (asNum != other.asNum) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(asNum);
    }

    /**
     * Autonomous system address reader class.
     */
    public static class AsAddressReader implements LispAddressReader<LispAsAddress> {

        @Override
        public LispAsAddress readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {
            throw new LispReaderException("Unimplemented method");
        }
    }

    /**
     * Autonomous system address writer class.
     */
    public static class AsAddressWriter implements LispAddressWriter<LispAsAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispAsAddress address) throws LispWriterException {
            throw new LispWriterException("Unimplemented method");
        }
    }
}
