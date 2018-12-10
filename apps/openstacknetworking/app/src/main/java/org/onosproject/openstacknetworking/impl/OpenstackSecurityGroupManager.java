/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupEvent;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupListener;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupStore;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupStoreDelegate;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroup;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing OpenStack security
 * groups.
 */
@Component(
    immediate = true,
    service = { OpenstackSecurityGroupAdminService.class, OpenstackSecurityGroupService.class }
)
public class OpenstackSecurityGroupManager
        extends ListenerRegistry<OpenstackSecurityGroupEvent, OpenstackSecurityGroupListener>
        implements OpenstackSecurityGroupAdminService, OpenstackSecurityGroupService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_SG = "OpenStack security group %s %s";
    private static final String MSG_SG_RULE = "OpenStack security group rule %s %s";

    private static final String MSG_CREATED = "created";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_SG =
                                "OpenStack security group cannot be null";
    private static final String ERR_NULL_SG_ID =
                                "OpenStack security group ID cannot be null";
    private static final String ERR_NULL_SG_RULE =
                                "OpenStack security group rule cannot be null";
    private static final String ERR_NULL_SG_RULE_ID =
                                "OpenStack security group rule ID cannot be null";
    private static final String ERR_NOT_FOUND = "not found";
    private static final String ERR_DUPLICATE = "already exist";

    private boolean useSecurityGroup = false;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackSecurityGroupStore osSecurityGroupStore;

    private final OpenstackSecurityGroupStoreDelegate
                            delegate = new InternalSecurityGroupStoreDelegate();

    @Activate
    protected void activate() {
        coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);
        osSecurityGroupStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osSecurityGroupStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createSecurityGroup(SecurityGroup sg) {
        checkNotNull(sg, ERR_NULL_SG);
        checkArgument(!Strings.isNullOrEmpty(sg.getId()), ERR_NULL_SG_ID);

        osSecurityGroupStore.createSecurityGroup(sg);
        log.info(String.format(MSG_SG, sg.getId(), MSG_CREATED));
    }

    @Override
    public void updateSecurityGroup(SecurityGroup sg) {
        checkNotNull(sg, ERR_NULL_SG);
        checkArgument(!Strings.isNullOrEmpty(sg.getId()), ERR_NULL_SG_ID);

        osSecurityGroupStore.updateSecurityGroup(sg);
    }

    @Override
    public void removeSecurityGroup(String sgId) {
        checkArgument(!Strings.isNullOrEmpty(sgId), ERR_NULL_SG_ID);

        osSecurityGroupStore.removeSecurityGroup(sgId);
        log.info(String.format(MSG_SG, sgId, MSG_REMOVED));
    }

    @Override
    public void createSecurityGroupRule(SecurityGroupRule sgRule) {
        checkNotNull(sgRule, ERR_NULL_SG_RULE);
        checkArgument(!Strings.isNullOrEmpty(sgRule.getId()), ERR_NULL_SG_RULE_ID);
        checkArgument(!Strings.isNullOrEmpty(sgRule.getSecurityGroupId()), ERR_NULL_SG_ID);

        synchronized (this) {
            SecurityGroup sg = securityGroup(sgRule.getSecurityGroupId());
            if (sg == null) {
                final String error = String.format(MSG_SG,
                        sgRule.getSecurityGroupId(), ERR_NOT_FOUND);
                throw new IllegalStateException(error);
            }
            if (sg.getRules().stream().anyMatch(rule ->
                                Objects.equals(rule.getId(), sgRule.getId()))) {
                final String error = String.format(MSG_SG_RULE,
                        sgRule.getSecurityGroupId(), ERR_DUPLICATE);
                throw new IllegalStateException(error);
            }

            // FIXME we cannot add element to extend list
            List updatedSgRules = sg.getRules();
            updatedSgRules.add(sgRule);
            SecurityGroup updatedSg = NeutronSecurityGroup.builder().from(sg).build();
            osSecurityGroupStore.updateSecurityGroup(updatedSg);
        }

        log.info(String.format(MSG_SG_RULE, sgRule.getId(), MSG_CREATED));
    }

    @Override
    public void removeSecurityGroupRule(String sgRuleId) {
        checkArgument(!Strings.isNullOrEmpty(sgRuleId), ERR_NULL_SG_RULE_ID);

        synchronized (this) {
            SecurityGroupRule sgRule = securityGroupRule(sgRuleId);
            if (sgRule == null) {
                final String error = String.format(MSG_SG_RULE, sgRuleId, ERR_NOT_FOUND);
                throw new IllegalStateException(error);
            }

            SecurityGroup sg = securityGroup(sgRule.getSecurityGroupId());
            if (sg == null) {
                final String error = String.format(MSG_SG,
                                        sgRule.getSecurityGroupId(), ERR_NOT_FOUND);
                throw new IllegalStateException(error);
            }

            if (sg.getRules().stream().noneMatch(rule ->
                                Objects.equals(rule.getId(), sgRule.getId()))) {
                final String error = String.format(MSG_SG_RULE,
                        sgRule.getSecurityGroupId(), ERR_NOT_FOUND);
                throw new IllegalStateException(error);
            }

            // FIXME we cannot handle the element of extend list as a specific class object
            List updatedSgRules = sg.getRules();
            updatedSgRules.removeIf(r -> ((SecurityGroupRule) r).getId().equals(sgRuleId));
            SecurityGroup updatedSg = NeutronSecurityGroup.builder().from(sg).build();
            osSecurityGroupStore.updateSecurityGroup(updatedSg);
        }

        log.info(String.format(MSG_SG_RULE, sgRuleId, MSG_REMOVED));
    }

    @Override
    public Set<SecurityGroup> securityGroups() {
        return osSecurityGroupStore.securityGroups();
    }

    @Override
    public SecurityGroup securityGroup(String sgId) {
        checkArgument(!Strings.isNullOrEmpty(sgId), ERR_NULL_SG_ID);
        return osSecurityGroupStore.securityGroup(sgId);
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
    public void clear() {
        osSecurityGroupStore.clear();
    }

    private SecurityGroupRule securityGroupRule(String sgRuleId) {
        return osSecurityGroupStore.securityGroups().stream()
                .flatMap(sg -> sg.getRules().stream())
                .filter(sgRule -> Objects.equals(sgRule.getId(), sgRuleId))
                .findFirst().orElse(null);
    }

    private class InternalSecurityGroupStoreDelegate
                                implements OpenstackSecurityGroupStoreDelegate {

        @Override
        public void notify(OpenstackSecurityGroupEvent event) {
            if (event != null) {
                log.trace("send openstack security group event {}", event);
                process(event);
            }
        }
    }
}
