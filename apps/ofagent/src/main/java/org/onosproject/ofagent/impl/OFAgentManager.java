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

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentService;
import org.onosproject.ofagent.api.OFController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static org.onlab.util.BoundedThreadPool.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Implementation of OpenFlow agent service.
 */
@Component(immediate = true)
@Service
public class OFAgentManager implements OFAgentService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // TODO make it to be configurable with component config
    private static final int NUM_OF_THREADS = 1;
    private final ExecutorService eventExecutor = newFixedThreadPool(
            NUM_OF_THREADS,
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    // TODO change it to ConsistentMap and support multi-instance mode
    private ConcurrentHashMap<NetworkId, OFAgent> agentMap = new ConcurrentHashMap<>();
    private NioEventLoopGroup ioWorker;

    @Activate
    protected void activate() {
        // TODO listen to the virtual network event
        ioWorker = new NioEventLoopGroup();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        ioWorker.shutdownGracefully();
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public Set<OFAgent> agents() {
        // TODO return existing agents
        return null;
    }

    @Override
    public void createAgent(NetworkId networkId, OFController... controllers) {
        // TODO create OFAgent instance with the given network ID, controllers
        // TODO and device, flowRule, link, and packet service for the virtual network
        // TODO start the OFAgent only if the virtual network state is active
    }

    @Override
    public void removeAgent(NetworkId networkId) {
        // TODO stop and remove the OFAgent for the network
    }

    @Override
    public void startAgent(NetworkId networkId) {
        // TODO starts the agent for the network
    }

    @Override
    public void stopAgent(NetworkId networkId) {
        // TODO stops the agent for the network
    }

    @Override
    public boolean isActive(NetworkId networkId) {
        // TODO manage the OF agent status
        return false;
    }

    private class InternalVirtualNetworkListener implements VirtualNetworkListener {

        @Override
        public void event(VirtualNetworkEvent event) {
            // TODO handle virtual network start and stop
        }
    }
}
