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

package org.onosproject.net.intent.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.TestInstallableIntent;
import org.onosproject.store.intent.impl.IntentStoreAdapter;
import org.onosproject.store.service.WallClockTimestamp;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for install coordinator.
 */
public class InstallCoordinatorTest extends AbstractIntentTest {
    private static final int INSTALL_DELAY = 100;
    private static final int INSTALL_DURATION = 1000;

    private InstallCoordinator installCoordinator;
    private InstallerRegistry installerRegistry;
    private TestIntentStore intentStore;
    private TestIntentInstaller intentInstaller;


    @Before
    public void setup() {
        super.setUp();
        installerRegistry = new InstallerRegistry();
        intentStore = new TestIntentStore();
        intentInstaller = new TestIntentInstaller();
        installerRegistry.registerInstaller(TestInstallableIntent.class, intentInstaller);

        installCoordinator = new InstallCoordinator(installerRegistry, intentStore);
    }

    @After
    public void tearDown() {
        installerRegistry.unregisterInstaller(TestInstallableIntent.class);
        super.tearDown();
    }

    /**
     * Installs test Intents.
     */
    @Test
    public void testInstallIntent() {
        IntentData toInstall = new IntentData(createTestIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        List<Intent> intents = Lists.newArrayList();
        IntStream.range(0, 10).forEach(val -> {
            intents.add(new TestInstallableIntent(val));
        });
        toInstall = IntentData.compiled(toInstall, intents);
        installCoordinator.installIntents(Optional.empty(), Optional.of(toInstall));
        Intent toInstallIntent = toInstall.intent();
        TestTools.assertAfter(INSTALL_DELAY, INSTALL_DURATION, () -> {
            IntentData newData = intentStore.newData;
            assertEquals(toInstallIntent, newData.intent());
            assertEquals(IntentState.INSTALLED, newData.state());
            assertEquals(intents, newData.installables());
        });
    }

    /**
     * Uninstalls test Intents.
     */
    @Test
    public void testUninstallIntent() {
        IntentData toUninstall = new IntentData(createTestIntent(),
                                              IntentState.WITHDRAWING,
                                              new WallClockTimestamp());
        List<Intent> intents = Lists.newArrayList();

        IntStream.range(0, 10).forEach(val -> {
            intents.add(new TestInstallableIntent(val));
        });

        toUninstall = IntentData.compiled(toUninstall, intents);

        installCoordinator.installIntents(Optional.of(toUninstall), Optional.empty());
        Intent toUninstallIntent = toUninstall.intent();
        TestTools.assertAfter(INSTALL_DELAY, INSTALL_DURATION, () -> {
            IntentData newData = intentStore.newData;
            assertEquals(toUninstallIntent, newData.intent());
            assertEquals(IntentState.WITHDRAWN, newData.state());
            assertEquals(ImmutableList.of(), newData.installables());
        });

    }

    /**
     * Do both uninstall and install test Intents.
     */
    @Test
    public void testUninstallAndInstallIntent() {
        IntentData toUninstall = new IntentData(createTestIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        IntentData toInstall = new IntentData(createTestIntent(),
                                                IntentState.INSTALLING,
                                                new WallClockTimestamp());
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = Lists.newArrayList();

        IntStream.range(0, 10).forEach(val -> {
            intentsToUninstall.add(new TestInstallableIntent(val));
        });

        IntStream.range(10, 20).forEach(val -> {
            intentsToInstall.add(new TestInstallableIntent(val));
        });

        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);
        toInstall = IntentData.compiled(toInstall, intentsToInstall);

        installCoordinator.installIntents(Optional.of(toUninstall), Optional.of(toInstall));
        Intent toInstallIntent = toInstall.intent();

        TestTools.assertAfter(INSTALL_DELAY, INSTALL_DURATION, () -> {
            IntentData newData = intentStore.newData;
            assertEquals(toInstallIntent, newData.intent());
            assertEquals(IntentState.INSTALLED, newData.state());
            assertEquals(intentsToInstall, newData.installables());
        });
    }

    /**
     * Not uninstall nor install anything.
     */
    @Test
    public void testInstallNothing() {
        installCoordinator.installIntents(Optional.empty(), Optional.empty());
        assertNull(intentStore.newData);
    }

    /**
     * Test Intent install failed.
     */
    @Test
    public void testInstallFailed() {
        installerRegistry.unregisterInstaller(TestInstallableIntent.class);
        installerRegistry.registerInstaller(TestInstallableIntent.class, new TestFailedIntentInstaller());
        IntentData toUninstall = new IntentData(createTestIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        IntentData toInstall = new IntentData(createTestIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = Lists.newArrayList();

        IntStream.range(0, 10).forEach(val -> {
            intentsToUninstall.add(new TestInstallableIntent(val));
        });

        IntStream.range(10, 20).forEach(val -> {
            intentsToInstall.add(new TestInstallableIntent(val));
        });

        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);
        toInstall = IntentData.compiled(toInstall, intentsToInstall);

        installCoordinator.installIntents(Optional.of(toUninstall), Optional.of(toInstall));

        Intent toUninstallIntent = toUninstall.intent();
        TestTools.assertAfter(INSTALL_DELAY, INSTALL_DURATION, () -> {
            IntentData newData = intentStore.newData;
            assertEquals(toUninstallIntent, newData.intent());
            assertEquals(IntentState.CORRUPT, newData.state());
            assertEquals(intentsToUninstall, newData.installables());
        });
    }

    /**
     * Creates a test Intent.
     *
     * @return the test Intent.
     */
    private PointToPointIntent createTestIntent() {
        FilteredConnectPoint ingress = new FilteredConnectPoint(ConnectPoint.deviceConnectPoint("s1/1"));
        FilteredConnectPoint egress = new FilteredConnectPoint(ConnectPoint.deviceConnectPoint("s1/2"));
        ApplicationId appId = TestApplicationId.create("test App");
        return PointToPointIntent.builder()
                .filteredIngressPoint(ingress)
                .filteredEgressPoint(egress)
                .appId(appId)
                .key(Key.of("Test key", appId))
                .build();
    }

    /**
     * Test Intent store; records the newest Intent data.
     */
    class TestIntentStore extends IntentStoreAdapter {
        IntentData newData;
        @Override
        public void write(IntentData newData) {
            this.newData = newData;
        }
    }

    /**
     * Test Intent installer; always success for every Intent operation.
     */
    class TestIntentInstaller implements IntentInstaller<TestInstallableIntent> {
        @Override
        public void apply(IntentOperationContext<TestInstallableIntent> context) {
            installCoordinator.success(context);
        }
    }

    /**
     * Test Intent installer; always failed for every Intent operation.
     */
    class TestFailedIntentInstaller implements IntentInstaller<TestInstallableIntent> {
        @Override
        public void apply(IntentOperationContext<TestInstallableIntent> context) {
            installCoordinator.failed(context);
        }
    }
}
