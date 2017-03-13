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
package org.onosproject.ofagent.impl;

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
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentEvent;
import org.onosproject.ofagent.api.OFAgentEvent.Type;
import org.onosproject.ofagent.api.OFAgentStore;
import org.onosproject.ofagent.api.OFAgentStoreDelegate;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.ofagent.api.OFAgent.State.STARTED;
import static org.onosproject.ofagent.api.OFAgentEvent.Type.*;
import static org.onosproject.ofagent.api.OFAgentService.APPLICATION_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the {@link OFAgentStore} with consistent map.
 */
@Service
@Component(immediate = true)
public class DistributedOFAgentStore extends AbstractStore<OFAgentEvent, OFAgentStoreDelegate>
        implements OFAgentStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";

    private static final KryoNamespace SERIALIZER_OFAGENT = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(OFAgent.class)
            .register(OFAgent.State.class)
            .register(NetworkId.class)
            .register(DefaultOFAgent.class)
            .register(OFController.class)
            .register(DefaultOFController.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final MapEventListener<NetworkId, OFAgent> ofAgentMapListener = new OFAgentMapListener();

    private ConsistentMap<NetworkId, OFAgent> ofAgentStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APPLICATION_NAME);
        ofAgentStore = storageService.<NetworkId, OFAgent>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_OFAGENT))
                .withName("ofagentstore")
                .withApplicationId(appId)
                .build();
        ofAgentStore.addListener(ofAgentMapListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        ofAgentStore.removeListener(ofAgentMapListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createOfAgent(OFAgent ofAgent) {
        ofAgentStore.compute(ofAgent.networkId(), (id, existing) -> {
            final String error = ofAgent.networkId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return ofAgent;
        });
    }

    @Override
    public void updateOfAgent(OFAgent ofAgent) {
        ofAgentStore.compute(ofAgent.networkId(), (id, existing) -> {
            final String error = ofAgent.networkId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return ofAgent;
        });
    }

    @Override
    public OFAgent removeOfAgent(NetworkId networkId) {
        Versioned<OFAgent> ofAgent = ofAgentStore.remove(networkId);
        return ofAgent == null ? null : ofAgent.value();
    }

    @Override
    public OFAgent ofAgent(NetworkId networkId) {
        Versioned<OFAgent> ofAgent = ofAgentStore.get(networkId);
        return ofAgent == null ? null : ofAgent.value();
    }

    @Override
    public Set<OFAgent> ofAgents() {
        Set<OFAgent> ofAgents = ofAgentStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(ofAgents);
    }

    private class OFAgentMapListener implements MapEventListener<NetworkId, OFAgent> {

        @Override
        public void event(MapEvent<NetworkId, OFAgent> event) {
            switch (event.type()) {
                case INSERT:
                    eventExecutor.execute(() -> {
                        log.debug("OFAgent for network {} created", event.key());
                        notifyDelegate(new OFAgentEvent(
                                Type.OFAGENT_CREATED,
                                event.newValue().value()));
                    });
                    break;
                case UPDATE:
                    eventExecutor.execute(() -> {
                        log.debug("OFAgent for network {} updated", event.key());
                        processUpdated(event.oldValue().value(), event.newValue().value());
                    });
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> {
                        log.debug("OFAgent for network {} removed", event.key());
                        notifyDelegate(new OFAgentEvent(
                                Type.OFAGENT_REMOVED,
                                event.oldValue().value()));
                    });
                    break;
                default:
                    break;
            }
        }

        private void processUpdated(OFAgent oldValue, OFAgent newValue) {
            if (!oldValue.controllers().equals(newValue.controllers())) {
                oldValue.controllers().stream()
                        .filter(controller -> !newValue.controllers().contains(controller))
                        .forEach(controller -> notifyDelegate(new OFAgentEvent(
                                OFAGENT_CONTROLLER_REMOVED,
                                newValue,
                                controller)
                        ));

                newValue.controllers().stream()
                        .filter(controller -> !oldValue.controllers().contains(controller))
                        .forEach(controller -> notifyDelegate(new OFAgentEvent(
                                OFAGENT_CONTROLLER_ADDED,
                                newValue,
                                controller
                        )));
            }

            if (oldValue.state() != newValue.state()) {
                Type eventType = newValue.state() == STARTED ? OFAGENT_STARTED : OFAGENT_STOPPED;
                notifyDelegate(new OFAgentEvent(eventType, newValue));
            }
        }
    }
}
