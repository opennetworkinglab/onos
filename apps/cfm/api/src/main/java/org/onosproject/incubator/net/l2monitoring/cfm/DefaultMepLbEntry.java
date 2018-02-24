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

/**
 * The default implementation of {@link MepLbEntry}.
 */
public final class DefaultMepLbEntry implements MepLbEntry {
    private final long nextLbmIdentifier;
    private final long countLbrTransmitted;
    private final long countLbrReceived;
    private final long countLbrValidInOrder;
    private final long countLbrValidOutOfOrder;
    private final long countLbrMacMisMatch;

    private DefaultMepLbEntry(DefaultMepLbEntryBuilder builder) {
        this.nextLbmIdentifier = builder.nextLbmIdentifier;
        this.countLbrTransmitted = builder.countLbrTransmitted;
        this.countLbrReceived = builder.countLbrReceived;
        this.countLbrValidInOrder = builder.countLbrValidInOrder;
        this.countLbrValidOutOfOrder = builder.countLbrValidOutOfOrder;
        this.countLbrMacMisMatch = builder.countLbrMacMisMatch;
    }

    @Override
    public long nextLbmIdentifier() {
        return nextLbmIdentifier;
    }

    @Override
    public long countLbrTransmitted() {
        return countLbrTransmitted;
    }

    @Override
    public long countLbrReceived() {
        return countLbrReceived;
    }

    @Override
    public long countLbrValidInOrder() {
        return countLbrValidInOrder;
    }

    @Override
    public long countLbrValidOutOfOrder() {
        return countLbrValidOutOfOrder;
    }

    @Override
    public long countLbrMacMisMatch() {
        return countLbrMacMisMatch;
    }

    public static final MepLbEntryBuilder builder() {
        return new DefaultMepLbEntryBuilder();
    }

    private static final class DefaultMepLbEntryBuilder implements MepLbEntryBuilder {
        private long nextLbmIdentifier;
        private long countLbrTransmitted;
        private long countLbrReceived;
        private long countLbrValidInOrder;
        private long countLbrValidOutOfOrder;
        private long countLbrMacMisMatch;

        private DefaultMepLbEntryBuilder() {
            //Hidden
        }

        @Override
        public MepLbEntryBuilder nextLbmIdentifier(long nextLbmIdentifier) {
            this.nextLbmIdentifier = nextLbmIdentifier;
            return this;
        }

        @Override
        public MepLbEntryBuilder countLbrTransmitted(long countLbrTransmitted) {
            this.countLbrTransmitted = countLbrTransmitted;
            return this;
        }

        @Override
        public MepLbEntryBuilder countLbrReceived(long countLbrReceived) {
            this.countLbrReceived = countLbrReceived;
            return this;
        }

        @Override
        public MepLbEntryBuilder countLbrValidInOrder(
                long countLbrValidInOrder) {
            this.countLbrValidInOrder = countLbrValidInOrder;
            return this;
        }

        @Override
        public MepLbEntryBuilder countLbrValidOutOfOrder(
                long countLbrValidOutOfOrder) {
            this.countLbrValidOutOfOrder = countLbrValidOutOfOrder;
            return this;
        }

        @Override
        public MepLbEntryBuilder countLbrMacMisMatch(long countLbrMacMisMatch) {
            this.countLbrMacMisMatch = countLbrMacMisMatch;
            return this;
        }

        @Override
        public MepLbEntry build() {
            return new DefaultMepLbEntry(this);
        }
    }
}
