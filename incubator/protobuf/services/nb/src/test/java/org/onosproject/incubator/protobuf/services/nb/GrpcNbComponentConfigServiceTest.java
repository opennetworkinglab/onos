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

package org.onosproject.incubator.protobuf.services.nb;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.grpc.cfg.models.ConfigPropertyProtoOuterClass;
import org.onosproject.grpc.nb.cfg.ComponentConfigServiceGrpc;
import org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb;
import org.onosproject.grpc.nb.cfg.ComponentConfigServiceGrpc.ComponentConfigServiceBlockingStub;
import org.onosproject.incubator.protobuf.models.cfg.ConfigPropertyProtoTranslator;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.*;
import static org.junit.Assert.assertTrue;
import static org.onosproject.cfg.ConfigProperty.Type.STRING;

/**
 * Unit tests of gRPC northbound component config service.
 */
public class GrpcNbComponentConfigServiceTest {

    private static InProcessServer<BindableService> inprocessServer;
    private static ComponentConfigServiceBlockingStub blockingStub;
    private static ManagedChannel channel;

    private static Set<String> componentNames = new HashSet<>();
    private static final Map<String, ConfigProperty> PROPERTY_MAP = Maps.newConcurrentMap();
    private static final Map<String, ConfigProperty> PROPERTY_MAP1 = Maps.newConcurrentMap();
    private static final Map<String, String> STRING_MAP = Maps.newConcurrentMap();
    private static final Map<String, String> STRING_MAP1 = Maps.newConcurrentMap();
    private static final Map<String, String> STRING_MAP2 = Maps.newConcurrentMap();
    private static final ComponentConfigService MOCK_COMPONENTCONFIG = new MockComponentConfigService();
    private static final String COMPONENTCONFIGNAME = "org.onosprject.test";
    private static final String COMPONENTCONFIGNAME1 = "org.onosprject.test1";
    private static final String COMPONENTCONFIGNAME2 = "org.onosprject.test2";
    private static final String PROPERTY_TEST_KEY = COMPONENTCONFIGNAME + "#" + "test";
    private static final ConfigProperty C1 = ConfigProperty.defineProperty("foo", STRING, "dingo", "FOO");
    private static final ConfigProperty C2 = ConfigProperty.defineProperty("bar", STRING, "bat", "BAR");

    public GrpcNbComponentConfigServiceTest() {}

    private static void populateComponentNames() {

        componentNames.add(COMPONENTCONFIGNAME);
        componentNames.add(COMPONENTCONFIGNAME1);
        componentNames.add(COMPONENTCONFIGNAME2);
        PROPERTY_MAP1.put(COMPONENTCONFIGNAME, C1);
        STRING_MAP2.put(PROPERTY_TEST_KEY, "true");
    }

