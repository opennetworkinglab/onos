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
package org.onosproject.app.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationListener;
import org.onosproject.app.ApplicationState;
import org.onosproject.app.ApplicationStoreAdapter;
import org.onosproject.common.app.ApplicationArchive;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplication;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.Version;
import org.onosproject.core.VersionServiceAdapter;

import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onosproject.app.ApplicationEvent.Type.*;
import static org.onosproject.app.ApplicationState.ACTIVE;
import static org.onosproject.app.ApplicationState.INSTALLED;
import static org.onosproject.app.DefaultApplicationDescriptionTest.*;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Test of the application manager implementation.
 */
public class ApplicationManagerTest {

    public static final DefaultApplicationId APP_ID = new DefaultApplicationId(1, APP_NAME);
    private static final Version CORE_VERSION = Version.version(2, 1, "0", "");

    private ApplicationManager mgr = new ApplicationManager();
    private ApplicationListener listener = new TestListener();



    private boolean deactivated = false;

    @Before
    public void setUp() {
        injectEventDispatcher(mgr, new TestEventDispatcher());
        mgr.featuresService = new TestFeaturesService();
        mgr.store = new TestStore();
        mgr.activate();
        mgr.addListener(listener);
    }

    @After
    public void tearDown() {
        mgr.removeListener(listener);
        mgr.deactivate();
    }

    private void validate(Application app) {
        assertEquals("incorrect name", APP_NAME, app.id().name());
        assertEquals("incorrect version", VER, app.version());
        assertEquals("incorrect origin", ORIGIN, app.origin());

        assertEquals("incorrect description", DESC, app.description());
        assertEquals("incorrect features URI", FURL, app.featuresRepo().get());
        assertEquals("incorrect features", FEATURES, app.features());
    }

    private static class TestVersionService extends VersionServiceAdapter {

        @Override
        public Version version() {
            return CORE_VERSION;
        }
    }

    @Test
    public void install() {
        InputStream stream = ApplicationArchive.class.getResourceAsStream("app.zip");
        Application app = mgr.install(stream);
        validate(app);
        assertEquals("incorrect features URI used", app.featuresRepo().get(),
                     ((TestFeaturesService) mgr.featuresService).uri);
        assertEquals("incorrect app count", 1, mgr.getApplications().size());
        assertEquals("incorrect app", app, mgr.getApplication(APP_ID));
        assertEquals("incorrect app state", INSTALLED, mgr.getState(APP_ID));
        mgr.registerDeactivateHook(app.id(), this::deactivateHook);
    }

    private void deactivateHook() {
        deactivated = true;
    }

    @Test
    public void uninstall() {
        install();
        mgr.uninstall(APP_ID);
        assertEquals("incorrect app count", 0, mgr.getApplications().size());
    }

    @Test
    public void activate() {
        install();
        mgr.activate(APP_ID);
        assertEquals("incorrect app state", ACTIVE, mgr.getState(APP_ID));
        assertFalse("preDeactivate hook wrongly called", deactivated);
    }

    @Test
    public void deactivate() {
        activate();
        mgr.deactivate(APP_ID);
        assertEquals("incorrect app state", INSTALLED, mgr.getState(APP_ID));
        assertTrue("preDeactivate hook not called", deactivated);
    }


    private class TestListener implements ApplicationListener {
        private ApplicationEvent event;

        @Override
        public void event(ApplicationEvent event) {
            this.event = event;
        }
    }

    private class TestStore extends ApplicationStoreAdapter {

        private Application app;
        private ApplicationState state;

        @Override
        public Application create(InputStream appDescStream) {
            app = DefaultApplication.builder()
                    .withAppId(APP_ID)
                    .withVersion(VER)
                    .withTitle(TITLE)
                    .withDescription(DESC)
                    .withOrigin(ORIGIN)
                    .withCategory(CATEGORY)
                    .withUrl(URL)
                    .withReadme(README)
                    .withIcon(ICON)
                    .withRole(ROLE)
                    .withPermissions(PERMS)
                    .withFeaturesRepo(Optional.of(FURL))
                    .withFeatures(FEATURES)
                    .withRequiredApps(APPS)
                    .build();
            state = INSTALLED;
            delegate.notify(new ApplicationEvent(APP_INSTALLED, app));
            return app;
        }

        @Override
        public Set<Application> getApplications() {
            return app != null ? ImmutableSet.of(app) : ImmutableSet.of();
        }

        @Override
        public Application getApplication(ApplicationId appId) {
            return app;
        }

        @Override
        public void remove(ApplicationId appId) {
            delegate.notify(new ApplicationEvent(APP_UNINSTALLED, app));
            app = null;
            state = null;
        }

        @Override
        public ApplicationState getState(ApplicationId appId) {
            return state;
        }

        @Override
        public void activate(ApplicationId appId) {
            state = ApplicationState.ACTIVE;
            delegate.notify(new ApplicationEvent(APP_ACTIVATED, app));
        }

        @Override
        public void deactivate(ApplicationId appId) {
            state = INSTALLED;
            delegate.notify(new ApplicationEvent(APP_DEACTIVATED, app));
        }

        @Override
        public ApplicationId getId(String name) {
            return new DefaultApplicationId(0, name);
        }
    }

    private class TestFeaturesService extends FeaturesServiceAdapter {
        private URI uri;
        private Set<String> features = new HashSet<>();

        @Override
        public void addRepository(URI uri) throws Exception {
            this.uri = uri;
        }

        @Override
        public void removeRepository(URI uri) throws Exception {
            this.uri = null;
        }

        @Override
        public void installFeature(String name) throws Exception {
            features.add(name);
        }

        @Override
        public void uninstallFeature(String name) throws Exception {
            features.remove(name);
        }
    }

}