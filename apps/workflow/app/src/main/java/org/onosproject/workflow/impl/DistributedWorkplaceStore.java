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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.workflow.api.DataModelTree;
import org.onosproject.workflow.api.DefaultWorkplace;
import org.onosproject.workflow.api.DefaultWorkflowContext;
import org.onosproject.workflow.api.JsonDataModelTree;
import org.onosproject.workflow.api.ProgramCounter;
import org.onosproject.workflow.api.SystemWorkflowContext;
import org.onosproject.workflow.api.WorkflowContext;
import org.onosproject.workflow.api.WorkflowData;
import org.onosproject.workflow.api.WorkflowState;
import org.onosproject.workflow.api.Workplace;
import org.onosproject.workflow.api.WorkflowDataEvent;
import org.onosproject.workflow.api.WorkplaceStore;
import org.onosproject.workflow.api.WorkplaceStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, service = WorkplaceStore.class)
public class DistributedWorkplaceStore
    extends AbstractStore<WorkflowDataEvent, WorkplaceStoreDelegate> implements WorkplaceStore {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private ApplicationId appId;
    private final Logger log = getLogger(getClass());

    private final WorkplaceMapListener workplaceMapEventListener = new WorkplaceMapListener();
    private ConsistentMap<String, WorkflowData> workplaceMap;
    private Map<String, Workplace> localWorkplaceMap = Maps.newConcurrentMap();

    private final WorkflowContextMapListener contextMapEventListener = new WorkflowContextMapListener();
    private ConsistentMap<String, WorkflowData> contextMap;
    private Map<String, WorkflowContext> localContextMap = Maps.newConcurrentMap();

    private Map<String, Map<String, WorkflowContext>> localWorkplaceMemberMap = Maps.newConcurrentMap();

    @Activate
    public void activate() {

        appId = coreService.registerApplication("org.onosproject.workplacestore");
        log.info("appId=" + appId);

        KryoNamespace workplaceNamespace = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(WorkflowData.class)
                .register(Workplace.class)
                .register(DefaultWorkplace.class)
                .register(WorkflowContext.class)
                .register(DefaultWorkflowContext.class)
                .register(SystemWorkflowContext.class)
                .register(WorkflowState.class)
                .register(ProgramCounter.class)
                .register(DataModelTree.class)
                .register(JsonDataModelTree.class)
                .register(List.class)
                .register(ArrayList.class)
                .register(JsonNode.class)
                .register(ObjectNode.class)
                .register(TextNode.class)
                .register(LinkedHashMap.class)
                .register(ArrayNode.class)
                .register(BaseJsonNode.class)
                .register(BigIntegerNode.class)
                .register(BinaryNode.class)
                .register(BooleanNode.class)
                .register(ContainerNode.class)
                .register(DecimalNode.class)
                .register(DoubleNode.class)
                .register(FloatNode.class)
                .register(IntNode.class)
                .register(JsonNodeType.class)
                .register(LongNode.class)
                .register(MissingNode.class)
                .register(NullNode.class)
                .register(NumericNode.class)
                .register(POJONode.class)
                .register(ShortNode.class)
                .register(ValueNode.class)
                .register(JsonNodeCreator.class)
                .register(JsonNodeFactory.class)
                .build();

        localWorkplaceMap.clear();
        workplaceMap = storageService.<String, WorkflowData>consistentMapBuilder()
                .withSerializer(Serializer.using(workplaceNamespace))
                .withName("workplace-map")
                .withApplicationId(appId)
                .build();
        workplaceMap.addListener(workplaceMapEventListener);

        localContextMap.clear();
        contextMap = storageService.<String, WorkflowData>consistentMapBuilder()
                .withSerializer(Serializer.using(workplaceNamespace))
                .withName("workflow-context-map")
                .withApplicationId(appId)
                .build();
        contextMap.addListener(contextMapEventListener);

        workplaceMapEventListener.syncLocal();
        contextMapEventListener.syncLocal();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        workplaceMap.destroy();
        localWorkplaceMap.clear();
        contextMap.destroy();
        localContextMap.clear();

        log.info("Stopped");
    }

    @Override
    public void registerWorkplace(String name, Workplace workplace) throws StorageException {
        workplaceMap.put(name, workplace);
    }

    @Override
    public void removeWorkplace(String name) throws StorageException {
        removeWorkplaceContexts(name);
        workplaceMap.remove(name);
    }

    @Override
    public Workplace getWorkplace(String name) throws StorageException {
        return localWorkplaceMap.get(name);
    }

    @Override
    public Collection<Workplace> getWorkplaces() throws StorageException {
        return ImmutableList.copyOf(localWorkplaceMap.values());
    }

    @Override
    public void commitWorkplace(String name, Workplace workplace, boolean handleEvent) throws StorageException {
        workplace.setTriggerNext(handleEvent);
        if (workplaceMap.containsKey(name)) {
            workplaceMap.replace(name, workplace);
        } else {
            registerWorkplace(name, workplace);
        }
    }

    @Override
    public void registerContext(String name, WorkflowContext context) throws StorageException {
        contextMap.put(name, context);
    }

    @Override
    public void removeContext(String name) throws StorageException {
        contextMap.remove(name);
    }

    @Override
    public WorkflowContext getContext(String name) throws StorageException {
        return localContextMap.get(name);
    }

    @Override
    public void commitContext(String name, WorkflowContext context, boolean handleEvent) throws StorageException {
        context.setTriggerNext(handleEvent);
        if (contextMap.containsKey(name)) {
            contextMap.replace(name, context);
        } else {
            registerContext(name, context);
        }
    }

    @Override
    public Collection<WorkflowContext> getContexts() throws StorageException {
        return ImmutableList.copyOf(localContextMap.values());
    }

    @Override
    public Collection<WorkflowContext> getWorkplaceContexts(String workplaceName) {
        Map<String, WorkflowContext> ctxMap = localWorkplaceMemberMap.get(workplaceName);
        if (ctxMap == null) {
            return Collections.emptyList();
        }

        return ImmutableList.copyOf(ctxMap.values());
    }

    @Override
    public void removeWorkplaceContexts(String workplaceName) {
        for (WorkflowContext ctx : getWorkplaceContexts(workplaceName)) {
            removeContext(ctx.name());
        }
    }

    private class WorkplaceMapListener implements MapEventListener<String, WorkflowData> {

        @Override
        public void event(MapEvent<String, WorkflowData> event) {

            Workplace newWorkplace = (Workplace) Versioned.valueOrNull(event.newValue());
            Workplace oldWorkplace = (Workplace) Versioned.valueOrNull(event.oldValue());

            log.info("WorkplaceMap event: {}", event);
            switch (event.type()) {
                case INSERT:
                    insert(newWorkplace);
                    notifyDelegate(new WorkflowDataEvent(WorkflowDataEvent.Type.INSERT, newWorkplace));
                    break;
                case UPDATE:
                    update(newWorkplace);
                    notifyDelegate(new WorkflowDataEvent(WorkflowDataEvent.Type.UPDATE, newWorkplace));
                    break;
                case REMOVE:
                    remove(oldWorkplace);
                    notifyDelegate(new WorkflowDataEvent(WorkflowDataEvent.Type.REMOVE, oldWorkplace));
                    break;
                default:
            }
        }

        private void insert(Workplace workplace) {
            localWorkplaceMap.put(workplace.name(), workplace);
        }

        private void update(Workplace workplace) {
            localWorkplaceMap.replace(workplace.name(), workplace);
        }

        private void remove(Workplace workplace) {
            localWorkplaceMap.remove(workplace.name());
        }

        public void syncLocal() {
            workplaceMap.values().stream().forEach(
                    x -> insert((Workplace) (x.value()))
            );
        }
    }

    private class WorkflowContextMapListener implements MapEventListener<String, WorkflowData> {

        @Override
        public void event(MapEvent<String, WorkflowData> event) {

            WorkflowContext newContext = (WorkflowContext) Versioned.valueOrNull(event.newValue());
            WorkflowContext oldContext = (WorkflowContext) Versioned.valueOrNull(event.oldValue());

            log.debug("WorkflowContext event: {}", event);
            switch (event.type()) {
                case INSERT:
                    insert(newContext);
                    notifyDelegate(new WorkflowDataEvent(WorkflowDataEvent.Type.INSERT, newContext));
                    break;
                case UPDATE:
                    update(newContext);
                    notifyDelegate(new WorkflowDataEvent(WorkflowDataEvent.Type.UPDATE, newContext));
                    break;
                case REMOVE:
                    remove(oldContext);
                    notifyDelegate(new WorkflowDataEvent(WorkflowDataEvent.Type.REMOVE, oldContext));
                    break;
                default:
            }
        }

        /**
         * Inserts workflow context on local hash map.
         * @param context workflow context
         */
        private void insert(WorkflowContext context) {
            String workplaceName = context.workplaceName();
            Map<String, WorkflowContext> ctxMap = localWorkplaceMemberMap.get(workplaceName);
            if (ctxMap == null) {
                ctxMap = new HashMap<>();
                localWorkplaceMemberMap.put(workplaceName, ctxMap);
            }
            ctxMap.put(context.name(), context);

            localContextMap.put(context.name(), context);
        }

        /**
         * Updates workflow context on local hash map.
         * @param context workflow context
         */
        private void update(WorkflowContext context) {
            String workplaceName = context.workplaceName();
            Map<String, WorkflowContext> ctxMap = localWorkplaceMemberMap.get(workplaceName);
            if (ctxMap == null) {
                ctxMap = new HashMap<>();
                localWorkplaceMemberMap.put(workplaceName, ctxMap);
            }
            ctxMap.put(context.name(), context);

            localContextMap.put(context.name(), context);
        }

        /**
         * Removes workflow context from local hash map.
         * @param context workflow context
         */
        private void remove(WorkflowContext context) {
            localContextMap.remove(context.name());

            String workplaceName = context.workplaceName();
            Map<String, WorkflowContext> ctxMap = localWorkplaceMemberMap.get(workplaceName);
            if (ctxMap == null) {
                log.error("remove-context: Failed to find workplace({}) in localWorkplaceMemberMap", workplaceName);
                return;
            }
            ctxMap.remove(context.name());
            if (ctxMap.size() == 0) {
                localWorkplaceMemberMap.remove(workplaceName, ctxMap);
            }
        }

        /**
         * Synchronizes local hash map.
         */
        public void syncLocal() {
            contextMap.values().stream().forEach(
                    x -> insert((WorkflowContext) (x.value()))
            );
        }
    }
}
