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
package org.onosproject.incubator.net.l2monitoring.cfm.impl;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.l2monitoring.cfm.Component;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultComponent;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.DeviceId;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertFalse;
import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * CFM MD Manager test.
 */
public class CfmMdManagerTest {
    private static final NodeId NID_LOCAL = new NodeId("local");
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");
    private static final MaIdShort MA_ID_1_1 = MaIdCharStr.asMaId("test-ma-1-1");
    private static final MaIdShort MA_ID_1_2 = MaIdCharStr.asMaId("test-ma-1-2");
    private static final MdId MD_ID_1 = MdIdCharStr.asMdId("test-md-1");

    private final CfmMepService mepService = createMock(CfmMepService.class);

    private DistributedMdStore mdStore;
    private CfmMdService service;
    private CfmMdManager manager;

    @Before
    public void setup() throws Exception, CfmConfigException {
        mdStore = new DistributedMdStore();

        MaintenanceAssociation maTest11 = DefaultMaintenanceAssociation
                .builder(MA_ID_1_1, MD_ID_1.getNameLength())
                .ccmInterval(MaintenanceAssociation.CcmInterval.INTERVAL_10MIN)
                .maNumericId((short) 1)
                .addToRemoteMepIdList(MepId.valueOf((short) 101))
                .addToRemoteMepIdList(MepId.valueOf((short) 102))
                .addToComponentList(
                        DefaultComponent.builder(1)
                                .tagType(Component.TagType.VLAN_CTAG)
                                .build())
                .build();

        MaintenanceAssociation maTest12 = DefaultMaintenanceAssociation
                .builder(MA_ID_1_2, MD_ID_1.getNameLength())
                .ccmInterval(MaintenanceAssociation.CcmInterval.INTERVAL_10MIN)
                .maNumericId((short) 2)
                .addToRemoteMepIdList(MepId.valueOf((short) 201))
                .addToRemoteMepIdList(MepId.valueOf((short) 202))
                .addToComponentList(
                        DefaultComponent.builder(2)
                                .tagType(Component.TagType.VLAN_CTAG)
                                .build())
                .build();

        MaintenanceDomain mdTest1 = DefaultMaintenanceDomain
                .builder(MD_ID_1)
                .mdLevel(MaintenanceDomain.MdLevel.LEVEL1)
                .mdNumericId((short) 1)
                .addToMaList(maTest11)
                .addToMaList(maTest12)
                .build();

        TestUtils.setField(mdStore, "storageService", new TestStorageService());
        TestUtils.setField(mdStore, "clusterService", new CfmMdManagerTest.TestClusterService());
        TestUtils.setField(mdStore, "mastershipService", new CfmMdManagerTest.TestMastershipService());

        mdStore.activate();
        mdStore.createUpdateMaintenanceDomain(mdTest1);

        manager = new CfmMdManager();
        manager.store = mdStore;
        service = manager;
        TestUtils.setField(manager, "storageService", new TestStorageService());
        TestUtils.setField(manager, "coreService", new TestCoreService());
        TestUtils.setField(manager, "mepService", mepService);
        injectEventDispatcher(manager, new TestEventDispatcher());

        manager.appId = new CfmMdManagerTest.TestApplicationId(0, "CfmMdManagerTest");
        manager.activate();
    }

    @After
    public void tearDown() {

        manager.deactivate();
        injectEventDispatcher(manager, null);

    }

    @Test
    public void testGetAllMaintenanceDomain() {
        Collection<MaintenanceDomain> mdList = service.getAllMaintenanceDomain();
        assertEquals(1, mdList.size());

        MaintenanceDomain md = mdList.iterator().next();
        assertEquals(1, md.mdNumericId());

        assertEquals(2, md.maintenanceAssociationList().size());

        md.maintenanceAssociationList().iterator().forEachRemaining(ma ->
                assertTrue(ma.maId().maName().endsWith(String.valueOf(ma.maNumericId())))
        );
    }

