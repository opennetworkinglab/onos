/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtSecurityGroupRule;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupStore;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent.Type.KUBEVIRT_SECURITY_GROUP_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent.Type.KUBEVIRT_SECURITY_GROUP_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent.Type.KUBEVIRT_SECURITY_GROUP_RULE_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent.Type.KUBEVIRT_SECURITY_GROUP_RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubevirt security group store using consistent map.
 */
@Component(immediate = true, service = KubevirtSecurityGroupStore.class)
public class DistributedKubevirtSecurityGroupStore
        extends AbstractStore<KubevirtSecurityGroupEvent, KubevirtSecurityGroupStoreDelegate>
        implements KubevirtSecurityGroupStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.kubevirtnetwork";


    private static final KryoNamespace SERIALIZER_KUBEVIRT_SG = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KubevirtSecurityGroup.class)
            .register(KubevirtSecurityGroupRule.class)
            .register(DefaultKubevirtSecurityGroup.class)
            .register(DefaultKubevirtSecurityGroupRule.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, KubevirtSecurityGroup> securityGroupListener =
            new KubevirtSecurityGroupMapListener();

    private ConsistentMap<String, KubevirtSecurityGroup> sgStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        sgStore = storageService.<String, KubevirtSecurityGroup>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_KUBEVIRT_SG))
                .withName("kubevirt-securitygroupstore")
                .withApplicationId(appId)
                .build();
        sgStore.addListener(securityGroupListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        sgStore.removeListener(securityGroupListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createSecurityGroup(KubevirtSecurityGroup sg) {
        sgStore.compute(sg.id(), (id, existing) -> {
            final String error = sg.id() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return sg;
        });
    }

    @Override
    public void updateSecurityGroup(KubevirtSecurityGroup sg) {
        sgStore.compute(sg.id(), (id, existing) -> {
            final String error = sg.id() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return sg;
        });
    }

    @Override
    public KubevirtSecurityGroup removeSecurityGroup(String sgId) {
        Versioned<KubevirtSecurityGroup> sg = sgStore.remove(sgId);
        if (sg == null) {
            final String error = sgId + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return sg.value();
    }

    @Override
    public KubevirtSecurityGroup securityGroup(String sgId) {
        return sgStore.asJavaMap().get(sgId);
    }

    @Override
    public Set<KubevirtSecurityGroup> securityGroups() {
        return ImmutableSet.copyOf(sgStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        sgStore.clear();
    }

    private class KubevirtSecurityGroupMapListener
            implements MapEventListener<String, KubevirtSecurityGroup> {

        @Override
        public void event(MapEvent<String, KubevirtSecurityGroup> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubevirt security group created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtSecurityGroupEvent(
                                    KUBEVIRT_SECURITY_GROUP_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubevirt security group updated {}", event.newValue());
                    eventExecutor.execute(() -> processUpdate(
                            event.oldValue().value(),
                            event.newValue().value()));
                    break;
                case REMOVE:
                    log.debug("Kubevirt security group removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtSecurityGroupEvent(
                                    KUBEVIRT_SECURITY_GROUP_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processUpdate(KubevirtSecurityGroup oldSg, KubevirtSecurityGroup newSg) {
            Set<String> oldSgRuleIds = oldSg.rules().stream()
                    .map(KubevirtSecurityGroupRule::id).collect(Collectors.toSet());
            Set<String> newSgRuleIds = newSg.rules().stream()
                    .map(KubevirtSecurityGroupRule::id).collect(Collectors.toSet());

            oldSg.rules().stream().filter(sgRule -> !newSgRuleIds.contains(sgRule.id()))
                    .forEach(sgRule -> notifyDelegate(new KubevirtSecurityGroupEvent(
                            KUBEVIRT_SECURITY_GROUP_RULE_REMOVED, newSg, sgRule)));
            newSg.rules().stream().filter(sgRule -> !oldSgRuleIds.contains(sgRule.id()))
                    .forEach(sgRule -> notifyDelegate(new KubevirtSecurityGroupEvent(
                            KUBEVIRT_SECURITY_GROUP_RULE_CREATED, newSg, sgRule)));
        }
    }
}
