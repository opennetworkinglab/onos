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

import java.util.Base64;

import org.onlab.packet.MacAddress;
import org.onlab.util.HexString;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

/**
 * The default implementation of {@link MepLbCreate}.
 */
public final class DefaultMepLbCreate implements MepLbCreate {

    private final MacAddress remoteMepAddress;
    private final MepId remoteMepId;
    private final Integer numberMessages;
    private final String dataTlvHex;
    private final Mep.Priority vlanPriority;
    private final Boolean vlanDropEligible;

    private DefaultMepLbCreate(DefaultMepLbCreateBuilder builder) {
        this.remoteMepAddress = builder.remoteMepAddress;
        this.remoteMepId = builder.remoteMepId;
        this.numberMessages = builder.numberMessages;
        this.dataTlvHex = builder.dataTlvHex;
        this.vlanPriority = builder.vlanPriority;
        this.vlanDropEligible = builder.vlanDropEligible;
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
    public Integer numberMessages() {
        return numberMessages;
    }

    @Override
    public String dataTlvHex() {
        return dataTlvHex;
    }

    @Override
    public Mep.Priority vlanPriority() {
        return vlanPriority;
    }

    @Override
    public Boolean vlanDropEligible() {
        return vlanDropEligible;
    }

    public static final MepLbCreateBuilder builder(MacAddress remoteMepAddress) {
        return new DefaultMepLbCreateBuilder(remoteMepAddress);
    }

    public static final MepLbCreateBuilder builder(MepId remoteMepId) {
        return new DefaultMepLbCreateBuilder(remoteMepId);
    }

    private static final class DefaultMepLbCreateBuilder implements MepLbCreateBuilder {
        private final MacAddress remoteMepAddress;
        private final MepId remoteMepId;
        private Integer numberMessages;
        private String dataTlvHex;
        private Mep.Priority vlanPriority;
        private Boolean vlanDropEligible;

        private DefaultMepLbCreateBuilder(MacAddress remoteMepAddress) {
            this.remoteMepAddress = remoteMepAddress;
            this.remoteMepId = null;
        }

        private DefaultMepLbCreateBuilder(MepId remoteMepId) {
            this.remoteMepAddress = null;
            this.remoteMepId = remoteMepId;
        }

        @Override
        public MepLbCreateBuilder numberMessages(int numberMessages) {
            this.numberMessages = numberMessages;
            return this;
        }

        @Override
        public MepLbCreateBuilder dataTlv(byte[] dataTlv) {
            this.dataTlvHex = HexString.toHexString(dataTlv);
            return this;
        }

        @Override
        public MepLbCreateBuilder dataTlvHex(String dataTlvHex) {
            this.dataTlvHex = HexString.toHexString(
                    HexString.fromHexString(dataTlvHex));
            return this;
        }

        @Override
        public MepLbCreateBuilder dataTlvB64(String dataTlvB64) {
            this.dataTlvHex = HexString.toHexString(
                    Base64.getDecoder().decode(dataTlvB64));
            return this;
        }

        @Override
        public MepLbCreateBuilder vlanPriority(Mep.Priority vlanPriority) {
            this.vlanPriority = vlanPriority;
            return this;
        }

        @Override
        public MepLbCreateBuilder vlanDropEligible(boolean vlanDropEligible) {
            this.vlanDropEligible = vlanDropEligible;
            return this;
        }

        @Override
        public MepLbCreate build() {
            return new DefaultMepLbCreate(this);
        }
    }

}