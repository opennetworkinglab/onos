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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.workflow.api.AbstractWorkflow;
import org.onosproject.workflow.api.ImmutableListWorkflow;
import org.onosproject.workflow.api.Workflow;
import org.onosproject.workflow.api.WorkflowAttribute;
import org.onosproject.workflow.api.WorkflowStore;
import org.onosproject.workflow.api.WorkletDescription;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, service = WorkflowStore.class)
public class ECWorkFlowStore
        extends AbstractStore<GroupEvent, GroupStoreDelegate> implements WorkflowStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private ApplicationId appId;
    private EventuallyConsistentMap<URI, Workflow> workflowStore;
    private Set<ClassLoader> classloaders = Sets.newConcurrentHashSet();

    @Activate
    public void activate() {

        appId = coreService.registerApplication("org.onosproject.nfconfig");
        log.info("appId=" + appId);

        KryoNamespace.Builder workflowSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(URI.class)
                .register(Workflow.class)
                .register(AbstractWorkflow.class)
                .register(ImmutableListWorkflow.class)
                .register(WorkletDescription.class)
                .register(List.class)
                .register(ImmutableList.class)
                .register(Class.class)
                .register(WorkflowAttribute.class)
                .register(Set.class)
                .register(ImmutableSet.class)
                .register(HashSet.class);

        workflowStore = storageService.<URI, Workflow>eventuallyConsistentMapBuilder()
                .withName("workflow-workplaceStore")
                .withSerializer(workflowSerializer)
                .withAntiEntropyPeriod(5, TimeUnit.SECONDS)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withTombstonesDisabled()
                .build();

        classloaders.add(this.getClass().getClassLoader());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        workflowStore.destroy();
        log.info("Stopped");
    }

    @Override
    public void register(Workflow workflow) {
        workflowStore.put(workflow.id(), workflow);
    }

    @Override
    public void unregister(URI id) {
        workflowStore.remove(id);
    }

    @Override
    public Workflow get(URI id) {
        return workflowStore.get(id);
    }

    @Override
    public Collection<Workflow> getAll() {
        return workflowStore.values();
    }

    @Override
    public void registerLocal(ClassLoader loader) {
        classloaders.add(loader);
    }

    @Override
    public void unregisterLocal(ClassLoader loader) {
        classloaders.remove(loader);
    }

    @Override
    public Class getClass(String name) throws ClassNotFoundException {
        for (ClassLoader loader : classloaders) {
            Class cl = null;
            try {
                cl = loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // do nothing
            }
            if (cl != null) {
                return cl;
            }
        }
        throw new ClassNotFoundException(name);
    }
}
