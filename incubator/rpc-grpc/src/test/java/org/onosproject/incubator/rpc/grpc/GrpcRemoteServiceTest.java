/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.rpc.grpc;

import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onlab.packet.ChassisId;
import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.incubator.rpc.RemoteServiceContextProvider;
import org.onosproject.incubator.rpc.RemoteServiceContextProviderService;
import org.onosproject.incubator.rpc.RemoteServiceProviderRegistry;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * Set of tests of the gRPC RemoteService components.
 */
public class GrpcRemoteServiceTest {

    private static final DeviceId DEVICE_ID = deviceId("dev:000001");

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final ProviderId PID = new ProviderId("test", "com.exmaple.test");

    private static final URI DURI = URI.create("dev:000001");

    private static final String MFR = "mfr";

    private static final String HW = "hw";

    private static final String SW = "sw";

    private static final String SN = "serial";

    private static final ChassisId CHASSIS = new ChassisId(42);

    private static final SparseAnnotations ANON = DefaultAnnotations.builder()
                                                    .set("foo", "var")
                                                    .build();

    private static final PortNumber PORT = PortNumber.portNumber(99);

    private static final DeviceDescription DDESC
        = new DefaultDeviceDescription(DURI, Type.SWITCH, MFR, HW, SW, SN,
                                       CHASSIS, ANON);

    private GrpcRemoteServiceServer server;
    private GrpcRemoteServiceProvider client;

    private DeviceProvider svSideDeviceProvider;

    private MTestDeviceProviderService svDeviceProviderService;

    private ServerSideLinkProviderService svLinkProviderService;


    private CountDownLatch serverReady;

    private URI uri;

    @Before
    public void setUp() throws Exception {
        serverReady = new CountDownLatch(1);
        server = new GrpcRemoteServiceServer();
        server.deviceProviderRegistry = new MTestDeviceProviderRegistry();
        server.linkProviderRegistry = new ServerSideLinkProviderRegistry();
        server.listenPort = TestTools.findAvailablePort(11984);
        uri = URI.create("grpc://localhost:" + server.listenPort);
        // todo: pass proper ComponentContext
        server.activate(null);

        client = new GrpcRemoteServiceProvider();
        client.rpcRegistry = new NoOpRemoteServiceProviderRegistry();
        client.activate();
    }

    @After
    public void tearDown() {
        client.deactivate();
        server.deactivate();
        svLinkProviderService = null;
    }

    private static void assertEqualsButNotSame(Object expected, Object actual) {
        assertEquals(expected, actual);
        assertNotSame("Cannot be same instance if it properly went through gRPC",
                      expected, actual);
    }

