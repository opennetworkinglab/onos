/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of BGP-PCEP flow provider.
 */
@Component(immediate = true)
public class BgpcepFlowRuleProvider extends AbstractProvider
        implements FlowRuleProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleProviderRegistry providerRegistry;

    private FlowRuleProviderService providerService;

    /**
     * Creates a BgpFlow host provider.
     */
    public BgpcepFlowRuleProvider() {
        super(new ProviderId("l3", "org.onosproject.provider.bgpcep"));
    }

    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        // TODO Auto-generated method stub

    }

    @Override
    public void executeBatch(FlowRuleBatchOperation batch) {
        // TODO Auto-generated method stub

    }
}
