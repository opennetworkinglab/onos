/*
 * Copyright 2016-present Open Networking Laboratory
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
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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
import org.onosproject.app.ApplicationIdStore;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplication;
import org.onosproject.security.Permission;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Topic;
import org.onosproject.store.service.Versioned;
import org.onosproject.store.service.DistributedPrimitive.Status;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Multimaps.newSetMultimap;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;
import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.randomDelay;
import static org.onosproject.app.ApplicationEvent.Type.*;
import static org.onosproject.store.app.DistributedApplicationStore.InternalState.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of applications in a distributed data store providing
 * stronger consistency guarantees.
 */
@Component(immediate = true)
@Service
public class DistributedApplicationStore extends ApplicationArchive
        implements ApplicationStore {

    // FIXME: eliminate the need for this
    private static final int FIXME_ACTIVATION_DELAY = 500;

    private final Logger log = getLogger(getClass());

    private static final MessageSubject APP_BITS_REQUEST = new MessageSubject("app-bits-request");

    private static final int MAX_LOAD_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 2_000;

    private static final int FETCH_TIMEOUT_MS = 10_000;

    private static final int APP_LOAD_DELAY_MS = 500;

    private static List<String> pendingApps = Lists.newArrayList();

    public enum InternalState {
        INSTALLED, ACTIVATED, DEACTIVATED
    }

    private ScheduledExecutorService executor;
    private ExecutorService messageHandlingExecutor;

    private ConsistentMap<ApplicationId, InternalApplicationHolder> apps;
    private Topic<Application> appActivationTopic;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationIdStore idStore;

    private final InternalAppsListener appsListener = new InternalAppsListener();
    private final Consumer<Application> appActivator = new AppActivator();

    private Consumer<Status> statusChangeListener;

    // Multimap to track which apps are required by others apps
    // app -> { required-by, ... }
    // Apps explicitly activated will be required by the CORE app
    private final Multimap<ApplicationId, ApplicationId> requiredBy =
            synchronizedSetMultimap(newSetMultimap(Maps.newHashMap(), Sets::newHashSet));

    private ApplicationId coreAppId;

    @Activate
    public void activate() {
        messageHandlingExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("onos/store/app", "message-handler", log));
        clusterCommunicator.addSubscriber(APP_BITS_REQUEST,
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

        apps = storageService.<ApplicationId, InternalApplicationHolder>consistentMapBuilder()
                .withName("onos-apps")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(KryoNamespaces.API,
                                                 InternalApplicationHolder.class,
                                                 InternalState.class))
                .build();

        appActivationTopic = storageService.getTopic("onos-apps-activation-topic",
                                                     Serializer.using(KryoNamespaces.API));

        appActivationTopic.subscribe(appActivator, messageHandlingExecutor);

        executor = newSingleThreadScheduledExecutor(groupedThreads("onos/app", "store", log));
        statusChangeListener = status -> {
            if (status == Status.ACTIVE) {
                executor.execute(this::bootstrapExistingApplications);
            }
        };
        apps.addListener(appsListener, messageHandlingExecutor);
        apps.addStatusChangeListener(statusChangeListener);
        coreAppId = getId(CoreService.CORE_APP_NAME);
        log.info("Started");
    }

    /**
     * Processes existing applications from the distributed map. This is done to
     * account for events that this instance may be have missed due to a staggered start.
     */
    private void bootstrapExistingApplications() {
        apps.asJavaMap().forEach((appId, holder) -> setupApplicationAndNotify(appId, holder.app(), holder.state()));

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
        pendingApps.add(appName);

        for (int i = 0; i < MAX_LOAD_RETRIES; i++) {
            try {
                // Directly return if app already exists
                ApplicationId appId = getId(appName);
                if (appId != null) {
                    Application application = getApplication(appId);
                    if (application != null) {
                        pendingApps.remove(appName);
                        return application;
                    }
                }

                ApplicationDescription appDesc = getApplicationDescription(appName);

                Optional<String> loop = appDesc.requiredApps().stream()
                        .filter(app -> pendingApps.contains(app)).findAny();
                if (loop.isPresent()) {
                    log.error("Circular app dependency detected: {} -> {}", pendingApps, loop.get());
                    pendingApps.remove(appName);
                    return null;
                }

                boolean success = appDesc.requiredApps().stream()
                        .noneMatch(requiredApp -> loadFromDisk(requiredApp) == null);
                pendingApps.remove(appName);

                return success ? create(appDesc, false) : null;

            } catch (Exception e) {
                log.warn("Unable to load application {} from disk; retrying", appName);
                randomDelay(RETRY_DELAY_MS); //FIXME: This is a deliberate hack; fix in Falcon
            }
        }
        pendingApps.remove(appName);
        return null;
    }

    @Deactivate
    public void deactivate() {
        clusterCommunicator.removeSubscriber(APP_BITS_REQUEST);
        apps.removeStatusChangeListener(statusChangeListener);
        apps.removeListener(appsListener);
        appActivationTopic.unsubscribe(appActivator);
        messageHandlingExecutor.shutdown();
        executor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void setDelegate(ApplicationStoreDelegate delegate) {
        super.setDelegate(delegate);
        executor.execute(this::bootstrapExistingApplications);
        executor.schedule((Runnable) this::loadFromDisk, APP_LOAD_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public Set<Application> getApplications() {
        return ImmutableSet.copyOf(apps.values()
                                       .stream()
                                       .map(Versioned::value)
                                       .map(InternalApplicationHolder::app)
                                       .collect(Collectors.toSet()));
    }

    @Override
    public ApplicationId getId(String name) {
        return idStore.getAppId(name);
    }

    @Override
    public Application getApplication(ApplicationId appId) {
        InternalApplicationHolder appHolder = Versioned.valueOrNull(apps.get(appId));
        return appHolder != null ? appHolder.app() : null;
    }

    @Override
    public ApplicationState getState(ApplicationId appId) {
        InternalApplicationHolder appHolder = Versioned.valueOrNull(apps.get(appId));
        InternalState state = appHolder != null ? appHolder.state() : null;
        return state == null ? null : state == ACTIVATED ? ApplicationState.ACTIVE : ApplicationState.INSTALLED;
    }

    @Override
    public Application create(InputStream appDescStream) {
        ApplicationDescription appDesc = saveApplication(appDescStream);
        if (hasPrerequisites(appDesc)) {
            return create(appDesc, true);
        }
        // Purge bits off disk if we don't have prerequisites to allow app to be
        // reinstalled later
        purgeApplication(appDesc.name());
        throw new ApplicationException("Missing dependencies for app " + appDesc.name());
    }

    private boolean hasPrerequisites(ApplicationDescription app) {
        for (String required : app.requiredApps()) {
            ApplicationId id = getId(required);
            if (id == null || getApplication(id) == null) {
                log.error("{} required for {} not available", required, app.name());
                return false;
            }
        }
        return true;
    }

    private Application create(ApplicationDescription appDesc, boolean updateTime) {
        Application app = registerApp(appDesc);
        if (updateTime) {
            updateTime(app.id().name());
        }
        InternalApplicationHolder previousApp =
                Versioned.valueOrNull(apps.putIfAbsent(app.id(), new InternalApplicationHolder(app, INSTALLED, null)));
        return previousApp != null ? previousApp.app() : app;
    }

    @Override
    public void remove(ApplicationId appId) {
        uninstallDependentApps(appId);
        apps.remove(appId);
    }

    // Uninstalls all apps that depend on the given app.
    private void uninstallDependentApps(ApplicationId appId) {
        getApplications().stream()
                .filter(a -> a.requiredApps().contains(appId.name()))
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
        Versioned<InternalApplicationHolder> vAppHolder = apps.get(appId);
        if (vAppHolder != null) {
            if (updateTime) {
                updateTime(appId.name());
            }
            activateRequiredApps(vAppHolder.value().app());

            apps.computeIf(appId, v -> v != null && v.state() != ACTIVATED,
                    (k, v) -> new InternalApplicationHolder(
                            v.app(), ACTIVATED, v.permissions()));
            appActivationTopic.publish(vAppHolder.value().app());
        }
    }

    // Activates all apps required by this application.
    private void activateRequiredApps(Application app) {
        app.requiredApps().stream().map(this::getId).forEach(id -> activate(id, app.id()));
    }

    @Override
    public void deactivate(ApplicationId appId) {
        deactivateDependentApps(appId);
        deactivate(appId, coreAppId);
    }

    private void deactivate(ApplicationId appId, ApplicationId forAppId) {
        requiredBy.remove(appId, forAppId);
        if (requiredBy.get(appId).isEmpty()) {
            AtomicBoolean stateChanged = new AtomicBoolean(false);
            apps.computeIf(appId,
                v -> v != null && v.state() != DEACTIVATED,
                (k, v) -> {
                    stateChanged.set(true);
                    return new InternalApplicationHolder(v.app(), DEACTIVATED, v.permissions());
                });
            if (stateChanged.get()) {
                updateTime(appId.name());
                deactivateRequiredApps(appId);
            }
        }
    }

    // Deactivates all apps that require this application.
    private void deactivateDependentApps(ApplicationId appId) {
        apps.values()
            .stream()
            .map(Versioned::value)
            .filter(a -> a.state() == ACTIVATED)
            .filter(a -> a.app().requiredApps().contains(appId.name()))
            .forEach(a -> deactivate(a.app().id()));
    }

    // Deactivates all apps required by this application.
    private void deactivateRequiredApps(ApplicationId appId) {
        getApplication(appId).requiredApps()
                             .stream()
                             .map(this::getId)
                             .map(apps::get)
                             .map(Versioned::value)
                             .filter(a -> a.state() == ACTIVATED)
                             .forEach(a -> deactivate(a.app().id(), appId));
    }

    @Override
    public Set<Permission> getPermissions(ApplicationId appId) {
        InternalApplicationHolder app = Versioned.valueOrNull(apps.get(appId));
        return app != null ? ImmutableSet.copyOf(app.permissions()) : ImmutableSet.of();
    }

    @Override
    public void setPermissions(ApplicationId appId, Set<Permission> permissions) {
        AtomicBoolean permissionsChanged = new AtomicBoolean(false);
        Versioned<InternalApplicationHolder> appHolder = apps.computeIf(appId,
            v -> v != null && !Sets.symmetricDifference(v.permissions(), permissions).isEmpty(),
            (k, v) -> {
                permissionsChanged.set(true);
                return new InternalApplicationHolder(v.app(), v.state(), ImmutableSet.copyOf(permissions));
            });
        if (permissionsChanged.get()) {
            notifyDelegate(new ApplicationEvent(APP_PERMISSIONS_CHANGED, appHolder.value().app()));
        }
    }

    private class AppActivator implements Consumer<Application> {
        @Override
        public void accept(Application app) {
            String appName = app.id().name();
            installAppIfNeeded(app);
            setActive(appName);
            notifyDelegate(new ApplicationEvent(APP_ACTIVATED, app));
        }
    }

    /**
     * Listener to application state distributed map changes.
     */
    private final class InternalAppsListener
            implements MapEventListener<ApplicationId, InternalApplicationHolder> {
        @Override
        public void event(MapEvent<ApplicationId, InternalApplicationHolder> event) {
            if (delegate == null) {
                return;
            }

            ApplicationId appId = event.key();
            InternalApplicationHolder newApp = event.newValue() == null ? null : event.newValue().value();
            InternalApplicationHolder oldApp = event.oldValue() == null ? null : event.oldValue().value();
            if (event.type() == MapEvent.Type.INSERT || event.type() == MapEvent.Type.UPDATE) {
                if (event.type() == MapEvent.Type.UPDATE && newApp.state() == oldApp.state()) {
                    return;
                }
                setupApplicationAndNotify(appId, newApp.app(), newApp.state());
            } else if (event.type() == MapEvent.Type.REMOVE) {
                notifyDelegate(new ApplicationEvent(APP_UNINSTALLED, oldApp.app()));
                purgeApplication(appId.name());
            }
        }
    }

    private void setupApplicationAndNotify(ApplicationId appId, Application app, InternalState state) {
        // ACTIVATED state is handled separately in NextAppToActivateValueListener
        if (state == INSTALLED) {
            fetchBitsIfNeeded(app);
            notifyDelegate(new ApplicationEvent(APP_INSTALLED, app));
        } else if (state == DEACTIVATED) {
            clearActive(appId.name());
            notifyDelegate(new ApplicationEvent(APP_DEACTIVATED, app));
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
            notifyDelegate(new ApplicationEvent(APP_INSTALLED, app));
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
     * Produces a registered application from the supplied description.
     */
    private Application registerApp(ApplicationDescription appDesc) {
        ApplicationId appId = idStore.registerApplication(appDesc.name());
        return new DefaultApplication(appId,
                                      appDesc.version(),
                                      appDesc.title(),
                                      appDesc.description(),
                                      appDesc.origin(),
                                      appDesc.category(),
                                      appDesc.url(),
                                      appDesc.readme(),
                                      appDesc.icon(),
                                      appDesc.role(),
                                      appDesc.permissions(),
                                      appDesc.featuresRepo(),
                                      appDesc.features(),
                                      appDesc.requiredApps());
    }

    /**
     * Internal class for holding app information.
     */
    private static final class InternalApplicationHolder {
        private final Application app;
        private final InternalState state;
        private final Set<Permission> permissions;

        @SuppressWarnings("unused")
        private InternalApplicationHolder() {
            app = null;
            state = null;
            permissions = null;
        }

        private InternalApplicationHolder(Application app, InternalState state, Set<Permission> permissions) {
            this.app = Preconditions.checkNotNull(app);
            this.state = state;
            this.permissions = permissions == null ? null : ImmutableSet.copyOf(permissions);
        }

        public Application app() {
            return app;
        }

        public InternalState state() {
            return state;
        }

        public Set<Permission> permissions() {
            return permissions;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("app", app.id())
                    .add("state", state)
                    .toString();
        }
    }
}
