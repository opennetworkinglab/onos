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

import java.util.ArrayList;
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.DefaultMeasurementCreateBase;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;

/**
 * The default implementation of {@link DelayMeasurementCreate}.
 */
public class DefaultDelayMeasurementCreate extends DefaultMeasurementCreateBase
                            implements DelayMeasurementCreate {

    protected final DmType dmCfgType;
    protected final Collection<MeasurementOption> measurementsEnabled;
    protected final Short binsPerFdInterval;
    protected final Short binsPerIfdvInterval;
    protected final Short ifdvSelectionOffset;
    protected final Short binsPerFdrInterval;
    protected final Collection<DelayMeasurementThreshold> thresholds;

    protected DefaultDelayMeasurementCreate(DefaultDmCreateBuilder builder) {
        super(builder);
        this.dmCfgType = builder.dmCfgType;
        this.measurementsEnabled = builder.measurementsEnabled;
        this.binsPerFdInterval = builder.binsPerFdInterval;
        this.binsPerIfdvInterval = builder.binsPerIfdvInterval;
        this.ifdvSelectionOffset = builder.ifdvSelectionOffset;
        this.binsPerFdrInterval = builder.binsPerFdrInterval;
        this.thresholds = builder.thresholds;
    }

    @Override
    public DmType dmCfgType() {
        return dmCfgType;
    }

    @Override
    public Collection<MeasurementOption> measurementsEnabled() {
        return measurementsEnabled;
    }

    @Override
    public Short binsPerFdInterval() {
        return binsPerFdInterval;
    }

    @Override
    public Short binsPerIfdvInterval() {
        return binsPerIfdvInterval;
    }

    @Override
    public Short ifdvSelectionOffset() {
        return ifdvSelectionOffset;
    }

    @Override
    public Short binsPerFdrInterval() {
        return binsPerFdrInterval;
    }

    @Override
    public Collection<DelayMeasurementThreshold> thresholds() {
        return thresholds;
    }

    public static DmCreateBuilder builder(DmType dmCfgType,
                                          Version version, MepId remoteMepId, Mep.Priority priority)
                    throws SoamConfigException {
        return new DefaultDmCreateBuilder(dmCfgType, version, remoteMepId, priority);
    }

    /**
     * Builder for {@link DelayMeasurementCreate}.
     */
    protected static class DefaultDmCreateBuilder extends DefaultMeasCreateBaseBuilder
                    implements DmCreateBuilder {
        protected final DmType dmCfgType;

        protected Collection<MeasurementOption> measurementsEnabled;
        protected Short binsPerFdInterval;
        protected Short binsPerIfdvInterval;
        protected Short ifdvSelectionOffset;
        protected Short binsPerFdrInterval;
        protected Collection<DelayMeasurementThreshold> thresholds;

        protected DefaultDmCreateBuilder(DmType dmCfgType, Version version,
                MepId remoteMepId, Mep.Priority priority)
                        throws SoamConfigException {
            super(version, remoteMepId, priority);
            this.dmCfgType = dmCfgType;
            measurementsEnabled = new ArrayList<>();
            thresholds = new ArrayList<>();
        }

        @Override
        public DmCreateBuilder addToMeasurementsEnabled(
                MeasurementOption measurementEnabled) {
            this.measurementsEnabled.add(measurementEnabled);
            return this;
        }

        @Override
        public DmCreateBuilder binsPerFdInterval(Short binsPerFdInterval)
                throws SoamConfigException {
            if (binsPerFdInterval < 2 || binsPerFdInterval > 100) {
                throw new SoamConfigException(
                        "Bins Per Fd Interval must be between 2..100. Rejecting: " +
                                binsPerFdInterval);
            }
            this.binsPerFdInterval = binsPerFdInterval;
            return this;
        }

        @Override
        public DmCreateBuilder binsPerIfdvInterval(Short binsPerIfdvInterval)
                throws SoamConfigException {
            if (binsPerIfdvInterval < 2 || binsPerIfdvInterval > 100) {
                throw new SoamConfigException(
                        "Bins Per Ifdv Interval must be between 2..100. Rejecting: " +
                                binsPerIfdvInterval);
            }
            this.binsPerIfdvInterval = binsPerIfdvInterval;
            return this;
        }

        @Override
        public DmCreateBuilder ifdvSelectionOffset(Short ifdvSelectionOffset)
                throws SoamConfigException {
            if (ifdvSelectionOffset < 2 || ifdvSelectionOffset > 100) {
                throw new SoamConfigException(
                        "IFDV Selection Offset must be between 2..100. Rejecting: " +
                                ifdvSelectionOffset);
            }
            this.ifdvSelectionOffset = ifdvSelectionOffset;
            return this;
        }

        @Override
        public DmCreateBuilder binsPerFdrInterval(Short binsPerFdrInterval)
                throws SoamConfigException {
            if (binsPerFdrInterval < 2 || binsPerFdrInterval > 100) {
                throw new SoamConfigException(
                        "Bins Per Fd Interval must be between 2..100. Rejecting: " +
                                binsPerFdrInterval);
            }
            this.binsPerFdrInterval = binsPerFdrInterval;
            return this;
        }

        @Override
        public DmCreateBuilder addToThresholds(
                DelayMeasurementThreshold threshold) {
            this.thresholds.add(threshold);
            return this;
        }

        @Override
        public DelayMeasurementCreate build() {
            return new DefaultDelayMeasurementCreate(this);
        }
    }
}
