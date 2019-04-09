/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.api.model.Endpoints;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snetworking.api.K8sEndpointsAdminService;
import org.onosproject.k8snetworking.api.K8sEndpointsEvent;
import org.onosproject.k8snetworking.api.K8sEndpointsListener;
import org.onosproject.k8snetworking.api.K8sEndpointsService;
import org.onosproject.k8snetworking.api.K8sEndpointsStore;
import org.onosproject.k8snetworking.api.K8sEndpointsStoreDelegate;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing kubernetes endpoints.
 */
@Component(
        immediate = true,
        service = {K8sEndpointsAdminService.class, K8sEndpointsService.class}
)
public class K8sEndpointsManager
        extends ListenerRegistry<K8sEndpointsEvent, K8sEndpointsListener>
        implements K8sEndpointsAdminService, K8sEndpointsService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_ENDPOINTS  = "Kubernetes endpoints %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_ENDPOINTS  = "Kubernetes endpoints cannot be null";
    private static final String ERR_NULL_ENDPOINTS_UID  = "Kubernetes endpoints UID cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sEndpointsStore k8sEndpointsStore;

    private final K8sEndpointsStoreDelegate
            delegate = new InternalEndpointsStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        k8sEndpointsStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sEndpointsStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createEndpoints(Endpoints endpoints) {
        checkNotNull(endpoints, ERR_NULL_ENDPOINTS);
        checkArgument(!Strings.isNullOrEmpty(endpoints.getMetadata().getUid()),
                ERR_NULL_ENDPOINTS_UID);

        k8sEndpointsStore.createEndpoints(endpoints);

        log.info(String.format(MSG_ENDPOINTS, endpoints.getMetadata().getName(), MSG_CREATED));
    }

    @Override
    public void updateEndpoints(Endpoints endpoints) {
        checkNotNull(endpoints, ERR_NULL_ENDPOINTS);
        checkArgument(!Strings.isNullOrEmpty(endpoints.getMetadata().getUid()),
                ERR_NULL_ENDPOINTS_UID);

        k8sEndpointsStore.updateEndpoints(endpoints);

        log.debug(String.format(MSG_ENDPOINTS, endpoints.getMetadata().getName(), MSG_UPDATED));
    }

    @Override
    public void removeEndpoints(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_ENDPOINTS_UID);

        synchronized (this) {
            if (isEndpointsInUse(uid)) {
                final String error = String.format(MSG_ENDPOINTS, uid, ERR_IN_USE);
                throw new IllegalStateException(error);
            }

            Endpoints endpoints = k8sEndpointsStore.removeEndpoints(uid);

            if (endpoints != null) {
                log.info(String.format(MSG_ENDPOINTS,
                        endpoints.getMetadata().getName(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        k8sEndpointsStore.clear();
    }

    @Override
    public Endpoints endpoints(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_ENDPOINTS_UID);
        return k8sEndpointsStore.endpoints(uid);
    }

    @Override
    public Set<Endpoints> endpointses() {
        return ImmutableSet.copyOf(k8sEndpointsStore.endpointses());
    }

    private boolean isEndpointsInUse(String uid) {
        return false;
    }

    private class InternalEndpointsStorageDelegate implements K8sEndpointsStoreDelegate {

        @Override
        public void notify(K8sEndpointsEvent event) {
            if (event != null) {
                log.trace("send kubernetes endpoints event {}", event);
                process(event);
            }
        }
    }
}
