/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onlab.stc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.onlab.stc.MonitorLayout.Box;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static org.onlab.stc.Coordinator.Status.IN_PROGRESS;

/**
 * Scenario test monitor.
 */
public class Monitor implements StepProcessListener {

    private final ObjectMapper mapper = new ObjectMapper();

    private final Coordinator coordinator;
    private final Compiler compiler;
    private final MonitorLayout layout;

    private MonitorDelegate delegate;

    private Map<Step, Box> boxes = Maps.newHashMap();

    /**
     * Creates a new shared process flow monitor.
     *
     * @param coordinator process flow coordinator
     * @param compiler    scenario compiler
     */
    Monitor(Coordinator coordinator, Compiler compiler) {
        this.coordinator = coordinator;
        this.compiler = compiler;
        this.layout = new MonitorLayout(compiler);
        coordinator.addListener(this);
    }

    /**
     * Sets the process monitor delegate.
     *
     * @param delegate process monitor delegate
     */
    void setDelegate(MonitorDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Notifies the process monitor delegate with the specified event.
     *
     * @param event JSON event data
     */
    public void notify(ObjectNode event) {
        if (delegate != null) {
            delegate.notify(event);
        }
    }

    /**
     * Returns the scenario process flow as JSON data.
     *
     * @return scenario process flow data
     */
    ObjectNode scenarioData() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode steps = mapper.createArrayNode();
        ArrayNode requirements = mapper.createArrayNode();

        ProcessFlow pf = compiler.processFlow();
        pf.getVertexes().forEach(step -> add(step, steps));
        pf.getEdges().forEach(requirement -> add(requirement, requirements));

        root.set("steps", steps);
        root.set("requirements", requirements);

        try (FileWriter fw = new FileWriter("/tmp/data.json");
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(root.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }


    private void add(Step step, ArrayNode steps) {
        Box box = layout.get(step);
        ObjectNode sn = mapper.createObjectNode()
                .put("name", step.name())
                .put("isGroup", step instanceof Group)
                .put("status", status(coordinator.getStatus(step)))
                .put("tier", box.tier())
                .put("depth", box.depth());
        if (step.group() != null) {
            sn.put("group", step.group().name());
        }
        steps.add(sn);
    }

    private String status(Coordinator.Status status) {
        return status.toString().toLowerCase();
    }

    private void add(Dependency requirement, ArrayNode requirements) {
        ObjectNode rn = mapper.createObjectNode();
        rn.put("src", requirement.src().name())
                .put("dst", requirement.dst().name())
                .put("isSoft", requirement.isSoft());
        requirements.add(rn);
    }

    @Override
    public void onStart(Step step, String command) {
        notify(event(step, status(IN_PROGRESS)));
    }

    @Override
    public void onCompletion(Step step, Coordinator.Status status) {
        notify(event(step, status(status)));
    }

    @Override
    public void onOutput(Step step, String line) {

    }

    private ObjectNode event(Step step, String status) {
        ObjectNode event = mapper.createObjectNode()
                .put("name", step.name())
                .put("status", status);
        return event;
    }

}
