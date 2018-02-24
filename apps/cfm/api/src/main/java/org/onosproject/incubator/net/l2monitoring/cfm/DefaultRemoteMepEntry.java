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

import java.time.Duration;

import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.SenderIdTlv.SenderIdTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

/**
 * The default implementation of {@link RemoteMepEntry}.
 */
public class DefaultRemoteMepEntry implements RemoteMepEntry {

    private final MepId remoteMepId;
    private final RemoteMepState state;
    private final Duration failedOrOkTime;
    private final MacAddress macAddress;
    private final boolean rdi;
    private final PortStatusTlvType portStatusTlvType;
    private final InterfaceStatusTlvType interfaceStatusTlvType;
    private final SenderIdTlvType senderIdTlvType;

    protected DefaultRemoteMepEntry(DefaultRemoteMepEntryBuilder rmepBuilder) {
        this.remoteMepId = rmepBuilder.remoteMepId;
        this.state = rmepBuilder.state;
        this.failedOrOkTime = rmepBuilder.failedOrOkTime;
        this.macAddress = rmepBuilder.macAddress;
        this.rdi = rmepBuilder.rdi;
        this.portStatusTlvType = rmepBuilder.portStatusTlvType;
        this.interfaceStatusTlvType = rmepBuilder.interfaceStatusTlvType;
        this.senderIdTlvType = rmepBuilder.senderIdTlvType;
    }

    @Override
    public MepId remoteMepId() {
        return this.remoteMepId;
    }

    @Override
    public RemoteMepState state() {
        return this.state;
    }

    @Override
    public Duration failedOrOkTime() {
        return failedOrOkTime;
    }

    @Override
    public MacAddress macAddress() {
        return macAddress;
    }

    @Override
    public boolean rdi() {
        return rdi;
    }

    @Override
    public PortStatusTlvType portStatusTlvType() {
        return portStatusTlvType;
    }

    @Override
    public InterfaceStatusTlvType interfaceStatusTlvType() {
        return interfaceStatusTlvType;
    }

    @Override
    public SenderIdTlvType senderIdTlvType() {
        return senderIdTlvType;
    }

    public static RemoteMepEntryBuilder builder(
            MepId remoteMepId, RemoteMepState state) throws CfmConfigException {
        return new DefaultRemoteMepEntryBuilder(remoteMepId, state);
    }

    private static class DefaultRemoteMepEntryBuilder implements RemoteMepEntryBuilder {
        private final MepId remoteMepId;
        private final RemoteMepState state;
        private Duration failedOrOkTime;
        private MacAddress macAddress;
        private boolean rdi;
        private PortStatusTlvType portStatusTlvType;
        private InterfaceStatusTlvType interfaceStatusTlvType;
        private SenderIdTlvType senderIdTlvType;

        protected DefaultRemoteMepEntryBuilder(MepId remoteMepId,
                                            RemoteMepState state) throws CfmConfigException {
            if (remoteMepId == null) {
                throw new CfmConfigException("remoteMepId is null");
            } else if (state == null) {
                throw new CfmConfigException("state is null");
            }
            this.remoteMepId = remoteMepId;
            this.state = state;
        }

        @Override
        public RemoteMepEntryBuilder failedOrOkTime(Duration failedOrOkTime) {
            this.failedOrOkTime = failedOrOkTime;
            return this;
        }

        @Override
        public RemoteMepEntryBuilder macAddress(MacAddress macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        @Override
        public RemoteMepEntryBuilder rdi(boolean rdi) {
            this.rdi = rdi;
            return this;
        }

        @Override
        public RemoteMepEntryBuilder portStatusTlvType(PortStatusTlvType portStatusTlvType) {
            this.portStatusTlvType = portStatusTlvType;
            return this;
        }

        @Override
        public RemoteMepEntryBuilder interfaceStatusTlvType(
                InterfaceStatusTlvType interfaceStatusTlvType) {
            this.interfaceStatusTlvType = interfaceStatusTlvType;
            return this;
        }

        @Override
        public RemoteMepEntryBuilder senderIdTlvType(
                SenderIdTlvType senderIdTlvType) {
            this.senderIdTlvType = senderIdTlvType;
            return this;
        }

        @Override
        public RemoteMepEntry build() {
            return new DefaultRemoteMepEntry(this);
        }
    }
}
