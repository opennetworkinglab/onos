/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupEvent;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupStore;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroup;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroupRule;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.OpenstackSecurityGroupEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of OpenStack security group using a {@code ConsistentMap}.
 *
 */
@Service
@Component(immediate = true)
public class DistributedSecurityGroupStore
        extends AbstractStore<OpenstackSecurityGroupEvent, OpenstackSecurityGroupStoreDelegate>
        implements OpenstackSecurityGroupStore {

    protected final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";

    private static final KryoNamespace SERIALIZER_SECURITY_GROUP = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(SecurityGroup.class)
            .register(SecurityGroupRule.class)
            .register(NeutronSecurityGroupRule.class)
            .register(NeutronSecurityGroup.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, SecurityGroup> securityGroupMapListener =
            new OpenstackSecurityGroupMapListener();

    private ConsistentMap<String, SecurityGroup> osSecurityGroupStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);

        osSecurityGroupStore = storageService.<String, SecurityGroup>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_SECURITY_GROUP))
                .withName("openstack-securitygroupstore")
                .withApplicationId(appId)
                .build();
        osSecurityGroupStore.addListener(securityGroupMapListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osSecurityGroupStore.removeListener(securityGroupMapListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createSecurityGroup(SecurityGroup sg) {
        osSecurityGroupStore.compute(sg.getId(), (id, existing) -> {
            final String error = sg.getName() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return sg;
        });
    }

    @Override
    public void updateSecurityGroup(SecurityGroup sg) {
        osSecurityGroupStore.compute(sg.getId(), (id, existing) -> {
            final String error = sg.getName() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return sg;
        });
    }

    @Override
    public SecurityGroup removeSecurityGroup(String sgId) {
        Versioned<SecurityGroup> sg = osSecurityGroupStore.remove(sgId);
        return sg == null ? null : sg.value();
    }

    @Override
    public SecurityGroup securityGroup(String sgId) {
        Versioned<SecurityGroup> osSg = osSecurityGroupStore.get(sgId);
        return osSg == null ? null : osSg.value();
    }

    @Override
    public Set<SecurityGroup> securityGroups() {
        Set<SecurityGroup> osSgs = osSecurityGroupStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osSgs);
    }

    @Override
    public void clear() {
        osSecurityGroupStore.clear();
    }

    private class OpenstackSecurityGroupMapListener implements MapEventListener<String, SecurityGroup> {

        @Override
        public void event(MapEvent<String, SecurityGroup> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("OpenStack security group created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new OpenstackSecurityGroupEvent(
                                    OPENSTACK_SECURITY_GROUP_CREATED,
                                    event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("OpenStack security group updated {}", event.newValue());
                    eventExecutor.execute(() -> processUpdate(
                            event.oldValue().value(),
                            event.newValue().value()));
                    break;
                case REMOVE:
                    log.debug("OpenStack security group removed {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new OpenstackSecurityGroupEvent(
                                    OPENSTACK_SECURITY_GROUP_REMOVED,
                                    event.oldValue().value())));
                    break;
                default:
            }
        }

        private void processUpdate(SecurityGroup oldSg, SecurityGroup newSg) {
            Set<String> oldSgRuleIds = oldSg.getRules().stream()
                    .map(SecurityGroupRule::getId).collect(Collectors.toSet());
            Set<String> newSgRuleIds = newSg.getRules().stream()
                    .map(SecurityGroupRule::getId).collect(Collectors.toSet());

            oldSg.getRules().stream().filter(sgRule -> !newSgRuleIds.contains(sgRule.getId()))
                    .forEach(sgRule -> notifyDelegate(new OpenstackSecurityGroupEvent(
                            OPENSTACK_SECURITY_GROUP_RULE_REMOVED, newSg, sgRule)
                    ));
            newSg.getRules().stream().filter(sgRule -> !oldSgRuleIds.contains(sgRule.getId()))
                    .forEach(sgRule -> notifyDelegate(new OpenstackSecurityGroupEvent(
                            OPENSTACK_SECURITY_GROUP_RULE_CREATED, newSg, sgRule)
                    ));
        }
    }
}
