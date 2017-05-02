/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.intent.impl.installer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription;
import org.onosproject.net.behaviour.protection.TransportEndpointDescription;
import org.onosproject.net.config.BaseConfig;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentInstallationContext;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.ProtectionEndpointIntent;
import org.onosproject.store.service.WallClockTimestamp;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

/**
 * Tests for protection endpoint Intent installer.
 */
public class ProtectionEndpointIntentInstallerTest extends AbstractIntentInstallerTest {
    private static final String FINGERPRINT = "Test fingerprint";
    protected ProtectionEndpointIntentInstaller installer;
    protected NetworkConfigService networkConfigService;
    private static TestServiceDirectory directory;
    private static ServiceDirectory original;

    @BeforeClass
    public static void setUpClass() throws TestUtils.TestUtilsException {
        directory = new TestServiceDirectory();

        CodecManager codecService = new CodecManager();
        codecService.activate();
        directory.add(CodecService.class, codecService);

        // replace service directory used by BaseConfig
        original = TestUtils.getField(BaseConfig.class, "services");
        TestUtils.setField(BaseConfig.class, "services", directory);
    }

    @AfterClass
    public static void tearDownClass() throws TestUtils.TestUtilsException {
        TestUtils.setField(BaseConfig.class, "services", original);
    }

    @Before
    public void setup() {
        super.setup();
        networkConfigService = new TestNetworkConfigService();
        installer = new ProtectionEndpointIntentInstaller();
        installer.networkConfigService = networkConfigService;
        installer.intentExtensionService = intentExtensionService;
        installer.intentInstallCoordinator = intentInstallCoordinator;
        installer.trackerService = trackerService;

        installer.activate();
    }

    @After
    public void tearDown() {
        super.tearDown();
        installer.deactivated();
    }

    /**
     * Installs protection endpoint Intents.
     * framework.
     */
    @Test
    public void testInstallIntents() {
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = createProtectionIntents(CP2);
        IntentData toUninstall = null;
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentOperationContext<ProtectionEndpointIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);
        installer.apply(operationContext);
        assertEquals(intentInstallCoordinator.successContext, operationContext);
    }

    /**
     * Uninstalls protection endpoint Intents.
     * framework.
     */
    @Test
    public void testUninstallIntents() {
        List<Intent> intentsToUninstall = createProtectionIntents(CP2);
        List<Intent> intentsToInstall = Lists.newArrayList();
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLING,
                                                new WallClockTimestamp());
        IntentData toInstall = null;
        IntentOperationContext<ProtectionEndpointIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);
        installer.apply(operationContext);
        assertEquals(intentInstallCoordinator.successContext, operationContext);
    }

    /**
     * Test both uninstall and install protection endpoint Intents.
     * framework.
     */
    @Test
    public void testUninstallAndInstallIntents() {
        List<Intent> intentsToUninstall = createProtectionIntents(CP2);
        List<Intent> intentsToInstall = createProtectionIntents(CP3);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = new IntentData(toUninstall, intentsToInstall);
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentOperationContext<ProtectionEndpointIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);
        installer.apply(operationContext);
        assertEquals(intentInstallCoordinator.successContext, operationContext);
    }

    /**
     * Nothing to uninstall or install.
     */
    @Test
    public void testNoAnyIntentToApply() {
        IntentData toInstall = null;
        IntentData toUninstall = null;
        IntentOperationContext<ProtectionEndpointIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext<>(ImmutableList.of(), ImmutableList.of(), context);
        installer.apply(operationContext);
        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);
    }

    /**
     * Test if installation failed.
     * framework.
     */
    @Test
    public void testInstallFailed() {
        networkConfigService = new TestFailedNetworkConfigService();
        installer.networkConfigService = networkConfigService;
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = createProtectionIntents(CP2);
        IntentData toUninstall = null;
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentOperationContext<ProtectionEndpointIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);
        installer.apply(operationContext);
        assertEquals(intentInstallCoordinator.failedContext, operationContext);
    }

    /**
     * Creates protection endpoint Intents by givent output point.
     *
     * @param output the output point
     * @return the protection endpoint Intents
     */
    private List<Intent> createProtectionIntents(ConnectPoint output) {
        FilteredConnectPoint filteredOutput = new FilteredConnectPoint(output);
        TransportEndpointDescription path = TransportEndpointDescription.builder()
                .withOutput(filteredOutput)
                .withEnabled(true).build();

        List<TransportEndpointDescription> paths = ImmutableList.of(path);
        ProtectedTransportEndpointDescription description =
                ProtectedTransportEndpointDescription.of(paths, CP2.deviceId(), FINGERPRINT);
        ProtectionEndpointIntent intent = ProtectionEndpointIntent.builder()
                .appId(APP_ID)
                .description(description)
                .deviceId(CP1.deviceId())
                .key(KEY1)
                .resourceGroup(RG1)
                .build();

        return ImmutableList.of(intent);
    }

    class TestNetworkConfigService extends NetworkConfigServiceAdapter {
        protected Set<NetworkConfigListener> listeners = Sets.newHashSet();

        @Override
        public void addListener(NetworkConfigListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeListener(NetworkConfigListener listener) {
            listeners.remove(listener);
        }

        @Override
        public <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass, JsonNode json) {
            NetworkConfigEvent event = new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                                              subject,
                                                              configClass);
            CompletableFuture.runAsync(() -> {
                listeners.forEach(listener -> listener.event(event));
            });
            return null;
        }

        @Override
        public <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass) {
            NetworkConfigEvent event = new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_REMOVED,
                                                              subject,
                                                              configClass);
            CompletableFuture.runAsync(() -> {
                listeners.forEach(listener -> listener.event(event));
            });
        }
    }

    /**
     * Test network config service; will send wrong events to listeners.
     */
    class TestFailedNetworkConfigService extends TestNetworkConfigService {

        @Override
        public <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass, JsonNode json) {
            NetworkConfigEvent event = new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_REMOVED,
                                                              subject,
                                                              configClass);
            CompletableFuture.runAsync(() -> {
                listeners.forEach(listener -> listener.event(event));
            });
            return null;
        }

        @Override
        public <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass) {
            NetworkConfigEvent event = new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                                              subject,
                                                              configClass);
            CompletableFuture.runAsync(() -> {
                listeners.forEach(listener -> listener.event(event));
            });
        }
    }
}
