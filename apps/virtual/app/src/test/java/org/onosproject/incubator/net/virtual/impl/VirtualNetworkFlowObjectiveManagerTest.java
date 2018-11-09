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

package org.onosproject.incubator.net.virtual.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.TestApplicationId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowObjectiveStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowRuleStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.impl.provider.VirtualProviderManager;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualFlowRuleProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.incubator.net.virtual.store.impl.DistributedVirtualNetworkStore;
import org.onosproject.incubator.net.virtual.store.impl.SimpleVirtualFlowObjectiveStore;
import org.onosproject.incubator.net.virtual.store.impl.SimpleVirtualFlowRuleStore;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TestStorageService;

import static org.junit.Assert.assertEquals;

/**
 * Junit tests for VirtualNetworkFlowObjectiveManager.
 */
public class VirtualNetworkFlowObjectiveManagerTest
        extends VirtualNetworkTestUtil {

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private ServiceDirectory testDirectory;
    protected SimpleVirtualFlowObjectiveStore flowObjectiveStore;

    private VirtualProviderManager providerRegistryService;
    private EventDeliveryService eventDeliveryService;

    private ApplicationId appId;

    private VirtualNetwork vnet1;
    private VirtualNetwork vnet2;

    private FlowObjectiveService service1;
    private FlowObjectiveService service2;

    //FIXME: referring flowrule service, store, and provider shouldn't be here
    private VirtualFlowRuleProvider flowRuleProvider = new TestProvider();
    private SimpleVirtualFlowRuleStore flowRuleStore;
    protected StorageService storageService = new TestStorageService();

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        CoreService coreService = new TestCoreService();
        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService", storageService);
        virtualNetworkManagerStore.activate();

        flowObjectiveStore = new SimpleVirtualFlowObjectiveStore();
        TestUtils.setField(flowObjectiveStore, "storageService", storageService);
        flowObjectiveStore.activate();
        flowRuleStore = new SimpleVirtualFlowRuleStore();
        flowRuleStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        TestUtils.setField(manager, "coreService", coreService);

        providerRegistryService = new VirtualProviderManager();
        providerRegistryService.registerProvider(flowRuleProvider);

        eventDeliveryService = new TestEventDispatcher();
        NetTestTools.injectEventDispatcher(manager, eventDeliveryService);

        appId = new TestApplicationId("FlowRuleManagerTest");

        testDirectory = new TestServiceDirectory()
                .add(VirtualNetworkStore.class, virtualNetworkManagerStore)
                .add(CoreService.class, coreService)
                .add(EventDeliveryService.class, eventDeliveryService)
                .add(VirtualProviderRegistryService.class, providerRegistryService)
                .add(VirtualNetworkFlowRuleStore.class, flowRuleStore)
                .add(VirtualNetworkFlowObjectiveStore.class, flowObjectiveStore);
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        manager.activate();

        vnet1 = setupVirtualNetworkTopology(manager, TID1);
        vnet2 = setupVirtualNetworkTopology(manager, TID2);

        service1 = new VirtualNetworkFlowObjectiveManager(manager, vnet1.id());
        service2 = new VirtualNetworkFlowObjectiveManager(manager, vnet2.id());
    }

    @After
    public void tearDownTest() {
        manager.deactivate();
        virtualNetworkManagerStore.deactivate();
    }

    /**
     * Tests adding a forwarding objective.
     */
    @Test
    public void forwardingObjective() {
        TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        ForwardingObjective forward =
                DefaultForwardingObjective.builder()
                        .fromApp(NetTestTools.APP_ID)
                        .withFlag(ForwardingObjective.Flag.SPECIFIC)
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .makePermanent()
                        .add(new ObjectiveContext() {
                            @Override
                            public void onSuccess(Objective objective) {
                                assertEquals("1 flowrule entry expected",
                                             1, flowRuleStore.getFlowRuleCount(vnet1.id()));
                                assertEquals("0 flowrule entry expected",
                                             0, flowRuleStore.getFlowRuleCount(vnet2.id()));
                            }
                        });

        service1.forward(VDID1, forward);
    }

    /**
     * Tests adding a next objective.
     */
    @Test
    public void nextObjective() {
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(service1.allocateNextId())
                .fromApp(NetTestTools.APP_ID)
                .addTreatment(treatment)
                .withType(NextObjective.Type.BROADCAST)
                .makePermanent()
                .add(new ObjectiveContext() {
                    @Override
                    public void onSuccess(Objective objective) {
                        assertEquals("1 next map entry expected",
                                     1, service1.getNextMappings().size());
                        assertEquals("0 next map entry expected",
                                     0, service2.getNextMappings().size());
                    }
                });

        service1.next(VDID1, nextObjective);
    }

    /**
     * Tests adding a filtering objective.
     */
    @Test
    public void filteringObjective() {
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        FilteringObjective filter =
                DefaultFilteringObjective.builder()
                        .fromApp(NetTestTools.APP_ID)
                        .withMeta(treatment)
                        .makePermanent()
                        .deny()
                        .addCondition(Criteria.matchEthType(12))
                        .add(new ObjectiveContext() {
                            @Override
                            public void onSuccess(Objective objective) {
                                assertEquals("1 flowrule entry expected",
                                             1,
                                             flowRuleStore.getFlowRuleCount(vnet1.id()));
                                assertEquals("0 flowrule entry expected",
                                             0,
                                             flowRuleStore.getFlowRuleCount(vnet2.id()));

                            }
                        });

        service1.filter(VDID1, filter);
    }

    //TODO: More test cases for filter, forward, and next

    private class TestProvider extends AbstractVirtualProvider
            implements VirtualFlowRuleProvider {

        protected TestProvider() {
            super(new ProviderId("test", "org.onosproject.virtual.testprovider"));
        }

        @Override
        public void applyFlowRule(NetworkId networkId, FlowRule... flowRules) {

        }

        @Override
        public void removeFlowRule(NetworkId networkId, FlowRule... flowRules) {

        }

        @Override
        public void executeBatch(NetworkId networkId, FlowRuleBatchOperation batch) {

        }
    }
}
