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
package org.onosproject.app.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationListener;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.app.ApplicationStore;
import org.onosproject.app.ApplicationStoreDelegate;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.security.Permission;
import org.onosproject.security.SecurityUtil;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.app.ApplicationEvent.Type.*;
import static org.onosproject.security.AppPermission.Type.*;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the application management service.
 */
@Component(immediate = true)
@Service
public class ApplicationManager
        extends AbstractListenerManager<ApplicationEvent, ApplicationListener>
        implements ApplicationService, ApplicationAdminService {

    private final Logger log = getLogger(getClass());

    private static final String APP_ID_NULL = "Application ID cannot be null";

    private final ApplicationStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FeaturesService featuresService;

    private boolean initializing;

    @Activate
    public void activate() {
        eventDispatcher.addSink(ApplicationEvent.class, listenerRegistry);

        initializing = true;
        store.setDelegate(delegate);
        initializing = false;

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(ApplicationEvent.class);
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public Set<Application> getApplications() {
        checkPermission(APP_READ);
        return store.getApplications();
    }

    @Override
    public ApplicationId getId(String name) {
        checkPermission(APP_READ);
        checkNotNull(name, "Name cannot be null");
        return store.getId(name);
    }

    @Override
    public Application getApplication(ApplicationId appId) {
        checkPermission(APP_READ);
        checkNotNull(appId, APP_ID_NULL);
        return store.getApplication(appId);
    }

    @Override
    public ApplicationState getState(ApplicationId appId) {
        checkPermission(APP_READ);
        checkNotNull(appId, APP_ID_NULL);
        return store.getState(appId);
    }

    @Override
    public Set<Permission> getPermissions(ApplicationId appId) {
        checkPermission(APP_READ);
        checkNotNull(appId, APP_ID_NULL);
        return store.getPermissions(appId);
    }

    @Override
    public Application install(InputStream appDescStream) {
        checkNotNull(appDescStream, "Application archive stream cannot be null");
        Application app = store.create(appDescStream);
        SecurityUtil.register(app.id());
        return app;
    }

    @Override
    public void uninstall(ApplicationId appId) {
        checkNotNull(appId, APP_ID_NULL);
        try {
            store.remove(appId);
        } catch (Exception e) {
            log.warn("Unable to purge application directory for {}", appId.name());
        }
    }

    @Override
    public void activate(ApplicationId appId) {
        checkNotNull(appId, APP_ID_NULL);
        if (!SecurityUtil.isAppSecured(appId)) {
            return;
        }
        store.activate(appId);
    }

    @Override
    public void deactivate(ApplicationId appId) {
        checkNotNull(appId, APP_ID_NULL);
        store.deactivate(appId);
    }

    @Override
    public void setPermissions(ApplicationId appId, Set<Permission> permissions) {
        checkNotNull(appId, APP_ID_NULL);
        checkNotNull(permissions, "Permissions cannot be null");
        store.setPermissions(appId, permissions);
    }

    private class InternalStoreDelegate implements ApplicationStoreDelegate {
        @Override
        public void notify(ApplicationEvent event) {
            ApplicationEvent.Type type = event.type();
            Application app = event.subject();
            try {
                if (type == APP_ACTIVATED) {
                    if (installAppFeatures(app)) {
                        log.info("Application {} has been activated", app.id().name());
                    }

                } else if (type == APP_DEACTIVATED) {
                    if (uninstallAppFeatures(app)) {
                        log.info("Application {} has been deactivated", app.id().name());
                    }

                } else if (type == APP_INSTALLED) {
                    if (installAppArtifacts(app)) {
                        log.info("Application {} has been installed", app.id().name());
                    }

                } else if (type == APP_UNINSTALLED) {
                    if (uninstallAppFeatures(app) || uninstallAppArtifacts(app)) {
                        log.info("Application {} has been uninstalled", app.id().name());
                    }

                }
                post(event);

            } catch (Exception e) {
                log.warn("Unable to perform operation on application " + app.id().name(), e);
            }
        }
    }

    // The following methods are fully synchronized to guard against remote vs.
    // locally induced feature service interactions.

    private synchronized boolean installAppArtifacts(Application app) throws Exception {
        if (app.featuresRepo().isPresent() &&
                featuresService.getRepository(app.featuresRepo().get()) == null) {
            featuresService.addRepository(app.featuresRepo().get());
            return true;
        }
        return false;
    }

    private synchronized boolean uninstallAppArtifacts(Application app) throws Exception {
        if (app.featuresRepo().isPresent() &&
                featuresService.getRepository(app.featuresRepo().get()) != null) {
            featuresService.removeRepository(app.featuresRepo().get());
            return true;
        }
        return false;
    }

    private synchronized boolean installAppFeatures(Application app) throws Exception {
        boolean changed = false;
        for (String name : app.features()) {
            Feature feature = featuresService.getFeature(name);
            if (feature != null && !featuresService.isInstalled(feature)) {
                featuresService.installFeature(name);
                changed = true;
            } else if (feature == null && !initializing) {
                // Suppress feature-not-found reporting during startup since these
                // can arise naturally from the staggered cluster install.
                log.warn("Feature {} not found", name);
            }
        }
        return changed;
    }

    private synchronized boolean uninstallAppFeatures(Application app) throws Exception {
        boolean changed = false;
        for (String name : app.features()) {
            Feature feature = featuresService.getFeature(name);
            if (feature != null && featuresService.isInstalled(feature)) {
                featuresService.uninstallFeature(name);
                changed = true;
            } else if (feature == null) {
                log.warn("Feature {} not found", name);
            }
        }
        return changed;
    }

}
