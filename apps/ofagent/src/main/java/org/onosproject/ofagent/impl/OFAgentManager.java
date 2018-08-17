/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentAdminService;
import org.onosproject.ofagent.api.OFAgentEvent;
import org.onosproject.ofagent.api.OFAgentListener;
import org.onosproject.ofagent.api.OFAgentService;
import org.onosproject.ofagent.api.OFAgentStore;
import org.onosproject.ofagent.api.OFAgentStoreDelegate;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.BoundedThreadPool.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.ofagent.api.OFAgent.State.STARTED;
import static org.onosproject.ofagent.api.OFAgent.State.STOPPED;

/**
 * Implementation of OpenFlow agent service.
 */
@Component(immediate = true, service = { OFAgentService.class, OFAgentAdminService.class })
public class OFAgentManager extends ListenerRegistry<OFAgentEvent, OFAgentListener>
        implements OFAgentService, OFAgentAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MSG_OFAGENT = "OFAgent for network %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";
    private static final String MSG_STARTED = "started";
    private static final String MSG_STOPPED = "stopped";
    private static final String MSG_IN_STARTED = "is already in active state, do nothing";
    private static final String MSG_IN_STOPPED = "is already in inactive state, do nothing";

    private static final String ERR_NULL_OFAGENT = "OFAgent cannot be null";
    private static final String ERR_NULL_NETID = "Network ID cannot be null";
    private static final String ERR_NOT_EXIST = "does not exist";
    private static final String ERR_IN_USE = "is in start state, stop the agent first";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualNetworkService virtualNetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OFAgentStore ofAgentStore;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final LeadershipEventListener leadershipListener = new InternalLeadershipListener();
    private final VirtualNetworkListener virtualNetListener = new InternalVirtualNetworkListener();
    private final OFAgentStoreDelegate delegate = new InternalOFAgentStoreDelegate();

    private ApplicationId appId;
    private NodeId localId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APPLICATION_NAME);
        localId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        ofAgentStore.setDelegate(delegate);
        virtualNetService.addListener(virtualNetListener);
        leadershipService.addListener(leadershipListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.removeListener(leadershipListener);
        virtualNetService.removeListener(virtualNetListener);
        ofAgentStore.unsetDelegate(delegate);
        ofAgentStore.ofAgents().stream()
                .filter(ofAgent -> ofAgent.state() == STARTED)
                .forEach(ofAgent -> stopAgent(ofAgent.networkId()));

        eventExecutor.shutdown();
        leadershipService.withdraw(appId.name());

        log.info("Stopped");
    }

    @Override
    public void createAgent(OFAgent ofAgent) {
        checkNotNull(ofAgent, ERR_NULL_OFAGENT);
        if (ofAgent.state() == STARTED) {
            log.warn(String.format(MSG_OFAGENT, ofAgent.networkId(), ERR_IN_USE));
            return;
        }
        // TODO check if the virtual network exists
        ofAgentStore.createOfAgent(ofAgent);
        log.info(String.format(MSG_OFAGENT, ofAgent.networkId(), MSG_CREATED));
    }

    @Override
    public void updateAgent(OFAgent ofAgent) {
        checkNotNull(ofAgent, ERR_NULL_OFAGENT);
        ofAgentStore.updateOfAgent(ofAgent);
        log.info(String.format(MSG_OFAGENT, ofAgent.networkId(), MSG_UPDATED));
    }

    @Override
    public OFAgent removeAgent(NetworkId networkId) {
        checkNotNull(networkId, ERR_NULL_NETID);
        synchronized (this) {
            OFAgent existing = ofAgentStore.ofAgent(networkId);
            if (existing == null) {
                final String error = String.format(MSG_OFAGENT, networkId, ERR_NOT_EXIST);
                throw new IllegalStateException(error);
            }
            if (existing.state() == STARTED) {
                final String error = String.format(MSG_OFAGENT, networkId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            log.info(String.format(MSG_OFAGENT, networkId, MSG_REMOVED));
            return ofAgentStore.removeOfAgent(networkId);
        }
    }

    @Override
    public void startAgent(NetworkId networkId) {
        checkNotNull(networkId, ERR_NULL_NETID);
        synchronized (this) {
            OFAgent existing = ofAgentStore.ofAgent(networkId);
            if (existing == null) {
                final String error = String.format(MSG_OFAGENT, networkId, ERR_NOT_EXIST);
                throw new IllegalStateException(error);
            }
            if (existing.state() == STARTED) {
                final String error = String.format(MSG_OFAGENT, networkId, MSG_IN_STARTED);
                throw new IllegalStateException(error);
            }
            OFAgent updated = DefaultOFAgent.builder()
                    .from(existing).state(STARTED).build();
            ofAgentStore.updateOfAgent(updated);
            log.info(String.format(MSG_OFAGENT, networkId, MSG_STARTED));
        }
    }

    @Override
    public void stopAgent(NetworkId networkId) {
        checkNotNull(networkId, ERR_NULL_NETID);
        synchronized (this) {
            OFAgent existing = ofAgentStore.ofAgent(networkId);
            if (existing == null) {
                final String error = String.format(MSG_OFAGENT, networkId, ERR_NOT_EXIST);
                throw new IllegalStateException(error);
            }
            if (existing.state() == STOPPED) {
                final String error = String.format(MSG_OFAGENT, networkId, MSG_IN_STOPPED);
                throw new IllegalStateException(error);
            }
            OFAgent updated = DefaultOFAgent.builder()
                    .from(existing).state(STOPPED).build();
            ofAgentStore.updateOfAgent(updated);
            log.info(String.format(MSG_OFAGENT, networkId, MSG_STOPPED));
        }
    }

    @Override
    public Set<OFAgent> agents() {
        return ofAgentStore.ofAgents();
    }

    @Override
    public OFAgent agent(NetworkId networkId) {
        checkNotNull(networkId, ERR_NULL_NETID);
        return ofAgentStore.ofAgent(networkId);
    }

    private class InternalLeadershipListener implements LeadershipEventListener {

        @Override
        public boolean isRelevant(LeadershipEvent event) {
            // TODO check if local node is relevant to the leadership change event
            return false;
        }

        @Override
        public void event(LeadershipEvent event) {
            switch (event.type()) {
                case LEADER_CHANGED:
                case LEADER_AND_CANDIDATES_CHANGED:
                    // TODO handle leadership changed events -> restart agents?
                default:
                    break;
            }
        }
    }

    private class InternalVirtualNetworkListener implements VirtualNetworkListener {

        @Override
        public boolean isRelevant(VirtualNetworkEvent event) {
            // do not allow without leadership
            return Objects.equals(localId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(VirtualNetworkEvent event) {
            switch (event.type()) {
                case NETWORK_UPDATED:
                    // TODO handle virtual network stopped -> stop agent
                    break;
                case NETWORK_REMOVED:
                    // TODO remove related OFAgent -> stop agent
                    break;
                case NETWORK_ADDED:
                case VIRTUAL_DEVICE_ADDED:
                case VIRTUAL_DEVICE_UPDATED:
                case VIRTUAL_DEVICE_REMOVED:
                case VIRTUAL_PORT_ADDED:
                case VIRTUAL_PORT_UPDATED:
                case VIRTUAL_PORT_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class InternalOFAgentStoreDelegate implements OFAgentStoreDelegate {

        @Override
        public void notify(OFAgentEvent event) {
            if (event != null) {
                log.trace("send ofagent event {}", event);
                process(event);
            }
        }
    }
}
