/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.domain.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.graph.AdjacencyListsGraph;
import org.onlab.graph.Graph;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.incubator.net.domain.DomainEdge;
import org.onosproject.incubator.net.domain.DomainVertex;
import org.onosproject.incubator.net.domain.IntentDomain;
import org.onosproject.incubator.net.domain.IntentDomainAdminService;
import org.onosproject.incubator.net.domain.IntentDomainConfig;
import org.onosproject.incubator.net.domain.IntentDomainId;
import org.onosproject.incubator.net.domain.IntentDomainListener;
import org.onosproject.incubator.net.domain.IntentDomainProvider;
import org.onosproject.incubator.net.domain.IntentDomainService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Implementation of the intent domain service.
 */
@Component(immediate = true)
@Service
public class IntentDomainManager
        implements IntentDomainService, IntentDomainAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    private NetworkConfigListener cfgListener = new InternalConfigListener();

    private final ConcurrentMap<IntentDomainId, IntentDomain> domains = Maps.newConcurrentMap();

    private final Multimap<String, IntentDomainId> appToDomain =
            Multimaps.synchronizedSetMultimap(HashMultimap.<String, IntentDomainId>create());

    private Graph<DomainVertex, DomainEdge> graph;

    @Activate
    protected void activate() {
        configService.addListener(cfgListener);
        configService.getSubjects(IntentDomainId.class, IntentDomainConfig.class)
                     .forEach(this::processConfig);
        graph = buildGraph();
        log.debug("Graph: {}", graph);
        log.info("Started");
    }

    private void processConfig(IntentDomainId intentDomainId) {
        IntentDomainConfig cfg = configService.getConfig(intentDomainId,
                                                         IntentDomainConfig.class);

        domains.put(intentDomainId, createDomain(intentDomainId, cfg));
        appToDomain.put(cfg.applicationName(), intentDomainId);
    }

    private IntentDomain createDomain(IntentDomainId id, IntentDomainConfig cfg) {
        return new IntentDomain(id, cfg.domainName(), cfg.internalDevices(), cfg.edgePorts());
    }

    private Graph<DomainVertex, DomainEdge> buildGraph() {
        Set<DomainVertex> vertices = Sets.newHashSet();
        Set<DomainEdge> edges = Sets.newHashSet();

        Map<DeviceId, DomainVertex> deviceVertices = Maps.newHashMap();
        domains.forEach((id, domain) -> {
            DomainVertex domainVertex = new DomainVertex(id);

            // Add vertex for domain
            vertices.add(domainVertex);

            // Add vertices for connection devices
            domain.edgePorts().stream()
                  .map(ConnectPoint::deviceId)
                  .collect(Collectors.toSet())
                    .forEach(did -> deviceVertices.putIfAbsent(did, new DomainVertex(did)));

            // Add bi-directional edges between each domain and connection device
            domain.edgePorts().forEach(cp -> {
                DomainVertex deviceVertex = deviceVertices.get(cp.deviceId());
                edges.add(new DomainEdge(domainVertex, deviceVertex, cp));
                edges.add(new DomainEdge(deviceVertex, domainVertex, cp));
            });
        });

        vertices.addAll(deviceVertices.values());
        //FIXME verify graph integrity...
        return new AdjacencyListsGraph<>(vertices, edges);
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(cfgListener);
        log.info("Stopped");
    }

    @Override
    public IntentDomain getDomain(IntentDomainId id) {
        return domains.get(id);
    }

    @Override
    public Set<IntentDomain> getDomains() {
        return ImmutableSet.copyOf(domains.values());
    }

    @Override
    public Set<IntentDomain> getDomains(DeviceId deviceId) {
        return domains.values().stream()
                .filter(domain ->
                                domain.internalDevices().contains(deviceId) ||
                                domain.edgePorts().stream()
                                        .map(ConnectPoint::deviceId)
                                        .anyMatch(d -> d.equals(deviceId)))
                .collect(Collectors.toSet());
    }

    @Override
    public Graph<DomainVertex, DomainEdge> getDomainGraph() {
        return graph;
    }

    @Override
    public void addListener(IntentDomainListener listener) {
        //TODO slide in AbstractListenerManager
    }

    @Override
    public void removeListener(IntentDomainListener listener) {
        //TODO slide in AbstractListenerManager
    }

    @Override
    public void registerApplication(ApplicationId applicationId, IntentDomainProvider provider) {
        appToDomain.get(applicationId.name()).forEach(d -> domains.get(d).setProvider(provider));
    }

    @Override
    public void unregisterApplication(ApplicationId applicationId) {
        appToDomain.get(applicationId.name()).forEach(d -> domains.get(d).unsetProvider());
    }

    private class InternalConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    processConfig((IntentDomainId) event.subject());
                    graph = buildGraph();
                    log.debug("Graph: {}", graph);
                    break;

                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED:
                case CONFIG_REMOVED:
                default:
                    //TODO
                    break;
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(IntentDomainConfig.class);
        }
    }
}