    @Test
    public void testGetMaintenanceDomain() {
        Optional<MaintenanceDomain> md =
                service.getMaintenanceDomain(MdIdCharStr.asMdId("test-md-1"));
        assertTrue(md.isPresent());

        assertEquals(1, md.get().mdNumericId());

        assertEquals(2, md.get().maintenanceAssociationList().size());



        //Now try an invalid name
        Optional<MaintenanceDomain> mdInvalid =
                service.getMaintenanceDomain(MdIdCharStr.asMdId("test-md-3"));
        assertFalse(mdInvalid.isPresent());
    }

    @Test
    public void testDeleteMaintenanceDomain() {
        try {
            assertTrue(service.deleteMaintenanceDomain(
                    MdIdCharStr.asMdId("test-md-1")));
        } catch (CfmConfigException e) {
            fail("Should not have thrown exception: " + e.getMessage());
        }

        //Now try an invalid name
        try {
            assertFalse(service.deleteMaintenanceDomain(
                    MdIdCharStr.asMdId("test-md-3")));
        } catch (CfmConfigException e) {
            fail("Should not have thrown exception: " + e.getMessage());
        }
    }

    @Test
    public void testCreateMaintenanceDomain() throws CfmConfigException {

        MaintenanceAssociation maTest21 = DefaultMaintenanceAssociation
                .builder(MaIdCharStr.asMaId("test-ma-2-1"), 9)
                .ccmInterval(MaintenanceAssociation.CcmInterval.INTERVAL_1S)
                .maNumericId((short) 1)
                .addToRemoteMepIdList(MepId.valueOf((short) 101))
                .addToRemoteMepIdList(MepId.valueOf((short) 102))
                .addToComponentList(
                        DefaultComponent.builder(1)
                                .tagType(Component.TagType.VLAN_STAG)
                                .build())
                .build();

        MaintenanceDomain mdTest2 = DefaultMaintenanceDomain
                .builder(MdIdCharStr.asMdId("test-md-2"))
                .mdLevel(MaintenanceDomain.MdLevel.LEVEL2)
                .mdNumericId((short) 2)
                .addToMaList(maTest21)
                .build();


        //We expect false here because there should have been no previous value
        //with that ID in the store
        assertFalse(service.createMaintenanceDomain(mdTest2));
    }

    @Test
    public void testGetAllMaintenanceAssociation() {
        Collection<MaintenanceAssociation> maListMd1 =
            service.getAllMaintenanceAssociation(
                    MdIdCharStr.asMdId("test-md-1"));

        assertEquals(2, maListMd1.size());

        maListMd1.iterator().forEachRemaining(ma ->
            assertTrue(ma.maId().maName().endsWith(String.valueOf(ma.maNumericId())))
        );

        //Now try with an invalid name
        try {
            service.getAllMaintenanceAssociation(
                    MdIdCharStr.asMdId("test-md-2"));
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown MD test-md-2", e.getMessage());
        }
    }

