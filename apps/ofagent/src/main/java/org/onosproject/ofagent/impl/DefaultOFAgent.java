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
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.ofagent.api.OFSwitch;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Implementation of OF agent.
 */
public final class DefaultOFAgent implements OFAgent {

    private final NetworkId networkId;
    private final Map<Class<?>, Object> services;
    private final Set<OFController> controllers;
    private final ExecutorService eventExecutor;
    private final NioEventLoopGroup ioWorker;

    private final ConcurrentHashMap<DeviceId, OFSwitch> switchMap = new ConcurrentHashMap<>();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final FlowRuleListener flowRuleListener = new InternalFlowRuleListener();
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();

    private DefaultOFAgent(NetworkId networkId,
                           Map<Class<?>, Object> services,
                           Set<OFController> controllers,
                           ExecutorService eventExecutor,
                           NioEventLoopGroup ioWorker) {
        this.networkId = networkId;
        this.services = services;
        this.controllers = controllers;
        this.eventExecutor = eventExecutor;
        this.ioWorker = ioWorker;
    }

    @Override
    public NetworkId networkId() {
        return null;
    }

    @Override
    public Set<OFController> controllers() {
        return null;
    }

    @Override
    public void start() {
        // TODO add listeners to the services
        // TODO connect all virtual devices in this network to the controllers
    }

    @Override
    public void stop() {
        // TODO remove listeners from the services
        // TODO disconnect all active connections
    }

    private void connect(OFSwitch ofSwitch, OFController controller) {
        // TODO connect the switch to the controller
    }

    private void disconnect(OFSwitch ofSwitch, OFController controller) {
        // TODO disconnect the controller from the ofSwitch
    }

    private class InternalFlowRuleListener implements FlowRuleListener {

        @Override
        public void event(FlowRuleEvent event) {
            // TODO handle flow rule event
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            // TODO handle device event
            // device detected: connect the device to controllers
            // device removed: disconnect and remove the switch from the map
            // device state available: connect the switch to the controllers
            // device state unavailable: disconnect the switch from the controllers
            // port added: send out features reply
            // port status change
        }
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // TODO handle packet-in
        }
    }

    // TODO implement builder
}
