/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.mobility;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;

import com.google.common.collect.Lists;


/**
 * Sample mobility application. Cleans up flowmods when a host moves.
 */
@Component(immediate = true)
public class HostMobility {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private ApplicationId appId;
    private ExecutorService eventHandler;
    private final HostListener hostListener = new InternalHostListener();

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.mobility");
        eventHandler = newSingleThreadScheduledExecutor(groupedThreads("onos/app-mobility", "event-handler", log));
        hostService.addListener(hostListener);
        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        // TODO we never actually add any flow rules
        flowRuleService.removeFlowRulesById(appId);
        hostService.removeListener(hostListener);
        eventHandler.shutdown();
        log.info("Stopped");
    }

    public class InternalHostListener
        implements HostListener {

        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_REMOVED:
                case HOST_UPDATED:
                    // don't care if a host has been added, removed.
                    break;
                case HOST_MOVED:
                    log.info("Host {} has moved; cleaning up.", event.subject());
                    eventHandler.execute(() -> cleanup(event.subject()));
                    break;

                default:
                    break;
            }
        }

        /**
         * For a given host, remove any flow rule which references it's addresses.
         * @param host the host to clean up for
         */
        private void cleanup(Host host) {
            Iterable<Device> devices = deviceService.getDevices();
            List<FlowRule> flowRules = Lists.newLinkedList();
            for (Device device : devices) {
                   flowRules.addAll(cleanupDevice(device, host));
            }
            FlowRule[] flows = new FlowRule[flowRules.size()];
            flows = flowRules.toArray(flows);
            flowRuleService.removeFlowRules(flows);
        }

        private Collection<? extends FlowRule> cleanupDevice(Device device, Host host) {
            List<FlowRule> flowRules = Lists.newLinkedList();
            MacAddress mac = host.mac();
            for (FlowRule rule : flowRuleService.getFlowEntries(device.id())) {
                for (Criterion c : rule.selector().criteria()) {
                    if (c.type() == Type.ETH_DST || c.type() == Type.ETH_SRC) {
                        EthCriterion eth = (EthCriterion) c;
                        if (eth.mac().equals(mac)) {
                            flowRules.add(rule);
                        }
                    }
                }
            }
            return flowRules;
        }
    }
}


