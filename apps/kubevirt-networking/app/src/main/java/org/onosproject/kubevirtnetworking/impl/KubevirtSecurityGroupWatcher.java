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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.AbstractWatcher;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigListener;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.mastership.MastershipService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.k8sClient;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.waitFor;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt security group watcher used for feeding kubevirt security group information.
 */
@Component(immediate = true)
public class KubevirtSecurityGroupWatcher extends AbstractWatcher {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtSecurityGroupAdminService adminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtApiConfigService configService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final InternalSecurityGroupWatcher
            sgWatcher = new InternalSecurityGroupWatcher();
    private final InternalSecurityGroupRuleWatcher
            sgrWatcher = new InternalSecurityGroupRuleWatcher();
    private final InternalKubevirtApiConfigListener
            configListener = new InternalKubevirtApiConfigListener();

    CustomResourceDefinitionContext securityGroupCrdCxt = new CustomResourceDefinitionContext
            .Builder()
            .withGroup("kubevirt.io")
            .withScope("Cluster")
            .withVersion("v1")
            .withPlural("securitygroups")
            .build();

    CustomResourceDefinitionContext securityGroupRuleCrdCxt = new CustomResourceDefinitionContext
            .Builder()
            .withGroup("kubevirt.io")
            .withScope("Cluster")
            .withVersion("v1")
            .withPlural("securitygrouprules")
            .build();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        configService.addListener(configListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void instantiateSgWatcher() {
        KubernetesClient client = k8sClient(configService);

        if (client != null) {
            try {
                client.customResource(securityGroupCrdCxt).watch(sgWatcher);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void instantiateSgrWatcher() {
        KubernetesClient client = k8sClient(configService);

        if (client != null) {
            try {
                client.customResource(securityGroupRuleCrdCxt).watch(sgrWatcher);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private KubevirtSecurityGroup parseSecurityGroup(String resource) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(resource);
            ObjectNode spec = (ObjectNode) json.get("spec");
            return codec(KubevirtSecurityGroup.class).decode(spec, this);
        } catch (IOException e) {
            log.error("Failed to parse kubevirt security group object");
        }

        return null;
    }

    private KubevirtSecurityGroupRule parseSecurityGroupRule(String resource) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(resource);
            ObjectNode spec = (ObjectNode) json.get("spec");
            return codec(KubevirtSecurityGroupRule.class).decode(spec, this);
        } catch (IOException e) {
            log.error("Failed to parse kubevirt security group rule object");
        }

        return null;
    }

    private class InternalKubevirtApiConfigListener implements KubevirtApiConfigListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtApiConfigEvent event) {

            switch (event.type()) {
                case KUBEVIRT_API_CONFIG_UPDATED:
                    eventExecutor.execute(this::processConfigUpdate);
                    break;
                case KUBEVIRT_API_CONFIG_CREATED:
                case KUBEVIRT_API_CONFIG_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processConfigUpdate() {
            if (!isRelevantHelper()) {
                return;
            }

            instantiateSgWatcher();
            instantiateSgrWatcher();
        }
    }

    private class InternalSecurityGroupWatcher implements Watcher<String> {

        @Override
        public void eventReceived(Action action, String resource) {
            switch (action) {
                case ADDED:
                    eventExecutor.execute(() -> processAddition(resource));
                    break;
                case MODIFIED:
                    eventExecutor.execute(() -> processModification(resource));
                    break;
                case DELETED:
                    eventExecutor.execute(() -> processDeletion(resource));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        @Override
        public void onClose(WatcherException e) {
            // due to the bugs in fabric8, the watcher might be closed,
            // we will re-instantiate the watcher in this case
            // FIXME: https://github.com/fabric8io/kubernetes-client/issues/2135
            log.warn("Security Group watcher OnClose, re-instantiate the watcher...");

            instantiateSgWatcher();
        }

        private void processAddition(String resource) {
            if (!isMaster()) {
                return;
            }

            KubevirtSecurityGroup sg = parseSecurityGroup(resource);

            if (sg != null) {
                log.trace("Process Security Group {} creating event from API server.", sg.name());

                if (adminService.securityGroup(sg.id()) == null) {
                    adminService.createSecurityGroup(sg);
                }
            }
        }

        private void processModification(String resource) {
            if (!isMaster()) {
                return;
            }

            KubevirtSecurityGroup sg = parseSecurityGroup(resource);

            if (sg != null) {
                log.trace("Process Security Group {} updating event from API server.", sg.name());

                // since Security Group CRD does not contains any rules information,
                // we need to manually add all rules from original to the updated one
                KubevirtSecurityGroup orig = adminService.securityGroup(sg.id());
                if (orig != null) {
                    KubevirtSecurityGroup updated = sg.updateRules(orig.rules());
                    adminService.updateSecurityGroup(updated);
                }
            }
        }

        private void processDeletion(String resource) {
            if (!isMaster()) {
                return;
            }

            KubevirtSecurityGroup sg = parseSecurityGroup(resource);

            if (sg != null) {
                log.trace("Process Security Group {} removal event from API server.", sg.name());

                adminService.removeSecurityGroup(sg.id());
            }
        }
    }

    private class InternalSecurityGroupRuleWatcher implements Watcher<String> {

        @Override
        public void eventReceived(Action action, String resource) {
            switch (action) {
                case ADDED:
                    eventExecutor.execute(() -> processAddition(resource));
                    break;
                case MODIFIED:
                    eventExecutor.execute(() -> processModification(resource));
                    break;
                case DELETED:
                    eventExecutor.execute(() -> processDeletion(resource));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        @Override
        public void onClose(WatcherException e) {
            // due to the bugs in fabric8, the watcher might be closed,
            // we will re-instantiate the watcher in this case
            // FIXME: https://github.com/fabric8io/kubernetes-client/issues/2135
            log.warn("Security Group Rule watcher OnClose, re-instantiate the watcher...");

            instantiateSgrWatcher();
        }

        private void processAddition(String resource) {
            if (!isMaster()) {
                return;
            }

            KubevirtSecurityGroupRule sgr = parseSecurityGroupRule(resource);

            if (sgr != null) {
                log.trace("Process Security Group Rule {} creating event from API server.", sgr.id());

                KubevirtSecurityGroup sg = adminService.securityGroup(sgr.securityGroupId());
                if (sg == null) {
                    log.warn("Security Group {} is not found, we wait 5 seconds until " +
                             "the group to be installed.", sgr.securityGroupId());
                    waitFor(5);
                }

                if (adminService.securityGroupRule(sgr.id()) == null) {
                    adminService.createSecurityGroupRule(sgr);
                }
            }
        }

        private void processModification(String resource) {
            if (!isMaster()) {
                return;
            }

            // we do not handle the update case, as we assume the security group rule
            // object is immutable
        }

        private void processDeletion(String resource) {
            if (!isMaster()) {
                return;
            }

            KubevirtSecurityGroupRule sgr = parseSecurityGroupRule(resource);

            if (sgr != null) {
                log.trace("Process Security Group Rule {} removal event from API server.", sgr.id());

                adminService.removeSecurityGroupRule(sgr.id());
            }
        }
    }

    private boolean isMaster() {
        return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
    }
}
