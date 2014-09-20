package org.onlab.onos.net.flow.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_ADDED;
import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_REMOVED;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.event.impl.TestEventDispatcher;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Device.Type;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.onos.net.trivial.flow.impl.SimpleFlowRuleStore;

/**
 * Test codifying the flow rule service & flow rule provider service contracts.
 */
public class SimpleFlowRuleManagerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID = DeviceId.deviceId("of:001");
    private static final Device DEV = new DefaultDevice(
            PID, DID, Type.SWITCH, "", "", "", "");

    private SimpleFlowRuleManager mgr;

    protected FlowRuleService service;
    protected FlowRuleProviderRegistry registry;
    protected FlowRuleProviderService providerSerivce;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();

    @Before
    public void setUp() {
        mgr = new SimpleFlowRuleManager();
        mgr.store = new SimpleFlowRuleStore();
        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.deviceService = new TestDeviceService();
        service = mgr;
        registry = mgr;

        mgr.activate();
        mgr.addListener(listener);
        provider = new TestProvider(PID);
        providerSerivce = registry.register(provider);
        assertTrue("provider should be registered",
                registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                registry.getProviders().contains(provider.id()));
        service.removeListener(listener);
        mgr.deactivate();
        mgr.eventDispatcher = null;
        mgr.deviceService = null;
    }

    private FlowRule flowRule(int tsval, int trval) {
        TestSelector ts = new TestSelector(tsval);
        TestTreatment tr = new TestTreatment(trval);
        return new DefaultFlowRule(DID, ts, tr, 0);
    }

    private void addFlowRule(int hval) {
        FlowRule rule = flowRule(hval, hval);
        providerSerivce.flowAdded(rule);
        assertNotNull("rule should be found", service.getFlowEntries(DID));
    }

    private void validateEvents(FlowRuleEvent.Type ... events) {
        if (events == null) {
            assertTrue("events generated", listener.events.isEmpty());
        }

        int i = 0;
        for (FlowRuleEvent e : listener.events) {
            assertTrue("unexpected event", e.type().equals(events[i]));
            i++;
        }

        assertEquals("mispredicted number of events",
                events.length, listener.events.size());

        listener.events.clear();
    }

    private int flowCount() {
        return Sets.newHashSet(service.getFlowEntries(DID)).size();
    }
    @Test
    public void getFlowEntries() {
        assertTrue("store should be empty",
                Sets.newHashSet(service.getFlowEntries(DID)).isEmpty());
        addFlowRule(1);
        addFlowRule(2);
        assertEquals("2 rules should exist", 2, flowCount());
        validateEvents(RULE_ADDED, RULE_ADDED);

        addFlowRule(1);
        assertEquals("should still be 2 rules", 2, flowCount());
        validateEvents();
    }

    @Test
    public void applyFlowRules() {
        TestSelector ts = new TestSelector(1);
        FlowRule r1 = flowRule(1, 1);
        FlowRule r2 = flowRule(1, 2);
        FlowRule r3 = flowRule(1, 3);

        //current FlowRules always return 0. FlowEntries inherit the value
        FlowRule e1 = new DefaultFlowRule(DID, ts, r1.treatment(), 0);
        FlowRule e2 = new DefaultFlowRule(DID, ts, r2.treatment(), 0);
        FlowRule e3 = new DefaultFlowRule(DID, ts, r3.treatment(), 0);
        List<FlowRule> fel = Lists.newArrayList(e1, e2, e3);

        assertTrue("store should be empty",
                Sets.newHashSet(service.getFlowEntries(DID)).isEmpty());
        List<FlowRule> ret = mgr.applyFlowRules(r1, r2, r3);
        assertEquals("3 rules should exist", 3, flowCount());
        assertTrue("3 entries should result", fel.containsAll(ret));
    }

    @Test
    public void removeFlowRules() {
        addFlowRule(1);
        addFlowRule(2);
        addFlowRule(3);
        assertEquals("3 rules should exist", 3, flowCount());
        validateEvents(RULE_ADDED, RULE_ADDED, RULE_ADDED);

        FlowRule rem1 = flowRule(1, 1);
        FlowRule rem2 = flowRule(2, 2);
        mgr.removeFlowRules(rem1, rem2);
        //removing from north, so no events generated
        validateEvents();
        assertEquals("1 rule should exist", 1, flowCount());

        mgr.removeFlowRules(rem1);
        assertEquals("1 rule should still exist", 1, flowCount());
    }

    @Test
    public void flowRemoved() {
        addFlowRule(1);
        addFlowRule(2);
        FlowRule rem1 = flowRule(1, 1);
        providerSerivce.flowRemoved(rem1);
        validateEvents(RULE_ADDED, RULE_ADDED, RULE_REMOVED);

        providerSerivce.flowRemoved(rem1);
        validateEvents();
    }

    private static class TestListener implements FlowRuleListener {
        final List<FlowRuleEvent> events = new ArrayList<>();

        @Override
        public void event(FlowRuleEvent event) {
            events.add(event);
        }
    }

    private static class TestDeviceService implements DeviceService {

        @Override
        public int getDeviceCount() {
            return 0;
        }

        @Override
        public Iterable<Device> getDevices() {
            return null;
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return DEV;
        }

        @Override
        public MastershipRole getRole(DeviceId deviceId) {
            return null;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return null;
        }

        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            return null;
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return false;
        }

        @Override
        public void addListener(DeviceListener listener) {
        }

        @Override
        public void removeListener(DeviceListener listener) {
        }

    }

    private class TestProvider extends AbstractProvider implements FlowRuleProvider {

        protected TestProvider(ProviderId id) {
            super(PID);
        }

        @Override
        public void applyFlowRule(FlowRule... flowRules) {
        }

        @Override
        public void removeFlowRule(FlowRule... flowRules) {
        }

    }

    private class TestSelector implements TrafficSelector {

        //for controlling hashcode uniqueness;
        private final int testval;

        public TestSelector(int val) {
            testval = val;
        }

        @Override
        public List<Criterion> criteria() {
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
        public List<Instruction> instructions() {
            return null;
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

    }

}
