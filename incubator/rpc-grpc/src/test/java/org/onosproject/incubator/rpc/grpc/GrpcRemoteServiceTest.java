/*
 * Copyright 2015 Open Networking Laboratory
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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.incubator.rpc.RemoteServiceContextProvider;
import org.onosproject.incubator.rpc.RemoteServiceContextProviderService;
import org.onosproject.incubator.rpc.RemoteServiceProviderRegistry;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
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

    private CountDownLatch serverReady;

    private URI uri;

    public static int pickListenPort() {
        try {
            // pick unused port
            ServerSocket socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException e) {
            // something went wrong, try picking randomly
            return RandomUtils.nextInt(49152, 0xFFFF + 1);
        }
    }

    @Before
    public void setUp() throws Exception {
        serverReady = new CountDownLatch(1);
        server = new GrpcRemoteServiceServer();
        server.deviceProviderRegistry = new MTestDeviceProviderRegistry();
        // todo: pass proper ComponentContext
        server.listenPort = pickListenPort();
        uri = URI.create("grpc://localhost:" + server.listenPort);
        server.activate(null);

        client = new GrpcRemoteServiceProvider();
        client.rpcRegistry = new NoOpRemoteServiceProviderRegistry();
        client.activate();
    }

    @After
    public void tearDown() {
        client.deactivate();
        server.deactivate();
    }

    private static void assertEqualsButNotSame(Object expected, Object actual) {
        assertEquals(expected, actual);
        assertNotSame("Cannot be same instance if it properly went through gRPC",
                      expected, actual);
    }

    @Test
    public void basics() throws InterruptedException {
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

}
