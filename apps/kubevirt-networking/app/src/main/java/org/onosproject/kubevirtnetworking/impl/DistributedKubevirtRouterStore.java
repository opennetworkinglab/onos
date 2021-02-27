/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtPeerRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterStore;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_ASSOCIATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_DISASSOCIATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_FLOATING_IP_UPDATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent.Type.KUBEVIRT_ROUTER_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubevirt router store using consistent map.
 */
@Component(immediate = true, service = KubevirtRouterStore.class)
public class DistributedKubevirtRouterStore
        extends AbstractStore<KubevirtRouterEvent, KubevirtRouterStoreDelegate>
        implements KubevirtRouterStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.kubevirtnetwork";

    private static final KryoNamespace
            SERIALIZER_KUBEVIRT_ROUTER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KubevirtRouter.class)
            .register(DefaultKubevirtRouter.class)
            .register(KubevirtPeerRouter.class)
            .register(KubevirtFloatingIp.class)
            .register(DefaultKubevirtFloatingIp.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, KubevirtRouter> routerMapListener =
            new KubevirtRouterMapListener();
    private final MapEventListener<String, KubevirtFloatingIp> fipMapListener =
            new KubevirtFloatingIpMapListener();

    private ConsistentMap<String, KubevirtRouter> routerStore;
    private ConsistentMap<String, KubevirtFloatingIp> fipStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        routerStore = storageService.<String, KubevirtRouter>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_KUBEVIRT_ROUTER))
                .withName("kubevirt-routerstore")
                .withApplicationId(appId)
                .build();
        fipStore = storageService.<String, KubevirtFloatingIp>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_KUBEVIRT_ROUTER))
                .withName("kubevirt-fipstore")
                .withApplicationId(appId)
                .build();
        routerStore.addListener(routerMapListener);
        fipStore.addListener(fipMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        routerStore.removeListener(routerMapListener);
        fipStore.removeListener(fipMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createRouter(KubevirtRouter router) {
        routerStore.compute(router.name(), (name, existing) -> {
            final String error = router.name() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return router;
        });
    }

    @Override
    public void updateRouter(KubevirtRouter router) {
        routerStore.compute(router.name(), (name, existing) -> {
            final String error = router.name() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return router;
        });
    }

    @Override
    public KubevirtRouter removeRouter(String name) {
        Versioned<KubevirtRouter> router = routerStore.remove(name);
        if (router == null) {
            final String error = name + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return router.value();
    }

    @Override
    public KubevirtRouter router(String name) {
        return routerStore.asJavaMap().get(name);
    }

    @Override
    public Set<KubevirtRouter> routers() {
        return ImmutableSet.copyOf(routerStore.asJavaMap().values());
    }

    @Override
    public void createFloatingIp(KubevirtFloatingIp floatingIp) {
        fipStore.compute(floatingIp.id(), (id, existing) -> {
            final String error = floatingIp.id() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return floatingIp;
        });
    }

    @Override
    public void updateFloatingIp(KubevirtFloatingIp floatingIp) {
        fipStore.compute(floatingIp.id(), (id, existing) -> {
            final String error = floatingIp.id() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return floatingIp;
        });
    }

    @Override
    public KubevirtFloatingIp removeFloatingIp(String id) {
        Versioned<KubevirtFloatingIp> floatingIp = fipStore.remove(id);
        if (floatingIp == null) {
            final String error = id + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return floatingIp.value();
    }

    @Override
    public KubevirtFloatingIp floatingIp(String id) {
        return fipStore.asJavaMap().get(id);
    }

    @Override
    public Set<KubevirtFloatingIp> floatingIps() {
        return ImmutableSet.copyOf(fipStore.asJavaMap().values());
    }


    @Override
    public void clear() {
        routerStore.clear();
        fipStore.clear();
    }

    private class KubevirtRouterMapListener implements MapEventListener<String, KubevirtRouter> {

        @Override
        public void event(MapEvent<String, KubevirtRouter> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("Kubevirt router created");
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtRouterEvent(
                                    KUBEVIRT_ROUTER_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubevirt router updated");
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtRouterEvent(
                                    KUBEVIRT_ROUTER_UPDATED, event.newValue().value())));
                    break;
                case REMOVE:
                    log.debug("Kubevirt router removed");
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtRouterEvent(
                                    KUBEVIRT_ROUTER_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class KubevirtFloatingIpMapListener implements MapEventListener<String, KubevirtFloatingIp> {

        @Override
        public void event(MapEvent<String, KubevirtFloatingIp> event) {
            switch (event.type()) {
                case INSERT:
                    eventExecutor.execute(() -> processFloatingIpMapInsertion(event));
                    break;
                case UPDATE:
                    eventExecutor.execute(()  -> processFloatingIpMapUpdate(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processFloatingIpMapRemoval(event));
                    break;
                default:
                    break;
            }
        }

        private void processFloatingIpMapInsertion(MapEvent<String, KubevirtFloatingIp> event) {
            log.debug("Kubevirt floating IP created");
            KubevirtRouter router = Strings.isNullOrEmpty(
                    event.newValue().value().routerName()) ?
                    null :
                    router(event.newValue().value().routerName());
            notifyDelegate(new KubevirtRouterEvent(
                    KUBEVIRT_FLOATING_IP_CREATED,
                    router,
                    event.newValue().value()));
        }

        private void processFloatingIpMapUpdate(MapEvent<String, KubevirtFloatingIp> event) {
            log.debug("Kubevirt floating IP updated");
            KubevirtRouter router = Strings.isNullOrEmpty(
                    event.newValue().value().routerName()) ?
                    null :
                    router(event.newValue().value().routerName());
            notifyDelegate(new KubevirtRouterEvent(
                    KUBEVIRT_FLOATING_IP_UPDATED,
                    router,
                    event.newValue().value()));
            processFloatingIpUpdate(event, router);
        }

        private void processFloatingIpMapRemoval(MapEvent<String, KubevirtFloatingIp> event) {
            log.debug("Kubevirt floating IP removed");
            KubevirtRouter router = Strings.isNullOrEmpty(
                    event.oldValue().value().routerName()) ?
                    null :
                    router(event.oldValue().value().routerName());
            notifyDelegate(new KubevirtRouterEvent(
                    KUBEVIRT_FLOATING_IP_REMOVED,
                    router,
                    event.oldValue().value()));
        }

        private void processFloatingIpUpdate(MapEvent<String, KubevirtFloatingIp> event,
                                             KubevirtRouter router) {
            String oldPodName = event.oldValue().value().podName();
            String newPodName = event.newValue().value().podName();

            if (Strings.isNullOrEmpty(oldPodName) && !Strings.isNullOrEmpty(newPodName)) {
                notifyDelegate(new KubevirtRouterEvent(
                        KUBEVIRT_FLOATING_IP_ASSOCIATED,
                        router,
                        event.newValue().value(), newPodName));
            }

            if (!Strings.isNullOrEmpty(oldPodName) && Strings.isNullOrEmpty(newPodName)) {
                notifyDelegate(new KubevirtRouterEvent(
                        KUBEVIRT_FLOATING_IP_DISASSOCIATED,
                        router,
                        event.newValue().value(), oldPodName));
            }
        }
    }
}
