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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispTeRecord.TeRecordWriter;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Traffic Engineering (TE) type LCAF address class.
 * <p>
 * Traffic Engineering type is defined in draft-ietf-lisp-lcaf-20
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-20#page-16
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 10   |     Rsvd2     |            Length             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           Rsvd3         |L|P|S|           AFI = x             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Reencap Hop 1  ...                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           Rsvd3         |L|P|S|           AFI = x             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Reencap Hop k  ...                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispTeLcafAddress extends LispLcafAddress {

    private final List<LispTeRecord> records;

    /**
     * Initializes Traffic Engineering type LCAF address.
     *
     * @param records a collection of Re-encapsulated RLOC addresses
     */
    private LispTeLcafAddress(short length, List<LispTeRecord> records) {
        super(LispCanonicalAddressFormatEnum.TRAFFIC_ENGINEERING, length);
        this.records = records;
    }

    /**
     * Obtains a collection of TE records.
     *
     * @return a collection of TE records
     */
    public List<LispTeRecord> getTeRecords() {
        return ImmutableList.copyOf(records);
    }

    @Override
    public int hashCode() {
        return Objects.hash(records);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispTeLcafAddress) {
            final LispTeLcafAddress other = (LispTeLcafAddress) obj;
            return Objects.equals(records, other.records);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("TE records", records).toString();
    }

    public static final class TeAddressBuilder extends LcafAddressBuilder<TeAddressBuilder> {
        private List<LispTeRecord> records;
        private short length;
        private static final int SIZE_OF_AFI_RECORD = 8;

        /**
         * Sets a collection of TE records.
         *
         * @param records a collection of TE records
         * @return TeAddressBuilder object
         */
        public TeAddressBuilder withTeRecords(List<LispTeRecord> records) {
            this.records = records;
            this.length = (short) (records.size() * SIZE_OF_AFI_RECORD);
            return this;
        }

        /**
         * Builds LispTeLcafAddress instance.
         *
         * @return LispTeLcafAddress instance
         */
        public LispTeLcafAddress build() {
            return new LispTeLcafAddress(length, records);
        }

        /**
         * TE LCAF address reader class.
         */
        public static class TeLcafAddressReader implements LispAddressReader<LispTeLcafAddress> {

            private static final int SIZE_OF_AFI_RECORD = 8;

            @Override
            public LispTeLcafAddress readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

                LispLcafAddress lcafAddress = LispLcafAddress.deserializeCommon(byteBuf);

                // TODO: for RTR RLOC is IPv4 only for now
                int numOfRecords = lcafAddress.getLength() / SIZE_OF_AFI_RECORD;

                List<LispTeRecord> teRecords = Lists.newArrayList();
                for (int i = 0; i < numOfRecords; i++) {
                    teRecords.add(new LispTeRecord.TeRecordReader().readFrom(byteBuf));
                }

                return new TeAddressBuilder()
                            .withTeRecords(teRecords)
                            .build();
            }
        }

        /**
         * TE LCAF address writer class.
         */
        public static class TeLcafAddressWriter implements LispAddressWriter<LispTeLcafAddress> {

            @Override
            public void writeTo(ByteBuf byteBuf, LispTeLcafAddress address) throws LispWriterException {
                LispLcafAddress.serializeCommon(byteBuf, address);

                TeRecordWriter writer = new TeRecordWriter();

                List<LispTeRecord> teRecords = address.getTeRecords();
                for (int i = 0; i < teRecords.size(); i++) {
                    writer.writeTo(byteBuf, teRecords.get(i));
                }
            }
        }
    }
}
