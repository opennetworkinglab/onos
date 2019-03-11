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
package org.onosproject.net.flowobjective.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.ChassisId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.behaviour.DefaultNextGroup;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.PipelinerAdapter;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdapter;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveEvent;
import org.onosproject.net.intent.TestTools;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.TestUtils.TestUtilsException;

/**
 * Tests for the flow objective manager.
 */
public class FlowObjectiveManagerTest {

    private static final int RETRY_MS = 250;
    private FlowObjectiveManager manager;
    DeviceId id1 = NetTestTools.did("d1");
    DefaultDevice d1 = new DefaultDevice(NetTestTools.PID, id1, Device.Type.SWITCH,
                                         "test", "1.0", "1.0",
                                         "abacab", new ChassisId("c"),
                                         DefaultAnnotations.EMPTY);

    DeviceId id2 = NetTestTools.did("d2");
    DefaultDevice d2 = new DefaultDevice(NetTestTools.PID, id2, Device.Type.SWITCH,
                                         "test", "1.0", "1.0",
                                         "abacab", new ChassisId("c"),
                                         DefaultAnnotations.EMPTY);

    List<String> filteringObjectives;
    List<String> forwardingObjectives;
    List<String> nextObjectives;

    private class TestDeviceService extends DeviceServiceAdapter {

        List<Device> deviceList;

        TestDeviceService() {
            deviceList = new ArrayList<>();

            deviceList.add(d1);
        }

        @Override
        public Iterable<Device> getDevices() {
            return deviceList;
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return true;
        }
    }

    private class TestFlowObjectiveStore extends FlowObjectiveStoreAdapter {
        @Override
        public NextGroup getNextGroup(Integer nextId) {
            if (nextId != 4) {
                byte[] data = new byte[1];
                data[0] = 5;
                return new DefaultNextGroup(data);
            } else {
                return null;
            }
        }

    }

    private class TestDriver extends DriverAdapter {

        @Override
        public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Behaviour> T createBehaviour(DriverData data, Class<T> behaviourClass) {
            return (T) new TestPipeliner();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Behaviour> T createBehaviour(DriverHandler handler, Class<T> behaviourClass) {
            return (T) new TestPipeliner();
        }

    }

    private class TestPipeliner extends PipelinerAdapter {
        DeviceId deviceId;

        @Override
        public void init(DeviceId deviceId, PipelinerContext context) {
            this.deviceId = deviceId;
        }

        @Override
        public void filter(FilteringObjective filterObjective) {
            filteringObjectives.add(deviceId.toString());
        }

        @Override
        public void forward(ForwardingObjective forwardObjective) {
            forwardingObjectives.add(deviceId.toString());
        }

        @Override
        public void next(NextObjective nextObjective) {
            nextObjectives.add(deviceId.toString());
        }
    }

    private class TestDriverService extends DriverServiceAdapter {
        @Override
        public DriverHandler createHandler(DeviceId deviceId, String... credentials) {
            Driver driver = new TestDriver();
            return new DefaultDriverHandler(new DefaultDriverData(driver, id1));
        }
    }

    private class TestComponentConfigService extends ComponentConfigAdapter {
    }

    @Before
    public void initializeTest() {
        manager = new FlowObjectiveManager();
        manager.flowObjectiveStore = new TestFlowObjectiveStore();
        manager.deviceService = new TestDeviceService();
        manager.driverService = new TestDriverService();
        manager.cfgService = new TestComponentConfigService();

        filteringObjectives = new ArrayList<>();
        forwardingObjectives = new ArrayList<>();
        nextObjectives = new ArrayList<>();
        manager.activate(null);
    }

