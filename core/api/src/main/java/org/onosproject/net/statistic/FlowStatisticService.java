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

package org.onosproject.net.statistic;

import java.util.List;
import java.util.Map;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.instructions.Instruction;

/**
 * Service for obtaining individual flow statistic information about device and link in the system.
 * Basic statistics are obtained from the StatisticService
 */
public interface FlowStatisticService {

    /**
     * Obtain the summary load list for the device with the given link.
     *
     * @param device the Device  to query.
     * @return map of summary flow entry load
     */
    Map<ConnectPoint, SummaryFlowEntryWithLoad> loadSummary(Device device);

    /**
     * Obtain the summary load for the device with the given link or port.
     *
     * @param device the Device to query.
     * @param pNumber the port number to query.
     * @return summary flow entry load
     */
    SummaryFlowEntryWithLoad loadSummary(Device device, PortNumber pNumber);

    /**
     * Obtain the set of the flow type and load list for the device with the given link.
     *
     * @param device the Device  to query.
     * @param liveType the FlowLiveType  to filter, null means no filtering .
     * @param instType the InstructionType to filter, null means no filtering.
     * @return map of flow entry load
     */
    Map<ConnectPoint, List<FlowEntryWithLoad>> loadAllByType(Device device,
                                                             FlowEntry.FlowLiveType liveType,
                                                             Instruction.Type instType);

    /**
     * Obtain the flow type and load list for the device with the given link or port.
     *
     * @param device the Device to query.
     * @param pNumber the port number of the Device to query
     * @param liveType the FlowLiveType  to filter, null means no filtering .
     * @param instType the InstructionType to filter, null means no filtering.
     * @return list of flow entry load
     */
    List<FlowEntryWithLoad> loadAllByType(Device device,
                                          PortNumber pNumber,
                                          FlowEntry.FlowLiveType liveType,
                                          Instruction.Type instType);

    /**
     * Obtain the set of the flow type and load topn list for the device with the given link.
     *
     * @param device the Device  to query.
     * @param liveType the FlowLiveType  to filter, null means no filtering .
     * @param instType the InstructionType to filter, null means no filtering.
     * @param topn the top number to filter, null means no filtering.
     * @return map of flow entry load
     */
    Map<ConnectPoint, List<FlowEntryWithLoad>> loadTopnByType(Device device,
                                                              FlowEntry.FlowLiveType liveType,
                                                              Instruction.Type instType,
                                                              int topn);

    /**
     * Obtain the flow type and load topn list for the device with the given link or port.
     *
     * @param device the Device  to query.
     * @param pNumber the port number of the Device to query
     * @param liveType the FlowLiveType  to filter, null means no filtering .
     * @param instType the InstructionType to filter, null means no filtering.
     * @param topn the top n list entry
     * @return list of flow entry load
     */
    List<FlowEntryWithLoad> loadTopnByType(Device device,
                                           PortNumber pNumber,
                                           FlowEntry.FlowLiveType liveType,
                                           Instruction.Type instType,
                                           int topn);
}


