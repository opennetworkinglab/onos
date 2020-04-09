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
package org.onosproject.vpls;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.host.HostEvent;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.api.VplsOperation;
import org.onosproject.vpls.api.VplsOperationService;
import org.onosproject.vpls.store.VplsStoreEvent;

import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onosproject.net.EncapsulationType.*;
import static org.onosproject.vpls.api.VplsData.VplsState.*;

/**
 * Test for {@link VplsManager}.
 */
public class VplsManagerTest extends VplsTest {
    private VplsManager vplsManager;
    private CodecService codecService = new TestCodecService();
    private TestVplsStore vplsStore = new TestVplsStore();
    private TestHostService hostService = new TestHostService();
    private TestInterfaceService interfaceService = new TestInterfaceService();
    private TestVplsOperationService vplsOperationService = new TestVplsOperationService();

    @Before
    public void setup() {
        vplsManager = new VplsManager();
        vplsManager.codecService = codecService;
        vplsManager.hostService = hostService;
        vplsManager.vplsStore = vplsStore;
        vplsManager.operationService = vplsOperationService;
        vplsManager.interfaceService = interfaceService;
        vplsStore.clear();
        vplsOperationService.clear();
        vplsManager.activate();
    }

    @After
    public void tearDown() {
        vplsManager.deactivate();
    }

    /**
     * Creates VPLS by given name and encapsulation type.
     */
    @Test
    public void testCreateVpls() {
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        assertEquals(VPLS1, vplsData.name());
        assertEquals(NONE, vplsData.encapsulationType());

        vplsData = vplsStore.getVpls(VPLS1);
        assertEquals(vplsData.state(), ADDING);
    }

    /**
     * Gets VPLS by VPLS name.
     */
    @Test
    public void testGetVpls() {
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.state(ADDED);
        vplsStore.addVpls(vplsData);

        VplsData result = vplsManager.getVpls(VPLS1);
        assertEquals(vplsData, result);

        result = vplsManager.getVpls(VPLS2);
        assertNull(result);
    }

    /**
     * Removes a VPLS.
     */
    @Test
    public void testRemoveVpls() {
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        vplsManager.removeVpls(vplsData);
        assertEquals(vplsData.state(), REMOVING);
        vplsData = vplsStore.getVpls(VPLS1);
        assertNull(vplsData);
        Collection<VplsData> allVpls = vplsStore.getAllVpls();
        assertEquals(0, allVpls.size());
    }

    /**
     * Removes all VPLS.
     */
    @Test
    public void testRemoveAllVpls() {
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.state(ADDED);
        vplsStore.addVpls(vplsData);

        vplsData = VplsData.of(VPLS2, VLAN);
        vplsData.state(ADDED);
        vplsStore.addVpls(vplsData);

        vplsManager.removeAllVpls();
        assertEquals(0, vplsStore.getAllVpls().size());
    }

    /**
     * Adds network interfaces one by one to a VPLS.
     */
    @Test
    public void testAddInterface() {
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        vplsManager.addInterface(vplsData, V100H1);
        vplsManager.addInterface(vplsData, V100H2);
        vplsData = vplsStore.getVpls(VPLS1);
        assertNotNull(vplsData);
        assertEquals(vplsData.state(), UPDATING);
        assertEquals(2, vplsData.interfaces().size());
        assertTrue(vplsData.interfaces().contains(V100H1));
        assertTrue(vplsData.interfaces().contains(V100H2));
    }

    /**
     * Adds network interfaces to a VPLS.
     */
    @Test
    public void testAddInterfaces() {
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        vplsManager.addInterfaces(vplsData, ImmutableSet.of(V100H1, V100H2));
        vplsData = vplsStore.getVpls(VPLS1);
        assertNotNull(vplsData);
        assertEquals(vplsData.state(), UPDATING);
        assertEquals(2, vplsData.interfaces().size());
        assertTrue(vplsData.interfaces().contains(V100H1));
        assertTrue(vplsData.interfaces().contains(V100H2));
    }

    /**
     * Removes network interfaces one by one from a VPLS.
     */
    @Test
    public void testRemoveInterface() {
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        vplsManager.addInterface(vplsData, V100H1);
        vplsManager.addInterface(vplsData, V100H2);
        vplsManager.removeInterface(vplsData, V100H1);
        vplsData = vplsStore.getVpls(VPLS1);
        assertNotNull(vplsData);
        assertEquals(vplsData.state(), UPDATING);
        assertEquals(1, vplsData.interfaces().size());
        assertTrue(vplsData.interfaces().contains(V100H2));
    }

    /**
     * Removes network interfaces from a VPLS.
     */
    @Test
    public void testRemoveInterfaces() {
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        vplsManager.addInterface(vplsData, V100H1);
        vplsManager.addInterface(vplsData, V100H2);
        vplsManager.removeInterfaces(vplsData, ImmutableSet.of(V100H1, V100H2));
        vplsData = vplsStore.getVpls(VPLS1);
        assertNotNull(vplsData);
        assertEquals(vplsData.state(), UPDATING);
        assertEquals(0, vplsData.interfaces().size());
    }

    /**
     * Sets encapsulation type for a VPLS.
     */
    @Test
    public void testSetEncapsulationType() {
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        vplsManager.setEncapsulationType(vplsData, EncapsulationType.VLAN);
        vplsData = vplsStore.getVpls(VPLS1);
        assertNotNull(vplsData);
        assertEquals(vplsData.state(), UPDATING);
        assertEquals(vplsData.encapsulationType(), EncapsulationType.VLAN);
    }