    @After
    public void tearDownTest() {
        manager.deactivate();
        manager = null;
        filteringObjectives.clear();
        forwardingObjectives.clear();
        nextObjectives.clear();
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
                        .add();

        manager.forward(id1, forward);

        TestTools.assertAfter(RETRY_MS, () ->
            assertThat(forwardingObjectives, hasSize(1)));

        assertThat(forwardingObjectives, hasItem("of:d1"));
        assertThat(filteringObjectives, hasSize(0));
        assertThat(nextObjectives, hasSize(0));
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
                        .add();

        manager.activate(null);
        manager.filter(id1, filter);

        TestTools.assertAfter(RETRY_MS, () ->
                assertThat(filteringObjectives, hasSize(1)));

        assertThat(forwardingObjectives, hasSize(0));
        assertThat(filteringObjectives, hasItem("of:d1"));
        assertThat(nextObjectives, hasSize(0));
    }

    /**
     * Tests adding a next objective.
     */
    @Test
    public void nextObjective() {
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        NextObjective next =
                DefaultNextObjective.builder()
                        .withId(manager.allocateNextId())
                        .addTreatment(treatment)
                        .withType(NextObjective.Type.BROADCAST)
                        .fromApp(NetTestTools.APP_ID)
                        .makePermanent()
                        .add();

        manager.next(id1, next);

        TestTools.assertAfter(RETRY_MS, () ->
                assertThat(nextObjectives, hasSize(1)));

        assertThat(forwardingObjectives, hasSize(0));
        assertThat(filteringObjectives, hasSize(0));
        assertThat(nextObjectives, hasItem("of:d1"));
    }

    /**
     * Tests adding a pending forwarding objective.
     *
     * @throws TestUtilsException if lookup of a field fails
     */
    @Test
    public void pendingForwardingObjective() throws TestUtilsException {
        TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        ForwardingObjective forward4 =
                DefaultForwardingObjective.builder()
                        .fromApp(NetTestTools.APP_ID)
                        .withFlag(ForwardingObjective.Flag.SPECIFIC)
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .makePermanent()
                        .nextStep(4)
                        .add();
        ForwardingObjective forward5 =
                DefaultForwardingObjective.builder()
                        .fromApp(NetTestTools.APP_ID)
                        .withFlag(ForwardingObjective.Flag.SPECIFIC)
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .makePermanent()
                        .nextStep(5)
                        .add();

        //  multiple pending forwards should be combined
        manager.forward(id1, forward4);
        manager.forward(id1, forward4);
        manager.forward(id1, forward5);


        //  1 should be complete, 1 pending
        TestTools.assertAfter(RETRY_MS, () ->
                assertThat(forwardingObjectives, hasSize(1)));

        assertThat(forwardingObjectives, hasItem("of:d1"));
        assertThat(filteringObjectives, hasSize(0));
        assertThat(nextObjectives, hasSize(0));

        // Now send events to trigger the objective still in the queue
        ObjectiveEvent event1 = new ObjectiveEvent(ObjectiveEvent.Type.ADD, 4);
        FlowObjectiveStoreDelegate delegate = TestUtils.getField(manager, "delegate");
        delegate.notify(event1);

        // all should be processed now
        TestTools.assertAfter(RETRY_MS, () ->
                assertThat(forwardingObjectives, hasSize(2)));
        assertThat(forwardingObjectives, hasItem("of:d1"));
        assertThat(filteringObjectives, hasSize(0));
        assertThat(nextObjectives, hasSize(0));
    }

    /**
     * Tests receipt of a device up event.
     *
     * @throws TestUtilsException if lookup of a field fails
     */
    @Test
    public void deviceUpEvent() throws TestUtilsException {
        TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        DeviceEvent event = new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, d2);
        DeviceListener listener = TestUtils.getField(manager, "deviceListener");
        assertThat(listener, notNullValue());

        listener.event(event);

        ForwardingObjective forward =
                DefaultForwardingObjective.builder()
                        .fromApp(NetTestTools.APP_ID)
                        .withFlag(ForwardingObjective.Flag.SPECIFIC)
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .makePermanent()
                        .add();
        manager.forward(id2, forward);

        // new device should have an objective now
        TestTools.assertAfter(RETRY_MS, () ->
                assertThat(forwardingObjectives, hasSize(1)));

        assertThat(forwardingObjectives, hasItem("of:d2"));
        assertThat(filteringObjectives, hasSize(0));
        assertThat(nextObjectives, hasSize(0));
    }
}
