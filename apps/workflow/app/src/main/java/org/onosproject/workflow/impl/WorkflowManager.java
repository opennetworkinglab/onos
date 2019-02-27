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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.workflow.api.WorkflowService;
import org.onosproject.workflow.api.WorkflowExecutionService;
import org.onosproject.workflow.api.WorkplaceStore;
import org.onosproject.workflow.api.WorkflowStore;
import org.onosproject.workflow.api.WorkplaceDescription;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.DefaultWorkplace;
import org.onosproject.workflow.api.JsonDataModelTree;
import org.onosproject.workflow.api.WorkflowDescription;
import org.onosproject.workflow.api.Workplace;
import org.onosproject.workflow.api.WorkflowDataModelException;
import org.onosproject.workflow.api.Workflow;
import org.onosproject.workflow.api.Worklet;
import org.onosproject.workflow.api.WorkflowContext;
import org.onosproject.workflow.api.JsonDataModel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, service = WorkflowService.class)
public class WorkflowManager implements WorkflowService {

    protected static final Logger log = getLogger(WorkflowManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private WorkflowExecutionService workflowExecutionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected WorkplaceStore workplaceStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected WorkflowStore workflowStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigRegistry networkConfigRegistry;

    private WorkflowNetConfigListener netcfgListener;

    @Activate
    public void activate() {
        netcfgListener = new WorkflowNetConfigListener(this);
        networkConfigRegistry.registerConfigFactory(netcfgListener.getConfigFactory());
        networkConfigService.addListener(netcfgListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        networkConfigService.removeListener(netcfgListener);
        networkConfigRegistry.unregisterConfigFactory(netcfgListener.getConfigFactory());
        log.info("Stopped");
    }

    @Override
    public void createWorkplace(WorkplaceDescription wpDesc) throws WorkflowException {
        log.info("createWorkplace: {}", wpDesc);

        JsonNode root;
        if (wpDesc.data().isPresent()) {
            root = wpDesc.data().get();
        } else {
            root = JsonNodeFactory.instance.objectNode();
        }
        DefaultWorkplace workplace =
                new DefaultWorkplace(wpDesc.name(), new JsonDataModelTree(root));
        workplaceStore.registerWorkplace(wpDesc.name(), workplace);
    }

    @Override
    public void removeWorkplace(WorkplaceDescription wpDesc) throws WorkflowException {
        log.info("removeWorkplace: {}", wpDesc);
        //TODO: Removing workflows belong to this workplace
        workplaceStore.removeWorkplace(wpDesc.name());
    }

    @Override
    public void clearWorkplace() throws WorkflowException {
        log.info("clearWorkplace");
        workplaceStore.getWorkplaces().stream()
                .filter(wp -> !Objects.equals(wp.name(), Workplace.SYSTEM_WORKPLACE))
                .forEach(wp -> workplaceStore.removeWorkplace(wp.name()));
    }

    @Override
    public void invokeWorkflow(WorkflowDescription wfDesc) throws WorkflowException {
        invokeWorkflow(wfDesc.toJson());
    }

    @Override
    public void invokeWorkflow(JsonNode worklowDescJson) throws WorkflowException {
        log.info("invokeWorkflow: {}", worklowDescJson);
        Workplace workplace = workplaceStore.getWorkplace(Workplace.SYSTEM_WORKPLACE);
        if (Objects.isNull(workplace)) {
            throw new WorkflowException("Invalid system workplace");
        }

        Workflow workflow = workflowStore.get(URI.create(worklowDescJson.get("id").asText()));
        if (Objects.isNull(workflow)) {
            throw new WorkflowException("Invalid Workflow");
        }

        if (!checkWorkflowSchema(workflow, worklowDescJson)) {
            throw new WorkflowException("Invalid Workflow " + worklowDescJson.get("id").asText());
        }

        Workflow wfCreationWf = workflowStore.get(URI.create(WorkplaceWorkflow.WF_CREATE_WORKFLOW));
        if (Objects.isNull(wfCreationWf)) {
            throw new WorkflowException("Invalid workflow " + WorkplaceWorkflow.WF_CREATE_WORKFLOW);
        }

        WorkflowContext context = wfCreationWf.buildSystemContext(workplace, new JsonDataModelTree(worklowDescJson));
        workflowExecutionService.execInitWorklet(context);
    }

    /**
     * Checks if the type of worklet is same as that of wfdesc Json.
     *
     * @param workflow workflow
     * @param jsonNode jsonNode
     * @throws WorkflowException workflow exception
     */

    private boolean checkWorkflowSchema(Workflow workflow, JsonNode jsonNode) throws WorkflowException {

        Map<String, Map<String, String>> workletDataTypeMap = new HashMap<>();
        for (String workletType : workflow.getWorkletTypeList()) {
            Map<String, String> jsonDataModelMap = new HashMap<>();
            if (Objects.equals(workletType, Worklet.Common.INIT.tag())
                    || (Objects.equals(workletType, Worklet.Common.COMPLETED.tag()))) {
                continue;
            }
            Worklet worklet = workflow.getWorkletInstance(workletType);
            Class cls = worklet.getClass();
            for (Field field : cls.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                Annotation[] annotations = field.getAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation instanceof JsonDataModel) {
                        JsonDataModel jsonDataModel = (JsonDataModel) annotation;
                        Matcher matcher = Pattern.compile("(\\w+)").matcher(jsonDataModel.path());
                        if (!matcher.find()) {
                            throw new WorkflowException("Invalid Json Data Model Path");
                        }
                        String path = matcher.group(1);
                        if (checkJsonNodeDataType(jsonNode, field, path)) {
                            jsonDataModelMap.put(path, field.getType().getName());
                        }
                    }
                }
            }
            if (!jsonDataModelMap.isEmpty()) {
                workletDataTypeMap.put(worklet.tag(), jsonDataModelMap);
            }

        }
        if (!workletDataTypeMap.isEmpty()) {
            throw new WorkflowDataModelException("invalid workflow ", workflow.id().toString(), workletDataTypeMap);
        }
        return true;
    }


    private boolean checkJsonNodeDataType(JsonNode jsonNode, Field field, String path) throws WorkflowException {
        if (!Objects.nonNull(jsonNode.get("data")) && !Objects.nonNull(jsonNode.get("data").get(path))) {
            throw new WorkflowException("Invalid Json");
        }
        JsonNodeType jsonNodeType = jsonNode.get("data").get(path).getNodeType();
        if (jsonNodeType != null) {
            switch (jsonNodeType) {
                case NUMBER:
                    if (!(field.getType().isAssignableFrom(Integer.class))) {
                        return true;
                    }
                    break;
                case STRING:
                    if (!(field.getType().isAssignableFrom(String.class))) {
                        return true;
                    }
                    break;
                case OBJECT:
                    if (!(field.getType().isAssignableFrom(Objects.class))) {
                        return true;
                    }
                    break;
                case BOOLEAN:
                    if (!(field.getType().isAssignableFrom(Boolean.class))) {
                        return true;
                    }
                    break;
                case ARRAY:
                    if (!(field.getType().isAssignableFrom(Arrays.class))) {
                        return true;
                    }
                    break;
                default:
                    return true;
            }
        } else {
            return false;

        }
        return false;
    }

    @Override
    public void terminateWorkflow(WorkflowDescription wfDesc) throws WorkflowException {
        log.info("terminateWorkflow: {}", wfDesc);
        if (Objects.nonNull(workplaceStore.getContext(wfDesc.workflowContextName()))) {
            workplaceStore.removeContext(wfDesc.workflowContextName());
        }
    }
}