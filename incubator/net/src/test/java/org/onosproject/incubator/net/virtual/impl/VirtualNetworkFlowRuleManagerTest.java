/*
 * Copyright 2016-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
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
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowRuleStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.event.VirtualEvent;
import org.onosproject.incubator.net.virtual.event.VirtualListenerRegistryManager;
import org.onosproject.incubator.net.virtual.impl.provider.VirtualProviderManager;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualFlowRuleProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualFlowRuleProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.incubator.store.virtual.impl.SimpleVirtualFlowRuleStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.TestStorageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onosproject.net.flow.FlowRuleEvent.Type.*;

public class VirtualNetworkFlowRuleManagerTest extends TestDeviceParams {
    private static final int TIMEOUT = 10;

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private TestableIntentService intentService = new FakeIntentManager();
    private ServiceDirectory testDirectory;
    private VirtualNetworkFlowRuleStore flowRuleStore;
    private VirtualProviderManager providerRegistryService;

    private EventDeliveryService eventDeliveryService;
    VirtualListenerRegistryManager listenerRegistryManager =
            VirtualListenerRegistryManager.getInstance();

    private VirtualNetworkFlowRuleManager vnetFlowRuleService1;
    private VirtualNetworkFlowRuleManager vnetFlowRuleService2;

    private VirtualFlowRuleProvider provider = new TestProvider();
    private VirtualFlowRuleProviderService providerService1;
    private VirtualFlowRuleProviderService providerService2;

    protected TestFlowRuleListener listener1 = new TestFlowRuleListener();
    protected TestFlowRuleListener listener2 = new TestFlowRuleListener();

    private final TenantId tid1 = TenantId.tenantId("tid1");
    private final TenantId tid2 = TenantId.tenantId("tid2");

    private VirtualNetwork vnet1;
    private VirtualNetwork vnet2;

    private ApplicationId appId;

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        CoreService coreService = new TestCoreService();
        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        flowRuleStore = new SimpleVirtualFlowRuleStore();

        providerRegistryService = new VirtualProviderManager();
        providerRegistryService.registerProvider(provider);

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.intentService = intentService;
        TestUtils.setField(manager, "coreService", coreService);

        eventDeliveryService = new TestEventDispatcher();
        NetTestTools.injectEventDispatcher(manager, eventDeliveryService);
        eventDeliveryService.addSink(VirtualEvent.class, listenerRegistryManager);

        appId = new TestApplicationId("FlowRuleManagerTest");

        testDirectory = new TestServiceDirectory()
                .add(VirtualNetworkStore.class, virtualNetworkManagerStore)
                .add(CoreService.class, coreService)
                .add(VirtualProviderRegistryService.class, providerRegistryService)
                .add(EventDeliveryService.class, eventDeliveryService)
                .add(VirtualNetworkFlowRuleStore.class, flowRuleStore);
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        manager.activate();

        vnet1 = setupVirtualNetworkTopology(tid1);
        vnet2 = setupVirtualNetworkTopology(tid2);

        vnetFlowRuleService1 = new VirtualNetworkFlowRuleManager(manager, vnet1.id());
        vnetFlowRuleService2 = new VirtualNetworkFlowRuleManager(manager, vnet2.id());
        vnetFlowRuleService1.addListener(listener1);
        vnetFlowRuleService2.addListener(listener2);

        vnetFlowRuleService1.operationsService = MoreExecutors.newDirectExecutorService();
        vnetFlowRuleService2.operationsService = MoreExecutors.newDirectExecutorService();
        vnetFlowRuleService1.deviceInstallers = MoreExecutors.newDirectExecutorService();
        vnetFlowRuleService2.deviceInstallers = MoreExecutors.newDirectExecutorService();

        providerService1 = (VirtualFlowRuleProviderService)
                providerRegistryService.getProviderService(vnet1.id(), VirtualFlowRuleProvider.class);
        providerService2 = (VirtualFlowRuleProviderService)
                providerRegistryService.getProviderService(vnet2.id(), VirtualFlowRuleProvider.class);
    }

    @After
    public void tearDown() {
        manager.deactivate();
        virtualNetworkManagerStore.deactivate();
    }

    /**
     * Method to create the virtual network for further testing.
     *
     * @return virtual network
     */
    private VirtualNetwork setupVirtualNetworkTopology(TenantId tenantId) {
        manager.registerTenantId(tenantId);
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(tenantId);

        VirtualDevice virtualDevice1 =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice virtualDevice2 =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);
        VirtualDevice virtualDevice3 =
                manager.createVirtualDevice(virtualNetwork.id(), DID3);
        VirtualDevice virtualDevice4 =
                manager.createVirtualDevice(virtualNetwork.id(), DID4);

        ConnectPoint cp1 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), cp1.deviceId(), cp1.port(), cp1);

        ConnectPoint cp2 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), cp2.deviceId(), cp2.port(), cp2);

        ConnectPoint cp3 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(3));
        manager.createVirtualPort(virtualNetwork.id(), cp3.deviceId(), cp3.port(), cp3);

        ConnectPoint cp4 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(4));
        manager.createVirtualPort(virtualNetwork.id(), cp4.deviceId(), cp4.port(), cp4);

        ConnectPoint cp5 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(5));
        manager.createVirtualPort(virtualNetwork.id(), cp5.deviceId(), cp5.port(), cp5);

        ConnectPoint cp6 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(6));
        manager.createVirtualPort(virtualNetwork.id(), cp6.deviceId(), cp6.port(), cp6);

        VirtualLink link1 = manager.createVirtualLink(virtualNetwork.id(), cp1, cp3);
        virtualNetworkManagerStore.updateLink(link1, link1.tunnelId(), Link.State.ACTIVE);
        VirtualLink link2 = manager.createVirtualLink(virtualNetwork.id(), cp3, cp1);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.ACTIVE);
        VirtualLink link3 = manager.createVirtualLink(virtualNetwork.id(), cp4, cp5);
        virtualNetworkManagerStore.updateLink(link3, link3.tunnelId(), Link.State.ACTIVE);
        VirtualLink link4 = manager.createVirtualLink(virtualNetwork.id(), cp5, cp4);
        virtualNetworkManagerStore.updateLink(link4, link4.tunnelId(), Link.State.ACTIVE);
        VirtualLink link5 = manager.createVirtualLink(virtualNetwork.id(), cp2, cp6);
        virtualNetworkManagerStore.updateLink(link5, link5.tunnelId(), Link.State.ACTIVE);
        VirtualLink link6 = manager.createVirtualLink(virtualNetwork.id(), cp6, cp2);
        virtualNetworkManagerStore.updateLink(link6, link6.tunnelId(), Link.State.ACTIVE);

        return virtualNetwork;
    }

    private FlowRule flowRule(int tsval, int trval) {
        return flowRule(DID1, tsval, trval);
    }

    private FlowRule flowRule(DeviceId did, int tsval, int trval) {
        TestSelector ts = new TestSelector(tsval);
        TestTreatment tr = new TestTreatment(trval);
        return DefaultFlowRule.builder()
                .forDevice(did)
                .withSelector(ts)
                .withTreatment(tr)
                .withPriority(10)
                .fromApp(appId)
                .makeTemporary(TIMEOUT)
                .build();
    }

    private FlowRule addFlowRule(int hval) {
        FlowRule rule = flowRule(hval, hval);
        vnetFlowRuleService1.applyFlowRules(rule);

        assertNotNull("rule should be found", vnetFlowRuleService1.getFlowEntries(DID1));
        return rule;
    }

    private int flowCount(FlowRuleService service) {
        List<FlowEntry> entries = Lists.newArrayList();
        service.getFlowEntries(DID1).forEach(entries::add);
        return entries.size();
    }

    @Test
    public void getFlowEntries() {
        assertTrue("store should be empty",
                   Sets.newHashSet(vnetFlowRuleService1.getFlowEntries(DID1)).isEmpty());
        assertTrue("store should be empty",
                   Sets.newHashSet(vnetFlowRuleService2.getFlowEntries(DID1)).isEmpty());

        FlowRule f1 = addFlowRule(1);
        FlowRule f2 = addFlowRule(2);

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);

        assertEquals("2 rules should exist", 2, flowCount(vnetFlowRuleService1));
        assertEquals("0 rules should exist", 0, flowCount(vnetFlowRuleService2));

        providerService1.pushFlowMetrics(DID1, ImmutableList.of(fe1, fe2));
        validateEvents(listener1, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_ADDED, RULE_ADDED);

        addFlowRule(1);
        assertEquals("should still be 2 rules", 2, flowCount(vnetFlowRuleService1));
        System.err.println("events :" + listener1.events);
        assertEquals("0 rules should exist", 0, flowCount(vnetFlowRuleService2));

        providerService1.pushFlowMetrics(DID1, ImmutableList.of(fe1));
        validateEvents(listener1, RULE_UPDATED, RULE_UPDATED);
    }

    @Test
    public void applyFlowRules() {
        FlowRule r1 = flowRule(1, 1);
        FlowRule r2 = flowRule(2, 2);
        FlowRule r3 = flowRule(3, 3);

        assertTrue("store should be empty",
                   Sets.newHashSet(vnetFlowRuleService1.getFlowEntries(DID1)).isEmpty());
        vnetFlowRuleService1.applyFlowRules(r1, r2, r3);
        assertEquals("3 rules should exist", 3, flowCount(vnetFlowRuleService1));
        assertTrue("Entries should be pending add.",
                   validateState(ImmutableMap.of(
                           r1, FlowEntry.FlowEntryState.PENDING_ADD,
                           r2, FlowEntry.FlowEntryState.PENDING_ADD,
                           r3, FlowEntry.FlowEntryState.PENDING_ADD)));
    }

    @Test
    public void purgeFlowRules() {
        FlowRule f1 = addFlowRule(1);
        FlowRule f2 = addFlowRule(2);
        FlowRule f3 = addFlowRule(3);
        assertEquals("3 rules should exist", 3, flowCount(vnetFlowRuleService1));
        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);
        FlowEntry fe3 = new DefaultFlowEntry(f3);
        providerService1.pushFlowMetrics(DID1, ImmutableList.of(fe1, fe2, fe3));
        validateEvents(listener1, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_ADDED, RULE_ADDED, RULE_ADDED);
        vnetFlowRuleService1.purgeFlowRules(DID1);
        assertEquals("0 rule should exist", 0, flowCount(vnetFlowRuleService1));
    }

    @Test
    public void removeFlowRules() {
        FlowRule f1 = addFlowRule(1);
        FlowRule f2 = addFlowRule(2);
        FlowRule f3 = addFlowRule(3);
        assertEquals("3 rules should exist", 3, flowCount(vnetFlowRuleService1));

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);
        FlowEntry fe3 = new DefaultFlowEntry(f3);
        providerService1.pushFlowMetrics(DID1, ImmutableList.of(fe1, fe2, fe3));
        validateEvents(listener1, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_ADDED, RULE_ADDED, RULE_ADDED);

        vnetFlowRuleService1.removeFlowRules(f1, f2);
        //removing from north, so no events generated
        validateEvents(listener1, RULE_REMOVE_REQUESTED, RULE_REMOVE_REQUESTED);
        assertEquals("3 rule should exist", 3, flowCount(vnetFlowRuleService1));
        assertTrue("Entries should be pending remove.",
                   validateState(ImmutableMap.of(
                           f1, FlowEntry.FlowEntryState.PENDING_REMOVE,
                           f2, FlowEntry.FlowEntryState.PENDING_REMOVE,
                           f3, FlowEntry.FlowEntryState.ADDED)));

        vnetFlowRuleService1.removeFlowRules(f1);
        assertEquals("3 rule should still exist", 3, flowCount(vnetFlowRuleService1));
    }

    @Test
    public void flowRemoved() {
        FlowRule f1 = addFlowRule(1);
        FlowRule f2 = addFlowRule(2);
        StoredFlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);

        providerService1.pushFlowMetrics(DID1, ImmutableList.of(fe1, fe2));
        vnetFlowRuleService1.removeFlowRules(f1);

        //FIXME modification of "stored" flow entry outside of store
        fe1.setState(FlowEntry.FlowEntryState.REMOVED);

        providerService1.flowRemoved(fe1);

        validateEvents(listener1, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADDED,
                       RULE_ADDED, RULE_REMOVE_REQUESTED, RULE_REMOVED);

        providerService1.flowRemoved(fe1);
        validateEvents(listener1);

        FlowRule f3 = flowRule(3, 3);
        FlowEntry fe3 = new DefaultFlowEntry(f3);
        vnetFlowRuleService1.applyFlowRules(f3);

        providerService1.pushFlowMetrics(DID1, Collections.singletonList(fe3));
        validateEvents(listener1, RULE_ADD_REQUESTED, RULE_ADDED, RULE_UPDATED);

        providerService1.flowRemoved(fe3);
        validateEvents(listener1);
    }

    @Test
    public void extraneousFlow() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);
        FlowRule f3 = flowRule(3, 3);
        vnetFlowRuleService1.applyFlowRules(f1, f2);

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);
        FlowEntry fe3 = new DefaultFlowEntry(f3);


        providerService1.pushFlowMetrics(DID1, Lists.newArrayList(fe1, fe2, fe3));

        validateEvents(listener1, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_ADDED, RULE_ADDED);
    }

    /*
     * Tests whether a rule that was marked for removal but no flowRemoved was received
     * is indeed removed at the next stats update.
     */
    @Test
    public void flowMissingRemove() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);
        FlowRule f3 = flowRule(3, 3);

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);
        vnetFlowRuleService1.applyFlowRules(f1, f2, f3);

        vnetFlowRuleService1.removeFlowRules(f3);

        providerService1.pushFlowMetrics(DID1, Lists.newArrayList(fe1, fe2));

        validateEvents(listener1, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_REMOVE_REQUESTED, RULE_ADDED, RULE_ADDED, RULE_REMOVED);
    }

    @Test
    public void removeByAppId() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);
        vnetFlowRuleService1.applyFlowRules(f1, f2);

        vnetFlowRuleService1.removeFlowRulesById(appId);

        //only check that we are in pending remove. Events and actual remove state will
        // be set by flowRemoved call.
        validateState(ImmutableMap.of(
                f1, FlowEntry.FlowEntryState.PENDING_REMOVE,
                f2, FlowEntry.FlowEntryState.PENDING_REMOVE));
    }

    //TODO:Tests for fallback

    private boolean validateState(Map<FlowRule, FlowEntry.FlowEntryState> expected) {
        Map<FlowRule, FlowEntry.FlowEntryState> expectedToCheck = new HashMap<>(expected);
        Iterable<FlowEntry> rules = vnetFlowRuleService1.getFlowEntries(DID1);
        for (FlowEntry f : rules) {
            assertTrue("Unexpected FlowRule " + f, expectedToCheck.containsKey(f));
            assertEquals("FlowEntry" + f, expectedToCheck.get(f), f.state());
            expectedToCheck.remove(f);
        }
        assertEquals(Collections.emptySet(), expectedToCheck.entrySet());
        return true;
    }

    private class TestSelector implements TrafficSelector {

        //for controlling hashcode uniqueness;
        private final int testval;

        public TestSelector(int val) {
            testval = val;
        }

        @Override
        public Set<Criterion> criteria() {
            return Collections.emptySet();
        }

        @Override
        public Criterion getCriterion(
                org.onosproject.net.flow.criteria.Criterion.Type type) {
            return null;
        }

        @Override
        public int hashCode() {
            return testval;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TestSelector) {
                return this.testval == ((TestSelector) o).testval;
            }
            return false;
        }
    }

    private class TestTreatment implements TrafficTreatment {

        //for controlling hashcode uniqueness;
        private final int testval;

        public TestTreatment(int val) {
            testval = val;
        }

        @Override
        public List<Instruction> deferred() {
            return null;
        }

        @Override
        public List<Instruction> immediate() {
            return null;
        }

        @Override
        public List<Instruction> allInstructions() {
            return null;
        }

        @Override
        public Instructions.TableTypeTransition tableTransition() {
            return null;
        }

        @Override
        public boolean clearedDeferred() {
            return false;
        }

        @Override
        public int hashCode() {
            return testval;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TestTreatment) {
                return this.testval == ((TestTreatment) o).testval;
            }
            return false;
        }

        @Override
        public Instructions.MetadataInstruction writeMetadata() {
            return null;
        }

        @Override
        public Instructions.MeterInstruction metered() {
            return null;
        }
    }

    private void validateEvents(TestFlowRuleListener listener, FlowRuleEvent.Type... events) {
        if (events == null) {
            assertTrue("events generated", listener.events.isEmpty());
        }

        int i = 0;
        System.err.println("events :" + listener.events);
        for (FlowRuleEvent e : listener.events) {
            assertEquals("unexpected event", events[i], e.type());
            i++;
        }

        assertEquals("mispredicted number of events",
                     events.length, listener.events.size());

        listener.events.clear();
    }

    private class TestFlowRuleListener implements FlowRuleListener {

        public final List<FlowRuleEvent> events = new ArrayList<>();

        @Override
        public void event(FlowRuleEvent event) {
            events.add(event);
        }
    }

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
