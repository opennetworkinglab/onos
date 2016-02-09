/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.provider.of.message.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cpman.message.ControlMessageProvider;
import org.onosproject.cpman.message.ControlMessageProviderRegistry;
import org.onosproject.cpman.message.ControlMessageProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to collect control message.
 */
@Component(immediate = true)
public class OpenFlowControlMessageProvider extends AbstractProvider
        implements ControlMessageProvider {

    private static final Logger LOG = getLogger(OpenFlowControlMessageProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ControlMessageProviderRegistry providerRegistry;

    private ControlMessageProviderService providerService;

    /**
     * Creates a provider with the supplier identifier.
     */
    public OpenFlowControlMessageProvider() {
        super(new ProviderId("of", "org.onosproject.provider.openflow"));
    }

    @Activate
    protected void activate() {
        providerService = providerRegistry.register(this);
        LOG.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;
        LOG.info("Stopped");
    }
}
