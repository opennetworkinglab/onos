/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.provider.bgpcep.flow.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of BGP-PCEP flow provider.
 */
@Component(immediate = true)
public class BgpcepFlowRuleProvider extends AbstractProvider
        implements FlowRuleProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BgpController bgpController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PcepClientController pcepController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    private FlowRuleProviderService providerService;

    /**
     * Creates a BgpFlow host provider.
     */
    public BgpcepFlowRuleProvider() {
        super(new ProviderId("l3", "org.onosproject.provider.bgpcep"));
    }

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            applyRule(flowRule);
        }
    }

    private void applyRule(FlowRule flowRule) {
        flowRule.selector().criteria()
                .forEach(c -> {
                    // If Criterion type is MPLS_LABEL, push labels through PCEP client
                        if (c.type() == Criterion.Type.MPLS_LABEL) {
                            PcepClient pcc;
                            /** PCC client session is based on LSR ID, get the LSR ID for a specific device to
                            push the flows */

                            //TODO: commented code has dependency with other patch
                     /*     Set<TeRouterId> lrsIds = resourceService.getAvailableResourceValues(Resources
                                    .discrete(flowRule.deviceId()).id(), TeRouterId.class);

                            lrsIds.forEach(lsrId ->
                            {
                                if (pcepController.getClient(PccId.pccId(lsrId)) != null) {
                                    pcc = pcepController.getClient(PccId.pccId(lsrId));
                                }
                            });*/
                            // TODO: Build message and send the PCEP label message via PCEP client
                        } else {
                            // TODO: Get the BGP peer based on deviceId and send the message
                        }
                    });
    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            removeRule(flowRule);
        }
    }

    private void removeRule(FlowRule flowRule) {
        flowRule.selector().criteria()
        .forEach(c -> {
            // If Criterion type is MPLS_LABEL, remove the specified flow rules
                if (c.type() == Criterion.Type.MPLS_LABEL) {
                    PcepClient pcc;
                    /** PCC client session is based on LSR ID, get the LSR ID for a specific device to
                    push the flows */

                    //TODO: commented code has dependency with other patch
             /*     Set<TeRouterId> lrsIds = resourceService.getAvailableResourceValues(Resources
                            .discrete(flowRule.deviceId()).id(), TeRouterId.class);

                    lrsIds.forEach(lsrId ->
                    {
                        if (pcepController.getClient(PccId.pccId(lsrId)) != null) {
                            pcc = pcepController.getClient(PccId.pccId(lsrId));
                        }
                    });*/
                    // TODO: Build message and send the PCEP label message via PCEP client
                } else {
                    // TODO: Get the BGP peer based on deviceId and send the message
                }
            });
    }

    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        // TODO
        removeFlowRule(flowRules);
    }

    @Override
    public void executeBatch(FlowRuleBatchOperation batch) {
    //TODO
    }
}
