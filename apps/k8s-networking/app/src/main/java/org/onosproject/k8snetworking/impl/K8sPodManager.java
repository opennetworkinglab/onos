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
import io.fabric8.kubernetes.api.model.Pod;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snetworking.api.K8sPodAdminService;
import org.onosproject.k8snetworking.api.K8sPodEvent;
import org.onosproject.k8snetworking.api.K8sPodListener;
import org.onosproject.k8snetworking.api.K8sPodService;
import org.onosproject.k8snetworking.api.K8sPodStore;
import org.onosproject.k8snetworking.api.K8sPodStoreDelegate;
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
 * Provides implementation of administering and interfacing kubernetes pod.
 */
@Component(
        immediate = true,
        service = {K8sPodAdminService.class, K8sPodService.class}
)
public class K8sPodManager
        extends ListenerRegistry<K8sPodEvent, K8sPodListener>
        implements K8sPodAdminService, K8sPodService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_POD  = "Kubernetes pod %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_POD  = "Kubernetes pod cannot be null";
    private static final String ERR_NULL_POD_UID  = "Kubernetes pod UID cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sPodStore k8sPodStore;

    private final K8sPodStoreDelegate delegate = new InternalPodStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        k8sPodStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sPodStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createPod(Pod pod) {
        checkNotNull(pod, ERR_NULL_POD);
        checkArgument(!Strings.isNullOrEmpty(pod.getMetadata().getUid()),
                ERR_NULL_POD_UID);

        k8sPodStore.createPod(pod);

        log.info(String.format(MSG_POD, pod.getMetadata().getName(), MSG_CREATED));
    }

    @Override
    public void updatePod(Pod pod) {
        checkNotNull(pod, ERR_NULL_POD);
        checkArgument(!Strings.isNullOrEmpty(pod.getMetadata().getUid()),
                ERR_NULL_POD_UID);

        k8sPodStore.updatePod(pod);

        log.info(String.format(MSG_POD, pod.getMetadata().getName(), MSG_UPDATED));
    }

    @Override
    public void removePod(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_POD_UID);

        synchronized (this) {
            if (isPodInUse(uid)) {
                final String error = String.format(MSG_POD, uid, ERR_IN_USE);
                throw new IllegalStateException(error);
            }

            Pod pod = k8sPodStore.removePod(uid);

            if (pod != null) {
                log.info(String.format(MSG_POD,
                        pod.getMetadata().getName(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        k8sPodStore.clear();
    }

    @Override
    public Pod pod(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_POD_UID);
        return k8sPodStore.pod(uid);
    }

    @Override
    public Set<Pod> pods() {
        return ImmutableSet.copyOf(k8sPodStore.pods());
    }

    private boolean isPodInUse(String uid) {
        return false;
    }

    private class InternalPodStorageDelegate implements K8sPodStoreDelegate {

        @Override
        public void notify(K8sPodEvent event) {
            if (event != null) {
                log.trace("send kubernetes pod event {}", event);
                process(event);
            }
        }
    }
}
