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
package org.onosproject.cluster.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.ProxyFactory;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;

import static org.junit.Assert.assertEquals;

/**
 * Proxy manager test.
 */
public class ProxyManagerTest {
    @Test
    public void testProxyManager() throws Exception {
        TestClusterCommunicationServiceFactory clusterCommunicatorFactory =
            new TestClusterCommunicationServiceFactory();

        NodeId a = NodeId.nodeId("a");
        NodeId b = NodeId.nodeId("b");

        Serializer serializer = Serializer.using(KryoNamespaces.BASIC);

        ProxyInterfaceImpl proxyInterface1 = new ProxyInterfaceImpl();
        ProxyManager proxyManager1 = new ProxyManager();
        proxyManager1.clusterCommunicator = clusterCommunicatorFactory.newCommunicationService(a);
        proxyManager1.activate();
        proxyManager1.registerProxyService(ProxyInterface.class, proxyInterface1, serializer);

        ProxyInterfaceImpl proxyInterface2 = new ProxyInterfaceImpl();
        ProxyManager proxyManager2 = new ProxyManager();
        proxyManager2.clusterCommunicator = clusterCommunicatorFactory.newCommunicationService(b);
        proxyManager2.activate();
        proxyManager2.registerProxyService(ProxyInterface.class, proxyInterface2, serializer);

        ProxyFactory<ProxyInterface> proxyFactory1 = proxyManager1.getProxyFactory(ProxyInterface.class, serializer);
        assertEquals("Hello world!", proxyFactory1.getProxyFor(b).sync("Hello world!"));
        assertEquals(1, proxyInterface2.syncCalls.get());
        assertEquals("Hello world!", proxyFactory1.getProxyFor(b).async("Hello world!").join());
        assertEquals(1, proxyInterface2.asyncCalls.get());

        ProxyFactory<ProxyInterface> proxyFactory2 = proxyManager2.getProxyFactory(ProxyInterface.class, serializer);
        assertEquals("Hello world!", proxyFactory2.getProxyFor(b).sync("Hello world!"));
        assertEquals(2, proxyInterface2.syncCalls.get());
        assertEquals("Hello world!", proxyFactory2.getProxyFor(b).async("Hello world!").join());
        assertEquals(2, proxyInterface2.asyncCalls.get());

        proxyManager1.deactivate();
        proxyManager2.deactivate();
    }

    interface ProxyInterface {
        String sync(String arg);
        CompletableFuture<String> async(String arg);
    }

    class ProxyInterfaceImpl implements ProxyInterface {
        private final AtomicInteger syncCalls = new AtomicInteger();
        private final AtomicInteger asyncCalls = new AtomicInteger();

        @Override
        public String sync(String arg) {
            syncCalls.incrementAndGet();
            return arg;
        }

        @Override
        public CompletableFuture<String> async(String arg) {
            asyncCalls.incrementAndGet();
            return CompletableFuture.completedFuture(arg);
        }
    }
}
