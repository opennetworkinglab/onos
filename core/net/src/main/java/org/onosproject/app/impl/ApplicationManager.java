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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Uninterruptibles;
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
import org.onosproject.core.VersionService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.security.Permission;
import org.onosproject.security.SecurityUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.app.ApplicationEvent.Type.APP_ACTIVATED;
import static org.onosproject.app.ApplicationEvent.Type.APP_DEACTIVATED;
import static org.onosproject.app.ApplicationEvent.Type.APP_INSTALLED;
import static org.onosproject.app.ApplicationEvent.Type.APP_UNINSTALLED;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.APP_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the application management service.
 */
@Component(immediate = true, service = {ApplicationService.class, ApplicationAdminService.class})
public class ApplicationManager
        extends AbstractListenerManager<ApplicationEvent, ApplicationListener>
        implements ApplicationService, ApplicationAdminService {

    private final Logger log = getLogger(getClass());

    private static final String APP_ID_NULL = "Application ID cannot be null";
    private static final long DEFAULT_OPERATION_TIMEOUT_MILLIS = 2000;
    private final ApplicationStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ApplicationStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FeaturesService featuresService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VersionService versionService;

    // Application supplied hooks for pre-activation processing.
    private final Multimap<String, Runnable> deactivateHooks = HashMultimap.create();
    private final Cache<ApplicationId, CountDownLatch> pendingOperations =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(DEFAULT_OPERATION_TIMEOUT_MILLIS * 2, TimeUnit.MILLISECONDS)
                    .build();

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
    public void registerDeactivateHook(ApplicationId appId, Runnable hook) {
        checkPermission(APP_READ);
        checkNotNull(appId, APP_ID_NULL);
        checkNotNull(hook, "Hook cannot be null");
        deactivateHooks.put(appId.name(), hook);
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
        updateStoreAndWaitForNotificationHandling(appId, store::remove);
    }

    @Override
    public void activate(ApplicationId appId) {
        checkNotNull(appId, APP_ID_NULL);
        if (!SecurityUtil.isAppSecured(appId)) {
            return;
        }
        updateStoreAndWaitForNotificationHandling(appId, store::activate);
    }

    @Override
    public void deactivate(ApplicationId appId) {
        checkNotNull(appId, APP_ID_NULL);
        updateStoreAndWaitForNotificationHandling(appId, store::deactivate);
    }

    @Override
    public void setPermissions(ApplicationId appId, Set<Permission> permissions) {
        checkNotNull(appId, APP_ID_NULL);
        checkNotNull(permissions, "Permissions cannot be null");
        store.setPermissions(appId, permissions);
    }

    @Override
    public InputStream getApplicationArchive(ApplicationId appId) {
        checkNotNull(appId, APP_ID_NULL);
        return store.getApplicationArchive(appId);
    }

    private void updateStoreAndWaitForNotificationHandling(ApplicationId appId,
                                                           Consumer<ApplicationId> storeUpdateTask) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            pendingOperations.put(appId, latch);
            storeUpdateTask.accept(appId);
        } catch (Exception e) {
            pendingOperations.invalidate(appId);
            latch.countDown();
            log.warn("Failed to update store for {}", appId.name(), e);
        }
        Uninterruptibles.awaitUninterruptibly(latch, DEFAULT_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    private class InternalStoreDelegate implements ApplicationStoreDelegate {
        @Override
        public void notify(ApplicationEvent event) {
            ApplicationEvent.Type type = event.type();
            Application app = event.subject();
            CountDownLatch latch = pendingOperations.getIfPresent(app.id());
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
            } finally {
                if (latch != null) {
                    latch.countDown();
                    pendingOperations.invalidate(app.id());
                }
            }
        }
    }

    // The following methods are fully synchronized to guard against remote vs.
    // locally induced feature service interactions.

    // Installs all feature repositories required by the specified app.
    private synchronized boolean installAppArtifacts(Application app) throws Exception {
        if (app.featuresRepo().isPresent() &&
                featuresService.getRepository(app.featuresRepo().get()) == null) {
            featuresService.addRepository(app.featuresRepo().get());
            return true;
        }
        return false;
    }

    // Uninstalls all the feature repositories required by the specified app.
    private synchronized boolean uninstallAppArtifacts(Application app) throws Exception {
        if (app.featuresRepo().isPresent() &&
                featuresService.getRepository(app.featuresRepo().get()) != null) {
            featuresService.removeRepository(app.featuresRepo().get());
            return true;
        }
        return false;
    }

    // Installs all features that define the specified app.
    private synchronized boolean installAppFeatures(Application app) throws Exception {
        boolean changed = false;
        for (String name : app.features()) {
            Feature feature = featuresService.getFeature(name);

            // If we see an attempt at activation of a non-existent feature
            // attempt to install the app artifacts first and then retry.
            // This can be triggered by a race condition between different ONOS
            // instances "installing" the apps from disk at their own pace.
            // Perhaps there is a more elegant solution to be explored in the
            // future.
            if (feature == null) {
                installAppArtifacts(app);
                feature = featuresService.getFeature(name);
            }

            if (feature != null && !featuresService.isInstalled(feature)) {
                featuresService.installFeature(name);
                changed = true;
            } else if (feature == null) {
                log.warn("Feature {} not found", name);
            } else if (log.isDebugEnabled()) {
                log.debug("Feature already installed for {}", app.id());
            }
        }
        return changed;
    }

    // Uninstalls all features that define the specified app.
    private synchronized boolean uninstallAppFeatures(Application app) throws Exception {
        boolean changed = false;
        deactivateHooks.removeAll(app.id().name()).forEach(hook -> invokeHook(hook, app.id()));
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

    // Invokes the specified function, if not null.
    @java.lang.SuppressWarnings("squid:S1217")
    // We really do mean to call run()
    private void invokeHook(Runnable hook, ApplicationId appId) {
        if (hook != null) {
            try {
                hook.run();
            } catch (Exception e) {
                log.warn("Deactivate hook for application {} encountered an error",
                         appId.name(), e);
            }
        }
    }

    @Override
    public Set<Application> getRegisteredApplications() {
        return ImmutableSet.of();
    }

}