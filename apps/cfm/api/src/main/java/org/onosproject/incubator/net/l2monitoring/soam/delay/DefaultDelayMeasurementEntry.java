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
package org.onosproject.incubator.net.l2monitoring.soam.delay;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;

import com.google.common.collect.Lists;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;

/**
 * The default implementation of {@link DelayMeasurementEntry}.
 */
public final class DefaultDelayMeasurementEntry
    extends DefaultDelayMeasurementCreate implements DelayMeasurementEntry {

    private final SoamId dmId;
    private final SessionStatus sessionStatus;
    private final Duration frameDelayTwoWay;
    private final Duration frameDelayForward;
    private final Duration frameDelayBackward;
    private final Duration interFrameDelayVariationTwoWay;
    private final Duration interFrameDelayVariationForward;
    private final Duration interFrameDelayVariationBackward;
    private final DelayMeasurementStatCurrent currentResult;
    private final Collection<DelayMeasurementStatHistory> historicalResults;

    private DefaultDelayMeasurementEntry(DefaultDmEntryBuilder builder) {
        super(builder);
        this.dmId = builder.dmId;
        this.currentResult = builder.currentResult;
        this.historicalResults = builder.historicalResults;

        this.sessionStatus = builder.sessionStatus;
        this.frameDelayTwoWay = builder.frameDelayTwoWay;
        this.frameDelayForward = builder.frameDelayForward;
        this.frameDelayBackward = builder.frameDelayBackward;
        this.interFrameDelayVariationTwoWay = builder.interFrameDelayVariationTwoWay;
        this.interFrameDelayVariationForward = builder.interFrameDelayVariationForward;
        this.interFrameDelayVariationBackward = builder.interFrameDelayVariationBackward;
    }

    @Override
    public SoamId dmId() {
        return dmId;
    }

    @Override
    public SessionStatus sessionStatus() {
        return sessionStatus;
    }

    @Override
    public Duration frameDelayTwoWay() {
        return frameDelayTwoWay;
    }

    @Override
    public Duration frameDelayForward() {
        return frameDelayForward;
    }

    @Override
    public Duration frameDelayBackward() {
        return frameDelayBackward;
    }

    @Override
    public Duration interFrameDelayVariationTwoWay() {
        return interFrameDelayVariationTwoWay;
    }

    @Override
    public Duration interFrameDelayVariationForward() {
        return interFrameDelayVariationForward;
    }

    @Override
    public Duration interFrameDelayVariationBackward() {
        return interFrameDelayVariationBackward;
    }

    @Override
    public DelayMeasurementStatCurrent currentResult() {
        return currentResult;
    }

    @Override
    public Collection<DelayMeasurementStatHistory> historicalResults() {
        if (historicalResults != null) {
            return Lists.newArrayList(historicalResults);
        }
        return null;
    }

    public static DmEntryBuilder builder(SoamId dmId, DmType dmCfgType,
            Version version, MepId remoteMepId, Mep.Priority priority)
                    throws SoamConfigException {
        return new DefaultDmEntryBuilder(dmId, dmCfgType, version,
                remoteMepId, priority);
    }

    /**
     * Builder for {@link DelayMeasurementEntry}.
     */
    private static final class DefaultDmEntryBuilder extends DefaultDmCreateBuilder
                                        implements DmEntryBuilder {
        private final SoamId dmId;
        private SessionStatus sessionStatus;
        private Duration frameDelayTwoWay;
        private Duration frameDelayForward;
        private Duration frameDelayBackward;
        private Duration interFrameDelayVariationTwoWay;
        private Duration interFrameDelayVariationForward;
        private Duration interFrameDelayVariationBackward;
        private DelayMeasurementStatCurrent currentResult;
        private Collection<DelayMeasurementStatHistory> historicalResults;

        private DefaultDmEntryBuilder(SoamId dmId, DmType dmCfgType,
                Version version, MepId remoteMepId, Mep.Priority priority)
                        throws SoamConfigException {
            super(dmCfgType, version, remoteMepId, priority);
            if (dmId == null) {
                throw new SoamConfigException("DmId is null");
            }
            this.dmId = dmId;
            historicalResults = new ArrayList<>();
        }

        @Override
        public DmEntryBuilder sessionStatus(SessionStatus sessionStatus) {
            this.sessionStatus = sessionStatus;
            return this;
        }

        @Override
        public DmEntryBuilder frameDelayTwoWay(Duration frameDelayTwoWay) {
            this.frameDelayTwoWay = frameDelayTwoWay;
            return this;
        }

        @Override
        public DmEntryBuilder frameDelayForward(Duration frameDelayForward) {
            this.frameDelayForward = frameDelayForward;
            return this;
        }

        @Override
        public DmEntryBuilder frameDelayBackward(Duration frameDelayBackward) {
            this.frameDelayBackward = frameDelayBackward;
            return this;
        }

        @Override
        public DmEntryBuilder interFrameDelayVariationTwoWay(
                Duration interFrameDelayVariationTwoWay) {
            this.interFrameDelayVariationTwoWay = interFrameDelayVariationTwoWay;
            return this;
        }

        @Override
        public DmEntryBuilder interFrameDelayVariationForward(
                Duration interFrameDelayVariationForward) {
            this.interFrameDelayVariationForward = interFrameDelayVariationForward;
            return this;
        }

        @Override
        public DmEntryBuilder interFrameDelayVariationBackward(
                Duration interFrameDelayVariationBackward) {
            this.interFrameDelayVariationBackward = interFrameDelayVariationBackward;
            return this;
        }

        @Override
        public DmEntryBuilder currentResult(DelayMeasurementStatCurrent currentResult) {
            this.currentResult = currentResult;
            return this;
        }

        @Override
        public DmEntryBuilder addToHistoricalResults(
                DelayMeasurementStatHistory historicalResult) {
            this.historicalResults.add(historicalResult);
            return this;
        }

        @Override
        public DelayMeasurementEntry build() {
            return new DefaultDelayMeasurementEntry(this);
        }
    }
}
