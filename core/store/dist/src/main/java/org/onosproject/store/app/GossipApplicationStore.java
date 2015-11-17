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
package org.onosproject.store.app;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.app.ApplicationDescription;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationException;
import org.onosproject.app.ApplicationState;
import org.onosproject.app.ApplicationStore;
import org.onosproject.app.ApplicationStoreDelegate;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.common.app.ApplicationArchive;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationIdStore;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplication;
import org.onosproject.security.Permission;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import static com.google.common.collect.Multimaps.newSetMultimap;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;
import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.randomDelay;
import static org.onosproject.app.ApplicationEvent.Type.*;
import static org.onosproject.store.app.GossipApplicationStore.InternalState.*;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of applications in a distributed data store that uses
 * optimistic replication and gossip based anti-entropy techniques.
 */
@Component(immediate = true)
@Service
public class GossipApplicationStore extends ApplicationArchive
        implements ApplicationStore {

    private final Logger log = getLogger(getClass());

    private static final MessageSubject APP_BITS_REQUEST = new MessageSubject("app-bits-request");

    private static final int MAX_LOAD_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 2_000;

    private static final int FETCH_TIMEOUT_MS = 10_000;

    public enum InternalState {
        INSTALLED, ACTIVATED, DEACTIVATED
    }

    private ScheduledExecutorService executor;
    private ExecutorService messageHandlingExecutor;

    private EventuallyConsistentMap<ApplicationId, Application> apps;
    private EventuallyConsistentMap<Application, InternalState> states;
    private EventuallyConsistentMap<Application, Set<Permission>> permissions;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationIdStore idStore;

    // Multimap to track which apps are required by others apps
    // app -> { required-by, ... }
    // Apps explicitly activated will be required by the CORE app
    private final Multimap<ApplicationId, ApplicationId> requiredBy =
            synchronizedSetMultimap(newSetMultimap(Maps.newHashMap(), Sets::newHashSet));

    private ApplicationId coreAppId;

    @Activate
    public void activate() {
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class)
                .register(InternalState.class);

        executor = Executors.newSingleThreadScheduledExecutor(groupedThreads("onos/app", "store"));

        messageHandlingExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("onos/store/app", "message-handler"));

        clusterCommunicator.<String, byte[]>addSubscriber(APP_BITS_REQUEST,
                                                          bytes -> new String(bytes, Charsets.UTF_8),
                                                          name -> {
                                                              try {
                                                                  return toByteArray(getApplicationInputStream(name));
                                                              } catch (IOException e) {
                                                                  throw new StorageException(e);
                                                              }
                                                          },
                                                          Function.identity(),
                                                          messageHandlingExecutor);

        // FIXME: Consider consolidating into a single map.

        apps = storageService.<ApplicationId, Application>eventuallyConsistentMapBuilder()
                .withName("apps")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        states = storageService.<Application, InternalState>eventuallyConsistentMapBuilder()
                .withName("app-states")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        states.addListener(new InternalAppStatesListener());

        permissions = storageService.<Application, Set<Permission>>eventuallyConsistentMapBuilder()
                .withName("app-permissions")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        coreAppId = getId(CoreService.CORE_APP_NAME);
        log.info("Started");
    }

    /**
     * Loads the application inventory from the disk and activates apps if
     * they are marked to be active.
     */
    private void loadFromDisk() {
        getApplicationNames().forEach(appName -> {
            Application app = loadFromDisk(appName);
            if (app != null && isActive(app.id().name())) {
                activate(app.id(), false);
                // TODO Load app permissions
            }
        });
    }

    private Application loadFromDisk(String appName) {
        for (int i = 0; i < MAX_LOAD_RETRIES; i++) {
            try {
                // Directly return if app already exists
                ApplicationId appId = getId(appName);
                if (appId != null) {
                    return getApplication(appId);
                }

                ApplicationDescription appDesc = getApplicationDescription(appName);
                boolean success = appDesc.requiredApps().stream()
                        .noneMatch(requiredApp -> loadFromDisk(requiredApp) == null);
                return success ? create(appDesc, false) : null;
            } catch (Exception e) {
                log.warn("Unable to load application {} from disk; retrying", appName);
                randomDelay(RETRY_DELAY_MS); //FIXME: This is a deliberate hack; fix in Falcon
            }
        }
        return null;
    }

    @Deactivate
    public void deactivate() {
        clusterCommunicator.removeSubscriber(APP_BITS_REQUEST);
        messageHandlingExecutor.shutdown();
        executor.shutdown();
        apps.destroy();
        states.destroy();
        permissions.destroy();
        log.info("Stopped");
    }

    @Override
    public void setDelegate(ApplicationStoreDelegate delegate) {
        super.setDelegate(delegate);
        loadFromDisk();
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
        Application app = apps.get(appId);
        InternalState s = app == null ? null : states.get(app);
        return s == null ? null : s == ACTIVATED ?
                ApplicationState.ACTIVE : ApplicationState.INSTALLED;
    }

    @Override
    public Application create(InputStream appDescStream) {
        ApplicationDescription appDesc = saveApplication(appDescStream);
        if (hasPrerequisites(appDesc)) {
            return create(appDesc, true);
        }
        throw new ApplicationException("Missing dependencies for app " + appDesc.name());
    }

    private boolean hasPrerequisites(ApplicationDescription app) {
        return !app.requiredApps().stream().map(n -> getId(n))
                .anyMatch(id -> id == null || getApplication(id) == null);
    }

    private Application create(ApplicationDescription appDesc, boolean updateTime) {
        Application app = registerApp(appDesc);
        if (updateTime) {
            updateTime(app.id().name());
        }
        apps.put(app.id(), app);
        states.put(app, INSTALLED);
        return app;
    }

    @Override
    public void remove(ApplicationId appId) {
        Application app = apps.get(appId);
        if (app != null) {
            uninstallDependentApps(app);
            apps.remove(appId);
            states.remove(app);
            permissions.remove(app);
        }
    }

    // Uninstalls all apps that depend on the given app.
    private void uninstallDependentApps(Application app) {
        getApplications().stream()
                .filter(a -> a.requiredApps().contains(app.id().name()))
                .forEach(a -> remove(a.id()));
    }

    @Override
    public void activate(ApplicationId appId) {
        activate(appId, coreAppId);
    }

    private void activate(ApplicationId appId, ApplicationId forAppId) {
        requiredBy.put(appId, forAppId);
        activate(appId, true);
    }


    private void activate(ApplicationId appId, boolean updateTime) {
        Application app = apps.get(appId);
        if (app != null) {
            if (updateTime) {
                updateTime(appId.name());
            }
            activateRequiredApps(app);
            states.put(app, ACTIVATED);
        }
    }

    // Activates all apps required by this application.
    private void activateRequiredApps(Application app) {
        app.requiredApps().stream().map(this::getId).forEach(id -> activate(id, app.id()));
    }

    @Override
    public void deactivate(ApplicationId appId) {
        deactivateDependentApps(getApplication(appId));
        deactivate(appId, coreAppId);
    }

    private void deactivate(ApplicationId appId, ApplicationId forAppId) {
        requiredBy.remove(appId, forAppId);
        if (requiredBy.get(appId).isEmpty()) {
            Application app = apps.get(appId);
            if (app != null) {
                updateTime(appId.name());
                states.put(app, DEACTIVATED);
                deactivateRequiredApps(app);
            }
        }
    }

    // Deactivates all apps that require this application.
    private void deactivateDependentApps(Application app) {
        getApplications().stream()
                .filter(a -> states.get(a) == ACTIVATED)
                .filter(a -> a.requiredApps().contains(app.id().name()))
                .forEach(a -> deactivate(a.id()));
    }

    // Deactivates all apps required by this application.
    private void deactivateRequiredApps(Application app) {
        app.requiredApps().stream().map(this::getId).map(this::getApplication)
                .filter(a -> states.get(a) == ACTIVATED)
                .forEach(a -> deactivate(a.id(), app.id()));
    }

    @Override
    public Set<Permission> getPermissions(ApplicationId appId) {
        Application app = apps.get(appId);
        return app != null ? permissions.get(app) : null;
    }

    @Override
    public void setPermissions(ApplicationId appId, Set<Permission> permissions) {
        Application app = getApplication(appId);
        if (app != null) {
            this.permissions.put(app, permissions);
            delegate.notify(new ApplicationEvent(APP_PERMISSIONS_CHANGED, app));
        }
    }

    /**
     * Listener to application state distributed map changes.
     */
    private final class InternalAppStatesListener
            implements EventuallyConsistentMapListener<Application, InternalState> {
        @Override
        public void event(EventuallyConsistentMapEvent<Application, InternalState> event) {
            // If we do not have a delegate, refuse to process any events entirely.
            // This is to allow the anti-entropy to kick in and process the events
            // perhaps a bit later, but with opportunity to notify delegate.
            if (delegate == null) {
                return;
            }

            Application app = event.key();
            InternalState state = event.value();

            if (event.type() == PUT) {
                if (state == INSTALLED) {
                    fetchBitsIfNeeded(app);
                    delegate.notify(new ApplicationEvent(APP_INSTALLED, app));

                } else if (state == ACTIVATED) {
                    installAppIfNeeded(app);
                    setActive(app.id().name());
                    delegate.notify(new ApplicationEvent(APP_ACTIVATED, app));

                } else if (state == DEACTIVATED) {
                    clearActive(app.id().name());
                    delegate.notify(new ApplicationEvent(APP_DEACTIVATED, app));
                }
            } else if (event.type() == REMOVE) {
                delegate.notify(new ApplicationEvent(APP_UNINSTALLED, app));
                purgeApplication(app.id().name());
            }
        }
    }

    /**
     * Determines if the application bits are available locally.
     */
    private boolean appBitsAvailable(Application app) {
        try {
            ApplicationDescription appDesc = getApplicationDescription(app.id().name());
            return appDesc.version().equals(app.version());
        } catch (ApplicationException e) {
            return false;
        }
    }

    /**
     * Fetches the bits from the cluster peers if necessary.
     */
    private void fetchBitsIfNeeded(Application app) {
        if (!appBitsAvailable(app)) {
            fetchBits(app);
        }
    }

    /**
     * Installs the application if necessary from the application peers.
     */
    private void installAppIfNeeded(Application app) {
        if (!appBitsAvailable(app)) {
            fetchBits(app);
            delegate.notify(new ApplicationEvent(APP_INSTALLED, app));
        }
    }

    /**
     * Fetches the bits from the cluster peers.
     */
    private void fetchBits(Application app) {
        ControllerNode localNode = clusterService.getLocalNode();
        CountDownLatch latch = new CountDownLatch(1);

        // FIXME: send message with name & version to make sure we don't get served old bits

        log.info("Downloading bits for application {}", app.id().name());
        for (ControllerNode node : clusterService.getNodes()) {
            if (latch.getCount() == 0) {
                break;
            }
            if (node.equals(localNode)) {
                continue;
            }
            clusterCommunicator.sendAndReceive(app.id().name(),
                                               APP_BITS_REQUEST,
                                               s -> s.getBytes(Charsets.UTF_8),
                                               Function.identity(),
                                               node.id())
                    .whenCompleteAsync((bits, error) -> {
                        if (error == null && latch.getCount() > 0) {
                            saveApplication(new ByteArrayInputStream(bits));
                            log.info("Downloaded bits for application {} from node {}",
                                     app.id().name(), node.id());
                            latch.countDown();
                        } else if (error != null) {
                            log.warn("Unable to fetch bits for application {} from node {}",
                                     app.id().name(), node.id());
                        }
                    }, executor);
        }

        try {
            if (!latch.await(FETCH_TIMEOUT_MS, MILLISECONDS)) {
                log.warn("Unable to fetch bits for application {}", app.id().name());
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while fetching bits for application {}", app.id().name());
        }
    }

    /**
     * Prunes applications which are not in the map, but are on disk.
     */
    private void pruneUninstalledApps() {
        for (String name : getApplicationNames()) {
            if (getApplication(getId(name)) == null) {
                Application app = registerApp(getApplicationDescription(name));
                delegate.notify(new ApplicationEvent(APP_UNINSTALLED, app));
                purgeApplication(app.id().name());
            }
        }
    }

    /**
     * Produces a registered application from the supplied description.
     */
    private Application registerApp(ApplicationDescription appDesc) {
        ApplicationId appId = idStore.registerApplication(appDesc.name());
        return new DefaultApplication(appId, appDesc.version(), appDesc.description(),
                                      appDesc.origin(), appDesc.role(), appDesc.permissions(),
                                      appDesc.featuresRepo(), appDesc.features(),
                                      appDesc.requiredApps());
    }
}
