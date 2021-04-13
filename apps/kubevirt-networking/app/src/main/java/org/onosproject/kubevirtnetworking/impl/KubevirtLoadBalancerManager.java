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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerListener;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerService;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerStore;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerStoreDelegate;
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
 * Provides implementation of administering and interfacing kubevirt load balancer.
 */
@Component(
        immediate = true,
        service = {KubevirtLoadBalancerAdminService.class, KubevirtLoadBalancerService.class}
)
public class KubevirtLoadBalancerManager
        extends ListenerRegistry<KubevirtLoadBalancerEvent, KubevirtLoadBalancerListener>
        implements KubevirtLoadBalancerAdminService, KubevirtLoadBalancerService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_LOAD_BALANCER = "Kubevirt load balancer %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_LOAD_BALANCER = "Kubevirt load balancer cannot be null";
    private static final String ERR_NULL_LOAD_BALANCER_NAME = "Kubevirt load balancer name cannot be null";
    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtLoadBalancerStore kubevirtLoadBalancerStore;

    private final InternalKubevirtLoadBalancerStorageDelegate delegate =
            new InternalKubevirtLoadBalancerStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);

        kubevirtLoadBalancerStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        kubevirtLoadBalancerStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createLoadBalancer(KubevirtLoadBalancer lb) {
        checkNotNull(lb, ERR_NULL_LOAD_BALANCER);
        checkArgument(!Strings.isNullOrEmpty(lb.name()), ERR_NULL_LOAD_BALANCER_NAME);

        kubevirtLoadBalancerStore.createLoadBalancer(lb);
        log.info(String.format(MSG_LOAD_BALANCER, lb.name(), MSG_CREATED));
    }

    @Override
    public void updateLoadBalancer(KubevirtLoadBalancer lb) {
        checkNotNull(lb, ERR_NULL_LOAD_BALANCER);
        checkArgument(!Strings.isNullOrEmpty(lb.name()), ERR_NULL_LOAD_BALANCER_NAME);

        kubevirtLoadBalancerStore.updateLoadBalancer(lb);
        log.info(String.format(MSG_LOAD_BALANCER, lb.name(), MSG_UPDATED));
    }

    @Override
    public void removeLoadBalancer(String name) {
        checkArgument(name != null, ERR_NULL_LOAD_BALANCER_NAME);
        synchronized (this) {
            if (isLoadBalancerInUse(name)) {
                final String error = String.format(MSG_LOAD_BALANCER, name, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
        }

        KubevirtLoadBalancer lb = kubevirtLoadBalancerStore.removeLoadBalancer(name);
        if (lb != null) {
            log.info(String.format(MSG_LOAD_BALANCER, lb.name(), MSG_REMOVED));
        }
    }

    @Override
    public void clear() {
        kubevirtLoadBalancerStore.clear();
    }

    @Override
    public KubevirtLoadBalancer loadBalancer(String name) {
        checkArgument(name != null, ERR_NULL_LOAD_BALANCER_NAME);
        return kubevirtLoadBalancerStore.loadBalancer(name);
    }

    @Override
    public Set<KubevirtLoadBalancer> loadBalancers() {
        return ImmutableSet.copyOf(kubevirtLoadBalancerStore.loadBalancers());
    }

    private boolean isLoadBalancerInUse(String name) {
        return false;
    }

    private class InternalKubevirtLoadBalancerStorageDelegate
            implements KubevirtLoadBalancerStoreDelegate {

        @Override
        public void notify(KubevirtLoadBalancerEvent event) {
            log.trace("send kubevirt load balancer event {}", event);
            process(event);
        }
    }
}
