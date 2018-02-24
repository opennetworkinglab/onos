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
package org.onosproject.incubator.net.l2monitoring.cfm;

import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

import java.util.BitSet;

/**
 * The default implementation of {@link MepLtCreate}.
 */
public final class DefaultMepLtCreate implements MepLtCreate {
    private final MacAddress remoteMepAddress;
    private final MepId remoteMepId;
    private BitSet transmitLtmFlags;
    private Short defaultTtl;

    private DefaultMepLtCreate(DefaultMepLtCreateBuilder builder) {
        this.remoteMepAddress = builder.remoteMepAddress;
        this.remoteMepId = builder.remoteMepId;
        this.defaultTtl = builder.defaultTtl;
        this.transmitLtmFlags = builder.transmitLtmFlags;
    }

    @Override
    public MacAddress remoteMepAddress() {
        return remoteMepAddress;
    }

    @Override
    public MepId remoteMepId() {
        return remoteMepId;
    }

    @Override
    public BitSet transmitLtmFlags() {
        return transmitLtmFlags;
    }

    @Override
    public Short defaultTtl() {
        return defaultTtl;
    }

    public static final MepLtCreate.MepLtCreateBuilder builder(MacAddress remoteMepAddress) {
        return new DefaultMepLtCreate.DefaultMepLtCreateBuilder(remoteMepAddress);
    }

    public static final MepLtCreate.MepLtCreateBuilder builder(MepId remoteMepId) {
        return new DefaultMepLtCreate.DefaultMepLtCreateBuilder(remoteMepId);
    }

    private static final class DefaultMepLtCreateBuilder implements MepLtCreate.MepLtCreateBuilder {
        private final MacAddress remoteMepAddress;
        private final MepId remoteMepId;
        private BitSet transmitLtmFlags;
        private Short defaultTtl;

        private DefaultMepLtCreateBuilder(MacAddress remoteMepAddress) {
            this.remoteMepId = null;
            this.remoteMepAddress = remoteMepAddress;
        }

        private DefaultMepLtCreateBuilder(MepId remoteMepId) {
            this.remoteMepId = remoteMepId;
            this.remoteMepAddress = null;
        }

        @Override
        public MepLtCreateBuilder transmitLtmFlags(BitSet transmitLtmFlags) {
            this.transmitLtmFlags = transmitLtmFlags;
            return this;
        }

        @Override
        public MepLtCreateBuilder defaultTtl(Short defaultTtl) {
            this.defaultTtl = defaultTtl;
            return this;
        }

        @Override
        public MepLtCreate build() {
            return new DefaultMepLtCreate(this);
        }
    }
}
