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
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyListener;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyService;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyStore;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyStoreDelegate;
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
 * Provides implementation of administering and interfacing kubernetes network policy.
 */
@Component(
        immediate = true,
        service = {K8sNetworkPolicyAdminService.class, K8sNetworkPolicyService.class }
)
public class K8sNetworkPolicyManager
        extends ListenerRegistry<K8sNetworkPolicyEvent, K8sNetworkPolicyListener>
        implements K8sNetworkPolicyAdminService, K8sNetworkPolicyService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_NETWORK_POLICY  = "Kubernetes network policy %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String
            ERR_NULL_NETWORK_POLICY = "Kubernetes network policy cannot be null";
    private static final String
            ERR_NULL_NETWORK_POLICY_UID  = "Kubernetes network policy UID cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkPolicyStore k8sNetworkPolicyStore;

    private final K8sNetworkPolicyStoreDelegate
            delegate = new InternalNetworkPolicyStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        k8sNetworkPolicyStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNetworkPolicyStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createNetworkPolicy(NetworkPolicy networkPolicy) {
        checkNotNull(networkPolicy, ERR_NULL_NETWORK_POLICY);
        checkArgument(!Strings.isNullOrEmpty(networkPolicy.getMetadata().getUid()),
                ERR_NULL_NETWORK_POLICY_UID);

        k8sNetworkPolicyStore.createNetworkPolicy(networkPolicy);

        log.info(String.format(MSG_NETWORK_POLICY,
                networkPolicy.getMetadata().getName(), MSG_CREATED));
    }

    @Override
    public void updateNetworkPolicy(NetworkPolicy networkPolicy) {
        checkNotNull(networkPolicy, ERR_NULL_NETWORK_POLICY);
        checkArgument(!Strings.isNullOrEmpty(networkPolicy.getMetadata().getUid()),
                ERR_NULL_NETWORK_POLICY_UID);

        k8sNetworkPolicyStore.updateNetworkPolicy(networkPolicy);

        log.info(String.format(MSG_NETWORK_POLICY,
                networkPolicy.getMetadata().getName(), MSG_UPDATED));
    }

    @Override
    public void removeNetworkPolicy(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_NETWORK_POLICY_UID);

        synchronized (this) {
            if (isNetworkPolicyInUse(uid)) {
                final String error = String.format(MSG_NETWORK_POLICY, uid, ERR_IN_USE);
                throw new IllegalStateException(error);
            }

            NetworkPolicy networkPolicy = k8sNetworkPolicyStore.removeNetworkPolicy(uid);

            if (networkPolicy != null) {
                log.info(String.format(MSG_NETWORK_POLICY,
                        networkPolicy.getMetadata().getName(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        k8sNetworkPolicyStore.clear();
    }

    @Override
    public NetworkPolicy networkPolicy(String uid) {
        checkArgument(!Strings.isNullOrEmpty(uid), ERR_NULL_NETWORK_POLICY_UID);
        return k8sNetworkPolicyStore.networkPolicy(uid);
    }

    @Override
    public Set<NetworkPolicy> networkPolicies() {
        return ImmutableSet.copyOf(k8sNetworkPolicyStore.networkPolicies());
    }

    private boolean isNetworkPolicyInUse(String uid) {
        return false;
    }

    private class InternalNetworkPolicyStorageDelegate
            implements K8sNetworkPolicyStoreDelegate {

        @Override
        public void notify(K8sNetworkPolicyEvent event) {
            if (event != null) {
                log.trace("send kubernetes network policy event {}", event);
                process(event);
            }
        }
    }
}
