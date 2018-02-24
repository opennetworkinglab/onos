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
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Abstract default implementation of DelayMeasurementStat.
 * {@link DelayMeasurementStat}.
 */
public abstract class DefaultDelayMeasurementStat implements DelayMeasurementStat {

    private final Duration elapsedTime;
    private final boolean suspectStatus;
    private final Duration frameDelayTwoWayMin;
    private final Duration frameDelayTwoWayMax;
    private final Duration frameDelayTwoWayAvg;
    private final Duration frameDelayForwardMin;
    private final Duration frameDelayForwardMax;
    private final Duration frameDelayForwardAvg;
    private final Duration frameDelayBackwardMin;
    private final Duration frameDelayBackwardMax;
    private final Duration frameDelayBackwardAvg;
    private final Duration interFrameDelayVariationTwoWayMin;
    private final Duration interFrameDelayVariationTwoWayMax;
    private final Duration interFrameDelayVariationTwoWayAvg;
    private final Duration interFrameDelayVariationForwardMin;
    private final Duration interFrameDelayVariationForwardMax;
    private final Duration interFrameDelayVariationForwardAvg;
    private final Duration interFrameDelayVariationBackwardMin;
    private final Duration interFrameDelayVariationBackwardMax;
    private final Duration interFrameDelayVariationBackwardAvg;
    private final Duration frameDelayRangeTwoWayMax;
    private final Duration frameDelayRangeTwoWayAvg;
    private final Duration frameDelayRangeForwardMax;
    private final Duration frameDelayRangeForwardAvg;
    private final Duration frameDelayRangeBackwardMax;
    private final Duration frameDelayRangeBackwardAvg;
    private final Integer soamPdusSent;
    private final Integer soamPdusReceived;
    private final Map<Duration, Integer> frameDelayTwoWayBins;
    private final Map<Duration, Integer> frameDelayForwardBins;
    private final Map<Duration, Integer> frameDelayBackwardBins;
    private final Map<Duration, Integer> interFrameDelayVariationTwoWayBins;
    private final Map<Duration, Integer> interFrameDelayVariationForwardBins;
    private final Map<Duration, Integer> interFrameDelayVariationBackwardBins;
    private final Map<Duration, Integer> frameDelayRangeTwoWayBins;
    private final Map<Duration, Integer> frameDelayRangeForwardBins;
    private final Map<Duration, Integer> frameDelayRangeBackwardBins;

