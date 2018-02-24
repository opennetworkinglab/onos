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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 * The default implementation of {@link MepEntry}.
 */
public final class DefaultMepEntry extends DefaultMep implements MepEntry {

    private final MacAddress macAddress;
    private final FngState fngState;
    private final FaultDefectType highestPriorityDefect;
    private final boolean activeRdiCcmDefect;
    private final boolean activeMacStatusDefect;
    private final boolean activeRemoteCcmDefect;
    private final boolean activeErrorCcmDefect;
    private final boolean activeXconCcmDefect;
    private final byte[] lastErrorCcm;
    private final byte[] lastXconCcm;
    private final int ccmSequenceErrorCount;
    private final int totalCcmsTransmitted;
    private final MepLbEntry loopbackAttributes;
    private final MepLtEntry linktraceAttributes;
    private final Collection<RemoteMepEntry> activeRemoteMepList;
    private final Collection<DelayMeasurementEntry> delayMeasurementList;
    private final Collection<LossMeasurementEntry> lossMeasurementList;

    private DefaultMepEntry(DefaultMepEntryBuilder mepEntryBuilder) throws CfmConfigException {
        super((DefaultMepBuilder) DefaultMep.builder(
                mepEntryBuilder.mepId,
                mepEntryBuilder.deviceId,
                mepEntryBuilder.port,
                mepEntryBuilder.direction,
                mepEntryBuilder.mdId,
                mepEntryBuilder.maId)
                .administrativeState(mepEntryBuilder.administrativeState)
                .cciEnabled(mepEntryBuilder.cciEnabled)
                .ccmLtmPriority(mepEntryBuilder.ccmLtmPriority));

        this.macAddress = mepEntryBuilder.macAddress;
        this.fngState = mepEntryBuilder.fngState;
        this.highestPriorityDefect = mepEntryBuilder.highestPriorityDefect;
        this.activeRdiCcmDefect = mepEntryBuilder.activeRdiCcmDefect;
        this.activeMacStatusDefect = mepEntryBuilder.activeMacStatusDefect;
        this.activeRemoteCcmDefect = mepEntryBuilder.activeRemoteCcmDefect;
        this.activeErrorCcmDefect = mepEntryBuilder.activeErrorCcmDefect;
        this.activeXconCcmDefect = mepEntryBuilder.activeXconCcmDefect;
        this.lastErrorCcm = mepEntryBuilder.lastErrorCcm;
        this.lastXconCcm = mepEntryBuilder.lastXconCcm;
        this.ccmSequenceErrorCount = mepEntryBuilder.ccmSequenceErrorCount;
        this.totalCcmsTransmitted = mepEntryBuilder.totalCcmsTransmitted;
        this.loopbackAttributes = mepEntryBuilder.loopbackAttributes;
        this.linktraceAttributes = mepEntryBuilder.linktraceAttributes;
        this.activeRemoteMepList = mepEntryBuilder.activeRemoteMepList;
        this.delayMeasurementList = mepEntryBuilder.delayMeasurementList;
        this.lossMeasurementList = mepEntryBuilder.lossMeasurementList;
    }

    @Override
    public MacAddress macAddress() {
        return macAddress;
    }

    @Override
    public FngState fngState() {
        return fngState;
    }

    @Override
    public FaultDefectType getHighestPriorityDefect() {
        return highestPriorityDefect;
    }

    @Override
    public boolean activeRdiCcmDefect() {
        return activeRdiCcmDefect;
    }

    @Override
    public boolean activeMacStatusDefect() {
        return activeMacStatusDefect;
    }

    @Override
    public boolean activeRemoteCcmDefect() {
        return activeRemoteCcmDefect;
    }

    @Override
    public boolean activeErrorCcmDefect() {
        return activeErrorCcmDefect;
    }

    @Override
    public boolean activeXconCcmDefect() {
        return activeXconCcmDefect;
    }

    @Override
    public byte[] lastErrorCcm() {
        return lastErrorCcm;
    }

