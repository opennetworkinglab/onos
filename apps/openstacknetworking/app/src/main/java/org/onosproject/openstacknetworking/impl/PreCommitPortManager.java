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
package org.onosproject.openstacknetworking.impl;

import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortAdminService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type;
import org.onosproject.openstacknetworking.api.PreCommitPortService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.InstancePort.State.REMOVE_PENDING;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of pre-commit service.
 */
@Component(immediate = true, service = PreCommitPortService.class)
public class PreCommitPortManager implements PreCommitPortService {

    protected final Logger log = getLogger(getClass());

    private static final KryoNamespace SERIALIZER_PRE_COMMIT = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(OpenstackNetworkEvent.Type.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private ConsistentMap<String, Map<Type, Set<String>>> store;

    @Activate
    protected void activate() {

        ApplicationId appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);

        store = storageService.<String, Map<Type, Set<String>>>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_PRE_COMMIT))
                .withName("openstack-pre-commit-store")
                .withApplicationId(appId)
                .build();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void subscribePreCommit(String portId, Type eventType, String className) {
        store.computeIfAbsent(portId, s -> new HashMap<>());

        store.compute(portId, (k, v) -> {

            if (className == null || className.isEmpty()) {
                return null;
            }

            Objects.requireNonNull(v).computeIfAbsent(eventType, s -> new HashSet<>());


            Objects.requireNonNull(v).compute(eventType, (i, j) -> {
                Objects.requireNonNull(j).add(className);
                return j;
            });

            return v;
        });
    }

    @Override
    public void unsubscribePreCommit(String portId, Type eventType,
                                     InstancePortAdminService service, String className) {

        store.computeIfPresent(portId, (k, v) -> {

            if (className == null || className.isEmpty()) {
                return null;
            }

            Objects.requireNonNull(v).computeIfPresent(eventType, (i, j) -> {
                Objects.requireNonNull(j).remove(className);
                return j;
            });

            return v;
        });

        if (subscriberCountByEventType(portId, eventType) == 0) {

            InstancePort instPort = service.instancePort(portId);

            if (instPort != null && instPort.state() == REMOVE_PENDING) {
                service.removeInstancePort(portId);
            }
        }
    }

    @Override
    public int subscriberCountByEventType(String portId, Type eventType) {

        Map<Type, Set<String>> typeMap = store.asJavaMap().get(portId);

        if (typeMap == null || typeMap.isEmpty()) {
            return 0;
        }

        if (typeMap.get(eventType) == null || typeMap.get(eventType).isEmpty()) {
            return 0;
        }

        return typeMap.get(eventType).size();
    }

    @Override
    public int subscriberCount(String portId) {

        Map<Type, Set<String>> typeMap = store.asJavaMap().get(portId);

        if (typeMap == null || typeMap.isEmpty()) {
            return 0;
        }

        return typeMap.values().stream()
                .filter(Objects::nonNull)
                .map(Set::size)
                .reduce(0, Integer::sum);
    }
}
