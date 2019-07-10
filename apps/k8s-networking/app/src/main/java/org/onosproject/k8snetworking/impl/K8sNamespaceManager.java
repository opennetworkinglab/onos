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
import io.fabric8.kubernetes.api.model.Namespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snetworking.api.K8sNamespaceAdminService;
import org.onosproject.k8snetworking.api.K8sNamespaceEvent;
import org.onosproject.k8snetworking.api.K8sNamespaceListener;
import org.onosproject.k8snetworking.api.K8sNamespaceService;
import org.onosproject.k8snetworking.api.K8sNamespaceStore;
import org.onosproject.k8snetworking.api.K8sNamespaceStoreDelegate;
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
 * Provides implementation of administering and interfacing kubernetes namespace.
 */
@Component(
        immediate = true,
        service = {K8sNamespaceAdminService.class, K8sNamespaceService.class}
)
public class K8sNamespaceManager
        extends ListenerRegistry<K8sNamespaceEvent, K8sNamespaceListener>
        implements K8sNamespaceAdminService, K8sNamespaceService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_NAMESPACE  = "Kubernetes namespace %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String
            ERR_NULL_NAMESPACE = "Kubernetes namespace cannot be null";
    private static final String
            ERR_NULL_NAMESPACE_UID  = "Kubernetes namespace UID cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNamespaceStore k8sNamespaceStore;

    private final K8sNamespaceStoreDelegate delegate =
            new InternalNamespaceStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        k8sNamespaceStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNamespaceStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createNamespace(Namespace namespace) {
        checkNotNull(namespace, ERR_NULL_NAMESPACE);
        checkArgument(!Strings.isNullOrEmpty(namespace.getMetadata().getUid()),
                ERR_NULL_NAMESPACE_UID);

        k8sNamespaceStore.createNamespace(namespace);

        log.info(String.format(MSG_NAMESPACE,
                namespace.getMetadata().getName(), MSG_CREATED));
    }

    @Override
    public void updateNamespace(Namespace namespace) {
        checkNotNull(namespace, ERR_NULL_NAMESPACE);
        checkArgument(!Strings.isNullOrEmpty(namespace.getMetadata().getUid()),
                ERR_NULL_NAMESPACE_UID);

        k8sNamespaceStore.updateNamespace(namespace);

        log.info(String.format(MSG_NAMESPACE,
                namespace.getMetadata().getName(), MSG_UPDATED));
    }

    @Override
    public void removeNamespace(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_NAMESPACE_UID);

        synchronized (this) {
            if (isNamespaceInUse(uid)) {
                final String error = String.format(MSG_NAMESPACE, uid, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
        }

        Namespace namespace = k8sNamespaceStore.removeNamespace(uid);

        if (namespace != null) {
            log.info(String.format(MSG_NAMESPACE,
                    namespace.getMetadata().getName(), MSG_REMOVED));
        }
    }

    @Override
    public void clear() {
        k8sNamespaceStore.clear();
    }

    @Override
    public Namespace namespace(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_NAMESPACE_UID);
        return k8sNamespaceStore.namespace(uid);
    }

    @Override
    public Set<Namespace> namespaces() {
        return ImmutableSet.copyOf(k8sNamespaceStore.namespaces());
    }

    private boolean isNamespaceInUse(String uid) {
        return false;
    }

    private class InternalNamespaceStorageDelegate
            implements K8sNamespaceStoreDelegate {

        @Override
        public void notify(K8sNamespaceEvent event) {
            if (event != null) {
                log.trace("send kubernetes namespace event {}", event);
                process(event);
            }
        }
    }
}