    @Override
    public byte[] lastXconCcm() {
        return lastXconCcm;
    }

    @Override
    public int ccmSequenceErrorCount() {
        return ccmSequenceErrorCount;
    }

    @Override
    public int totalCcmsTransmitted() {
        return totalCcmsTransmitted;
    }

    @Override
    public MepLbEntry loopbackAttributes() {
        return loopbackAttributes;
    }

    @Override
    public MepLtEntry linktraceAttributes() {
        return linktraceAttributes;
    }

    @Override
    public List<RemoteMepEntry> activeRemoteMepList() {
        if (activeRemoteMepList == null) {
            return null;
        } else {
            return Lists.newArrayList(activeRemoteMepList);
        }
    }

    @Override
    public Collection<DelayMeasurementEntry> delayMeasurementList() {
        return delayMeasurementList;
    }

    @Override
    public Collection<LossMeasurementEntry> lossMeasurementList() {
        return lossMeasurementList;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mepId.value();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DefaultMepEntry other = (DefaultMepEntry) obj;
        if (mepId != other.mepId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("id", mepId).toString();
    }

    public static MepEntryBuilder builder(
            MepId mepId,
            DeviceId deviceId,
            PortNumber port,
            MepDirection direction,
            MdId mdName,
            MaIdShort maName)
            throws CfmConfigException {
        return new DefaultMepEntryBuilder(mepId, deviceId, port, direction, mdName, maName);
    }

    public static MepEntryBuilder builder(Mep mep) throws CfmConfigException {
        return new DefaultMepEntryBuilder(mep);
    }

    private static final class DefaultMepEntryBuilder
        extends DefaultMep.DefaultMepBuilder implements MepEntry.MepEntryBuilder {
        private MacAddress macAddress;
        private FngState fngState;
        private FaultDefectType highestPriorityDefect;
        private boolean activeRdiCcmDefect;
        private boolean activeMacStatusDefect;
        private boolean activeRemoteCcmDefect;
        private boolean activeErrorCcmDefect;
        private boolean activeXconCcmDefect;
        private byte[] lastErrorCcm;
        private byte[] lastXconCcm;
        private int ccmSequenceErrorCount;
        private int totalCcmsTransmitted;
        private MepLbEntry loopbackAttributes;
        private MepLtEntry linktraceAttributes;
        private Collection<RemoteMepEntry> activeRemoteMepList;
        private Collection<DelayMeasurementEntry> delayMeasurementList;
        private Collection<LossMeasurementEntry> lossMeasurementList;

        private DefaultMepEntryBuilder(MepId mepId, DeviceId deviceId, PortNumber port, MepDirection direction,
                MdId mdName, MaIdShort maName)
                throws CfmConfigException {
            super(mepId, deviceId, port, direction, mdName, maName);

            activeRemoteMepList = new ArrayList<>();
            delayMeasurementList = new ArrayList<>();
            lossMeasurementList = new ArrayList<>();
        }

        private DefaultMepEntryBuilder(MepEntry mepEntry) throws CfmConfigException {
            super(mepEntry.mepId(), mepEntry.deviceId(),
                    mepEntry.port(), mepEntry.direction(), mepEntry.mdId(), mepEntry.maId());
            this.macAddress = mepEntry.macAddress();
            this.fngState = mepEntry.fngState();
            this.highestPriorityDefect = mepEntry.getHighestPriorityDefect();
            this.activeRdiCcmDefect = mepEntry.activeRdiCcmDefect();
            this.activeMacStatusDefect = mepEntry.activeMacStatusDefect();
            this.activeRemoteCcmDefect = mepEntry.activeRemoteCcmDefect();
            this.activeErrorCcmDefect = mepEntry.activeErrorCcmDefect();
            this.activeXconCcmDefect = mepEntry.activeXconCcmDefect();
            this.lastErrorCcm = mepEntry.lastErrorCcm();
            this.lastXconCcm = mepEntry.lastXconCcm();
            this.ccmSequenceErrorCount = mepEntry.ccmSequenceErrorCount();
            this.totalCcmsTransmitted = mepEntry.totalCcmsTransmitted();
            this.loopbackAttributes = mepEntry.loopbackAttributes();
            this.linktraceAttributes = mepEntry.linktraceAttributes();
            this.activeRemoteMepList = Lists.newArrayList(mepEntry.activeRemoteMepList());
            this.delayMeasurementList = Lists.newArrayList(mepEntry.delayMeasurementList());
            this.lossMeasurementList = Lists.newArrayList(mepEntry.lossMeasurementList());
        }

        private DefaultMepEntryBuilder(Mep mep) throws CfmConfigException {
            super(mep.mepId(), mep.deviceId(),
                    mep.port(), mep.direction(),
                    mep.mdId(), mep.maId());
        }

        @Override
        public MepEntryBuilder macAddress(MacAddress macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        @Override
        public MepEntryBuilder fngState(FngState fngState) {
            this.fngState = fngState;
            return this;
        }

        @Override
        public MepEntryBuilder highestPriorityDefect(FaultDefectType highestPriorityDefect) {
            this.highestPriorityDefect = highestPriorityDefect;
            return this;
        }

        @Override
        public MepEntryBuilder activeRdiCcmDefect(boolean activeRdiCcmDefect) {
            this.activeRdiCcmDefect = activeRdiCcmDefect;
            return this;
        }

        @Override
        public MepEntryBuilder activeMacStatusDefect(boolean activeMacStatusDefect) {
            this.activeMacStatusDefect = activeMacStatusDefect;
            return this;
        }

        @Override
        public MepEntryBuilder activeRemoteCcmDefect(boolean activeRemoteCcmDefect) {
            this.activeRemoteCcmDefect = activeRemoteCcmDefect;
            return this;
        }

        @Override
        public MepEntryBuilder activeErrorCcmDefect(boolean activeErrorCcmDefect) {
            this.activeErrorCcmDefect = activeErrorCcmDefect;
            return this;
        }

        @Override
        public MepEntryBuilder activeXconCcmDefect(boolean activeXconCcmDefect) {
            this.activeXconCcmDefect = activeXconCcmDefect;
            return this;
        }

        @Override
        public MepEntryBuilder lastErrorCcm(byte[] lastErrorCcm) {
            this.lastErrorCcm = lastErrorCcm;
            return this;
        }

        @Override
        public MepEntryBuilder lastXconCcm(byte[] lastXconCcm) {
            this.lastXconCcm = lastXconCcm;
            return this;
        }

        @Override
        public MepEntryBuilder ccmSequenceErrorCount(int ccmSequenceErrorCount) {
            this.ccmSequenceErrorCount = ccmSequenceErrorCount;
            return this;
        }

        @Override
        public MepEntryBuilder totalCcmsTransmitted(int totalCcmsTransmitted) {
            this.totalCcmsTransmitted = totalCcmsTransmitted;
            return this;
        }

        @Override
        public MepEntryBuilder loopbackAttributes(MepLbEntry loopbackAttributes) {
            this.loopbackAttributes = loopbackAttributes;
            return this;
        }

        @Override
        public MepEntryBuilder linktraceAttributes(MepLtEntry linktraceAttributes) {
            this.linktraceAttributes = linktraceAttributes;
            return this;
        }

        @Override
        public MepEntryBuilder addToActiveRemoteMepList(
                RemoteMepEntry activeRemoteMep) {
            this.activeRemoteMepList.add(activeRemoteMep);
            return this;
        }

        @Override
        public MepEntryBuilder addToDelayMeasurementList(
                DelayMeasurementEntry delayMeasurement) {
            this.delayMeasurementList.add(delayMeasurement);
            return this;
        }

        @Override
        public MepEntryBuilder addToLossMeasurementList(
                LossMeasurementEntry lossMeasurement) {
            this.lossMeasurementList.add(lossMeasurement);
            return this;
        }

        @Override
        public MepEntry buildEntry() throws CfmConfigException {
            return new DefaultMepEntry(this);
        }
    }
}
