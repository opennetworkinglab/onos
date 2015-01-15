/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flowextend.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.SncFlowCompletedOperation;
import org.onosproject.net.flow.SncFlowRuleEntry;
import org.onosproject.net.flow.SncFlowRuleEvent;
import org.onosproject.net.flowextend.FlowExtendCompletedOperation;
import org.onosproject.net.flowextend.FlowRuleBatchExtendEvent;
import org.onosproject.net.flowextend.FlowRuleExtendEntry;
import org.onosproject.net.flowextend.FlowRuleExtendListener;
import org.onosproject.net.flowextend.FlowRuleExtendProvider;
import org.onosproject.net.flowextend.FlowRuleExtendProviderRegistry;
import org.onosproject.net.flowextend.FlowRuleExtendProviderService;
import org.onosproject.net.flowextend.FlowRuleExtendService;
import org.onosproject.net.flowextend.FlowRuleExtendStoreDelegate;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the flow NB &amp; SB APIs.
 */
@Component(immediate = true)
@Service
public class FlowRuleExtendManager
        extends AbstractProviderRegistry<FlowRuleExtendProvider, FlowRuleExtendProviderService>
        implements FlowRuleExtendService, FlowRuleExtendProviderRegistry {

    enum BatchState { STARTED, FINISHED, CANCELLED };

    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleEvent, FlowRuleListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final FlowRuleExtendStoreDelegate delegate = new InternalStoreDelegate();

    private ExecutorService futureService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        futureService =
                Executors.newFixedThreadPool(32, namedThreads("provider-future-listeners-%d"));
        store.setDelegate(delegate);
        eventDispatcher.addSink(FlowRuleEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        futureService.shutdownNow();

        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(FlowRuleEvent.class);
        log.info("Stopped");
    }

 // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements FlowRuleExtendStoreDelegate {

        // FIXME set appropriate default and make it configurable
        private static final int TIMEOUT_PER_OP = 500; // ms

        @Override
        public void notify(FlowRuleBatchExtendEvent event) {
            // TODO Auto-generated method stub
            switch (event.type()) {
            case BATCH_OPERATION_REQUESTED:
                    //send it
                    Collection<FlowRuleExtendEntry> subject = event.subject();
                    FlowRuleExtendProvider flowRuleProvider =
                                    getProvider(new ProviderId("igp","org.onosproject.provider.igp"));
                    flowRuleProvider.applyFlowRule(subject);
                    //do not have transation, assume it install success
                    FlowExtendCompletedOperation result = new FlowExtendCompletedOperation(true,
                                    Collections.<FlowRuleExtendEntry>emptySet());
                    store.batchOperationComplete(FlowRuleBatchExtendEvent.completed(subject, result));
                    break;
            case BATCH_OPERATION_COMPLETED:
                    
                    break;
            default:
                    break;
            }
        }
    }

    @Override
    public Iterable<FlowRuleExtendEntry> getFlowEntries(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<FlowExtendCompletedOperation> applyBatch(Collection<FlowRuleExtendEntry> batch) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addListener(FlowRuleExtendListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeListener(FlowRuleExtendListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Iterable<OFMessage> getOFMessages(DeviceId fpid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected FlowRuleExtendProviderService createProviderService(FlowRuleExtendProvider provider) {
        // TODO Auto-generated method stub
        return null;
    }

}
