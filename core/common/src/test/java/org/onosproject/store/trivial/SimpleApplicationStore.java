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
import org.onosproject.app.ApplicationDescription;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationIdStore;
import org.onosproject.app.ApplicationState;
import org.onosproject.app.ApplicationStore;
import org.onosproject.common.app.ApplicationArchive;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplication;
import org.onosproject.security.Permission;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.onosproject.app.ApplicationEvent.Type.APP_ACTIVATED;
import static org.onosproject.app.ApplicationEvent.Type.APP_DEACTIVATED;
import static org.onosproject.app.ApplicationEvent.Type.APP_INSTALLED;
import static org.onosproject.app.ApplicationEvent.Type.APP_PERMISSIONS_CHANGED;
import static org.onosproject.app.ApplicationEvent.Type.APP_UNINSTALLED;
import static org.onosproject.app.ApplicationState.ACTIVE;
import static org.onosproject.app.ApplicationState.INSTALLED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of network control applications.
 */
@Component(immediate = true, service = ApplicationStore.class)
public class SimpleApplicationStore extends ApplicationArchive
        implements ApplicationStore {

    private final Logger log = getLogger(getClass());

    // App inventory & states
    private final ConcurrentMap<ApplicationId, DefaultApplication> apps =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<ApplicationId, ApplicationState> states =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<ApplicationId, Set<Permission>> permissions =
            new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ApplicationIdStore idStore;

    @Activate
    public void activate() {
        loadFromDisk();
        log.info("Started");
    }

    private void loadFromDisk() {
        for (String name : getApplicationNames()) {
            ApplicationId appId = idStore.registerApplication(name);
            ApplicationDescription appDesc = getApplicationDescription(name);
            DefaultApplication app =
                DefaultApplication
                    .builder(appDesc)
                    .withAppId(appId)
                    .build();

            apps.put(appId, app);
            states.put(appId, isActive(name) ? INSTALLED : ACTIVE);
            // load app permissions
        }
    }

    @Deactivate
    public void deactivate() {
        apps.clear();
        states.clear();
        permissions.clear();
        log.info("Stopped");
    }

    @Override
    public Set<Application> getApplications() {
        return ImmutableSet.copyOf(apps.values());
    }

    @Override
    public ApplicationId getId(String name) {
        return idStore.getAppId(name);
    }

    @Override
    public Application getApplication(ApplicationId appId) {
        return apps.get(appId);
    }

    @Override
    public ApplicationState getState(ApplicationId appId) {
        return states.get(appId);
    }

    @Override
    public Application create(InputStream appDescStream) {
        ApplicationDescription appDesc = saveApplication(appDescStream);
        ApplicationId appId = idStore.registerApplication(appDesc.name());
        DefaultApplication app =
            DefaultApplication
                .builder(appDesc)
                .withAppId(appId)
                .build();

        apps.put(appId, app);
        states.put(appId, INSTALLED);
        delegate.notify(new ApplicationEvent(APP_INSTALLED, app));
        return app;
    }

    @Override
    public void remove(ApplicationId appId) {
        Application app = apps.remove(appId);
        if (app != null) {
            states.remove(appId);
            delegate.notify(new ApplicationEvent(APP_UNINSTALLED, app));
            purgeApplication(app.id().name());
        }
    }

    @Override
    public void activate(ApplicationId appId) {
        Application app = apps.get(appId);
        if (app != null) {
            setActive(appId.name());
            states.put(appId, ACTIVE);
            delegate.notify(new ApplicationEvent(APP_ACTIVATED, app));
        }
    }

    @Override
    public void deactivate(ApplicationId appId) {
        Application app = apps.get(appId);
        if (app != null) {
            clearActive(appId.name());
            states.put(appId, INSTALLED);
            delegate.notify(new ApplicationEvent(APP_DEACTIVATED, app));
        }
    }

    @Override
    public Set<Permission> getPermissions(ApplicationId appId) {
        return permissions.get(appId);
    }

    @Override
    public void setPermissions(ApplicationId appId, Set<Permission> permissions) {
        Application app = getApplication(appId);
        if (app != null) {
            this.permissions.put(appId, permissions);
            delegate.notify(new ApplicationEvent(APP_PERMISSIONS_CHANGED, app));
        }
    }
}