    protected DefaultDelayMeasurementStat(DefaultDmStatBuilder builder) {
        this.elapsedTime = builder.elapsedTime;
        this.suspectStatus = builder.suspectStatus;
        this.frameDelayTwoWayMin = builder.frameDelayTwoWayMin;
        this.frameDelayTwoWayMax = builder.frameDelayTwoWayMax;
        this.frameDelayTwoWayAvg = builder.frameDelayTwoWayAvg;
        this.frameDelayForwardMin = builder.frameDelayForwardMin;
        this.frameDelayForwardMax = builder.frameDelayForwardMax;
        this.frameDelayForwardAvg = builder.frameDelayForwardAvg;
        this.frameDelayBackwardMin = builder.frameDelayBackwardMin;
        this.frameDelayBackwardMax = builder.frameDelayBackwardMax;
        this.frameDelayBackwardAvg = builder.frameDelayBackwardAvg;
        this.interFrameDelayVariationTwoWayMin = builder.interFrameDelayVariationTwoWayMin;
        this.interFrameDelayVariationTwoWayMax = builder.interFrameDelayVariationTwoWayMax;
        this.interFrameDelayVariationTwoWayAvg = builder.interFrameDelayVariationTwoWayAvg;
        this.interFrameDelayVariationForwardMin = builder.interFrameDelayVariationForwardMin;
        this.interFrameDelayVariationForwardMax = builder.interFrameDelayVariationForwardMax;
        this.interFrameDelayVariationForwardAvg = builder.interFrameDelayVariationForwardAvg;
        this.interFrameDelayVariationBackwardMin = builder.interFrameDelayVariationBackwardMin;
        this.interFrameDelayVariationBackwardMax = builder.interFrameDelayVariationBackwardMax;
        this.interFrameDelayVariationBackwardAvg = builder.interFrameDelayVariationBackwardAvg;
        this.frameDelayRangeTwoWayMax = builder.frameDelayRangeTwoWayMax;
        this.frameDelayRangeTwoWayAvg = builder.frameDelayRangeTwoWayAvg;
        this.frameDelayRangeForwardMax = builder.frameDelayRangeForwardMax;
        this.frameDelayRangeForwardAvg = builder.frameDelayRangeForwardAvg;
        this.frameDelayRangeBackwardMax = builder.frameDelayRangeBackwardMax;
        this.frameDelayRangeBackwardAvg = builder.frameDelayRangeBackwardAvg;
        this.soamPdusSent = builder.soamPdusSent;
        this.soamPdusReceived = builder.soamPdusReceived;
        this.frameDelayTwoWayBins = builder.frameDelayTwoWayBins;
        this.frameDelayForwardBins = builder.frameDelayForwardBins;
        this.frameDelayBackwardBins = builder.frameDelayBackwardBins;
        this.interFrameDelayVariationTwoWayBins = builder.interFrameDelayVariationTwoWayBins;
        this.interFrameDelayVariationForwardBins = builder.interFrameDelayVariationForwardBins;
        this.interFrameDelayVariationBackwardBins = builder.interFrameDelayVariationBackwardBins;
        this.frameDelayRangeTwoWayBins = builder.frameDelayRangeTwoWayBins;
        this.frameDelayRangeForwardBins = builder.frameDelayRangeForwardBins;
        this.frameDelayRangeBackwardBins = builder.frameDelayRangeBackwardBins;
    }

    @Override
    public Duration elapsedTime() {
        return elapsedTime;
    }

    @Override
    public boolean suspectStatus() {
        return suspectStatus;
    }

    @Override
    public Duration frameDelayTwoWayMin() {
        return frameDelayTwoWayMin;
    }

    @Override
    public Duration frameDelayTwoWayMax() {
        return frameDelayTwoWayMax;
    }

    @Override
    public Duration frameDelayTwoWayAvg() {
        return frameDelayTwoWayAvg;
    }

    @Override
    public Duration frameDelayForwardMin() {
        return frameDelayForwardMin;
    }

    @Override
    public Duration frameDelayForwardMax() {
        return frameDelayForwardMax;
    }

    @Override
    public Duration frameDelayForwardAvg() {
        return frameDelayForwardAvg;
    }

    @Override
    public Duration frameDelayBackwardMin() {
        return frameDelayBackwardMin;
    }

    @Override
    public Duration frameDelayBackwardMax() {
        return frameDelayBackwardMax;
    }

    @Override
    public Duration frameDelayBackwardAvg() {
        return frameDelayBackwardAvg;
    }

    @Override
    public Duration interFrameDelayVariationTwoWayMin() {
        return interFrameDelayVariationTwoWayMin;
    }

    @Override
    public Duration interFrameDelayVariationTwoWayMax() {
        return interFrameDelayVariationTwoWayMax;
    }

    @Override
    public Duration interFrameDelayVariationTwoWayAvg() {
        return interFrameDelayVariationTwoWayAvg;
    }

    @Override
    public Duration interFrameDelayVariationForwardMin() {
        return interFrameDelayVariationForwardMin;
    }

    @Override
    public Duration interFrameDelayVariationForwardMax() {
        return interFrameDelayVariationForwardMax;
    }

    @Override
    public Duration interFrameDelayVariationForwardAvg() {
        return interFrameDelayVariationForwardAvg;
    }

    @Override
    public Duration interFrameDelayVariationBackwardMin() {
        return interFrameDelayVariationBackwardMin;
    }

