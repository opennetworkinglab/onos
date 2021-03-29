/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.inbandtelemetry.api;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.behaviour.inbandtelemetry.IntMetadataType;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an INT monitoring intent. Each intent is made up of a traffic slice
 * to be monitored, as a form of TrafficSelector, types of metadata to collect,
 * and other required parameters (INT header type, INT Report type, and Telemetry mode).
 *
 * IntIntent is converted to a set of flow rules to be installed on each INT-capable
 * switch in the network, by IntService.
 *
 * Terminologies and descriptions are borrowed from INT specification.
 *
 * @see <a href="https://github.com/p4lang/p4-applications/blob/master/docs/INT.pdf">
 *     INT sepcification</a>
 */
@Beta
public final class IntIntent {

    /**
     * Represents an INT header type.
     */
    public enum IntHeaderType {
        /**
         * Intermediate devices must process this type of INT header.
         */
        HOP_BY_HOP,
        /**
         * Intermediate devices must ignore this type of INT header.
         */
        DESTINATION
    }

    /**
     * Represents a type of telemetry report.
     */
    public enum IntReportType {
        /**
         * Report for flows matching certain definitions.
         */
        TRACKED_FLOW,
        /**
         * Reports for all dropped packets matching a drop watchlist.
         */
        DROPPED_PACKET,
        /**
         * Reports for traffic entering a specific queue during a period of queue congestion.
         */
        CONGESTED_QUEUE
    }

    /**
     * Represents telemetry mode.
     */
    public enum TelemetryMode {
        /**
         * Each network device generates its own telemetry reports.
         */
        POSTCARD,
        /**
         * Telemetry metadata is embedded in between the original
         * headers of data packets as they traverse the network.
         */
        INBAND_TELEMETRY
    }

    private static final int DEFAULT_PRIORITY = 10;

    // TrafficSelector to describe target flows to monitor
    private final TrafficSelector selector;
    // set of metadata type to collect
    private final Set<IntMetadataType> metadataTypes;
    // hop-by-hop or destination
    private final IntHeaderType headerType;
    // telemetry report types
    private final Set<IntReportType> reportTypes;
    // telemetry mode
    private final TelemetryMode telemetryMode;

    /**
     * Creates an IntIntent.
     *
     * @param selector      the traffic selector that identifies traffic to enable INT
     * @param metadataTypes the types of metadata to collect
     * @param headerType    the type of INT header
     * @param reportTypes   the types of report to be generated
     * @param telemetryMode the telemetry mode
     */
    private IntIntent(TrafficSelector selector, Set<IntMetadataType> metadataTypes,
                      IntHeaderType headerType, Set<IntReportType> reportTypes,
                      TelemetryMode telemetryMode) {
        this.selector = selector;
        this.metadataTypes = new HashSet<>(metadataTypes);
        this.headerType = headerType;
        this.reportTypes = new HashSet<>(reportTypes);
        this.telemetryMode = telemetryMode;
    }

    /**
     * Returns traffic selector of this intent.
     *
     * @return traffic selector
     */
    public TrafficSelector selector() {
        return selector;
    }

    /**
     * Returns a set of metadata type to be collected by this intent.
     *
     * @return set of metadata type
     */
    public Set<IntMetadataType> metadataTypes() {
        return metadataTypes;
    }

    /**
     * Returns a INT header type specified in this intent.
     *
     * @return INT header type
     */
    public IntHeaderType headerType() {
        return headerType;
    }

    /**
     * Returns a set of report type to be generated.
     *
     * @return set of report type
     */
    public Set<IntReportType> reportTypes() {
        return reportTypes;
    }

    /**
     * Returns a telemetry mode specified in this intent.
     *
     * @return telemtry mode
     */
    public TelemetryMode telemetryMode() {
        return telemetryMode;
    }

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntIntent intIntent = (IntIntent) o;
        return Objects.equal(selector, intIntent.selector) &&
                Objects.equal(metadataTypes, intIntent.metadataTypes) &&
                headerType == intIntent.headerType &&
                Objects.equal(reportTypes, intIntent.reportTypes) &&
                telemetryMode == intIntent.telemetryMode;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(selector, metadataTypes, headerType, reportTypes, telemetryMode);
    }

    /**
     * An IntIntent builder.
     */
    public static final class Builder {
        private TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        private Set<IntMetadataType> metadataTypes = new HashSet<>();
        private IntHeaderType headerType = IntHeaderType.HOP_BY_HOP;
        private Set<IntReportType> reportTypes = new HashSet<>();
        private TelemetryMode telemetryMode = TelemetryMode.INBAND_TELEMETRY;

        /**
         * Assigns a selector to the IntIntent.
         *
         * @param selector a traffic selector
         * @return an IntIntent builder
         */
        public IntIntent.Builder withSelector(TrafficSelector selector) {
            this.selector = selector;
            return this;
        }

        /**
         * Add a metadata type to the IntIntent.
         *
         * @param metadataType a type of metadata to collect
         * @return an IntIntent builder
         */
        public IntIntent.Builder withMetadataType(IntMetadataType metadataType) {
            this.metadataTypes.add(metadataType);
            return this;
        }

        /**
         * Assigns a header type to the IntIntent.
         *
         * @param headerType a header type
         * @return an IntIntent builder
         */
        public IntIntent.Builder withHeaderType(IntHeaderType headerType) {
            this.headerType = headerType;
            return this;
        }

        /**
         * Add a report type to the IntIntent.
         *
         * @param reportType a type of report
         * @return an IntIntent builder
         */
        public IntIntent.Builder withReportType(IntReportType reportType) {
            this.reportTypes.add(reportType);
            return this;
        }

        /**
         * Assigns a telemetry mode to the IntIntent.
         *
         * @param telemetryMode a telemetry mode
         * @return an IntIntent builder
         */
        public IntIntent.Builder withTelemetryMode(TelemetryMode telemetryMode) {
            this.telemetryMode = telemetryMode;
            return this;
        }

        /**
         * Builds the IntIntent.
         *
         * @return an IntIntent
         */
        public IntIntent build() {
            checkNotNull(headerType, "Header type cannot be null.");
            checkArgument(!reportTypes.isEmpty(), "Report types cannot be empty.");
            checkNotNull(telemetryMode, "Telemetry mode cannot be null.");

            return new IntIntent(selector, metadataTypes, headerType, reportTypes, telemetryMode);
        }
    }
}
