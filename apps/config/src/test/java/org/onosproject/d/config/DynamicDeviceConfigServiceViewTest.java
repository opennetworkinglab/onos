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
package org.onosproject.d.config;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigEvent.Type;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigServiceAdapter;
import org.onosproject.config.Filter;
import org.onosproject.net.DeviceId;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;

public class DynamicDeviceConfigServiceViewTest {

    static DeviceId did = DeviceId.deviceId("test:device");
    /**
     * Absolute ResourceId to {@code DID}.
     */
    static ResourceId rid = DeviceResourceIds.toResourceId(did);


    /**
     * Device relative ResourceId pointing to intf node.
     */
    static ResourceId relIntf = ResourceId.builder()
                .addBranchPointSchema("intf", "test")
                .build();

    DataNode node = null;

    TestDynamicConfigService service;
    DynamicDeviceConfigServiceView view;

    ResourceId realPath;
    ResourceId realId;
    Filter realFilter;

    DynamicConfigEvent viewRelevantEvent;
    DynamicConfigEvent viewEvent;

    @Before
    public void setUp() throws Exception {
        realPath = null;
        realId = null;
        realFilter = null;

        viewRelevantEvent = null;
        viewEvent = null;

        service = new TestDynamicConfigService();
        view = DynamicDeviceConfigServiceView.deviceView(service, did);
    }

    // FIXME add test scenario where irrelevant event get discarded.

    @Test
    public void testListener() throws CloneNotSupportedException, InterruptedException {
        ResourceId realIntf = ResourceId.builder()
            .append(rid)
            .addBranchPointSchema("intf", "test")
            .build();

        final CountDownLatch received = new CountDownLatch(1);

        DynamicConfigListener lsnr = new DynamicConfigListener() {

            @Override
            public boolean isRelevant(DynamicConfigEvent event) {
                viewRelevantEvent = event;
                return true;
            }

            @Override
            public void event(DynamicConfigEvent event) {
                viewEvent = event;
                received.countDown();
            }
        };
        view.addListener(lsnr);

        service.post(new DynamicConfigEvent(Type.NODE_ADDED, realIntf));

        assertTrue(received.await(5, TimeUnit.SECONDS));

        assertFalse("Expect relative path but was" + viewRelevantEvent.subject(),
                    ResourceIds.isPrefix(rid, viewRelevantEvent.subject()));
        assertFalse("Expect relative path but was" + viewEvent.subject(),
                    ResourceIds.isPrefix(rid, viewEvent.subject()));

        view.removeListener(lsnr);
    }

    @Test
    public void testCreateNode() {
        view.createNode(relIntf, node);

        assertTrue(ResourceIds.isPrefix(rid, realPath));
    }

    @Test
    public void testReadNode() {
        Filter filter = null;
        DataNode returned = view.readNode(relIntf, filter);

        assertTrue(ResourceIds.isPrefix(rid, realPath));

        // FIXME test realFilter

        // TODO do we expect something to happen on returned?
    }

    @Test
    public void testNodeExist() {
        view.nodeExist(relIntf);

        assertTrue(ResourceIds.isPrefix(rid, realPath));
    }

    @Test
    public void testUpdateNode() {
        view.updateNode(relIntf, node);

        assertTrue(ResourceIds.isPrefix(rid, realPath));
    }

    @Test
    public void testReplaceNode() {
        view.replaceNode(relIntf, node);

        assertTrue(ResourceIds.isPrefix(rid, realPath));
    }

    @Test
    public void testDeleteNode() {
        view.deleteNode(relIntf);

        assertTrue(ResourceIds.isPrefix(rid, realPath));
    }

    @Test
    public void testInvokeRpc() {
        RpcInput input = new RpcInput(relIntf, null);
        view.invokeRpc(input);

        assertTrue(ResourceIds.isPrefix(rid, realId));
    }

    private final class TestDynamicConfigService
            extends DynamicConfigServiceAdapter {
        @Override
        public void createNode(ResourceId path, DataNode node) {
            realPath = path;
        }

        @Override
        public DataNode readNode(ResourceId path, Filter filter) {
            realPath = path;
            realFilter = filter;
            return super.readNode(path, filter);
        }

        @Override
        public Boolean nodeExist(ResourceId path) {
            realPath = path;
            return super.nodeExist(path);
        }

        @Override
        public void updateNode(ResourceId path, DataNode node) {
            realPath = path;
        }

        @Override
        public void replaceNode(ResourceId path, DataNode node) {
            realPath = path;
        }

        @Override
        public void deleteNode(ResourceId path) {
            realPath = path;
        }

        @Override
        public CompletableFuture<RpcOutput> invokeRpc(RpcInput input) {
            realId = input.id();
            return super.invokeRpc(input);
        }

        public void post(DynamicConfigEvent event) {
            listenerRegistry.process(event);
        }
    }

}
