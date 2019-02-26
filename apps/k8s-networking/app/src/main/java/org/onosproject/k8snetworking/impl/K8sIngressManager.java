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
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snetworking.api.K8sIngressAdminService;
import org.onosproject.k8snetworking.api.K8sIngressEvent;
import org.onosproject.k8snetworking.api.K8sIngressListener;
import org.onosproject.k8snetworking.api.K8sIngressService;
import org.onosproject.k8snetworking.api.K8sIngressStore;
import org.onosproject.k8snetworking.api.K8sIngressStoreDelegate;
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
 * Provides implementation of administering and interfacing kubernetes ingress.
 */
@Component(
        immediate = true,
        service = {K8sIngressAdminService.class, K8sIngressService.class}
)
public class K8sIngressManager
        extends ListenerRegistry<K8sIngressEvent, K8sIngressListener>
        implements K8sIngressAdminService, K8sIngressService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_INGRESS = "Kubernetes ingress %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_INGRESS  = "Kubernetes ingress cannot be null";
    private static final String ERR_NULL_INGRESS_UID  = "Kubernetes ingress UID cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sIngressStore k8sIngressStore;

    private final K8sIngressStoreDelegate delegate = new InternalIngressStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        k8sIngressStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sIngressStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createIngress(Ingress ingress) {
        checkNotNull(ingress, ERR_NULL_INGRESS);
        checkArgument(!Strings.isNullOrEmpty(ingress.getMetadata().getUid()),
                ERR_NULL_INGRESS_UID);

        k8sIngressStore.createIngress(ingress);

        log.info(String.format(MSG_INGRESS, ingress.getMetadata().getName(), MSG_CREATED));
    }

    @Override
    public void updateIngress(Ingress ingress) {
        checkNotNull(ingress, ERR_NULL_INGRESS);
        checkArgument(!Strings.isNullOrEmpty(ingress.getMetadata().getUid()),
                ERR_NULL_INGRESS_UID);

        k8sIngressStore.updateIngress(ingress);

        log.info(String.format(MSG_INGRESS, ingress.getMetadata().getName(), MSG_UPDATED));
    }

    @Override
    public void removeIngress(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_INGRESS_UID);

        synchronized (this) {
            if (isIngressInUse(uid)) {
                final String error = String.format(MSG_INGRESS, uid, ERR_IN_USE);
                throw new IllegalStateException(error);
            }

            Ingress ingress = k8sIngressStore.removeIngress(uid);

            if (ingress != null) {
                log.info(String.format(MSG_INGRESS,
                        ingress.getMetadata().getName(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        k8sIngressStore.clear();
    }

    @Override
    public Ingress ingress(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_INGRESS_UID);
        return k8sIngressStore.ingress(uid);
    }

    @Override
    public Set<Ingress> ingresses() {
        return ImmutableSet.copyOf(k8sIngressStore.ingresses());
    }

    private boolean isIngressInUse(String uid) {
        return false;
    }

    private class InternalIngressStorageDelegate implements K8sIngressStoreDelegate {

        @Override
        public void notify(K8sIngressEvent event) {
            if (event != null) {
                log.trace("send kubernetes ingress event {}", event);
                process(event);
            }
        }
    }
}
