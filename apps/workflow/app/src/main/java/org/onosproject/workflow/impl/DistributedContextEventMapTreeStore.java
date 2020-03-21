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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.IllegalDocumentModificationException;
import org.onosproject.store.service.NoSuchDocumentPathException;
import org.onosproject.store.service.Ordering;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.onosproject.workflow.api.ContextEventMapStore;
import org.onosproject.workflow.api.WorkflowException;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, service = ContextEventMapStore.class)
public class DistributedContextEventMapTreeStore implements ContextEventMapStore {

    protected static final Logger log = getLogger(DistributedContextEventMapTreeStore.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private StorageService storageService;

    private ApplicationId appId;

    private AsyncDocumentTree<String> eventMapTree;

    private EventuallyConsistentMap<String, Set<String>> hintSetPerCxtMap;


    @Activate
    public void activate() {

        appId = coreService.registerApplication("org.onosproject.contexteventmapstore");
        log.info("appId=" + appId);

        KryoNamespace eventMapNamespace = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .build();

        eventMapTree = storageService.<String>documentTreeBuilder()
                .withSerializer(Serializer.using(eventMapNamespace))
                .withName("context-event-map-store")
                .withOrdering(Ordering.INSERTION)
                .buildDocumentTree();

        hintSetPerCxtMap = storageService.<String, Set<String>>eventuallyConsistentMapBuilder()
                .withName("workflow-event-hint-per-cxt")
                .withSerializer(eventMapNamespace)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventMapTree.destroy();
        hintSetPerCxtMap.destroy();
        log.info("Stopped");
    }

    @Override
    public void registerEventMap(String eventType, Set<String> eventHintSet,
                                 String contextName, String programCounterString) throws WorkflowException {
        for (String eventHint : eventHintSet) {
            //Insert in eventCxtPerHintMap
            DocumentPath dpathForCxt = DocumentPath.from(Lists.newArrayList(
                    "root", eventType, eventHint, contextName));
            String currentWorkletType = completeVersioned(eventMapTree.get(dpathForCxt));
            if (currentWorkletType == null) {
                complete(eventMapTree.createRecursive(dpathForCxt, programCounterString));
            } else {
                complete(eventMapTree.replace(dpathForCxt, programCounterString, currentWorkletType));
            }
            log.trace("RegisterEventMap for eventType:{}, eventSet:{}, contextName:{}, pc:{}",
                    eventType, eventHintSet, contextName, programCounterString);

        }
        hintSetPerCxtMap.put(contextName, eventHintSet);
        log.trace("RegisterEventMap in hintSetPerCxt for " +
                        "eventType:{}, eventSet:{}, contextName:{}, pc:{}",
                eventType, eventHintSet, contextName, programCounterString);
    }

    @Override
    public void unregisterEventMap(String eventType, String contextName)
            throws WorkflowException {

        Set<String> hints = hintSetPerCxtMap.get(contextName);
        for (String eventHint : hints) {
            //Remove from eventCxtPerHintMap
            complete(eventMapTree.removeNode(DocumentPath.from(Lists.newArrayList(
                    "root", eventType, eventHint, contextName))));
            log.trace("UnregisterEventMap from eventCxtPerHintMap for eventType:{}, eventSet:{}, contextName:{}",
                    eventType, eventHint, contextName);
        }
        hintSetPerCxtMap.remove(contextName);
    }


    @Override
    public Map<String, String> getEventMapByHint(String eventType, String eventHint) throws WorkflowException {
        DocumentPath path = DocumentPath.from(
                Lists.newArrayList("root", eventType, eventHint));
        Map<String, Versioned<String>> contexts = complete(eventMapTree.getChildren(path));
        Map<String, String> eventMap = Maps.newHashMap();
        if (Objects.isNull(contexts)) {
            return eventMap;
        }

        for (Map.Entry<String, Versioned<String>> entry : contexts.entrySet()) {
            eventMap.put(entry.getKey(), entry.getValue().value());
        }
        log.trace("getEventMapByHint returns eventMap {} ", eventMap);
        return eventMap;
    }

    @Override
    public boolean isEventMapPresent(String contextName) {
        Map<String, String> eventMap = Maps.newHashMap();
        Set<String> eventHintSet = hintSetPerCxtMap.get(contextName);
        if (Objects.nonNull(eventHintSet)) {
            log.trace("EventMap present for Context:{}", contextName);
            return true;
        } else {
            log.trace("EventMap Doesnt exist for Context:{}", contextName);
            return false;
        }
    }


    @Override
    public Map<String, Versioned<String>> getChildren(String path) throws WorkflowException {
        DocumentPath dpath = DocumentPath.from(path);
        Map<String, Versioned<String>> entries = complete(eventMapTree.getChildren(dpath));
        return entries;
    }

    @Override
    public DocumentPath getDocumentPath(String path) throws WorkflowException {
        DocumentPath dpath = DocumentPath.from(path);
        return dpath;
    }

    @Override
    public ObjectNode asJsonTree() throws WorkflowException {

        DocumentPath rootPath = DocumentPath.from(Lists.newArrayList("root"));
        Map<String, Versioned<String>> eventmap = complete(eventMapTree.getChildren(rootPath));

        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

        for (Map.Entry<String, Versioned<String>> eventTypeEntry : eventmap.entrySet()) {

            String eventType = eventTypeEntry.getKey();

            ObjectNode eventTypeNode = JsonNodeFactory.instance.objectNode();
            rootNode.put(eventType, eventTypeNode);

            DocumentPath eventTypePath = DocumentPath.from(Lists.newArrayList("root", eventType));
            Map<String, Versioned<String>> hintmap = complete(eventMapTree.getChildren(eventTypePath));

            for (Map.Entry<String, Versioned<String>> hintEntry : hintmap.entrySet()) {

                String hint = hintEntry.getKey();

                ObjectNode hintNode = JsonNodeFactory.instance.objectNode();
                eventTypeNode.put(hint, hintNode);

                DocumentPath hintPath = DocumentPath.from(Lists.newArrayList("root", eventType, hint));
                Map<String, Versioned<String>> contextmap = complete(eventMapTree.getChildren(hintPath));

                for (Map.Entry<String, Versioned<String>> ctxtEntry : contextmap.entrySet()) {
                    hintNode.put(ctxtEntry.getKey(), ctxtEntry.getValue().value());
                }
            }
        }

        return rootNode;
    }

    private <T> T complete(CompletableFuture<T> future) throws WorkflowException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorkflowException(e.getCause().getMessage());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalDocumentModificationException) {
                throw new WorkflowException("Node or parent does not exist or is root or is not a Leaf Node",
                        e.getCause());
            } else if (e.getCause() instanceof NoSuchDocumentPathException) {
                return null;
            } else {
                throw new WorkflowException("Datastore operation failed", e.getCause());
            }
        }
    }

    private <T> T completeVersioned(CompletableFuture<Versioned<T>> future) throws WorkflowException {
        return Optional.ofNullable(complete(future))
                .map(Versioned::value)
                .orElse(null);
    }
}
