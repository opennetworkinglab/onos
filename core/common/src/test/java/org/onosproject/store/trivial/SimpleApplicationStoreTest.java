/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.trivial;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.Tools;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationStoreDelegate;
import org.onosproject.common.app.ApplicationArchive;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.app.ApplicationIdStoreAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.security.AppPermission;
import org.onosproject.security.Permission;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.onosproject.app.ApplicationEvent.Type.*;
import static org.onosproject.app.ApplicationState.ACTIVE;
import static org.onosproject.app.ApplicationState.INSTALLED;

/**
 * Test of the trivial application store implementation.
 */
public class SimpleApplicationStoreTest {

    static final File STORE = Files.createTempDir();

    private TestApplicationStore store = new TestApplicationStore();
    private TestDelegate delegate = new TestDelegate();
    private static final Object LOCK = new Object();

    @Before
    public void setUp() {
        store.idStore = new TestIdStore();
        store.setRootPath(STORE.getAbsolutePath());
        store.setDelegate(delegate);
        store.activate();
    }

    @After
    public void tearDown() throws IOException {
        if (STORE.exists()) {
            Tools.removeDirectory(STORE);
        }
        store.deactivate();
    }

    private Application createTestApp() {
        synchronized (LOCK) {
            return store.create(ApplicationArchive.class.getResourceAsStream("app.zip"));
        }
    }

    @Test
    public void create() {
        Application app = createTestApp();
        assertEquals("incorrect name", "org.foo.app", app.id().name());
        assertEquals("incorrect app count", 1, store.getApplications().size());
        assertEquals("incorrect app", app, store.getApplication(app.id()));
        assertEquals("incorrect app state", INSTALLED, store.getState(app.id()));
        assertEquals("incorrect event type", APP_INSTALLED, delegate.event.type());
        assertEquals("incorrect event app", app, delegate.event.subject());
    }

    @Test
    public void remove() {
        Application app = createTestApp();
        store.remove(app.id());
        assertEquals("incorrect app count", 0, store.getApplications().size());
        assertEquals("incorrect event type", APP_UNINSTALLED, delegate.event.type());
        assertEquals("incorrect event app", app, delegate.event.subject());
    }

    @Test
    public void activate() {
        Application app = createTestApp();
        store.activate(app.id());
        assertEquals("incorrect app count", 1, store.getApplications().size());
        assertEquals("incorrect app state", ACTIVE, store.getState(app.id()));
        assertEquals("incorrect event type", APP_ACTIVATED, delegate.event.type());
        assertEquals("incorrect event app", app, delegate.event.subject());
    }

    @Test
    public void deactivate() {
        Application app = createTestApp();
        store.deactivate(app.id());
        assertEquals("incorrect app count", 1, store.getApplications().size());
        assertEquals("incorrect app state", INSTALLED, store.getState(app.id()));
        assertEquals("incorrect event type", APP_DEACTIVATED, delegate.event.type());
        assertEquals("incorrect event app", app, delegate.event.subject());
    }

    @Test
    public void permissions() {
        Application app = createTestApp();
        ImmutableSet<Permission> permissions =
                ImmutableSet.of(new Permission(AppPermission.class.getName(), "FLOWRULE_WRITE"));
        store.setPermissions(app.id(), permissions);
        assertEquals("incorrect app perms", 1, store.getPermissions(app.id()).size());
        assertEquals("incorrect app state", INSTALLED, store.getState(app.id()));
        assertEquals("incorrect event type", APP_PERMISSIONS_CHANGED, delegate.event.type());
        assertEquals("incorrect event app", app, delegate.event.subject());
    }

    private class TestIdStore extends ApplicationIdStoreAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(1, name);
        }

        @Override
        public ApplicationId getAppId(String name) {
            return new DefaultApplicationId(1, name);
        }
    }

    private class TestDelegate implements ApplicationStoreDelegate {
        private ApplicationEvent event;

        @Override
        public void notify(ApplicationEvent event) {
            this.event = event;
        }
    }

    private class TestApplicationStore extends SimpleApplicationStore {
        @Override
        public void setRootPath(String root) {
            super.setRootPath(root);
        }
    }
}