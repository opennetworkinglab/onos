/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.flowanalyzer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.Link;
import org.onosproject.net.topology.TopologyVertex;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple flow space analyzer app.
 */
@Component(immediate = true)
@Service(value = FlowAnalyzer.class)
public class FlowAnalyzer {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Activate
    public void activate(ComponentContext context) {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    TopologyGraph graph;
    Map<FlowEntry, String> label = new HashMap<>();
    Set<FlowEntry> ignoredFlows = new HashSet<>();

    /**
     * Analyzes and prints out a report on the status of every flow entry inside
     * the network. The possible states are: Cleared (implying that the entry leads to
     * a host), Cycle (implying that it is part of cycle), and Black Hole (implying
     * that the entry does not lead to a single host).
     *
     * @return result string
     */
    public String analyze() {
        graph = topologyService.getGraph(topologyService.currentTopology());
        for (TopologyVertex v: graph.getVertexes()) {
            DeviceId srcDevice = v.deviceId();
            Iterable<FlowEntry> flowTable = flowRuleService.getFlowEntries(srcDevice);
            for (FlowEntry flow: flowTable) {
                dfs(flow);
            }
        }

        //analyze the cycles to look for "critical flows" that can be removed
        //to break the cycle
        Set<FlowEntry> critpts = new HashSet<>();
        for (FlowEntry flow: label.keySet()) {
            if ("Cycle".equals(label.get(flow))) {
                Map<FlowEntry, String> labelSaved = label;
                label = new HashMap<FlowEntry, String>();
                ignoredFlows.add(flow);
                for (TopologyVertex v: graph.getVertexes()) {
                    DeviceId srcDevice = v.deviceId();
                    Iterable<FlowEntry> flowTable = flowRuleService.getFlowEntries(srcDevice);
                    for (FlowEntry flow1: flowTable) {
                        dfs(flow1);
                    }
                }

                boolean replacable = true;
                for (FlowEntry flow2: label.keySet()) {
                    if ("Cleared".equals(labelSaved.get(flow2)) && !("Cleared".equals(label.get(flow2)))) {
                        replacable = false;
                    }
                }
                if (replacable) {
                    critpts.add(flow);
                }
                label = labelSaved;
            }
        }

        for (FlowEntry flow: critpts) {
            label.put(flow, "Cycle Critical Point");
        }

        String s = "\n";
        for (FlowEntry flow: label.keySet()) {
            s += ("Flow Rule: " + flowEntryRepresentation(flow) + "\n");
            s += ("Analysis: " + label.get(flow) + "!\n\n");
        }
        s += ("Analyzed " + label.keySet().size() + " flows.");
        //log.info(s);
        return s;
    }

    public Map<FlowEntry, String> calcLabels() {
        analyze();
        return label;
    }
    public String analysisOutput()   {
        analyze();
        String s = "\n";
        for (FlowEntry flow: label.keySet()) {
            s += ("Flow Rule: " + flowEntryRepresentation(flow) + "\n");
            s += ("Analysis: " + label.get(flow) + "!\n\n");
        }
        return s;
    }

    private boolean dfs(FlowEntry flow) {
        if (ignoredFlows.contains(flow)) {
            return false;
        }
        if ("Cycle".equals(label.get(flow)) ||
                "Black Hole".equals(label.get(flow)) ||
                "Cleared".equals(label.get(flow)) ||
                 "NA".equals(label.get(flow)) ||
                "Cycle Critical Point".equals(label.get(flow))) {

            // This flow has already been analyzed and there is no need to analyze it further
            return !"Black Hole".equals(label.get(flow));
        }

        if ("Visiting".equals(label.get(flow))) {
            //you've detected a cycle because you reached the same entry again during your dfs
            //let it continue so you can label the whole cycle
            label.put(flow, "Cycle");
        } else {
            //otherwise, mark off the current flow entry as currently being visited
            label.put(flow, "Visiting");
        }

        boolean pointsToLiveEntry = false;

        List<Instruction> instructions = flow.treatment().allInstructions();
        for (Instruction i: instructions) {
            if (i instanceof Instructions.OutputInstruction) {
                pointsToLiveEntry |= analyzeInstruction(i, flow);
            }
            if ("NA".equals(label.get(flow))) {
                return pointsToLiveEntry;
            }
        }

        if (!pointsToLiveEntry) {
            //this entry does not point to any "live" entries thus must be a black hole
            label.put(flow, "Black Hole");
        } else if ("Visiting".equals(label.get(flow))) {
            //the flow is not in a cycle or in a black hole
            label.put(flow, "Cleared");
        }
        return pointsToLiveEntry;
    }

    private boolean analyzeInstruction(Instruction i, FlowEntry flow) {
        boolean pointsToLiveEntry = false;
        Instructions.OutputInstruction output = (Instructions.OutputInstruction) i;
        PortNumber port = output.port();
        PortNumber outPort = null;

        DeviceId egress = null;
        boolean hasHost = false;

        ConnectPoint portPt = new ConnectPoint(flow.deviceId(), port);
        for (Link l: linkService.getEgressLinks(portPt)) {
            if (l.dst().elementId() instanceof DeviceId) {
                egress = l.dst().deviceId();
                outPort = l.dst().port();
            } else if (l.dst().elementId() instanceof HostId) {
                //the port leads to a host: therefore it is not a dead link
                pointsToLiveEntry = true;
                hasHost = true;
            }
        }
        if (!topologyService.isInfrastructure(topologyService.currentTopology(), portPt) && egress == null) {
            pointsToLiveEntry = true;
            hasHost = true;
        }
        if (hasHost) {
            return pointsToLiveEntry;
        }
        if (egress == null) {
            //the port that the flow instructions tells you to send the packet
            //to doesn't exist or is a controller port
            label.put(flow, "NA");
            return pointsToLiveEntry;
        }

        Iterable<FlowEntry> dstFlowTable = flowRuleService.getFlowEntries(egress);

        Set<Criterion> flowCriteria = flow.selector().criteria();

        //filter the criteria in order to remove port dependency
        Set<Criterion> filteredCriteria = new HashSet<>();
        for (Criterion criterion : flowCriteria) {
            if (!(criterion instanceof PortCriterion)) {
                filteredCriteria.add(criterion);
            }
        }

        //ensure that the in port is equal to the port that it is coming in from
        filteredCriteria.add(Criteria.matchInPort(outPort));

        for (FlowEntry entry: dstFlowTable) {
            if (ignoredFlows.contains(entry)) {
                continue;
            }
            if (filteredCriteria.containsAll(entry.selector().criteria())) {
                dfs(entry);

                if (!"Black Hole".equals(label.get(entry))) {
                    //this entry is "live" i.e not a black hole
                    pointsToLiveEntry = true;
                }
            }
        }
        return pointsToLiveEntry;
    }
    public String flowEntryRepresentation(FlowEntry flow) {
        return "Device: " + flow.deviceId() + ", " + flow.selector().criteria() + ", " + flow.treatment().immediate();
    }
}
