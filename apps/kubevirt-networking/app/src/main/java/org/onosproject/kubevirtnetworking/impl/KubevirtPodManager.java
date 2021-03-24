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
import io.fabric8.kubernetes.api.model.Pod;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.kubevirtnetworking.api.KubevirtPodAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtPodEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPodListener;
import org.onosproject.kubevirtnetworking.api.KubevirtPodService;
import org.onosproject.kubevirtnetworking.api.KubevirtPodStore;
import org.onosproject.kubevirtnetworking.api.KubevirtPodStoreDelegate;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing kubernetes pod.
 */
@Component(
        immediate = true,
        service = {KubevirtPodAdminService.class, KubevirtPodService.class}
)
public class KubevirtPodManager
        extends ListenerRegistry<KubevirtPodEvent, KubevirtPodListener>
        implements KubevirtPodAdminService, KubevirtPodService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_POD  = "Kubernetes pod %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_POD  = "Kubevirt pod cannot be null";
    private static final String ERR_NULL_POD_UID  = "Kubevirt pod UID cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPodStore kubevirtPodStore;

    private final KubevirtPodStoreDelegate delegate = new InternalPodStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);

        kubevirtPodStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        kubevirtPodStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createPod(Pod pod) {
        checkNotNull(pod, ERR_NULL_POD);
        checkArgument(!Strings.isNullOrEmpty(pod.getMetadata().getUid()),
                ERR_NULL_POD_UID);

        kubevirtPodStore.createPod(pod);

        log.debug(String.format(MSG_POD, pod.getMetadata().getName(), MSG_CREATED));
    }

    @Override
    public void updatePod(Pod pod) {
        checkNotNull(pod, ERR_NULL_POD);
        checkArgument(!Strings.isNullOrEmpty(pod.getMetadata().getUid()),
                ERR_NULL_POD_UID);

        kubevirtPodStore.updatePod(pod);

        log.debug(String.format(MSG_POD, pod.getMetadata().getName(), MSG_UPDATED));
    }

    @Override
    public void removePod(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_POD_UID);

        synchronized (this) {
            if (isPodInUse(uid)) {
                final String error = String.format(MSG_POD, uid, ERR_IN_USE);
                throw new IllegalStateException(error);
            }

            Pod pod = kubevirtPodStore.removePod(uid);

            if (pod != null) {
                log.debug(String.format(MSG_POD,
                        pod.getMetadata().getName(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        kubevirtPodStore.clear();
    }

    @Override
    public Pod pod(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_POD_UID);
        return kubevirtPodStore.pod(uid);
    }

    @Override
    public Set<Pod> pods() {
        return ImmutableSet.copyOf(kubevirtPodStore.pods());
    }

    private boolean isPodInUse(String uid) {
        return false;
    }

    private class InternalPodStorageDelegate implements KubevirtPodStoreDelegate {

        @Override
        public void notify(KubevirtPodEvent event) {
            if (event != null) {
                log.trace("send kubernetes pod event {}", event);
                process(event);
            }
        }
    }
}
