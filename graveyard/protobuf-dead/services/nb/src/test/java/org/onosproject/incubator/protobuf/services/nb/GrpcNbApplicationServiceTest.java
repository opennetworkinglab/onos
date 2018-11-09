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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.app.ApplicationListener;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationRole;
import org.onosproject.core.DefaultApplication;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.Version;
import org.onosproject.grpc.app.models.ApplicationEnumsProto;
import org.onosproject.grpc.core.models.ApplicationIdProtoOuterClass;
import org.onosproject.grpc.core.models.ApplicationProtoOuterClass;
import org.onosproject.grpc.nb.app.ApplicationServiceGrpc;
import org.onosproject.incubator.protobuf.models.core.ApplicationEnumsProtoTranslator;
import org.onosproject.incubator.protobuf.models.core.ApplicationIdProtoTranslator;
import org.onosproject.incubator.protobuf.models.core.ApplicationProtoTranslator;
import org.onosproject.security.Permission;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.onosproject.grpc.nb.app.ApplicationServiceNb.*;

/**
 * Unit tests for applications gRPC NB services.
 */
public class GrpcNbApplicationServiceTest {

    private static InProcessServer<BindableService> inprocessServer;
    private static ApplicationServiceGrpc.ApplicationServiceBlockingStub blockingStub;
    private static ManagedChannel channel;

    private final ApplicationService appService = new MockApplicationService();

    private ApplicationId id1 = new DefaultApplicationId(1, "app1");
    private ApplicationId id2 = new DefaultApplicationId(2, "app2");
    private ApplicationId id3 = new DefaultApplicationId(3, "app3");
    private ApplicationId id4 = new DefaultApplicationId(4, "app4");

    private static final Version VER = Version.version(1, 2, "a", "b");

    private DefaultApplication.Builder baseBuilder = DefaultApplication.builder()
            .withVersion(VER)
            .withIcon(new byte[0])
            .withRole(ApplicationRole.ADMIN)
            .withPermissions(ImmutableSet.of())
            .withFeaturesRepo(Optional.empty())
            .withFeatures(ImmutableList.of("My Feature"))
            .withRequiredApps(ImmutableList.of());

    private Application app1 =
            DefaultApplication.builder(baseBuilder)
                    .withAppId(id1)
                    .withTitle("title1")
                    .withDescription("desc1")
                    .withOrigin("origin1")
                    .withCategory("category1")
                    .withUrl("url1")
                    .withReadme("readme1")
                    .build();
    private Application app2 =
            DefaultApplication.builder(baseBuilder)
                    .withAppId(id2)
                    .withTitle("title2")
                    .withDescription("desc2")
                    .withOrigin("origin2")
                    .withCategory("category2")
                    .withUrl("url2")
                    .withReadme("readme2")
                    .build();
    private Application app3 =
            DefaultApplication.builder(baseBuilder)
                    .withAppId(id3)
                    .withTitle("title3")
                    .withDescription("desc3")
                    .withOrigin("origin3")
                    .withCategory("category3")
                    .withUrl("url3")
                    .withReadme("readme3")
                    .build();
    private Application app4 =
            DefaultApplication.builder(baseBuilder)
                    .withAppId(id4)
                    .withTitle("title4")
                    .withDescription("desc4")
                    .withOrigin("origin4")
                    .withCategory("category4")
                    .withUrl("url4")
                    .withReadme("readme4")
                    .build();

    private Set apps = ImmutableSet.of(app1, app2, app3, app4);

    /**
     * Initializes the test environment.
     */
    @Before
    public void setUp() throws IllegalAccessException, IOException, InstantiationException {

        GrpcNbApplicationService grpcAppService = new GrpcNbApplicationService();
        grpcAppService.applicationService = appService;
        inprocessServer = grpcAppService.registerInProcessServer();
        inprocessServer.start();

        channel = InProcessChannelBuilder.forName("test").directExecutor()
                .usePlaintext(true).build();

        blockingStub = ApplicationServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Finalizes the test setup.
     */
    @After
    public void tearDown() {
        channel.shutdownNow();
        inprocessServer.stop();
    }

    /**
     * Tests the invocation result of getApplications method.
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetApplications() throws InterruptedException {
        getApplicationsRequest request = getApplicationsRequest.getDefaultInstance();
        getApplicationsReply reply;

        reply = blockingStub.getApplications(request);
        assertEquals(4, reply.getApplicationCount());
    }

    /**
     * Tests the invocation result of getId method.
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetId() throws InterruptedException {
        getIdRequest request = getIdRequest.newBuilder()
                .setName("one")
                .build();

        getIdReply reply = blockingStub.getId(request);
        ApplicationIdProtoOuterClass.ApplicationIdProto appIdProto = reply.getApplicationId();
        assertEquals(id1, ApplicationIdProtoTranslator.translate(appIdProto));
    }

    /**
     * Tests the invocation result of getApplication method.
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetApplication() throws InterruptedException {
        getApplicationRequest request = getApplicationRequest.newBuilder()
                .setApplicationId(ApplicationIdProtoTranslator.translate(id1))
                .build();

        getApplicationReply reply = blockingStub.getApplication(request);
        ApplicationProtoOuterClass.ApplicationProto appProto = reply.getApplication();
        assertEquals(app1, ApplicationProtoTranslator.translate(appProto));
    }

    /**
     * Tests the invocation result of getState method.
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetState() throws InterruptedException {
        getStateRequest request = getStateRequest.newBuilder()
                .setApplicationId(ApplicationIdProtoTranslator.translate(id1))
                .build();

        getStateReply reply = blockingStub.getState(request);
        ApplicationEnumsProto.ApplicationStateProto stateProto = reply.getState();
        assertEquals(Optional.of(ApplicationState.INSTALLED),
                ApplicationEnumsProtoTranslator.translate(stateProto));
    }

    /**
     * Mock class for application service.
     */
    private class MockApplicationService implements ApplicationService {

        MockApplicationService() {}

        @Override
        public Set<Application> getApplications() {
            return apps;
        }

        @Override
        public ApplicationId getId(String name) {

            if ("one".equals(name)) {
                return id1;
            }

            if ("two".equals(name)) {
                return id2;
            }

            if ("three".equals(name)) {
                return id3;
            }

            if ("four".equals(name)) {
                return id4;
            }

            return null;
        }

        @Override
        public Application getApplication(ApplicationId appId) {

            if (id1.equals(appId)) {
                return app1;
            }

            if (id2.equals(appId)) {
                return app2;
            }

            if (id3.equals(appId)) {
                return app3;
            }

            if (id4.equals(appId)) {
                return app4;
            }

            return null;
        }

        @Override
        public ApplicationState getState(ApplicationId appId) {
            return ApplicationState.INSTALLED;
        }

        @Override
        public Set<Permission> getPermissions(ApplicationId appId) {
            return null;
        }

        @Override
        public void registerDeactivateHook(ApplicationId appId, Runnable hook) {

        }

        @Override
        public void addListener(ApplicationListener listener) {

        }

        @Override
        public void removeListener(ApplicationListener listener) {

        }
    }
}