    /**
     * Adds hosts to a VPLS.
     */
    @Test
    public void testAddHost() {
        VplsData vplsData  = VplsData.of(VPLS1, NONE);
        vplsData.addInterface(V100H1);
        vplsData.state(ADDED);
        vplsStore.addVpls(vplsData);

        HostEvent hostEvent = new HostEvent(HostEvent.Type.HOST_ADDED, V100HOST1);
        hostService.postHostEvent(hostEvent);

        vplsData = vplsStore.getVpls(VPLS1);
        assertNotNull(vplsData);

        assertEquals(vplsData.state(), UPDATING);
    }

    /**
     * Removes hosts from a VPLS.
     */
    @Test
    public void testRemoveHost() {
        VplsData vplsData  = VplsData.of(VPLS1, NONE);
        vplsData.addInterface(V100H1);
        vplsData.state(ADDED);
        vplsStore.addVpls(vplsData);

        HostEvent hostEvent = new HostEvent(HostEvent.Type.HOST_REMOVED, V100HOST1);
        hostService.postHostEvent(hostEvent);
        vplsData = vplsStore.getVpls(VPLS1);
        assertNotNull(vplsData);

        assertEquals(vplsData.state(), UPDATING);
    }

    /**
     * Pass different VPLS store event to store delegate.
     * Include these cases:
     * <ul>
     *     <li>VPLS added</li>
     *     <li>VPLS updated</li>
     *     <li>VPLS state updated</li>
     *     <li>VPLS removed</li>
     * </ul>
     */
    @Test
    public void testStoreDelegate() {
        // Add
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        VplsStoreEvent event = new VplsStoreEvent(VplsStoreEvent.Type.ADD, vplsData);
        vplsStore.delegate().notify(event);

        VplsOperation vplsOperation = vplsOperationService.operation();
        assertEquals(vplsOperation.op(), VplsOperation.Operation.ADD);
        assertEquals(vplsOperation.vpls(), vplsData);
        vplsOperationService.clear();

        // Update info
        vplsData.encapsulationType(EncapsulationType.VLAN);
        vplsData.state(UPDATING);
        event = new VplsStoreEvent(VplsStoreEvent.Type.UPDATE, vplsData);
        vplsStore.delegate().notify(event);
        vplsOperation = vplsOperationService.operation();
        assertEquals(vplsOperation.op(), VplsOperation.Operation.UPDATE);
        assertEquals(vplsOperation.vpls(), vplsData);
        vplsOperationService.clear();

        // Update state (no operation)
        vplsData.state(VplsData.VplsState.ADDED);
        event = new VplsStoreEvent(VplsStoreEvent.Type.UPDATE, vplsData);
        vplsStore.delegate().notify(event);
        vplsOperation = vplsOperationService.operation();
        assertNull(vplsOperation);
        vplsOperationService.clear();

        // Remove
        event = new VplsStoreEvent(VplsStoreEvent.Type.REMOVE, vplsData);
        vplsStore.delegate().notify(event);
        vplsOperation = vplsOperationService.operation();
        assertEquals(vplsOperation.op(), VplsOperation.Operation.REMOVE);
        assertEquals(vplsOperation.vpls(), vplsData);
        vplsOperationService.clear();
    }

    /**
     * Trigger host event listener by HOST_ADDED event.
     */
    @Test
    public void hostAddEventTest() {
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        vplsManager.addInterface(vplsData, V100H1);
        HostEvent hostEvent = new HostEvent(HostEvent.Type.HOST_ADDED, V100HOST1);
        hostService.postHostEvent(hostEvent);

        vplsData = vplsStore.getVpls(VPLS1);
        assertEquals(UPDATING, vplsData.state());
    }

    /**
     * Trigger host event listener by HOST_REMOVED event.
     */
    @Test
    public void hostRemoveEventTest() {
        VplsData vplsData = vplsManager.createVpls(VPLS1, NONE);
        vplsManager.addInterface(vplsData, V100H1);
        HostEvent hostEvent = new HostEvent(HostEvent.Type.HOST_REMOVED, V100HOST1);
        hostService.postHostEvent(hostEvent);

        vplsData = vplsStore.getVpls(VPLS1);
        assertEquals(UPDATING, vplsData.state());
    }

    /**
     * Test VPLS operation service.
     * Stores last operation submitted by VPLS manager.
     */
    class TestVplsOperationService implements VplsOperationService {
        VplsOperation operation;

        @Override
        public void submit(VplsOperation vplsOperation) {
            this.operation = vplsOperation;
        }

        /**
         * Clears the VPLS operation.
         */
        public void clear() {
            operation = null;
        }

        /**
         * Gets the latest VPLS operation.
         * @return the latest VPLS operation.
         */
        public VplsOperation operation() {
            return operation;
        }
    }

    /**
     * Test Codec service.
     */
    class TestCodecService implements CodecService {


        @Override
        public Set<Class<?>> getCodecs() {
            return null;
        }

        @Override
        public <T> JsonCodec<T> getCodec(Class<T> entityClass) {
            return null;
        }

        @Override
        public <T> void registerCodec(Class<T> entityClass, JsonCodec<T> codec) {

        }

        @Override
        public void unregisterCodec(Class<?> entityClass) {

        }
    }

}
