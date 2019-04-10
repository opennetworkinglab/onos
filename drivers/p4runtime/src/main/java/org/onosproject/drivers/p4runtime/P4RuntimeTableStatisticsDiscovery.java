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
package org.onosproject.drivers.p4runtime;

import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.TableStatisticsDiscovery;
import org.onosproject.net.flow.DefaultTableStatisticsEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.IndexTableId;
import org.onosproject.net.flow.TableId;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverUtils.getInterpreter;

/**
 * Implementation of behaviour TableStatisticsDiscovery for P4Runtime.
 */
public class P4RuntimeTableStatisticsDiscovery extends AbstractP4RuntimeHandlerBehaviour
        implements TableStatisticsDiscovery {

    @Override
    public List<TableStatisticsEntry> getTableStatistics() {
        if (!setupBehaviour("getTableStatistics()")) {
            return Collections.emptyList();
        }
        FlowRuleService flowService = handler().get(FlowRuleService.class);
        PiPipelineInterpreter interpreter = getInterpreter(handler());
        PiPipelineModel model = pipeconf.pipelineModel();
        List<TableStatisticsEntry> tableStatsList;

        List<FlowEntry> rules = newArrayList(flowService.getFlowEntries(deviceId));
        Map<PiTableId, Integer> piTableFlowCount = piFlowRuleCounting(model, interpreter, rules);
        Map<PiTableId, Long> piTableMatchCount = piMatchedCounting(model, interpreter, rules);
        tableStatsList = generatePiFlowTableStatistics(piTableFlowCount, piTableMatchCount, model, deviceId);

        return tableStatsList;
    }

    /**
     * Returns the number of added flows in each table.
     *
     * @param model pipeline model
     * @param interpreter pipeline interpreter
     * @param rules flow rules in this device
     * @return hashmap containing matched packet counting for each table
     */
    private Map<PiTableId, Integer> piFlowRuleCounting(PiPipelineModel model, PiPipelineInterpreter interpreter,
                                                       List<FlowEntry> rules) {
        Map<PiTableId, Integer> piTableFlowCount = new HashMap<>();
        for (PiTableModel tableModel : model.tables()) {
            piTableFlowCount.put(tableModel.id(), 0);
        }
        for (FlowEntry f : rules) {
            if (f.state() == FlowEntry.FlowEntryState.ADDED) {
                PiTableId piTableId = getPiTableId(f, interpreter);
                if (piTableId != null) {
                    piTableFlowCount.put(piTableId, piTableFlowCount.get(piTableId) + 1);
                }
            }
        }
        return piTableFlowCount;
    }

    /**
     * Returns the number of matched packets for each table.
     *
     * @param model pipeline model
     * @param interpreter pipeline interpreter
     * @param rules flow rules in this device
     * @return hashmap containing flow rule counting for each table
     */
    private Map<PiTableId, Long> piMatchedCounting(PiPipelineModel model, PiPipelineInterpreter interpreter,
                                                   List<FlowEntry> rules) {
        Map<PiTableId, Long> piTableMatchCount = new HashMap<>();
        for (PiTableModel tableModel : model.tables()) {
            piTableMatchCount.put(tableModel.id(), (long) 0);
        }
        for (FlowEntry f : rules) {
            if (f.state() == FlowEntry.FlowEntryState.ADDED) {
                PiTableId piTableId = getPiTableId(f, interpreter);
                if (piTableId != null) {
                    piTableMatchCount.put(piTableId, piTableMatchCount.get(piTableId) + f.packets());
                }
            }
        }
        return piTableMatchCount;
    }

    /**
     * Returns the PiTableId of the pipeline independent table that contains the flow rule. If null is returned, it
     * means that the given flow rule's table ID is index table ID without a mapping with a pipeline independent table
     * ID.
     *
     * @param flowEntry flow rule
     * @param interpreter pipeline interpreter
     * @return PiTableId of the table containing input FlowEntry or null
     */
    private PiTableId getPiTableId(FlowEntry flowEntry, PiPipelineInterpreter interpreter) {
        return flowEntry.table().type() == TableId.Type.PIPELINE_INDEPENDENT ?  (PiTableId) flowEntry.table() :
                interpreter.mapFlowRuleTableId(((IndexTableId) flowEntry.table()).id()).orElse(null);
    }

    /**
     * Returns the list of table statistics for P4 switch.
     *
     * @param piTableFlowCount hashmap containing the number of flow rules for each table
     * @param piTableMatchCount hashmap containing the number of matched packets for each table
     * @param model pipeline model
     * @param deviceId device ID
     * @return list of table statistics for P4 switch
     */
    private List<TableStatisticsEntry> generatePiFlowTableStatistics(Map<PiTableId, Integer> piTableFlowCount,
                                                                       Map<PiTableId, Long> piTableMatchCount,
                                                                       PiPipelineModel model, DeviceId deviceId) {
        List<TableStatisticsEntry> tableStatsList;
        Iterator it = piTableFlowCount.entrySet().iterator();
        tableStatsList = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            TableStatisticsEntry tableStat = DefaultTableStatisticsEntry.builder()
                    .withDeviceId(deviceId)
                    .withTableId((PiTableId) pair.getKey())
                    .withActiveFlowEntries(piTableFlowCount.get(pair.getKey()))
                    .withPacketsMatchedCount(piTableMatchCount.get(pair.getKey()))
                    .withMaxSize(model.table((PiTableId) pair.getKey()).get().maxSize()).build();
            tableStatsList.add(tableStat);
            it.remove();
        }
        return tableStatsList;
    }
}