    @Override
    public Duration interFrameDelayVariationBackwardMax() {
        return interFrameDelayVariationBackwardMax;
    }

    @Override
    public Duration interFrameDelayVariationBackwardAvg() {
        return interFrameDelayVariationBackwardAvg;
    }

    @Override
    public Duration frameDelayRangeTwoWayMax() {
        return frameDelayRangeTwoWayMax;
    }

    @Override
    public Duration frameDelayRangeTwoWayAvg() {
        return frameDelayRangeTwoWayAvg;
    }

    @Override
    public Duration frameDelayRangeForwardMax() {
        return frameDelayRangeForwardMax;
    }

    @Override
    public Duration frameDelayRangeForwardAvg() {
        return frameDelayRangeForwardAvg;
    }

    @Override
    public Duration frameDelayRangeBackwardMax() {
        return frameDelayRangeBackwardMax;
    }

    @Override
    public Duration frameDelayRangeBackwardAvg() {
        return frameDelayRangeBackwardAvg;
    }

    @Override
    public Integer soamPdusSent() {
        return soamPdusSent;
    }

    @Override
    public Integer soamPdusReceived() {
        return soamPdusReceived;
    }

    @Override
    public Map<Duration, Integer> frameDelayTwoWayBins() {
        if (frameDelayTwoWayBins != null) {
            return Maps.newHashMap(frameDelayTwoWayBins);
        } else {
            return null;
        }
    }

    @Override
    public Map<Duration, Integer> frameDelayForwardBins() {
        if (frameDelayForwardBins != null) {
            return Maps.newHashMap(frameDelayForwardBins);
        } else {
            return null;
        }
    }

    @Override
    public Map<Duration, Integer> frameDelayBackwardBins() {
        if (frameDelayBackwardBins != null) {
            return Maps.newHashMap(frameDelayBackwardBins);
        } else {
            return null;
        }
    }

    @Override
    public Map<Duration, Integer> interFrameDelayVariationTwoWayBins() {
        if (interFrameDelayVariationTwoWayBins != null) {
            return Maps.newHashMap(interFrameDelayVariationTwoWayBins);
        } else {
            return null;
        }
    }

    @Override
    public Map<Duration, Integer> interFrameDelayVariationForwardBins() {
        if (interFrameDelayVariationForwardBins != null) {
            return Maps.newHashMap(interFrameDelayVariationForwardBins);
        } else {
            return null;
        }
    }

    @Override
    public Map<Duration, Integer> interFrameDelayVariationBackwardBins() {
        if (interFrameDelayVariationBackwardBins != null) {
            return Maps.newHashMap(interFrameDelayVariationBackwardBins);
        } else {
            return null;
        }
    }

    @Override
    public Map<Duration, Integer> frameDelayRangeTwoWayBins() {
        if (frameDelayRangeTwoWayBins != null) {
            return Maps.newHashMap(frameDelayRangeTwoWayBins);
        } else {
            return null;
        }
    }

    @Override
    public Map<Duration, Integer> frameDelayRangeForwardBins() {
        if (frameDelayRangeForwardBins != null) {
            return Maps.newHashMap(frameDelayRangeForwardBins);
        } else {
            return null;
        }
    }

    @Override
    public Map<Duration, Integer> frameDelayRangeBackwardBins() {
        if (frameDelayRangeBackwardBins != null) {
            return Maps.newHashMap(frameDelayRangeBackwardBins);
        } else {
            return null;
        }
    }

    /**
     * Abstract builder for {@link DelayMeasurementStat}.
     */
    protected abstract static class DefaultDmStatBuilder implements DmStatBuilder {

