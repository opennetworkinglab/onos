/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.dpi;

import java.util.List;
import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * DPI statistic information.
 */
public class DpiStatInfo {
    TrafficStatInfo trafficStatistics;
    List<ProtocolStatInfo> detectedProtos;
    List<FlowStatInfo> knownFlows;
    List<FlowStatInfo> unknownFlows;

    /**
     * Constructor for default DpiStatInfo class.
     */
    public DpiStatInfo() {
        this.trafficStatistics = null;
        this.detectedProtos = null;
        this.knownFlows = null;
        this.unknownFlows = null;
    }

    /**
     * Constructor for DpiStatistics class specified with trafficStatInfo.
     *
     * @param trafficStatistics traffic statistic information
     */
    public DpiStatInfo(TrafficStatInfo trafficStatistics) {
        this.trafficStatistics = trafficStatistics;
        this.detectedProtos = null;
        this.knownFlows = null;
        this.unknownFlows = null;
    }

    /**
     * Constructor for DpiStatistics class specified with trafficStatInfo and detectedProtos.
     *
     * @param trafficStatistics traffic statistic information
     * @param detectedProtos detected protocols statistic information
     */
    public DpiStatInfo(TrafficStatInfo trafficStatistics,
                       List<ProtocolStatInfo> detectedProtos) {
        this.trafficStatistics = trafficStatistics;
        this.detectedProtos = detectedProtos;
        this.knownFlows = null;
        this.unknownFlows = null;
    }

    /**
     * Constructor for DpiStatistics class specified with trafficStatInfo, detectedProtos and knownFlows.
     *
     * @param trafficStatistics traffic statistic information
     * @param detectedProtos detected protocols statistic information
     * @param knownFlows known flows
     */
    public DpiStatInfo(TrafficStatInfo trafficStatistics,
                       List<ProtocolStatInfo> detectedProtos,
                       List<FlowStatInfo> knownFlows) {
        this.trafficStatistics = trafficStatistics;
        this.detectedProtos = detectedProtos;
        this.knownFlows = knownFlows;
        this.unknownFlows = null;
    }

    /**
     * Constructor for DpiStatistics class specified with trafficStatInfo, detectedProtos, knownFlows and unknownFlows.
     *
     * @param trafficStatistics traffic statistic information
     * @param detectedProtos detected protocols statistic information
     * @param knownFlows known flows
     * @param unknownFlows unknown flows
     */
    public DpiStatInfo(TrafficStatInfo trafficStatistics,
                       List<ProtocolStatInfo> detectedProtos,
                       List<FlowStatInfo> knownFlows,
                       List<FlowStatInfo> unknownFlows) {
        this.trafficStatistics = trafficStatistics;
        this.detectedProtos = detectedProtos;
        this.knownFlows = knownFlows;
        this.unknownFlows = unknownFlows;
    }

    /**
     * Returns DPI traffic statistic information.
     *
     * @return trafficStatistics
     */
    public TrafficStatInfo trafficStatistics() {
        return trafficStatistics;
    }

    /**
     * Returns DPI detected protocols statistic information.
     *
     * @return detectedProtos
     */
    public List<ProtocolStatInfo> detectedProtos() {
        return detectedProtos;
    }

    /**
     * Returns DPI known flows.
     *
     * @return knownFlows
     */
    public List<FlowStatInfo> knownFlows() {
        return knownFlows;
    }

    /**
     * Returns DPI unknown flows.
     *
     * @return unknownFlows
     */
    public List<FlowStatInfo> unknownFlows() {
        return unknownFlows;
    }

    /**
     * Sets the traffic statistic information.
     *
     * @param trafficStatistics traffic statistics
     */
    public void setTrafficStatistics(TrafficStatInfo trafficStatistics) {
        this.trafficStatistics = trafficStatistics;
    }

    /**
     * Sets the detected protocols statistic information.
     *
     * @param detectedProtos detected protocols statistics
     */
    public void setDetectedProtos(List<ProtocolStatInfo> detectedProtos) {
        this.detectedProtos = detectedProtos;
    }

    /**
     * Sets the known flows information.
     *
     * @param knownFlows known flows
     */
    public void setKnownFlows(List<FlowStatInfo> knownFlows) {
        this.knownFlows = knownFlows;
    }

    /**
     * Sets the unknown flows information.
     *
     * @param unknownFlows unknown flows
     */
    public void setUnknownFlows(List<FlowStatInfo> unknownFlows) {
        this.unknownFlows = unknownFlows;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("trafficStatistics", trafficStatistics)
                .add("detectedProtos", detectedProtos)
                .add("knownFlows", knownFlows)
                .add("unknownFlows", unknownFlows)
                .toString();
    }
}
