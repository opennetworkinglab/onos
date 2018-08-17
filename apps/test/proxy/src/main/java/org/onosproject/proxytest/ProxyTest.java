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
package org.onosproject.proxytest;

import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.ProxyFactory;
import org.onosproject.cluster.ProxyService;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipProxyFactory;
import org.onosproject.mastership.MastershipProxyService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Proxy test application.
 */
@Component(immediate = true, service = ProxyTest.class)
public class ProxyTest {

    private static final Serializer SERIALIZER = Serializer.using(KryoNamespaces.API);

    private final Logger log = getLogger(getClass());

    private static final String APP_NAME = "org.onosproject.proxytest";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ProxyService proxyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipProxyService mastershipProxyService;

    private ProxyFactory<TestProxy> proxyFactory;
    private MastershipProxyFactory<TestProxy> mastershipProxyFactory;

    @Activate
    protected void activate() {
        coreService.registerApplication(APP_NAME);
        proxyService.registerProxyService(TestProxy.class, new TestProxyImpl(), SERIALIZER);
        mastershipProxyService.registerProxyService(TestProxy.class, new TestProxyImpl(), SERIALIZER);
        proxyFactory = proxyService.getProxyFactory(TestProxy.class, SERIALIZER);
        mastershipProxyFactory = mastershipProxyService.getProxyFactory(TestProxy.class, SERIALIZER);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        proxyService.unregisterProxyService(TestProxy.class);
        mastershipProxyService.unregisterProxyService(TestProxy.class);
        log.info("Stopped");
    }

    /**
     * Returns a proxy for the given node.
     *
     * @param nodeId the node for which to return the proxy
     * @return the proxy for the given node
     */
    public TestProxy getProxyFor(NodeId nodeId) {
        return proxyFactory.getProxyFor(nodeId);
    }

    /**
     * Returns a mastership-based proxy for the given device.
     *
     * @param deviceId the device for which to return the proxy
     * @return the proxy for the given device
     */
    public TestProxy getProxyFor(DeviceId deviceId) {
        return mastershipProxyFactory.getProxyFor(deviceId);
    }
}