        private final Duration elapsedTime;
        private final boolean suspectStatus;
        private Duration frameDelayTwoWayMin;
        private Duration frameDelayTwoWayMax;
        private Duration frameDelayTwoWayAvg;
        private Duration frameDelayForwardMin;
        private Duration frameDelayForwardMax;
        private Duration frameDelayForwardAvg;
        private Duration frameDelayBackwardMin;
        private Duration frameDelayBackwardMax;
        private Duration frameDelayBackwardAvg;
        private Duration interFrameDelayVariationTwoWayMin;
        private Duration interFrameDelayVariationTwoWayMax;
        private Duration interFrameDelayVariationTwoWayAvg;
        private Duration interFrameDelayVariationForwardMin;
        private Duration interFrameDelayVariationForwardMax;
        private Duration interFrameDelayVariationForwardAvg;
        private Duration interFrameDelayVariationBackwardMin;
        private Duration interFrameDelayVariationBackwardMax;
        private Duration interFrameDelayVariationBackwardAvg;
        private Duration frameDelayRangeTwoWayMax;
        private Duration frameDelayRangeTwoWayAvg;
        private Duration frameDelayRangeForwardMax;
        private Duration frameDelayRangeForwardAvg;
        private Duration frameDelayRangeBackwardMax;
        private Duration frameDelayRangeBackwardAvg;
        private Integer soamPdusSent;
        private Integer soamPdusReceived;
        private Map<Duration, Integer> frameDelayTwoWayBins;
        private Map<Duration, Integer> frameDelayForwardBins;
        private Map<Duration, Integer> frameDelayBackwardBins;
        private Map<Duration, Integer> interFrameDelayVariationTwoWayBins;
        private Map<Duration, Integer> interFrameDelayVariationForwardBins;
        private Map<Duration, Integer> interFrameDelayVariationBackwardBins;
        private Map<Duration, Integer> frameDelayRangeTwoWayBins;
        private Map<Duration, Integer> frameDelayRangeForwardBins;
        private Map<Duration, Integer> frameDelayRangeBackwardBins;

        protected DefaultDmStatBuilder(Duration elapsedTime, boolean suspectStatus) {
            this.elapsedTime = elapsedTime;
            this.suspectStatus = suspectStatus;
        }

