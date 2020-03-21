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
package org.onosproject.workflow.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.event.Event;
import org.onosproject.workflow.api.AbstractWorklet;
import org.onosproject.workflow.api.DefaultWorkflowContext;
import org.onosproject.workflow.api.DefaultWorkplace;
import org.onosproject.workflow.api.ImmutableListWorkflow;
import org.onosproject.workflow.api.JsonDataModelTree;
import org.onosproject.workflow.api.SystemWorkflowContext;
import org.onosproject.workflow.api.Workflow;
import org.onosproject.workflow.api.WorkflowAttribute;
import org.onosproject.workflow.api.WorkflowContext;
import org.onosproject.workflow.api.WorkflowData;
import org.onosproject.workflow.api.WorkflowDataEvent;
import org.onosproject.workflow.api.DefaultWorkflowDescription;
import org.onosproject.workflow.api.WorkflowDescription;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.WorkflowExecutionService;
import org.onosproject.workflow.api.WorkflowStore;
import org.onosproject.workflow.api.Workplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Objects;

import static org.onosproject.workflow.api.WorkflowDataEvent.Type.INSERT;

public class WorkplaceWorkflow {

    private static final Logger log = LoggerFactory.getLogger(WorkplaceWorkflow.class);

    private WorkflowExecutionService workflowExecutionService;
    private WorkflowStore workflowStore;

    public WorkplaceWorkflow(WorkflowExecutionService workflowExecutionService,
                             WorkflowStore workflowStore) {
        this.workflowExecutionService = workflowExecutionService;
        this.workflowStore = workflowStore;
    }

    public static final String WF_CREATE_WORKFLOW = "workplace.create-workflow";

    public void registerWorkflows() {

        Workflow workflow = ImmutableListWorkflow.builder()
                .id(URI.create(WF_CREATE_WORKFLOW))
                .attribute(WorkflowAttribute.REMOVE_AFTER_COMPLETE)
                .init(ChangeDistributor.class.getName())
                .chain(CreateWorkplace.class.getName())
                .chain(CreateWorkflowContext.class.getName())
                .build();
        workflowStore.register(workflow);
    }

    public abstract static class AbsWorkflowWorklet extends AbstractWorklet {

        protected WorkflowDescription getWorkflowDesc(WorkflowContext context) throws WorkflowException {

            JsonNode root = ((JsonDataModelTree) context.data()).root();
            return DefaultWorkflowDescription.valueOf(root);
        }
    }

    public static class ChangeDistributor extends AbsWorkflowWorklet {

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            String workplaceName = getWorkflowDesc(context).workplaceName();
            // Sets workflow job distribution hash value to make this workflow to be executed on the
            // same cluster node to execute workplace tasks.
            ((SystemWorkflowContext) context).setDistributor(workplaceName);

            context.completed();
        }
    }

    public static class CreateWorkplace extends AbsWorkflowWorklet {

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            WorkflowDescription wfDesc = getWorkflowDesc(context);

            Workplace workplace = context.workplaceStore().getWorkplace(wfDesc.workplaceName());
            return Objects.isNull(workplace);
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            WorkflowDescription wfDesc = getWorkflowDesc(context);

            // creates workplace with empty data model
            DefaultWorkplace workplace = new DefaultWorkplace(wfDesc.workplaceName(), new JsonDataModelTree());
            log.info("registerWorkplace {}", workplace);
            context.waitCompletion(WorkflowDataEvent.class, wfDesc.workplaceName(),
                    () -> context.workplaceStore().registerWorkplace(wfDesc.workplaceName(), workplace),
                    60000L
            );
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event)throws WorkflowException {

            if (!(event instanceof WorkflowDataEvent)) {
                return false;
            }

            WorkflowDataEvent wfEvent = (WorkflowDataEvent) event;
            WorkflowData wfData = wfEvent.subject();

            WorkflowDescription wfDesc = getWorkflowDesc(context);

            if (wfData instanceof Workplace
                    && Objects.equals(wfData.name(), wfDesc.workplaceName())
                    && wfEvent.type() == INSERT) {
                log.info("isCompleted(true): event:{}, context:{}, workplace:{}",
                        event, context, wfDesc.workplaceName());
                return true;
            } else {
                log.info("isCompleted(false) event:{}, context:{}, workplace:{}",
                        event, context, wfDesc.workplaceName());
                return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete the job of worklet by timeout
            } else {
                super.timeout(context);
            }
        }
    }

    public static class CreateWorkflowContext extends AbsWorkflowWorklet {

        private static final String SUBMITTED = "submitted";

        private boolean isSubmitted(WorkflowContext context) throws WorkflowException {
            JsonNode node = ((JsonDataModelTree) context.data()).nodeAt("/" + SUBMITTED);
            if (!(node instanceof BooleanNode)) {
                return false;
            }
            return node.asBoolean();
        }

        private void submitTrue(WorkflowContext context) throws WorkflowException {
            JsonNode root = ((JsonDataModelTree) context.data()).root();
            if (!(root instanceof ObjectNode)) {
                throw new WorkflowException("Invalid root node for " + context);
            }
            ((ObjectNode) root).put(SUBMITTED, true);
        }

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            WorkflowDescription wfDesc = getWorkflowDesc(context);

            String contextName = DefaultWorkflowContext.nameBuilder(wfDesc.id(), wfDesc.workplaceName());
            if (Objects.isNull(context.workplaceStore().getContext(contextName))) {
                return (!isSubmitted(context));
            } else {
                return false;
            }
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            WorkflowDescription wfDesc = getWorkflowDesc(context);

            Workplace workplace = context.workplaceStore().getWorkplace(wfDesc.workplaceName());
            if (Objects.isNull(workplace)) {

                log.error("Failed to find workplace with " + wfDesc.workplaceName());
                throw new WorkflowException("Failed to find workplace with " + wfDesc.workplaceName());
            }

            Workflow workflow = context.workflowStore().get(wfDesc.id());
            if (Objects.isNull(workflow)) {
                throw new WorkflowException("Failed to find workflow with " + wfDesc.id());
            }

            //String contextName = DefaultWorkflowContext.nameBuilder(wfDesc.id(), wfDesc.workplaceName());
            String contextName = wfDesc.workflowContextName();
            if (Objects.nonNull(context.workplaceStore().getContext(contextName))) {
                throw new WorkflowException(contextName + " exists already");
            }

            JsonDataModelTree subTree = new JsonDataModelTree(wfDesc.data());
            WorkflowContext buildingContext = workflow.buildContext(workplace, subTree);
            log.info("registerContext {}", buildingContext.name());
            context.workplaceStore().registerContext(buildingContext.name(), buildingContext);
            submitTrue(context);

            context.completed();
        }
    }
}
