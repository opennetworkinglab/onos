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
package org.onosproject.provider.nil.flow.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.util.Timer;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.BatchOperation;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Null provider to accept any flow and report them.
 */
@Component(immediate = true)
public class NullFlowRuleProvider extends AbstractProvider implements FlowRuleProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry providerRegistry;

    private Multimap<DeviceId, FlowEntry> flowTable = HashMultimap.create();

    private FlowRuleProviderService providerService;

    private HashedWheelTimer timer = Timer.getTimer();
    private Timeout timeout;

    public NullFlowRuleProvider() {
        super(new ProviderId("null", "org.onosproject.provider.nil"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        timeout = timer.newTimeout(new StatisticTask(), 5, TimeUnit.SECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;
        timeout.cancel();

        log.info("Stopped");
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            flowTable.put(flowRules[i].deviceId(), new DefaultFlowEntry(flowRules[i]));
        }
    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            flowTable.remove(flowRules[i].deviceId(), flowRules[i]);
        }
    }

    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        log.info("removal by app id not supported in null provider");
    }

    @Override
    public Future<CompletedBatchOperation> executeBatch(
            BatchOperation<FlowRuleBatchEntry> batch) {
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            switch (fbe.operator()) {
                case ADD:
                    applyFlowRule(fbe.target());
                    break;
                case REMOVE:
                    removeFlowRule(fbe.target());
                    break;
                case MODIFY:
                    removeFlowRule(fbe.target());
                    applyFlowRule(fbe.target());
                    break;
                default:
                    log.error("Unknown flow operation: {}", fbe);
            }
        }
        return Futures.immediateFuture(
                new CompletedBatchOperation(true, Collections.emptySet()));
    }

    private class StatisticTask implements TimerTask {

        @Override
        public void run(Timeout to) throws Exception {
            for (DeviceId devId : flowTable.keySet()) {
                providerService.pushFlowMetrics(devId, flowTable.get(devId));
            }

            timeout = timer.newTimeout(to.getTask(), 5, TimeUnit.SECONDS);
        }
    }
}