    @Test
    public void testGetMaintenanceAssociation() {
        Optional<MaintenanceAssociation> ma =
                service.getMaintenanceAssociation(
                        MdIdCharStr.asMdId("test-md-1"),
                        MaIdCharStr.asMaId("test-ma-1-2"));

        assertTrue(ma.isPresent());

        //Now try an invalid MD Name
        try {
            service.getMaintenanceAssociation(
                    MdIdCharStr.asMdId("test-md-2"),
                    MaIdCharStr.asMaId("test-ma-1-2"));
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown MD test-md-2", e.getMessage());
        }

        //Now try an invalid MA Name with a valid MD Name
        try {
            Optional<MaintenanceAssociation> maInvalid =
                    service.getMaintenanceAssociation(
                    MdIdCharStr.asMdId("test-md-1"),
                    MaIdCharStr.asMaId("test-ma-1-3"));
            assertFalse(maInvalid.isPresent());
        } catch (IllegalArgumentException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteMaintenanceAssociation() throws CfmConfigException {
        assertTrue(service.deleteMaintenanceAssociation(
                MdIdCharStr.asMdId("test-md-1"),
                MaIdCharStr.asMaId("test-ma-1-2")));

        //Now check it has actually been removed
        Collection<MaintenanceAssociation> maListUpdated =
                service.getAllMaintenanceAssociation(
                        MdIdCharStr.asMdId("test-md-1"));
        assertEquals(1, maListUpdated.size());
        maListUpdated.stream().findFirst().ifPresent(ma ->
                assertEquals("test-ma-1-1", ma.maId().maName())
        );

        //Now try with an invalid mdName
        try {
            service.deleteMaintenanceAssociation(
                    MdIdCharStr.asMdId("test-md-2"),
                    MaIdCharStr.asMaId("test-ma-1-2"));
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown MD: test-md-2", e.getMessage());
        }

        //Now try with an invalid maName
        try {
            assertFalse(service.deleteMaintenanceAssociation(
                            MdIdCharStr.asMdId("test-md-1"),
                            MaIdCharStr.asMaId("test-ma-1-3")));
        } catch (IllegalArgumentException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testCreateMaintenanceAssociation() throws CfmConfigException {
        MaintenanceAssociation maTest41 = DefaultMaintenanceAssociation
                .builder(MaIdCharStr.asMaId("test-ma-1-4"), 9)
                .ccmInterval(MaintenanceAssociation.CcmInterval.INTERVAL_1S)
                .maNumericId((short) 4)
                .addToRemoteMepIdList(MepId.valueOf((short) 401))
                .addToRemoteMepIdList(MepId.valueOf((short) 402))
                .addToComponentList(
                        DefaultComponent.builder(4)
                                .tagType(Component.TagType.VLAN_STAG)
                                .build())
                .build();

        //Should return false, as this MA did not exist before
        assertFalse(service.createMaintenanceAssociation(
                            MdIdCharStr.asMdId("test-md-1"), maTest41));
        assertEquals(3, service.getAllMaintenanceAssociation(
                                MdIdCharStr.asMdId("test-md-1")).size());

        //Now try with an invalid mdName
        try {
            service.createMaintenanceAssociation(
                    MdIdCharStr.asMdId("test-md-2"), maTest41);
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown MD: test-md-2", e.getMessage());
        }

        //Now try replacing an MA with some new values or Remote MEP
        MaintenanceAssociation maTest11 = DefaultMaintenanceAssociation
                .builder(MaIdCharStr.asMaId("test-ma-1-1"), 9)
                .ccmInterval(MaintenanceAssociation.CcmInterval.INTERVAL_10MIN)
                .maNumericId((short) 1)
                .addToRemoteMepIdList(MepId.valueOf((short) 111)) //Changed
                .addToRemoteMepIdList(MepId.valueOf((short) 112)) //Changed
                .addToComponentList(
                        DefaultComponent.builder(1)
                                .tagType(Component.TagType.VLAN_CTAG)
                                .build())
                .build();

        //Should return true, as this MA did exist before
        assertTrue(service.createMaintenanceAssociation(
                MdIdCharStr.asMdId("test-md-1"), maTest11));
        assertEquals(3, service.getAllMaintenanceAssociation(
                MdIdCharStr.asMdId("test-md-1")).size());
    }

    public class TestApplicationId extends DefaultApplicationId {
        public TestApplicationId(int id, String name) {
            super(id, name);
        }
    }

    private final class TestClusterService extends ClusterServiceAdapter {

        ControllerNode local = new DefaultControllerNode(NID_LOCAL, LOCALHOST);

        @Override
        public ControllerNode getLocalNode() {
            return local;
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return Sets.newHashSet();
        }

    }

    private class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return NID_LOCAL;
        }
    }

    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return new IdGenerator() {
                private AtomicLong counter = new AtomicLong(0);

                @Override
                public long getNewId() {
                    return counter.getAndIncrement();
                }
            };
        }
    }
}
