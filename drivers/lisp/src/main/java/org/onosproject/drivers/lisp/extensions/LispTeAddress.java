/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.lisp.extensions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.onlab.util.KryoNamespace;
import org.onosproject.mapping.addresses.ASMappingAddress;
import org.onosproject.mapping.addresses.DNMappingAddress;
import org.onosproject.mapping.addresses.EthMappingAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddressType;
import org.onosproject.mapping.addresses.IPMappingAddress;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.store.serializers.KryoNamespaces;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.mapping.addresses.ExtensionMappingAddressType
                    .ExtensionMappingAddressTypes.TRAFFIC_ENGINEERING_ADDRESS;

/**
 * Implementation of LISP traffic engineering address.
 * For a given EID lookup into the mapping database, this LCAF can be returned
 * to provide a list of locators in an explicit re-encapsulation path.
 */
public final class LispTeAddress extends AbstractExtension
                                            implements ExtensionMappingAddress {

    private static final String TE_RECORDS = "records";

    private List<TeRecord> records;

    private KryoNamespace appKryo = new KryoNamespace.Builder()
                                            .register(KryoNamespaces.API)
                                            .register(MappingAddress.class)
                                            .register(MappingAddress.Type.class)
                                            .register(IPMappingAddress.class)
                                            .register(ASMappingAddress.class)
                                            .register(DNMappingAddress.class)
                                            .register(EthMappingAddress.class)
                                            .register(TeRecord.class)
                                            .build();

    /**
     * Default constructor.
     */
    public LispTeAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param records a collection of TE records
     */
    private LispTeAddress(List<TeRecord> records) {
        this.records = records;
    }

    /**
     * Obtains a collection of TE records.
     *
     * @return a collection of TE records
     */
    public List<TeRecord> getTeRecords() {
        return ImmutableList.copyOf(records);
    }

    @Override
    public ExtensionMappingAddressType type() {
        return TRAFFIC_ENGINEERING_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> parameterMap = Maps.newHashMap();

        parameterMap.put(TE_RECORDS, records);
        return appKryo.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> parameterMap = appKryo.deserialize(data);

        this.records = (List<TeRecord>) parameterMap.get(TE_RECORDS);
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

        if (obj instanceof LispTeAddress) {
            final LispTeAddress other = (LispTeAddress) obj;
            return Objects.equals(records, other.records);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("TE records", records).toString();
    }

    /**
     * A builder for building LispTeAddress.
     */
    public static final class Builder {
        private List<TeRecord> records;

        /**
         * Sets a collection of TE records.
         *
         * @param records a collection of TE records
         * @return Builder object
         */
        public Builder withTeRecords(List<TeRecord> records) {
            this.records = records;
            return this;
        }

        /**
         * Builds LispTeAddress instance.
         *
         * @return LispTeAddress instance
         */
        public LispTeAddress build() {
            return new LispTeAddress(records);
        }
    }

    /**
     * Traffic engineering record class.
     */
    public static final class TeRecord {
        private boolean lookup;
        private boolean rlocProbe;
        private boolean strict;
        private MappingAddress address;

        /**
         *
         *
         * @param lookup     lookup bit
         * @param rlocProbe  rloc probe bit
         * @param strict     strict bit
         * @param address    RTR address
         */
        private TeRecord(boolean lookup, boolean rlocProbe, boolean strict,
                         MappingAddress address) {
            this.lookup = lookup;
            this.rlocProbe = rlocProbe;
            this.strict = strict;
            this.address = address;
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
        public MappingAddress getAddress() {
            return address;
        }

        @Override
        public int hashCode() {
            return Objects.hash(lookup, rlocProbe, strict, address);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof TeRecord) {
                final TeRecord other = (TeRecord) obj;
                return Objects.equals(this.lookup, other.lookup) &&
                        Objects.equals(this.rlocProbe, other.rlocProbe) &&
                        Objects.equals(this.strict, other.strict) &&
                        Objects.equals(this.address, other.address);
            }
            return false;
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                    .add("Lookup bit", lookup)
                    .add("RLOC probe bit", rlocProbe)
                    .add("strict bit", strict)
                    .add("RTR address", address)
                    .toString();
        }

        /**
         * A builder for building TeRecord.
         */
        public static final class Builder {
            private boolean lookup;
            private boolean rlocProbe;
            private boolean strict;
            private MappingAddress address;

            /**
             * Sets lookup flag.
             *
             * @param lookup lookup flag
             * @return Builder object
             */
            public Builder withIsLookup(boolean lookup) {
                this.lookup = lookup;
                return this;
            }

            /**
             * Sets RLOC probe flag.
             *
             * @param rlocProbe RLOC probe flag
             * @return Builder object
             */
            public Builder withIsRlocProbe(boolean rlocProbe) {
                this.rlocProbe = rlocProbe;
                return this;
            }

            /**
             * Sets strict flag.
             *
             * @param strict strict flag
             * @return Builder object
             */
            public Builder withIsStrict(boolean strict) {
                this.strict = strict;
                return this;
            }

            /**
             * Sets RTR RLOC address.
             *
             * @param address RTR RLOC address
             * @return Builder object
             */
            public Builder withRtrRlocAddress(MappingAddress address) {
                this.address = address;
                return this;
            }

            /**
             * Builds TeRecord instance.
             *
             * @return TeRcord instance
             */
            public TeRecord build() {

                return new TeRecord(lookup, rlocProbe, strict, address);
            }
        }
    }
}
