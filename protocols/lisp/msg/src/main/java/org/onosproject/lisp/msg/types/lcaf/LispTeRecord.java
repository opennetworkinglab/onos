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
package org.onosproject.lisp.msg.types.lcaf;

import io.netty.buffer.ByteBuf;
import org.onlab.util.ByteOperator;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAddressReader;
import org.onosproject.lisp.msg.types.LispAddressWriter;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressReader;
import org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressWriter;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Traffic Engineering record class.
 */
public class LispTeRecord {

    private final boolean lookup;
    private final boolean rlocProbe;
    private final boolean strict;
    private final LispAfiAddress rtrRlocAddress;

    /**
     * Initializes TE record.
     *
     * @param lookup     lookup bit
     * @param rlocProbe  rloc probe bit
     * @param strict     strict bit
     * @param rtrAddress RTR address
     */
    public LispTeRecord(boolean lookup, boolean rlocProbe,
                        boolean strict, LispAfiAddress rtrAddress) {
        this.lookup = lookup;
        this.rlocProbe = rlocProbe;
        this.strict = strict;
        this.rtrRlocAddress = rtrAddress;
    }

    /**
     * Obtains lookup bit flag.
     *
     * @return lookup bit flag
     */
    public boolean isLookup() {
        return lookup;
    }

    /**
     * Obtains RLOC probe bit flag.
     *
     * @return RLOC probe bit flag
     */
    public boolean isRlocProbe() {
        return rlocProbe;
    }

    /**
     * Obtains strict bit flag.
     *
     * @return strict bit flag
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * Obtains Re-encapsulated RLOC address.
     *
     * @return Re-encapsulated RLOC address
     */
    public LispAfiAddress getRtrRlocAddress() {
        return rtrRlocAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lookup, rlocProbe, strict, rtrRlocAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispTeRecord) {
            final LispTeRecord other = (LispTeRecord) obj;
            return Objects.equals(this.lookup, other.lookup) &&
                    Objects.equals(this.rlocProbe, other.rlocProbe) &&
                    Objects.equals(this.strict, other.strict) &&
                    Objects.equals(this.rtrRlocAddress, other.rtrRlocAddress);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("Lookup bit", lookup)
                .add("RLOC probe bit", rlocProbe)
                .add("strict bit", strict)
                .add("RTR address", rtrRlocAddress)
                .toString();
    }

    public static final class TeRecordBuilder {
        private boolean lookup;
        private boolean rlocProbe;
        private boolean strict;
        private LispAfiAddress rtrRlocAddress;

        /**
         * Sets lookup flag.
         *
         * @param lookup lookup flag
         * @return TeRecordBuilder object
         */
        public TeRecordBuilder withIsLookup(boolean lookup) {
            this.lookup = lookup;
            return this;
        }

        /**
         * Sets RLOC probe flag.
         *
         * @param rlocProbe RLOC probe flag
         * @return TeRecordBuilder object
         */
        public TeRecordBuilder withIsRlocProbe(boolean rlocProbe) {
            this.rlocProbe = rlocProbe;
            return this;
        }

        /**
         * Sets strict flag.
         *
         * @param strict strict flag
         * @return TeRecordBuilder object
         */
        public TeRecordBuilder withIsStrict(boolean strict) {
            this.strict = strict;
            return this;
        }

        /**
         * Sets RTR RLOC address.
         *
         * @param rtrRlocAddress RTR RLOC address
         * @return TeRecordBuilder object
         */
        public TeRecordBuilder withRtrRlocAddress(LispAfiAddress rtrRlocAddress) {
            this.rtrRlocAddress = rtrRlocAddress;
            return this;
        }

        /**
         * Builds TeRecord instance.
         *
         * @return TeRcord instance
         */
        public LispTeRecord build() {

            return new LispTeRecord(lookup, rlocProbe, strict, rtrRlocAddress);
        }
    }

    /**
     * Traffic Engineering record reader class.
     */
    public static class TeRecordReader implements LispAddressReader<LispTeRecord> {

        private static final int RESERVED_SKIP_LENGTH = 1;

        private static final int STRICT_INDEX = 1;
        private static final int RLOC_PROBE_INDEX = 2;
        private static final int LOOKUP_INDEX = 3;

        @Override
        public LispTeRecord readFrom(ByteBuf byteBuf)
                                    throws LispParseError, LispReaderException {

            // let's skip reserved 3
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            byte flags = byteBuf.readByte();

            // lookup -> 1 bit
            boolean lookup = ByteOperator.getBit(flags, LOOKUP_INDEX);

            // rlocProbe -> 1 bit
            boolean rlocProbe = ByteOperator.getBit(flags, RLOC_PROBE_INDEX);

            // strict -> 1 bit
            boolean strict = ByteOperator.getBit(flags, STRICT_INDEX);

            AfiAddressReader reader = new AfiAddressReader();

            LispAfiAddress rtrAddress = reader.readFrom(byteBuf);

            return new LispTeRecord(lookup, rlocProbe, strict, rtrAddress);
        }
    }

    /**
     * Traffic Engineering record writer class.
     */
    public static class TeRecordWriter implements LispAddressWriter<LispTeRecord> {

        private static final int LOOKUP_FLAG_SHIFT_BIT = 3;
        private static final int RLOC_PROBE_FLAG_SHIFT_BIT = 2;
        private static final int STRICT_FLAG_SHIFT_BIT = 1;

        private static final int ENABLE_BIT = 1;
        private static final int DISABLE_BIT = 0;

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispTeRecord record)
                                                    throws LispWriterException {

            byteBuf.writeByte(UNUSED_ZERO);

            // lookup flag
            byte lookup = DISABLE_BIT;
            if (record.isLookup()) {
                lookup = (byte) (ENABLE_BIT << LOOKUP_FLAG_SHIFT_BIT);
            }

            // RLOC probe flag
            byte rlocProbe = DISABLE_BIT;
            if (record.isRlocProbe()) {
                rlocProbe = (byte) (ENABLE_BIT << RLOC_PROBE_FLAG_SHIFT_BIT);
            }

            // strict flag
            byte strict = DISABLE_BIT;
            if (record.isStrict()) {
                strict = (byte) (ENABLE_BIT << STRICT_FLAG_SHIFT_BIT);
            }

            byteBuf.writeByte((byte) (lookup + rlocProbe + strict));

            // RTR RLOC address
            AfiAddressWriter writer = new AfiAddressWriter();
            writer.writeTo(byteBuf, record.rtrRlocAddress);
        }
    }
}
