/*
 * Copyright 2018-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerAdapter;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.flowobjective.ObjectiveEvent;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.onlab.junit.TestTools.assertAfter;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.OsgiPropertyConstants.IFOM_OBJ_TIMEOUT_MS_DEFAULT;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class InOrderFlowObjectiveManagerTest {
    private InOrderFlowObjectiveManager mgr;

    private static final int PRIORITY = 1000;
    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "org.onosproject.test");
    private static final DeviceId DEV1 = DeviceId.deviceId("of:1");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);
    private static final PortNumber P4 = PortNumber.portNumber(4);
    private static final MacAddress M1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress M2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final MacAddress M3 = MacAddress.valueOf("00:00:00:00:00:03");
    private static final VlanId V1 = VlanId.vlanId((short) 10);
    private static final VlanId V2 = VlanId.vlanId((short) 20);
    private static final VlanId V3 = VlanId.vlanId((short) 30);
    private static final TrafficSelector S1 = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4).matchIPDst(IpPrefix.valueOf("10.0.0.1/32")).build();
    private static final TrafficSelector S2 = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4).matchIPDst(IpPrefix.valueOf("10.0.0.2/32")).build();
    private static final int NID1 = 1;
    private static final int NID2 = 2;
    private static final NextGroup NGRP1 = () -> new byte[] {0x00, 0x01};
    private static final NextGroup NGRP2 = () -> new byte[] {0x02, 0x03};

    // Delay flow objectives OFFSET + rand(0, BOUND) millis
    private static final int DEFAULT_OFFSET = 10; // ms
    private static final int DEFAULT_BOUND = 40; // ms
    private static final int TIMEOUT_THRESH = 500; // ms
    private static int offset = DEFAULT_OFFSET;
    private static int bound = DEFAULT_BOUND;

    private static final FilteringObjective FILT1 = buildFilteringObjective(P2, V3, M3, 1).add();
    private static final FilteringObjective FILT2 = buildFilteringObjective(P2, V2, M2, 2).add();
    private static final FilteringObjective FILT3 = buildFilteringObjective(P2, V3, M3, 3).remove();
    private static final FilteringObjective FILT4 = buildFilteringObjective(P1, V1, M1, 4).add();
    private static final FilteringObjective FILT5 = buildFilteringObjective(P2, V2, M2, 5).remove();
    private static final FilteringObjective FILT6 = buildFilteringObjective(P1, V1, M1, 6).remove();
    private static final FilteringObjective FILT7 = buildFilteringObjective(P2, V3, M3, 7).add();
    private List<FilteringObjective> expectFiltObjs = Lists.newCopyOnWriteArrayList(
            Lists.newArrayList(FILT1, FILT2, FILT3, FILT4, FILT5, FILT6, FILT7));

    private static final NextObjective NEXT1 = buildNextObjective(NID1, V1, Sets.newHashSet(P1)).add();
    private static final NextObjective NEXT2 = buildNextObjective(NID2, V2, Sets.newHashSet(P3)).add();
    private static final NextObjective NEXT3 = buildNextObjective(NID1, V1, Sets.newHashSet(P1, P2)).addToExisting();
    private static final NextObjective NEXT4 = buildNextObjective(NID2, V2, Sets.newHashSet(P3, P4)).addToExisting();
    private static final NextObjective NEXT5 = buildNextObjective(NID1, V1, Sets.newHashSet(P1)).removeFromExisting();
    private static final NextObjective NEXT6 = buildNextObjective(NID2, V2, Sets.newHashSet(P3)).removeFromExisting();
    private static final NextObjective NEXT7 = buildNextObjective(NID1, V1, Sets.newHashSet()).remove();
    private static final NextObjective NEXT8 = buildNextObjective(NID2, V2, Sets.newHashSet()).remove();
    private List<NextObjective> expectNextObjs = Lists.newCopyOnWriteArrayList(
            Lists.newArrayList(NEXT1, NEXT2, NEXT3, NEXT4, NEXT5, NEXT6, NEXT7, NEXT8));
    private List<NextObjective> expectNextObjsPending = Lists.newCopyOnWriteArrayList(
            Lists.newArrayList(NEXT5, NEXT6, NEXT1, NEXT2, NEXT3, NEXT4, NEXT7, NEXT8));

    private static final ForwardingObjective FWD1 = buildFwdObjective(S1, NID1).add();
    private static final ForwardingObjective FWD2 = buildFwdObjective(S2, NID2).add();
    private static final ForwardingObjective FWD3 = buildFwdObjective(S1, NID2).add();
    private static final ForwardingObjective FWD4 = buildFwdObjective(S2, NID1).add();
    private static final ForwardingObjective FWD5 = buildFwdObjective(S1, NID2).remove();
    private static final ForwardingObjective FWD6 = buildFwdObjective(S2, NID1).remove();
    private List<ForwardingObjective> expectFwdObjs = Lists.newCopyOnWriteArrayList(
            Lists.newArrayList(FWD1, FWD2, FWD3, FWD4, FWD5, FWD6));

    private List<Objective> actualObjs = Lists.newCopyOnWriteArrayList();

    private Pipeliner pipeliner = new PipelinerAdapter() {
        @Override
        public void filter(FilteringObjective filterObjective) {
            recordObjective(filterObjective);
        }

        @Override
        public void forward(ForwardingObjective forwardObjective) {
            recordObjective(forwardObjective);
        }

        @Override
        public void next(NextObjective nextObjective) {
            recordObjective(nextObjective);

            // Notify delegate when the next obj is completed
            ObjectiveEvent.Type type;
            if (nextObjective.op() == Objective.Operation.ADD ||
                    nextObjective.op() == Objective.Operation.ADD_TO_EXISTING) {
                type = ObjectiveEvent.Type.ADD;
            } else if (nextObjective.op() == Objective.Operation.REMOVE ||
                    nextObjective.op() == Objective.Operation.REMOVE_FROM_EXISTING) {
                type = ObjectiveEvent.Type.REMOVE;
            } else {
                return;
            }
            mgr.delegate.notify(new ObjectiveEvent(type, nextObjective.id()));
        }

        /**
         * Record the objectives.
         * The random delay is introduced in order to mimic pipeline and flow operation behavior.
         *
         * @param obj Flow objective
         */
        private void recordObjective(Objective obj) {
            try {
                Thread.sleep(new Random().nextInt(bound) + offset);
                if (!actualObjs.contains(obj)) {
                    actualObjs.add(obj);
                }
                obj.context().ifPresent(c -> c.onSuccess(obj));
            } catch (Exception e) {
                obj.context().ifPresent(c -> c.onError(obj, ObjectiveError.UNKNOWN));
            }
        }
    };

    @Before
    public void setUp() {
        internalSetup(IFOM_OBJ_TIMEOUT_MS_DEFAULT);
    }

    private void internalSetup(int objTimeoutMs) {
        mgr = new InOrderFlowObjectiveManager();
        mgr.objectiveTimeoutMs = objTimeoutMs;
        mgr.pipeliners.put(DEV1, pipeliner);
        mgr.installerExecutor = newFixedThreadPool(4, groupedThreads("foo", "bar"));
        mgr.cfgService = createMock(ComponentConfigService.class);
        mgr.deviceService = createMock(DeviceService.class);
        mgr.driverService = createMock(DriverService.class);
        mgr.flowObjectiveStore = createMock(FlowObjectiveStore.class);
        mgr.activate(null);

        reset(mgr.flowObjectiveStore);
        offset = DEFAULT_OFFSET;
        bound = DEFAULT_BOUND;
        actualObjs.clear();
    }

    @Test
    public void filter() {
        expectFiltObjs.forEach(filtObj -> mgr.filter(DEV1, filtObj));

        // Wait for the pipeline operation to complete
        int expectedTime = (bound + offset) * 7;
        assertAfter(expectedTime, expectedTime * 5, () -> assertEquals(expectFiltObjs.size(), actualObjs.size()));

        assertTrue(actualObjs.indexOf(FILT1) < actualObjs.indexOf(FILT2));
        assertTrue(actualObjs.indexOf(FILT2) < actualObjs.indexOf(FILT3));
        assertTrue(actualObjs.indexOf(FILT3) < actualObjs.indexOf(FILT5));
        assertTrue(actualObjs.indexOf(FILT5) < actualObjs.indexOf(FILT7));
        assertTrue(actualObjs.indexOf(FILT4) < actualObjs.indexOf(FILT6));
    }

    @Test
    public void forward() {
        expect(mgr.flowObjectiveStore.getNextGroup(NID1)).andReturn(NGRP1).times(3);
        expect(mgr.flowObjectiveStore.getNextGroup(NID2)).andReturn(NGRP2).times(3);
        replay(mgr.flowObjectiveStore);

        expectFwdObjs.forEach(fwdObj -> mgr.forward(DEV1, fwdObj));

        // Wait for the pipeline operation to complete
        int expectedTime = (bound + offset) * 6;
        assertAfter(expectedTime, expectedTime * 5, () -> assertEquals(expectFwdObjs.size(), actualObjs.size()));

        assertTrue(actualObjs.indexOf(FWD1) < actualObjs.indexOf(FWD3));
        assertTrue(actualObjs.indexOf(FWD3) < actualObjs.indexOf(FWD5));
        assertTrue(actualObjs.indexOf(FWD2) < actualObjs.indexOf(FWD4));
        assertTrue(actualObjs.indexOf(FWD4) < actualObjs.indexOf(FWD6));

        verify(mgr.flowObjectiveStore);
    }

    @Test
    public void forwardTimeout() {
        final AtomicInteger counter = new AtomicInteger(0);
        ForwardingObjective fwdTimeout = buildFwdObjective(S1, NID2).add(new ObjectiveContext() {
            @Override
            public void onError(Objective objective, ObjectiveError error) {
                if (Objects.equals(ObjectiveError.INSTALLATIONTIMEOUT, error)) {
                    counter.incrementAndGet();
                }
            }
        });
        List<ForwardingObjective> expectFwdObjsTimeout = Lists.newCopyOnWriteArrayList(
                Lists.newArrayList(fwdTimeout, FWD1, FWD2));

        // Reduce timeout so the unit test doesn't have to wait many seconds
        internalSetup(TIMEOUT_THRESH);

        expect(mgr.flowObjectiveStore.getNextGroup(NID1)).andReturn(NGRP1).times(1);
        expect(mgr.flowObjectiveStore.getNextGroup(NID2)).andReturn(NGRP2).times(2);
        replay(mgr.flowObjectiveStore);

        // Force this objective to time out
        offset = mgr.objectiveTimeoutMs * 3;

        expectFwdObjsTimeout.forEach(fwdObj -> mgr.forward(DEV1, fwdObj));

        // Wait for the pipeline operation to complete
        int expectedTime = (bound + offset) * 3;
        assertAfter(expectedTime, expectedTime * 5, () -> assertEquals(expectFwdObjsTimeout.size(), actualObjs.size()));

        assertAfter(expectedTime, expectedTime * 5, () -> assertTrue(counter.get() != 0));
        assertTrue(actualObjs.indexOf(fwdTimeout) < actualObjs.indexOf(FWD1));

        verify(mgr.flowObjectiveStore);
    }

    @Test
    public void forwardPending() {
        // Note: current logic will double check if the next obj need to be queued
        //       it does not check when resubmitting pending next back to the queue
        expect(mgr.flowObjectiveStore.getNextGroup(NID1)).andReturn(null).times(2);
        expect(mgr.flowObjectiveStore.getNextGroup(NID2)).andReturn(null).times(2);
        expect(mgr.flowObjectiveStore.getNextGroup(NID1)).andReturn(NGRP1).times(3);
        expect(mgr.flowObjectiveStore.getNextGroup(NID2)).andReturn(NGRP2).times(3);
        replay(mgr.flowObjectiveStore);

        expectFwdObjs.forEach(fwdObj -> mgr.forward(DEV1, fwdObj));

        // Trigger the next objectives
        mgr.next(DEV1, NEXT1);
        mgr.next(DEV1, NEXT2);

        // Wait for the pipeline operation to complete
        int expectedTime = (bound + offset) * 8;
        assertAfter(expectedTime, expectedTime * 5, () -> assertEquals(expectFwdObjs.size() + 2, actualObjs.size()));

        assertTrue(actualObjs.indexOf(NEXT1) < actualObjs.indexOf(FWD1));
        assertTrue(actualObjs.indexOf(FWD1) < actualObjs.indexOf(FWD3));
        assertTrue(actualObjs.indexOf(FWD3) < actualObjs.indexOf(FWD5));
        assertTrue(actualObjs.indexOf(NEXT2) < actualObjs.indexOf(FWD2));
        assertTrue(actualObjs.indexOf(FWD2) < actualObjs.indexOf(FWD4));
        assertTrue(actualObjs.indexOf(FWD4) < actualObjs.indexOf(FWD6));

        verify(mgr.flowObjectiveStore);
    }

    @Test
    public void next() {
        // Note: ADD operation won't query this
        expect(mgr.flowObjectiveStore.getNextGroup(NID1)).andReturn(NGRP1).times(3);
        expect(mgr.flowObjectiveStore.getNextGroup(NID2)).andReturn(NGRP2).times(3);
        replay(mgr.flowObjectiveStore);

        expectNextObjs.forEach(nextObj -> mgr.next(DEV1, nextObj));

        // Wait for the pipeline operation to complete
        int expectedTime = (bound + offset) * 8;
        assertAfter(expectedTime, expectedTime * 5, () -> assertEquals(expectNextObjs.size(), actualObjs.size()));

        assertTrue(actualObjs.indexOf(NEXT1) < actualObjs.indexOf(NEXT3));
        assertTrue(actualObjs.indexOf(NEXT3) < actualObjs.indexOf(NEXT5));
        assertTrue(actualObjs.indexOf(NEXT5) < actualObjs.indexOf(NEXT7));
        assertTrue(actualObjs.indexOf(NEXT2) < actualObjs.indexOf(NEXT4));
        assertTrue(actualObjs.indexOf(NEXT4) < actualObjs.indexOf(NEXT6));
        assertTrue(actualObjs.indexOf(NEXT6) < actualObjs.indexOf(NEXT8));

        verify(mgr.flowObjectiveStore);
    }

    // FIXME We currently do not handle the case when an app sends edit/remove of a next id before add.
    //       The edit/remove operation will be queued by pendingNext, and the add operation will be
    //       queued by the ordering queue forever due to the deadlock. This can be improved by making
    //       pendingForwards, pendingNexts and ordering queue caches.
    @Test
    @Ignore("Not supported")
    public void nextPending() {
        // Note: current logic will double check if the next obj need to be queued
        //       it does not check when resubmitting pending next back to the queue
        expect(mgr.flowObjectiveStore.getNextGroup(NID1)).andReturn(null).times(6);
        expect(mgr.flowObjectiveStore.getNextGroup(NID2)).andReturn(null).times(6);
        replay(mgr.flowObjectiveStore);

        expectNextObjsPending.forEach(nextObj -> mgr.next(DEV1, nextObj));

        // Wait for the pipeline operation to complete
        int expectedTime = (bound + offset) * 8;
        assertAfter(expectedTime, expectedTime * 5, () -> assertEquals(expectNextObjs.size(), actualObjs.size()));

        assertTrue(actualObjs.indexOf(NEXT1) < actualObjs.indexOf(NEXT5));
        assertTrue(actualObjs.indexOf(NEXT5) < actualObjs.indexOf(NEXT3));
        assertTrue(actualObjs.indexOf(NEXT3) < actualObjs.indexOf(NEXT7));
        assertTrue(actualObjs.indexOf(NEXT2) < actualObjs.indexOf(NEXT6));
        assertTrue(actualObjs.indexOf(NEXT6) < actualObjs.indexOf(NEXT4));
        assertTrue(actualObjs.indexOf(NEXT4) < actualObjs.indexOf(NEXT8));

        verify(mgr.flowObjectiveStore);
    }

    /**
     * Creates filtering objective builder with a serial number encoded in MPLS label.
     * The serial number is used to identify same objective that occurs multiple times.
     *
     * @param portnum Port number
     * @param vlanId VLAN Id
     * @param mac MAC address
     * @param serial Serial number
     * @return Filtering objective builder
     */
    private static FilteringObjective.Builder buildFilteringObjective(PortNumber portnum, VlanId vlanId,
                                                                      MacAddress mac, int serial) {
        FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
        fob.withKey(Criteria.matchInPort(portnum))
                .addCondition(Criteria.matchEthDst(mac))
                .addCondition(Criteria.matchVlanId(VlanId.NONE))
                .addCondition(Criteria.matchMplsLabel(MplsLabel.mplsLabel(serial)))
                .withPriority(PRIORITY);

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.pushVlan().setVlanId(vlanId);
        fob.withMeta(tBuilder.build());

        fob.permit().fromApp(APP_ID);
        return fob;
    }

    /**
     * Creates next objective builder.
     *
     * @param nextId next ID
     * @param vlanId VLAN ID
     * @param ports Set of ports that is in the given VLAN ID
     *
     * @return Next objective builder
     */
    private static NextObjective.Builder buildNextObjective(int nextId, VlanId vlanId, Collection<PortNumber> ports) {
        TrafficSelector metadata =
                DefaultTrafficSelector.builder().matchVlanId(vlanId).build();

        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST).fromApp(APP_ID)
                .withMeta(metadata);

        ports.forEach(port -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.popVlan();
            tBuilder.setOutput(port);
            nextObjBuilder.addTreatment(tBuilder.build());
        });

        return nextObjBuilder;
    }

    /**
     * Creates forwarding objective builder.
     *
     * @param selector Traffic selector
     * @param nextId next ID
     * @return Forwarding objective builder
     */
    private static ForwardingObjective.Builder buildFwdObjective(TrafficSelector selector, int nextId) {
        return DefaultForwardingObjective.builder()
                .makePermanent()
                .withSelector(selector)
                .nextStep(nextId)
                .fromApp(APP_ID)
                .withPriority(PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC);
    }
}