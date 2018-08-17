/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.provider.of.message.impl;

import com.google.common.collect.Maps;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.metrics.MetricsService;
import org.onlab.util.SharedScheduledExecutorService;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.cpman.message.ControlMessageProvider;
import org.onosproject.cpman.message.ControlMessageProviderRegistry;
import org.onosproject.cpman.message.ControlMessageProviderService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowMessageListener;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.openflow.controller.Dpid.uri;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to collect control message.
 */
@Component(immediate = true)
public class OpenFlowControlMessageProvider extends AbstractProvider
        implements ControlMessageProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ControlMessageProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MetricsService metricsService;

    private ControlMessageProviderService providerService;

    private final InternalDeviceProvider listener = new InternalDeviceProvider();

    private final InternalControlMessageListener messageListener =
                    new InternalControlMessageListener();

    private HashMap<Dpid, OpenFlowControlMessageAggregator> aggregators = Maps.newHashMap();
    private SharedScheduledExecutorService executor;
    private static final int AGGR_INIT_DELAY = 1;
    private static final int AGGR_PERIOD = 1;
    private static final TimeUnit AGGR_TIME_UNIT = TimeUnit.MINUTES;
    private HashMap<Dpid, ScheduledFuture<?>> executorResults = Maps.newHashMap();

    /**
     * Creates a provider with the supplier identifier.
     */
    public OpenFlowControlMessageProvider() {
        super(new ProviderId("of", "org.onosproject.provider.openflow"));
    }

    @Activate
    protected void activate() {
        providerService = providerRegistry.register(this);

        // listens all OpenFlow device related events
        controller.addListener(listener);

        // listens all OpenFlow control message
        controller.addMessageListener(messageListener);

        executor = SharedScheduledExecutors.getSingleThreadExecutor();

        connectInitialDevices();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        disconnectDevices();
        controller.removeListener(listener);
        providerRegistry.unregister(this);
        providerService = null;

        // stops listening all OpenFlow control message events
        controller.removeMessageListener(messageListener);

        log.info("Stopped");
    }

    private void connectInitialDevices() {
        for (OpenFlowSwitch sw: controller.getSwitches()) {
            try {
                listener.switchAdded(new Dpid(sw.getId()));
            } catch (Exception e) {
                log.warn("Failed initially adding {} : {}", sw.getStringId(), e.getMessage());
                log.debug("Error details:", e);
            }
        }
    }

    private void disconnectDevices() {
        for (OpenFlowSwitch sw: controller.getSwitches()) {
            try {
                listener.switchRemoved(new Dpid(sw.getId()));
            } catch (Exception e) {
                log.warn("Failed to remove {} : {}", sw.getStringId(), e.getMessage());
                log.debug("Error details:", e);
            }
        }
    }

    /**
     * A listener for OpenFlow switch event.
     */
    private class InternalDeviceProvider implements OpenFlowSwitchListener {

        @Override
        public void switchAdded(Dpid dpid) {
            if (providerService == null) {
                return;
            }

            DeviceId deviceId = deviceId(uri(dpid));
            OpenFlowControlMessageAggregator ofcma =
                    new OpenFlowControlMessageAggregator(metricsService,
                            providerService, deviceId);
            ScheduledFuture result = executor.scheduleAtFixedRate(ofcma,
                    AGGR_INIT_DELAY, AGGR_PERIOD, AGGR_TIME_UNIT, true);
            aggregators.put(dpid, ofcma);
            executorResults.put(dpid, result);
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            if (providerService == null) {
                return;
            }

            // removes the aggregator when switch is removed
            // this also stops the aggregator from running
            OpenFlowControlMessageAggregator aggregator = aggregators.remove(dpid);
            if (aggregator != null) {
                executorResults.get(dpid).cancel(true);
                executorResults.remove(dpid);
            }
        }

        @Override
        public void switchChanged(Dpid dpid) {
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response) {
        }
    }

    /**
     * A listener for all OpenFlow control messages.
     */
    private class InternalControlMessageListener implements OpenFlowMessageListener {

        @Override
        public void handleIncomingMessage(Dpid dpid, OFMessage msg) {
            if (msg.getType() == OFType.PACKET_IN ||
                    msg.getType() == OFType.FLOW_MOD ||
                    msg.getType() == OFType.STATS_REPLY) {
                aggregators.computeIfPresent(dpid, (k, v) -> {
                    v.increment(msg);
                    return v;
                });
            }
        }

        @Override
        public void handleOutgoingMessage(Dpid dpid, List<OFMessage> msgs) {
            for (OFMessage msg : msgs) {
                if (msg.getType() == OFType.PACKET_OUT ||
                        msg.getType() == OFType.FLOW_MOD ||
                        msg.getType() == OFType.STATS_REQUEST) {
                    aggregators.computeIfPresent(dpid, (k, v) -> {
                        v.increment(msg);
                        return v;
                    });
                }
            }
        }
    }
}