    /**
     * Tests gRPC getComponentNames interface.
     */
    @Test
    public void testGetComponentNames() throws InterruptedException {
        getComponentNamesRequest request = ComponentConfigServiceNb.getComponentNamesRequest.getDefaultInstance();
        getComponentNamesReply reply;

        try {
            reply = blockingStub.getComponentNames(request);
            assertTrue(componentNames.size() == reply.getNamesCount());

            Set expectedNames = Collections.emptySet();
            expectedNames.addAll(componentNames);
            assertTrue(reply.equals(expectedNames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests gRPC registerProperties interface.
     */
    @Test
    public void testRegisterProperties() throws InterruptedException {
        registerPropertiesRequest request = ComponentConfigServiceNb.registerPropertiesRequest
                .newBuilder().setComponentClass(COMPONENTCONFIGNAME).build();

        try {
            blockingStub.registerProperties(request);
            assertTrue(PROPERTY_MAP.get(COMPONENTCONFIGNAME).equals(C1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests gRPC unregisterProperties interface.
     */
    @Test
    public void testUnregisterProperties() throws InterruptedException {
        unregisterPropertiesRequest request = ComponentConfigServiceNb.unregisterPropertiesRequest
                .newBuilder().setComponentClass(COMPONENTCONFIGNAME).build();

        try {
            blockingStub.unregisterProperties(request);
            assertTrue(PROPERTY_MAP1.get(COMPONENTCONFIGNAME) == null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests gRPC getProperty interface.
     */
    @Test
    public void tesGetProperties() throws InterruptedException {
        getPropertiesRequest request = ComponentConfigServiceNb.getPropertiesRequest.newBuilder()
                .setComponentName(COMPONENTCONFIGNAME).build();
        getPropertiesReply reply;

        try {
            reply = blockingStub.getProperties(request);

            Set<ConfigProperty> configProperties = new HashSet<>();
            for (ConfigPropertyProtoOuterClass.ConfigPropertyProto cfg : reply.getConfigPropertiesList()) {
                configProperties.add(ConfigPropertyProtoTranslator.translate(cfg));
            }

            assertTrue(configProperties.equals(ImmutableSet.of(C1, C2)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests gRPC setProperty interface.
     */
    @Test
    public void testSetProperty() throws InterruptedException {
        setPropertyRequest request = ComponentConfigServiceNb.setPropertyRequest.newBuilder()
                .setComponentName(COMPONENTCONFIGNAME)
                .setName("test")
                .setValue("true")
                .build();

        try {
            blockingStub.setProperty(request);
            assertTrue(STRING_MAP.get(PROPERTY_TEST_KEY).equals("true"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests gRPC preSetProperty interface.
     */
    @Test
    public void testPreSetProperty() throws InterruptedException {
        preSetPropertyRequest request = ComponentConfigServiceNb.preSetPropertyRequest.newBuilder()
                .setComponentName(COMPONENTCONFIGNAME)
                .setName("test")
                .setValue("true")
                .build();

        try {
            blockingStub.preSetProperty(request);
            assertTrue(STRING_MAP1.get(PROPERTY_TEST_KEY).equals("true"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests gRPC unsetProperty interface.
     */
    @Test
    public void testUnsetProperty() throws InterruptedException {
        unsetPropertyRequest request = ComponentConfigServiceNb.unsetPropertyRequest.newBuilder()
                .setComponentName(COMPONENTCONFIGNAME)
                .setName("test")
                .build();

        try {
            blockingStub.unsetProperty(request);
            assertTrue(STRING_MAP2.get(PROPERTY_TEST_KEY) == null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialization before start testing gRPC northbound component config service.
     */
    @BeforeClass
    public static void beforeClass() throws InstantiationException, IllegalAccessException, IOException {
        GrpcNbComponentConfigService componentConfigService = new GrpcNbComponentConfigService();
        componentConfigService.componentConfigService = MOCK_COMPONENTCONFIG;
        inprocessServer = componentConfigService.registerInProcessServer();
        inprocessServer.start();

        channel = InProcessChannelBuilder.forName("test").directExecutor()
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true).build();
        blockingStub = ComponentConfigServiceGrpc.newBlockingStub(channel);
        populateComponentNames();
    }

    /**
     * Finalization after test gRPC northbound component config service.
     */
    @AfterClass
    public static void afterClass() {

        channel.shutdownNow();
        inprocessServer.stop();
    }

    private static class MockComponentConfigService implements ComponentConfigService {

        MockComponentConfigService() {
        }

        @Override
        public Set<String> getComponentNames() {
            return componentNames;
        }

        @Override
        public void registerProperties(Class<?> componentClass) {
            PROPERTY_MAP.put(componentClass.getName(), C1);
        }

        @Override
        public void unregisterProperties(Class<?> componentClass, boolean clear) {
            PROPERTY_MAP1.remove(componentClass.getName());
        }

        @Override
        public Set<ConfigProperty> getProperties(String componentName) {
            return ImmutableSet.of(C1, C2);
        }

        @Override
        public void setProperty(String componentName, String name, String value) {
            STRING_MAP.put(componentName + "#" + name, value);
        }

        @Override
        public void preSetProperty(String componentName, String name, String value) {
            STRING_MAP1.put(componentName + "#" + name, value);
        }

        @Override
        public void unsetProperty(String componentName, String name) {
            STRING_MAP2.remove(componentName + "#" + name);
        }
    }
}
