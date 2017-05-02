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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.domain.DomainIntent;
import org.onosproject.net.domain.DomainIntentOperations;
import org.onosproject.net.domain.DomainIntentService;
import org.onosproject.net.domain.DomainPointToPointIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentInstallationContext;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentState;
import org.onosproject.store.service.WallClockTimestamp;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for domain Intent installer.
 */
public class DomainIntentInstallerTest extends AbstractIntentInstallerTest {
    protected DomainIntentInstaller installer;
    protected TestDomainIntentService domainIntentService;

    @Before
    public void setup() {
        super.setup();
        domainIntentService = new TestDomainIntentService();
        installer = new DomainIntentInstaller();
        installer.domainIntentService = domainIntentService;
        installer.trackerService = trackerService;
        installer.intentExtensionService = intentExtensionService;
        installer.intentInstallCoordinator = intentInstallCoordinator;

        installer.activated();
    }

    @After
    public void tearDown() {
        super.tearDown();
        installer.deactivated();
    }

    /**
     * Installs domain Intents.
     */
    @Test
    public void testInstall() {
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = createDomainIntents();
        IntentData toUninstall = null;
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentOperationContext<DomainIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);
        installer.apply(operationContext);
        assertEquals(intentInstallCoordinator.successContext, operationContext);
    }

    /**
     * Uninstall domain Intents.
     */
    @Test
    public void testUninstall() {
        List<Intent> intentsToUninstall = createDomainIntents();
        List<Intent> intentsToInstall = Lists.newArrayList();
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.WITHDRAWING,
                                                new WallClockTimestamp());
        IntentData toInstall = null;
        toUninstall = new IntentData(toUninstall, intentsToUninstall);
        IntentOperationContext<DomainIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);
        installer.apply(operationContext);
        assertEquals(intentInstallCoordinator.successContext, operationContext);
    }

    /**
     * Do both uninstall and install domain Intents.
     */
    @Test
    public void testUninstallAndInstall() {
        List<Intent> intentsToUninstall = createDomainIntents();
        List<Intent> intentsToInstall = createAnotherDomainIntents();
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = new IntentData(toUninstall, intentsToUninstall);
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentOperationContext<DomainIntent> operationContext;
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
        IntentOperationContext<DomainIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext<>(ImmutableList.of(), ImmutableList.of(), context);
        installer.apply(operationContext);
        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);
    }

    /**
     * Test if domain Intent installation operations failed.
     */
    @Test
    public void testInstallFailed() {
        domainIntentService = new TestFailedDomainIntentService();
        installer.domainIntentService = domainIntentService;
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = createDomainIntents();
        IntentData toUninstall = null;
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentOperationContext<DomainIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);
        installer.apply(operationContext);
        assertEquals(intentInstallCoordinator.failedContext, operationContext);
    }

    /**
     * Creates domain Intents.
     *
     * @return the domain Intents
     */
    private List<Intent> createDomainIntents() {
        FilteredConnectPoint ingress = new FilteredConnectPoint(CP1);
        FilteredConnectPoint egress = new FilteredConnectPoint(CP2);
        DomainPointToPointIntent intent = DomainPointToPointIntent.builder()
                .appId(APP_ID)
                .key(KEY1)
                .priority(DEFAULT_PRIORITY)
                .filteredIngressPoint(ingress)
                .filteredEgressPoint(egress)
                .links(ImmutableList.of())
                .build();

        return ImmutableList.of(intent);
    }

    /**
     * Create another domain Intents.
     *
     * @return the domain Intents
     */
    private List<Intent> createAnotherDomainIntents() {
        FilteredConnectPoint ingress = new FilteredConnectPoint(CP1);
        FilteredConnectPoint egress = new FilteredConnectPoint(CP3);
        DomainPointToPointIntent intent = DomainPointToPointIntent.builder()
                .appId(APP_ID)
                .key(KEY1)
                .priority(DEFAULT_PRIORITY)
                .filteredIngressPoint(ingress)
                .filteredEgressPoint(egress)
                .links(ImmutableList.of())
                .build();

        return ImmutableList.of(intent);
    }

    /**
     * Test domain Intent service; always success for all domain Intent operations.
     */
    class TestDomainIntentService implements DomainIntentService {
        @Override
        public void sumbit(DomainIntentOperations domainOperations) {
            domainOperations.callback().onSuccess(domainOperations);
        }
    }

    /**
     * Test domain Intent service; always failed of any domain Intent operation.
     */
    class TestFailedDomainIntentService extends TestDomainIntentService {

        @Override
        public void sumbit(DomainIntentOperations domainOperations) {
            domainOperations.callback().onError(domainOperations);
        }
    }

}
