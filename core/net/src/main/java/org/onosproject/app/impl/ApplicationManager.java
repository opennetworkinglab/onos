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
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.Permission;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.app.ApplicationEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the application management service.
 */
@Component(immediate = true)
@Service
public class ApplicationManager implements ApplicationService, ApplicationAdminService {

    private final Logger log = getLogger(getClass());

    private static final String APP_ID_NULL = "Application ID cannot be null";

    protected final AbstractListenerRegistry<ApplicationEvent, ApplicationListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final ApplicationStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FeaturesService featuresService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        eventDispatcher.addSink(ApplicationEvent.class, listenerRegistry);
        store.setDelegate(delegate);
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
        return store.getApplications();
    }

    @Override
    public ApplicationId getId(String name) {
        checkNotNull(name, "Name cannot be null");
        return store.getId(name);
    }

    @Override
    public Application getApplication(ApplicationId appId) {
        checkNotNull(appId, APP_ID_NULL);
        return store.getApplication(appId);
    }

    @Override
    public ApplicationState getState(ApplicationId appId) {
        checkNotNull(appId, APP_ID_NULL);
        return store.getState(appId);
    }

    @Override
    public Set<Permission> getPermissions(ApplicationId appId) {
        checkNotNull(appId, APP_ID_NULL);
        return store.getPermissions(appId);
    }

    @Override
    public Application install(InputStream appDescStream) {
        checkNotNull(appDescStream, "Application archive stream cannot be null");
        return store.create(appDescStream);
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

    @Override
    public void addListener(ApplicationListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(ApplicationListener listener) {
        listenerRegistry.removeListener(listener);
    }

    private class InternalStoreDelegate implements ApplicationStoreDelegate {
        @Override
        public void notify(ApplicationEvent event) {
            ApplicationEvent.Type type = event.type();
            Application app = event.subject();
            try {
                if (type == APP_ACTIVATED) {
                    installAppFeatures(app);
                    log.info("Application {} has been activated", app.id().name());

                } else if (type == APP_DEACTIVATED) {
                    uninstallAppFeatures(app);
                    log.info("Application {} has been deactivated", app.id().name());

                } else if (type == APP_INSTALLED) {
                    installAppArtifacts(app);
                    log.info("Application {} has been installed", app.id().name());

                } else if (type == APP_UNINSTALLED) {
                    uninstallAppFeatures(app);
                    uninstallAppArtifacts(app);
                    log.info("Application {} has been uninstalled", app.id().name());

                }
                eventDispatcher.post(event);

            } catch (Exception e) {
                log.warn("Unable to perform operation on application " + app.id().name(), e);
            }
        }
    }

    // The following methods are fully synchronized to guard against remote vs.
    // locally induced feature service interactions.

    private synchronized void installAppArtifacts(Application app) throws Exception {
        if (app.featuresRepo().isPresent()) {
            featuresService.addRepository(app.featuresRepo().get());
        }
    }

    private synchronized void uninstallAppArtifacts(Application app) throws Exception {
        if (app.featuresRepo().isPresent()) {
            featuresService.removeRepository(app.featuresRepo().get());
        }
    }

    private synchronized void installAppFeatures(Application app) throws Exception {
        for (String name : app.features()) {
            Feature feature = featuresService.getFeature(name);
            if (!featuresService.isInstalled(feature)) {
                featuresService.installFeature(name);
            }
        }
    }

    private synchronized void uninstallAppFeatures(Application app) throws Exception {
        for (String name : app.features()) {
            Feature feature = featuresService.getFeature(name);
            if (featuresService.isInstalled(feature)) {
                featuresService.uninstallFeature(name);
            }
        }
    }

}