        @Override
        public DmStatBuilder frameDelayTwoWayMin(Duration frameDelayTwoWayMin) {
            this.frameDelayTwoWayMin = frameDelayTwoWayMin;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayTwoWayMax(Duration frameDelayTwoWayMax) {
            this.frameDelayTwoWayMax = frameDelayTwoWayMax;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayTwoWayAvg(Duration frameDelayTwoWayAvg) {
            this.frameDelayTwoWayAvg = frameDelayTwoWayAvg;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayForwardMin(Duration frameDelayForwardMin) {
            this.frameDelayForwardMin = frameDelayForwardMin;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayForwardMax(Duration frameDelayForwardMax) {
            this.frameDelayForwardMax = frameDelayForwardMax;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayForwardAvg(Duration frameDelayForwardAvg) {
            this.frameDelayForwardAvg = frameDelayForwardAvg;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayBackwardMin(Duration frameDelayBackwardMin) {
            this.frameDelayBackwardMin = frameDelayBackwardMin;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayBackwardMax(Duration frameDelayBackwardMax) {
            this.frameDelayBackwardMax = frameDelayBackwardMax;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayBackwardAvg(Duration frameDelayBackwardAvg) {
            this.frameDelayBackwardAvg = frameDelayBackwardAvg;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationTwoWayMin(Duration interFrameDelayVariationTwoWayMin) {
            this.interFrameDelayVariationTwoWayMin = interFrameDelayVariationTwoWayMin;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationTwoWayMax(Duration interFrameDelayVariationTwoWayMax) {
            this.interFrameDelayVariationTwoWayMax = interFrameDelayVariationTwoWayMax;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationTwoWayAvg(Duration interFrameDelayVariationTwoWayAvg) {
            this.interFrameDelayVariationTwoWayAvg = interFrameDelayVariationTwoWayAvg;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationForwardMin(Duration interFrameDelayVariationForwardMin) {
            this.interFrameDelayVariationForwardMin = interFrameDelayVariationForwardMin;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationForwardMax(Duration interFrameDelayVariationForwardMax) {
            this.interFrameDelayVariationForwardMax = interFrameDelayVariationForwardMax;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationForwardAvg(Duration interFrameDelayVariationForwardAvg) {
            this.interFrameDelayVariationForwardAvg = interFrameDelayVariationForwardAvg;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationBackwardMin(Duration interFrameDelayVariationBackwardMin) {
            this.interFrameDelayVariationBackwardMin = interFrameDelayVariationBackwardMin;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationBackwardMax(Duration interFrameDelayVariationBackwardMax) {
            this.interFrameDelayVariationBackwardMax = interFrameDelayVariationBackwardMax;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationBackwardAvg(Duration interFrameDelayVariationBackwardAvg) {
            this.interFrameDelayVariationBackwardAvg = interFrameDelayVariationBackwardAvg;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayRangeTwoWayMax(Duration frameDelayRangeTwoWayMax) {
            this.frameDelayRangeTwoWayMax = frameDelayRangeTwoWayMax;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayRangeTwoWayAvg(Duration frameDelayRangeTwoWayAvg) {
            this.frameDelayRangeTwoWayAvg = frameDelayRangeTwoWayAvg;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayRangeForwardMax(Duration frameDelayRangeForwardMax) {
            this.frameDelayRangeForwardMax = frameDelayRangeForwardMax;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayRangeForwardAvg(Duration frameDelayRangeForwardAvg) {
            this.frameDelayRangeForwardAvg = frameDelayRangeForwardAvg;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayRangeBackwardMax(Duration frameDelayRangeBackwardMax) {
            this.frameDelayRangeBackwardMax = frameDelayRangeBackwardMax;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayRangeBackwardAvg(Duration frameDelayRangeBackwardAvg) {
            this.frameDelayRangeBackwardAvg = frameDelayRangeBackwardAvg;
            return this;
        }

        @Override
        public DmStatBuilder soamPdusSent(Integer soamPdusSent) {
            this.soamPdusSent = soamPdusSent;
            return this;
        }

        @Override
        public DmStatBuilder soamPdusReceived(Integer soamPdusReceived) {
            this.soamPdusReceived = soamPdusReceived;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayTwoWayBins(Map<Duration, Integer> frameDelayTwoWayBins) {
            this.frameDelayTwoWayBins = frameDelayTwoWayBins;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayForwardBins(Map<Duration, Integer> frameDelayForwardBins) {
            this.frameDelayForwardBins = frameDelayForwardBins;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayBackwardBins(Map<Duration, Integer> frameDelayBackwardBins) {
            this.frameDelayBackwardBins = frameDelayBackwardBins;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationTwoWayBins(
                Map<Duration, Integer> interFrameDelayVariationTwoWayBins) {
            this.interFrameDelayVariationTwoWayBins = interFrameDelayVariationTwoWayBins;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationForwardBins(
                Map<Duration, Integer> interFrameDelayVariationForwardBins) {
            this.interFrameDelayVariationForwardBins = interFrameDelayVariationForwardBins;
            return this;
        }

        @Override
        public DmStatBuilder interFrameDelayVariationBackwardBins(
                Map<Duration, Integer> interFrameDelayVariationBackwardBins) {
            this.interFrameDelayVariationBackwardBins = interFrameDelayVariationBackwardBins;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayRangeTwoWayBins(Map<Duration, Integer> frameDelayRangeTwoWayBins) {
            this.frameDelayRangeTwoWayBins = frameDelayRangeTwoWayBins;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayRangeForwardBins(Map<Duration, Integer> frameDelayRangeForwardBins) {
            this.frameDelayRangeForwardBins = frameDelayRangeForwardBins;
            return this;
        }

        @Override
        public DmStatBuilder frameDelayRangeBackwardBins(Map<Duration, Integer> frameDelayRangeBackwardBins) {
            this.frameDelayRangeBackwardBins = frameDelayRangeBackwardBins;
            return this;
        }
    }
}
