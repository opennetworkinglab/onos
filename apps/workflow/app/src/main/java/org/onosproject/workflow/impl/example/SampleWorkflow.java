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
package org.onosproject.workflow.impl.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.device.DeviceService;
import org.onosproject.workflow.api.AbstractWorklet;
import org.onosproject.workflow.api.DataModelTree;
import org.onosproject.workflow.api.ImmutableListWorkflow;
import org.onosproject.workflow.api.JsonDataModel;
import org.onosproject.workflow.api.JsonDataModelTree;
import org.onosproject.workflow.api.Workflow;
import org.onosproject.workflow.api.WorkflowContext;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.WorkflowService;
import org.onosproject.workflow.api.WorkflowStore;
import org.onosproject.workflow.api.StaticDataModel;
import org.onosproject.workflow.api.DefaultWorkletDescription;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Class for sample workflow.
 */
@Component(immediate = true)
public class SampleWorkflow {

    private static final Logger log = LoggerFactory.getLogger(SampleWorkflow.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected WorkflowStore workflowStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected WorkflowService workflowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;


    @Activate
    public void activate() {
        log.info("Activated");

        try {
            registerWorkflows();
        } catch (WorkflowException e) {
            log.error("invalid workflow");
        }
    }

    @Deactivate
    public void deactivate() {
        log.info("Deactivated");
    }

    /**
     * Registers example workflows.
     *
     * @throws WorkflowException wfex
     */
    public void registerWorkflows() throws WorkflowException {
        // registering class-loader
        workflowStore.registerLocal(this.getClass().getClassLoader());

        // registering new workflow definition
        URI uri = URI.create("sample.workflow-0");
        Workflow workflow = null;
        workflow = ImmutableListWorkflow.builder()
                .id(uri)
                .chain(SampleWorklet1.class.getName())
                .chain(SampleWorklet2.class.getName())
                .chain(SampleWorklet3.class.getName())
                .chain(SampleWorklet4.class.getName())
                .chain(SampleWorklet5.class.getName())
                .build();
        workflowService.register(workflow);

        // registering new workflow definition
        uri = URI.create("sample.workflow-1");
        workflow = ImmutableListWorkflow.builder()
                .id(uri)
                .chain(SampleWorklet3.class.getName())
                .chain(SampleWorklet2.class.getName())
                .chain(SampleWorklet1.class.getName())
                .chain(SampleWorklet4.class.getName())
                .chain(SampleWorklet5.class.getName())
                .build();
        workflowService.register(workflow);

        // registering new workflow definition
        uri = URI.create("sample.workflow-2");
        workflow = ImmutableListWorkflow.builder()
                .id(uri)
                .chain(SampleWorklet1.class.getName())
                .chain(SampleWorklet3.class.getName())
                .chain(SampleWorklet2.class.getName())
                .chain(SampleWorklet4.class.getName())
                .chain(SampleWorklet5.class.getName())
                .build();
        workflowService.register(workflow);

        // registering new workflow definition
        uri = URI.create("sample.workflow-invalid-datamodel-type");
        workflow = ImmutableListWorkflow.builder()
                .id(uri)
                .chain(SampleWorklet6.class.getName())
                .build();
        workflowService.register(workflow);

        // registering new workflow definition
        uri = URI.create("sample.workflow-static-datamodel");
        workflow = ImmutableListWorkflow.builder()
                .id(uri)
                .chain(DefaultWorkletDescription.builder()
                        .name(SampleWorklet6.class.getName())
                        .staticDataModel("/sample", "value")
                        .build())
                .build();
        workflowService.register(workflow);
    }

    /**
     * Abstract class for sample worklet.
     */
    public abstract static class AbsSampleWorklet extends AbstractWorklet {

        protected static final String MODEL_SAMPLE_JOB = "/sample/job";
        protected static final String MODEL_COUNT = "/count";

        /**
         * Constructor for sample worklet.
         */
        protected AbsSampleWorklet() {

        }


        /**
         * Allocates or gets data model.
         *
         * @param context workflow context
         * @return json object node
         * @throws WorkflowException workflow exception
         */
        protected ObjectNode allocOrGetModel(WorkflowContext context) throws WorkflowException {

            JsonDataModelTree tree = (JsonDataModelTree) context.data();
            JsonNode params = tree.root();

            if (params.at(MODEL_SAMPLE_JOB).getNodeType() == JsonNodeType.MISSING) {
                tree.alloc(MODEL_SAMPLE_JOB, DataModelTree.Nodetype.MAP);
            }
            return (ObjectNode) params.at(MODEL_SAMPLE_JOB);
        }

        /**
         * Gets data model.
         *
         * @param context workflow context
         * @return json object node
         * @throws WorkflowException workflow exception
         */
        protected ObjectNode getDataModel(WorkflowContext context) throws WorkflowException {
            DataModelTree tree = context.data();
            return ((JsonDataModelTree) tree.subtree(MODEL_SAMPLE_JOB)).rootObject();
        }

        /**
         * Sleeps for 'ms' milli seconds.
         *
         * @param ms milli seconds to sleep
         */
        protected void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                log.error("Exception: ", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Class for sample worklet-1.
     */
    public static class SampleWorklet1 extends AbsSampleWorklet {

        @JsonDataModel(path = MODEL_COUNT)
        Integer intCount;

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            ObjectNode node = getDataModel(context);
            node.put("work1", "done");
            log.info("workflow-process {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(30);
            context.completed(); //Complete the job of worklet in the process
        }

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {
            ObjectNode node = allocOrGetModel(context);
            log.info("workflow-isNext {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(30);
            return !node.has("work1");

        }
    }

    /**
     * Class for sample worklet-2 (using timeout).
     */
    public static class SampleWorklet2 extends AbsSampleWorklet {

        @JsonDataModel(path = MODEL_COUNT)
        Integer intCount;

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            ObjectNode node = getDataModel(context);
            node.put("work2", "done");
            log.info("workflow-process {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(50);

            intCount++;

            context.waitFor(50L); //Timeout will happen after 50 milli seconds.
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            context.completed(); //Complete the job of worklet by timeout
        }

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {
            ObjectNode node = allocOrGetModel(context);
            log.info("workflow-isNext {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(50);
            return !node.has("work2");
        }

    }

    public static class SampleWorklet3 extends AbsSampleWorklet {

        @JsonDataModel(path = MODEL_COUNT)
        Integer intCount;

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            ObjectNode node = getDataModel(context);
            node.put("work3", "done");
            log.info("workflow-process {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            intCount++;
            context.completed();
        }

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {
            ObjectNode node = allocOrGetModel(context);
            log.info("workflow-isNext {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            return !node.has("work3");
        }
    }

    public static class SampleWorklet4 extends AbsSampleWorklet {

        @JsonDataModel(path = MODEL_COUNT)
        Integer intCount;

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            ObjectNode node = getDataModel(context);
            node.put("work4", "done");
            log.info("workflow-process {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            intCount++;
            context.completed();
        }

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {
            ObjectNode node = allocOrGetModel(context);
            log.info("workflow-isNext {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            return !node.has("work4");
        }
    }

    public static class SampleWorklet5 extends AbsSampleWorklet {

        @JsonDataModel(path = MODEL_COUNT)
        Integer intCount;

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            ObjectNode node = getDataModel(context);
            node.put("work5", "done");
            log.info("workflow-process {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            intCount++;
            context.completed();
        }

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {
            ObjectNode node = allocOrGetModel(context);
            log.info("workflow-isNext {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            return !node.has("work5");
        }
    }

    /**
     * Class for sample worklet-6 to test workflow datamodel exception.
     */
    public static class SampleWorklet6 extends AbsSampleWorklet {

        @JsonDataModel(path = MODEL_COUNT)
        String str;

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            ObjectNode node = getDataModel(context);
            node.put("work6", "done");
            log.info("workflow-process {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            context.completed();
        }

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {
            ObjectNode node = allocOrGetModel(context);
            log.info("workflow-isNext {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            return !node.has("work6");
        }
    }

    /**
     * Class for sample worklet-7 to test workflow datamodel exception.
     */
    public static class SampleWorklet7 extends AbsSampleWorklet {

        @StaticDataModel(path = "/sample")
        String value;

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            ObjectNode node = getDataModel(context);
            node.put("work7", "done");
            log.info("inside worklet - static data model {}", value);
            log.info("workflow-process {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            context.completed();
        }

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {
            ObjectNode node = allocOrGetModel(context);
            log.info("workflow-isNext {}-{}", context.workplaceName(), this.getClass().getSimpleName());
            sleep(10);
            return !node.has("work7");
        }
    }

}
