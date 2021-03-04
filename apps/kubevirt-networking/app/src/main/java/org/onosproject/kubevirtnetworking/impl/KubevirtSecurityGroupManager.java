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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupListener;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupService;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupStore;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupStoreDelegate;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing kubevirt security groups.
 */
@Component(
        immediate = true,
        service = {KubevirtSecurityGroupAdminService.class, KubevirtSecurityGroupService.class }
)
public class KubevirtSecurityGroupManager
        extends ListenerRegistry<KubevirtSecurityGroupEvent, KubevirtSecurityGroupListener>
        implements KubevirtSecurityGroupAdminService, KubevirtSecurityGroupService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_SG = "Kubevirt security group %s %s";
    private static final String MSG_SG_RULE = "Kubevirt security group rule %s %s";

    private static final String MSG_CREATED = "created";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_SG =
            "Kubevirt security group cannot be null";
    private static final String ERR_NULL_SG_ID =
            "Kubevirt security group ID cannot be null";
    private static final String ERR_NULL_SG_RULE =
            "Kubevirt security group rule cannot be null";
    private static final String ERR_NULL_SG_RULE_ID =
            "Kubevirt security group rule ID cannot be null";
    private static final String ERR_NOT_FOUND = "not found";
    private static final String ERR_DUPLICATE = "already exist";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtSecurityGroupStore sgStore;

    private final KubevirtSecurityGroupStoreDelegate
            delegate = new InternalSecurityGroupStoreDelegate();

    private ApplicationId appId;
    private boolean useSecurityGroup = false;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);

        sgStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        sgStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createSecurityGroup(KubevirtSecurityGroup sg) {
        checkNotNull(sg, ERR_NULL_SG);
        checkArgument(!Strings.isNullOrEmpty(sg.id()), ERR_NULL_SG_ID);

        sgStore.createSecurityGroup(sg);
        log.info(String.format(MSG_SG, sg.id(), MSG_CREATED));
    }

    @Override
    public void updateSecurityGroup(KubevirtSecurityGroup sg) {
        checkNotNull(sg, ERR_NULL_SG);
        checkArgument(!Strings.isNullOrEmpty(sg.id()), ERR_NULL_SG_ID);

        sgStore.updateSecurityGroup(sg);
    }

    @Override
    public void removeSecurityGroup(String sgId) {
        checkArgument(!Strings.isNullOrEmpty(sgId), ERR_NULL_SG_ID);

        sgStore.removeSecurityGroup(sgId);
        log.info(String.format(MSG_SG, sgId, MSG_REMOVED));
    }

    @Override
    public void createSecurityGroupRule(KubevirtSecurityGroupRule sgRule) {
        checkNotNull(sgRule, ERR_NULL_SG_RULE);
        checkArgument(!Strings.isNullOrEmpty(sgRule.id()), ERR_NULL_SG_RULE_ID);
        checkArgument(!Strings.isNullOrEmpty(sgRule.securityGroupId()), ERR_NULL_SG_ID);

        synchronized (this) {
            KubevirtSecurityGroup sg = securityGroup(sgRule.securityGroupId());
            if (sg == null) {
                final String error = String.format(MSG_SG,
                        sgRule.securityGroupId(), ERR_NOT_FOUND);
                throw new IllegalStateException(error);
            }

            if (sg.rules().stream().anyMatch(rule -> Objects.equals(rule.id(), sgRule.id()))) {
                final String error = String.format(MSG_SG_RULE, sgRule.securityGroupId(), ERR_DUPLICATE);
                throw new IllegalStateException(error);
            }

            // FIXME we cannot add element to extend list
            Set<KubevirtSecurityGroupRule> updatedSgRules = new HashSet<>(sg.rules());
            updatedSgRules.add(sgRule);
            sgStore.updateSecurityGroup(sg.updateRules(updatedSgRules));
        }

        log.info(String.format(MSG_SG_RULE, sgRule.id(), MSG_CREATED));
    }

    @Override
    public void removeSecurityGroupRule(String sgRuleId) {
        checkArgument(!Strings.isNullOrEmpty(sgRuleId), ERR_NULL_SG_RULE_ID);

        synchronized (this) {
            KubevirtSecurityGroupRule sgRule = securityGroupRule(sgRuleId);
            if (sgRule == null) {
                final String error = String.format(MSG_SG_RULE, sgRuleId, ERR_NOT_FOUND);
                throw new IllegalStateException(error);
            }

            KubevirtSecurityGroup sg = securityGroup(sgRule.securityGroupId());
            if (sg == null) {
                final String error = String.format(MSG_SG,
                        sgRule.securityGroupId(), ERR_NOT_FOUND);
                throw new IllegalStateException(error);
            }

            if (sg.rules().stream().noneMatch(rule -> Objects.equals(rule.id(), sgRule.id()))) {
                final String error = String.format(MSG_SG_RULE,
                        sgRule.securityGroupId(), ERR_NOT_FOUND);
                throw new IllegalStateException(error);
            }

            Set<KubevirtSecurityGroupRule> updatedSgRules = new HashSet<>(sg.rules());
            updatedSgRules.removeIf(r -> r.id().equals(sgRuleId));
            sgStore.updateSecurityGroup(sg.updateRules(updatedSgRules));
        }

        log.info(String.format(MSG_SG_RULE, sgRuleId, MSG_REMOVED));
    }

    @Override
    public void clear() {
        sgStore.clear();
    }

    @Override
    public Set<KubevirtSecurityGroup> securityGroups() {
        return sgStore.securityGroups();
    }

    @Override
    public KubevirtSecurityGroup securityGroup(String sgId) {
        checkArgument(!Strings.isNullOrEmpty(sgId), ERR_NULL_SG_ID);
        return sgStore.securityGroup(sgId);
    }

    @Override
    public boolean isSecurityGroupEnabled() {
        return useSecurityGroup;
    }

    @Override
    public void setSecurityGroupEnabled(boolean option) {
        useSecurityGroup = option;
    }

    @Override
    public KubevirtSecurityGroupRule securityGroupRule(String sgRuleId) {
        return sgStore.securityGroups().stream()
                .flatMap(sg -> sg.rules().stream())
                .filter(sgRule -> Objects.equals(sgRule.id(), sgRuleId))
                .findAny().orElse(null);
    }

    private class InternalSecurityGroupStoreDelegate
            implements KubevirtSecurityGroupStoreDelegate {

        @Override
        public void notify(KubevirtSecurityGroupEvent event) {
            if (event != null) {
                log.trace("send kubevirt security group event {}", event);
                process(event);
            }
        }
    }
}