    @Test
    public void deviceServiceBasics() throws InterruptedException {
        RemoteServiceContext remoteServiceContext = client.get(uri);
        assertNotNull(remoteServiceContext);

        DeviceProviderRegistry deviceProviderRegistry = remoteServiceContext.get(DeviceProviderRegistry.class);
        assertNotNull(deviceProviderRegistry);

        CTestDeviceProvider clDeviceProvider = new CTestDeviceProvider();
        DeviceProviderService clDeviceProviderService = deviceProviderRegistry.register(clDeviceProvider);

        assertTrue(serverReady.await(10, TimeUnit.SECONDS));

        // client to server communication
        clDeviceProviderService.deviceConnected(DEVICE_ID, DDESC);
        assertTrue(svDeviceProviderService.deviceConnected.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(DEVICE_ID, svDeviceProviderService.deviceConnectedDid);
        assertEqualsButNotSame(DDESC, svDeviceProviderService.deviceConnectedDesc);

        PortDescription portDescription = new DefaultPortDescription(PORT, true, ANON);
        List<PortDescription> portDescriptions = ImmutableList.of(portDescription);
        clDeviceProviderService.updatePorts(DEVICE_ID, portDescriptions);
        assertTrue(svDeviceProviderService.updatePorts.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(DEVICE_ID, svDeviceProviderService.updatePortsDid);
        assertEqualsButNotSame(portDescriptions, svDeviceProviderService.updatePortsDescs);

        MastershipRole cRole = MastershipRole.MASTER;
        MastershipRole dRole = MastershipRole.STANDBY;
        clDeviceProviderService.receivedRoleReply(DEVICE_ID, cRole, dRole);
        assertTrue(svDeviceProviderService.receivedRoleReply.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(DEVICE_ID, svDeviceProviderService.receivedRoleReplyDid);
        assertEquals(cRole, svDeviceProviderService.receivedRoleReplyRequested);
        assertEquals(dRole, svDeviceProviderService.receivedRoleReplyResponse);

        clDeviceProviderService.portStatusChanged(DEVICE_ID, portDescription);
        assertTrue(svDeviceProviderService.portStatusChanged.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(DEVICE_ID, svDeviceProviderService.portStatusChangedDid);
        assertEqualsButNotSame(portDescription, svDeviceProviderService.portStatusChangedDesc);

        Collection<PortStatistics> portStatistics = Collections.emptyList();
        clDeviceProviderService.updatePortStatistics(DEVICE_ID, portStatistics);
        assertTrue(svDeviceProviderService.updatePortStatistics.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(DEVICE_ID, svDeviceProviderService.updatePortStatisticsDid);
        assertEqualsButNotSame(portStatistics, svDeviceProviderService.updatePortStatisticsStats);

        clDeviceProviderService.deviceDisconnected(DEVICE_ID);
        assertTrue(svDeviceProviderService.deviceDisconnected.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(DEVICE_ID, svDeviceProviderService.deviceDisconnectedDid);



        // server to client communication
        svSideDeviceProvider.triggerProbe(DEVICE_ID);
        assertTrue(clDeviceProvider.triggerProbe.await(10, TimeUnit.SECONDS));
        assertEquals(DEVICE_ID, clDeviceProvider.triggerProbeDid);
        assertNotSame("Cannot be same instance if it properly went through gRPC",
                      DEVICE_ID, clDeviceProvider.triggerProbeDid);

        svSideDeviceProvider.roleChanged(DEVICE_ID, MastershipRole.STANDBY);
        assertTrue(clDeviceProvider.roleChanged.await(10, TimeUnit.SECONDS));
        assertEquals(DEVICE_ID, clDeviceProvider.roleChangedDid);
        assertNotSame("Cannot be same instance if it properly went through gRPC",
                      DEVICE_ID, clDeviceProvider.roleChangedDid);
        assertEquals(MastershipRole.STANDBY, clDeviceProvider.roleChangedNewRole);

        clDeviceProvider.isReachableReply = false;
        assertEquals(clDeviceProvider.isReachableReply,
                     svSideDeviceProvider.isReachable(DEVICE_ID));
        assertTrue(clDeviceProvider.isReachable.await(10, TimeUnit.SECONDS));
        assertEquals(DEVICE_ID, clDeviceProvider.isReachableDid);
        assertNotSame("Cannot be same instance if it properly went through gRPC",
                      DEVICE_ID, clDeviceProvider.isReachableDid);
    }

    @Test
    public void linkVanishedDevice() throws InterruptedException {
        RemoteServiceContext remoteServiceContext = client.get(uri);
        assertNotNull(remoteServiceContext);

        LinkProviderRegistry providerRegistry = remoteServiceContext.get(LinkProviderRegistry.class);
        assertNotNull(providerRegistry);

        final String schemeTest = "test";
        LinkProviderService client = providerRegistry.register(new StubLinkProvider(schemeTest));
        assertNotNull(client);

        client.linksVanished(DEVICE_ID);

        assertEquals(schemeTest, svLinkProviderService.provider().id().scheme());
        assertTrue(svLinkProviderService.calls.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(DEVICE_ID, svLinkProviderService.arg);
    }

    @Test
    public void linkVanishedPort() throws InterruptedException {
        RemoteServiceContext remoteServiceContext = client.get(uri);
        assertNotNull(remoteServiceContext);

        LinkProviderRegistry providerRegistry = remoteServiceContext.get(LinkProviderRegistry.class);
        assertNotNull(providerRegistry);

        final String schemeTest = "test";
        LinkProviderService client = providerRegistry.register(new StubLinkProvider(schemeTest));
        assertNotNull(client);


        final ConnectPoint cp = new ConnectPoint(DEVICE_ID, PORT);
        client.linksVanished(cp);
        assertEquals(schemeTest, svLinkProviderService.provider().id().scheme());
        assertTrue(svLinkProviderService.calls.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(cp, svLinkProviderService.arg);
    }

    @Test
    public void linkVanishedDescription() throws InterruptedException {
        RemoteServiceContext remoteServiceContext = client.get(uri);
        assertNotNull(remoteServiceContext);

        LinkProviderRegistry providerRegistry = remoteServiceContext.get(LinkProviderRegistry.class);
        assertNotNull(providerRegistry);

        final String schemeTest = "test";
        LinkProviderService client = providerRegistry.register(new StubLinkProvider(schemeTest));
        assertNotNull(client);

        ConnectPoint src = new ConnectPoint(deviceId("dev:1"), portNumber(10));
        ConnectPoint dst = new ConnectPoint(deviceId("dev:2"), portNumber(20));
        LinkDescription linkDescription = new DefaultLinkDescription(src, dst, Link.Type.DIRECT, ANON);
        client.linkVanished(linkDescription);
        assertEquals(schemeTest, svLinkProviderService.provider().id().scheme());
        assertTrue(svLinkProviderService.calls.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(linkDescription, svLinkProviderService.arg);
    }

    @Test
    public void linkDetected() throws InterruptedException {
        RemoteServiceContext remoteServiceContext = client.get(uri);
        assertNotNull(remoteServiceContext);

        LinkProviderRegistry providerRegistry = remoteServiceContext.get(LinkProviderRegistry.class);
        assertNotNull(providerRegistry);

        final String schemeTest = "test";
        LinkProviderService client = providerRegistry.register(new StubLinkProvider(schemeTest));
        assertNotNull(client);

        ConnectPoint src = new ConnectPoint(deviceId("dev:1"), portNumber(10));
        ConnectPoint dst = new ConnectPoint(deviceId("dev:2"), portNumber(20));
        LinkDescription linkDescription = new DefaultLinkDescription(src, dst, Link.Type.DIRECT, ANON);
        client.linkDetected(linkDescription);
        assertEquals(schemeTest, svLinkProviderService.provider().id().scheme());
        assertTrue(svLinkProviderService.calls.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(linkDescription, svLinkProviderService.arg);
    }

    @Test
    public void linkServiceBasics() throws InterruptedException {
        RemoteServiceContext remoteServiceContext = client.get(uri);
        assertNotNull(remoteServiceContext);

        LinkProviderRegistry providerRegistry = remoteServiceContext.get(LinkProviderRegistry.class);
        assertNotNull(providerRegistry);

        final String schemeTest = "test";
        LinkProviderService client = providerRegistry.register(new StubLinkProvider(schemeTest));
        assertNotNull(client);

        ConnectPoint src = new ConnectPoint(deviceId("dev:1"), portNumber(10));
        ConnectPoint dst = new ConnectPoint(deviceId("dev:2"), portNumber(20));
        LinkDescription linkDescription = new DefaultLinkDescription(src, dst, Link.Type.DIRECT, ANON);

        client.linkDetected(linkDescription);
        assertEquals(schemeTest, svLinkProviderService.provider().id().scheme());
        assertTrue(svLinkProviderService.calls.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(linkDescription, svLinkProviderService.arg);

        svLinkProviderService.reset();

        client.linkVanished(linkDescription);
        assertEquals(schemeTest, svLinkProviderService.provider().id().scheme());
        assertTrue(svLinkProviderService.calls.await(10, TimeUnit.SECONDS));
        assertEqualsButNotSame(linkDescription, svLinkProviderService.arg);
    }

    /**
     * Device Provider on CO side.
     */
    public class CTestDeviceProvider implements DeviceProvider {

        final CountDownLatch triggerProbe = new CountDownLatch(1);
        DeviceId triggerProbeDid;

        final CountDownLatch roleChanged = new CountDownLatch(1);
        DeviceId roleChangedDid;
        MastershipRole roleChangedNewRole;

        final CountDownLatch isReachable = new CountDownLatch(1);
        DeviceId isReachableDid;
        boolean isReachableReply = false;

        final CountDownLatch portStateChanged = new CountDownLatch(1);
        DeviceId portStateChangedDid;
        PortNumber portStateChangedPort;

        @Override
        public ProviderId id() {
            return PID;
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
            log.info("triggerProbe({}) on Client called", deviceId);
            triggerProbeDid = deviceId;
            triggerProbe.countDown();
        }

        @Override
        public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
            log.info("roleChanged({},{}) on Client called", deviceId, newRole);
            roleChangedDid = deviceId;
            roleChangedNewRole = newRole;
            roleChanged.countDown();
        }

        @Override
        public boolean isReachable(DeviceId deviceId) {
            log.info("isReachable({}) on Client called", deviceId);
            isReachableDid = deviceId;
            isReachable.countDown();
            return isReachableReply;
        }

        @Override
        public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                    boolean enable) {
            log.info("portState change to {} on ({},{}) on Client called", enable,
                     deviceId, portNumber);
            portStateChangedDid = deviceId;
            portStateChangedPort = portNumber;
            portStateChanged.countDown();

        }

    }

    class NoOpRemoteServiceProviderRegistry
        implements RemoteServiceProviderRegistry {

        @Override
        public RemoteServiceContextProviderService register(RemoteServiceContextProvider provider) {
            return new RemoteServiceContextProviderService() {

                @Override
                public RemoteServiceContextProvider provider() {
                    return provider;
                }
            };
        }

        @Override
        public void unregister(RemoteServiceContextProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return Collections.emptySet();
        }
    }

    /**
     * DeviceProvider on Metro side.
     */
    public class MTestDeviceProviderRegistry
        extends AbstractProviderRegistry<DeviceProvider, DeviceProviderService>
        implements DeviceProviderRegistry {

        @Override
        protected DeviceProviderService createProviderService(DeviceProvider provider) {
            log.info("createProviderService({})", provider);
            svSideDeviceProvider = provider;
            svDeviceProviderService = new MTestDeviceProviderService(provider);
            serverReady.countDown();
            return svDeviceProviderService;
        }

    }

    private final class MTestDeviceProviderService
            extends AbstractProviderService<DeviceProvider>
            implements DeviceProviderService {

        public MTestDeviceProviderService(DeviceProvider provider) {
            super(provider);
        }


        final CountDownLatch deviceConnected = new CountDownLatch(1);
        DeviceId deviceConnectedDid;
        DeviceDescription deviceConnectedDesc;

        @Override
        public void deviceConnected(DeviceId deviceId,
                                    DeviceDescription deviceDescription) {
            log.info("deviceConnected({}, {}) on Server called", deviceId, deviceDescription);
            deviceConnectedDid = deviceId;
            deviceConnectedDesc = deviceDescription;
            deviceConnected.countDown();
        }

        final CountDownLatch updatePorts = new CountDownLatch(1);
        DeviceId updatePortsDid;
        List<PortDescription> updatePortsDescs;

        @Override
        public void updatePorts(DeviceId deviceId,
                                List<PortDescription> portDescriptions) {
            log.info("updatePorts({}, {}) on Server called", deviceId, portDescriptions);
            updatePortsDid = deviceId;
            updatePortsDescs = portDescriptions;
            updatePorts.countDown();
        }

        final CountDownLatch receivedRoleReply = new CountDownLatch(1);
        DeviceId receivedRoleReplyDid;
        MastershipRole receivedRoleReplyRequested;
        MastershipRole receivedRoleReplyResponse;

        @Override
        public void receivedRoleReply(DeviceId deviceId, MastershipRole requested,
                                      MastershipRole response) {
            log.info("receivedRoleReply({}, {}, {}) on Server called", deviceId, requested, response);
            receivedRoleReplyDid = deviceId;
            receivedRoleReplyRequested = requested;
            receivedRoleReplyResponse = response;
            receivedRoleReply.countDown();
        }

        final CountDownLatch portStatusChanged = new CountDownLatch(1);
        DeviceId portStatusChangedDid;
        PortDescription portStatusChangedDesc;


        @Override
        public void portStatusChanged(DeviceId deviceId,
                                      PortDescription portDescription) {
            log.info("portStatusChanged({}, {}) on Server called", deviceId, portDescription);
            portStatusChangedDid = deviceId;
            portStatusChangedDesc = portDescription;
            portStatusChanged.countDown();
        }

        final CountDownLatch updatePortStatistics = new CountDownLatch(1);
        DeviceId updatePortStatisticsDid;
        Collection<PortStatistics> updatePortStatisticsStats;


        @Override
        public void updatePortStatistics(DeviceId deviceId,
                                         Collection<PortStatistics> portStatistics) {
            log.info("updatePortStatistics({}, {}) on Server called", deviceId, portStatistics);
            updatePortStatisticsDid = deviceId;
            updatePortStatisticsStats = portStatistics;
            updatePortStatistics.countDown();
        }

        final CountDownLatch deviceDisconnected = new CountDownLatch(1);
        DeviceId deviceDisconnectedDid;

        @Override
        public void deviceDisconnected(DeviceId deviceId) {
            log.info("deviceDisconnected({}) on Server called", deviceId);
            deviceDisconnectedDid = deviceId;
            deviceDisconnected.countDown();
        }
    }

    public class ServerSideLinkProviderRegistry
            extends AbstractProviderRegistry<LinkProvider, LinkProviderService>
            implements LinkProviderRegistry {

        @Override
        protected LinkProviderService createProviderService(LinkProvider provider) {
            svLinkProviderService = new ServerSideLinkProviderService(provider);
            return svLinkProviderService;
        }

    }

    public class ServerSideLinkProviderService
            extends AbstractProviderService<LinkProvider>
            implements LinkProviderService {

        CountDownLatch calls = new CountDownLatch(1);
        Object arg = null;

        public void reset() {
            calls = new CountDownLatch(1);
            arg = null;
        }

        public ServerSideLinkProviderService(LinkProvider provider) {
            super(provider);
        }

        @Override
        public void linksVanished(DeviceId deviceId) {
            log.info("linksVanished({})", deviceId);
            arg = deviceId;
            calls.countDown();
        }

        @Override
        public void linksVanished(ConnectPoint connectPoint) {
            log.info("linksVanished({})", connectPoint);
            arg = connectPoint;
            calls.countDown();
        }

        @Override
        public void linkVanished(LinkDescription linkDescription) {
            log.info("linksVanished({})", linkDescription);
            arg = linkDescription;
            calls.countDown();
        }

        @Override
        public void linkDetected(LinkDescription linkDescription) {
            log.info("linkDetected({})", linkDescription);
            arg = linkDescription;
            calls.countDown();
        }
    }

}
