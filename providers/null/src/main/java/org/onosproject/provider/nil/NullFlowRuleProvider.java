/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.nil;

import com.google.common.collect.Sets;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import org.onlab.util.Timer;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Null provider to accept any flow and report them.
 */
class NullFlowRuleProvider extends NullProviders.AbstractNullProvider
        implements FlowRuleProvider {

    private final Logger log = getLogger(getClass());

    private ConcurrentMap<DeviceId, Set<FlowEntry>> flowTable = new ConcurrentHashMap<>();

    private FlowRuleProviderService providerService;

    private Timeout timeout;

    /**
     * Starts the flow rule provider simulation.
     *
     * @param providerService flow rule provider service
     */
    void start(FlowRuleProviderService providerService) {
        this.providerService = providerService;
        timeout = Timer.newTimeout(new StatisticTask(), 5, TimeUnit.SECONDS);
    }

    /**
     * Stops the flow rule provider simulation.
     */
    void stop() {
        timeout.cancel();
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        // FIXME: invoke executeBatch
    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        // FIXME: invoke executeBatch
    }

    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        throw new UnsupportedOperationException("Cannot remove by appId from null provider");
    }

    @Override
    public void executeBatch(FlowRuleBatchOperation batch) {
        // TODO: consider checking mastership
        Set<FlowEntry> entries =
                flowTable.getOrDefault(batch.deviceId(),
                                       Sets.newConcurrentHashSet());
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            switch (fbe.operator()) {
                case ADD:
                    entries.add(new DefaultFlowEntry(fbe.target()));
                    break;
                case REMOVE:
                    entries.remove(new DefaultFlowEntry(fbe.target()));
                    break;
                case MODIFY:
                    FlowEntry entry = new DefaultFlowEntry(fbe.target());
                    entries.remove(entry);
                    entries.add(entry);
                    break;
                default:
                    log.error("Unknown flow operation: {}", fbe);
            }
        }
        flowTable.put(batch.deviceId(), entries);
        CompletedBatchOperation op =
                new CompletedBatchOperation(true, Collections.emptySet(),
                                            batch.deviceId());
        providerService.batchOperationCompleted(batch.id(), op);
    }

    // Periodically reports flow rule statistics.
    private class StatisticTask implements TimerTask {
        @Override
        public void run(Timeout to) throws Exception {
            for (DeviceId devId : flowTable.keySet()) {
                Set<FlowEntry> entries =
                        flowTable.getOrDefault(devId, Collections.emptySet());
                providerService.pushFlowMetrics(devId, entries);
            }
            timeout = to.timer().newTimeout(to.task(), 5, TimeUnit.SECONDS);
        }
    }
}
