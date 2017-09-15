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
package org.onosproject.d.config.sync.impl;

import static org.junit.Assert.*;
import static org.onosproject.d.config.ResourceIds.ROOT_ID;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigServiceAdapter;
import org.onosproject.config.Filter;
import org.onosproject.d.config.DeviceResourceIds;
import org.onosproject.d.config.ResourceIds;
import org.onosproject.d.config.sync.DeviceConfigSynchronizationProvider;
import org.onosproject.d.config.sync.operation.SetRequest;
import org.onosproject.d.config.sync.operation.SetRequest.Change;
import org.onosproject.d.config.sync.operation.SetRequest.Change.Operation;
import org.onosproject.d.config.sync.operation.SetResponse;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.LeafNode;
import org.onosproject.yang.model.ResourceId;

import com.google.common.collect.Iterables;

public class DynamicDeviceConfigSynchronizerTest {

    static final String TEST_NS = "testNS";

    static final ResourceId REL_INTERFACES = ResourceId.builder()
                .addBranchPointSchema("interfaces", TEST_NS)
                .build();

    static final DeviceId DID = DeviceId.deviceId("test:device1");

    DynamicDeviceConfigSynchronizer sut;

    TestDynamicConfigService dyConService;

    CountDownLatch providerCalled = new CountDownLatch(1);

    /**
     * DynamicConfigService.readNode(ResourceId, Filter) stub.
     */
    BiFunction<ResourceId, Filter, DataNode> onDcsRead;

    BiFunction<DeviceId, SetRequest, CompletableFuture<SetResponse>> onSetConfiguration;

    @Before
    public void setUp() throws Exception {

        sut = new DynamicDeviceConfigSynchronizer();
        dyConService = new TestDynamicConfigService();
        sut.dynConfigService = dyConService;
        sut.netcfgService = new NetworkConfigServiceAdapter();

        sut.activate();

        sut.register(new MockDeviceConfigSynchronizerProvider());
    }

    @After
    public void tearDown() throws Exception {
        sut.deactivate();
    }

    @Test
    public void testDispatchRequest() throws Exception {

        ResourceId devicePath = DeviceResourceIds.toResourceId(DID);
        ResourceId cfgPath = REL_INTERFACES;
        ResourceId absPath = ResourceIds.concat(devicePath, cfgPath);
        ResourceId evtPath = ResourceIds.relativize(ROOT_ID, absPath);
        DynamicConfigEvent event = new DynamicConfigEvent(DynamicConfigEvent.Type.NODE_REPLACED, evtPath);

        // assertions
        onDcsRead = (path, filter) -> {
            assertTrue(filter.isEmptyFilter());
            assertEquals("DCService get access by root relative RID", evtPath, path);
            return deviceConfigNode();
        };

        onSetConfiguration = (deviceId, request) -> {
            assertEquals(DID, deviceId);
            assertEquals(1, request.changes().size());
            Change change = Iterables.get(request.changes(), 0);
            assertEquals("Provider get access by rel RID", REL_INTERFACES, change.path());
            assertEquals(Operation.REPLACE, change.op());
            assertEquals("interfaces", change.val().key().schemaId().name());
            // walk and test children if it adds value

            providerCalled.countDown();
            return CompletableFuture.completedFuture(SetResponse.ok(request));
        };

        // start test run

        // imitate event from DCS
        dyConService.postEvent(event);

        // assert that it reached the provider
        providerCalled.await(5, TimeUnit.HOURS);
    }

    /**
     * DataNode for testing.
     *
     * <pre>
     *   +-interfaces
     *      |
     *      +- interface{intf-name="en0"}
     *           |
     *           +- speed = "10G"
     *           +- state = "up"
     *
     * </pre>
     * @return DataNode
     */
    private DataNode deviceConfigNode() {
        InnerNode.Builder intfs = InnerNode.builder("interfaces", TEST_NS);
        intfs.type(DataNode.Type.SINGLE_INSTANCE_NODE);
        InnerNode.Builder intf = intfs.createChildBuilder("interface", TEST_NS);
        intf.type(DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE);
        intf.addKeyLeaf("name", TEST_NS, "Ethernet0/0");
        LeafNode.Builder speed = intf.createChildBuilder("mtu", TEST_NS, "1500");
        speed.type(DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE);

        intf.addNode(speed.build());
        intfs.addNode(intf.build());
        return intfs.build();
    }

    private class TestDynamicConfigService extends DynamicConfigServiceAdapter {

        public void postEvent(DynamicConfigEvent event) {
            listenerRegistry.process(event);
        }

        @Override
        public DataNode readNode(ResourceId path, Filter filter) {
            return onDcsRead.apply(path, filter);
        }
    }

    private class MockDeviceConfigSynchronizerProvider
            implements DeviceConfigSynchronizationProvider {

        @Override
        public ProviderId id() {
            return new ProviderId(DID.uri().getScheme(), "test-provider");
        }

        @Override
        public CompletableFuture<SetResponse> setConfiguration(DeviceId deviceId,
                                                               SetRequest request) {
            return onSetConfiguration.apply(deviceId, request);
        }
    }

}
