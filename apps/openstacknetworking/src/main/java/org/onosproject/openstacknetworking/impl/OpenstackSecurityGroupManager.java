/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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
import org.slf4j.Logger;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfaceing Openstack security
 * groups.
 *
 */
@Service
@Component(immediate = true)
public class OpenstackSecurityGroupManager
        extends ListenerRegistry<OpenstackSecurityGroupEvent, OpenstackSecurityGroupListener>
        implements OpenstackSecurityGroupAdminService, OpenstackSecurityGroupService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_SG = "OpenStack security group %s %s";
    private static final String MSG_SG_RULE = "OpenStack security group %s %s";


    private static final String MSG_CREATED = "created";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_SG = "OpenStack security group cannot be null";
    private static final String ERR_NULL_SG_ID = "OpenStack security group ID cannot be null";
    private static final String ERR_NULL_SG_RULE = "OpenStack security group rule cannot be null";
    private static final String ERR_NULL_SG_RULE_ID = "OpenStack security group rule ID cannot be null";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackSecurityGroupStore osSecurityGroupStore;

    private final OpenstackSecurityGroupStoreDelegate delegate = new InternalSecurityGroupStoreDelegate();

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
    public void removeSecurityGroup(String sgId) {
        checkNotNull(sgId, ERR_NULL_SG_ID);

        osSecurityGroupStore.removeSecurityGroup(sgId);
        log.info(String.format(MSG_SG, sgId, MSG_REMOVED));
    }

    @Override
    public void createSecurityGroupRule(SecurityGroupRule sgRule) {
        checkNotNull(sgRule, ERR_NULL_SG_RULE);
        checkArgument(!Strings.isNullOrEmpty(sgRule.getId()), ERR_NULL_SG_RULE_ID);

        synchronized (osSecurityGroupStore) {
            SecurityGroup sg = securityGroup(sgRule.getSecurityGroupId());
            List sgRules = sg.getRules();
            sgRules.add(sgRule);
            SecurityGroup newSg = new NeutronSecurityGroup.SecurityGroupConcreteBuilder().from(sg).build();
            SecurityGroup oldSg = osSecurityGroupStore.updateSecurityGroup(sgRule.getSecurityGroupId(), newSg);
            if (oldSg == null) {
                log.warn("Failed to add the security group rule {} to security group", sgRule.getId());
            }

            osSecurityGroupStore.createSecurityGroupRule(sgRule);
            log.info(String.format(MSG_SG_RULE, sgRule.getId(), MSG_CREATED));
        }
    }

    @Override
    public void removeSecurityGroupRule(String sgRuleId) {
        checkNotNull(sgRuleId, ERR_NULL_SG_RULE_ID);

        osSecurityGroupStore.removeSecurityGroupRule(sgRuleId);
        log.info(String.format(MSG_SG_RULE, sgRuleId, MSG_REMOVED));
    }

    @Override
    public SecurityGroup securityGroup(String sgId) {
        checkArgument(!Strings.isNullOrEmpty(sgId), ERR_NULL_SG_ID);
        return osSecurityGroupStore.securityGroup(sgId);
    }

    @Override
    public SecurityGroupRule securityGroupRule(String sgRuleId) {
        checkArgument(!Strings.isNullOrEmpty(sgRuleId), ERR_NULL_SG_RULE_ID);
        return osSecurityGroupStore.securityGroupRule(sgRuleId);
    }

    private class InternalSecurityGroupStoreDelegate implements OpenstackSecurityGroupStoreDelegate {

        @Override
        public void notify(OpenstackSecurityGroupEvent event) {
            if (event != null) {
                log.trace("send openstack security group event {}", event);
                process(event);
            }
        }
    }
}
