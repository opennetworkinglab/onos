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

package org.onosproject.provider.isis.device.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.isis.controller.IsisController;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which advertises device descriptions to the core.
 */
@Component(immediate = true)
public class IsisTopologyProvider extends AbstractProvider {

    private static final Logger log = getLogger(IsisTopologyProvider.class);
    final InternalDeviceProvider listener = new InternalDeviceProvider();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IsisController isisController;


    /**
     * Creates an ISIS device provider.
     */
    public IsisTopologyProvider() {
        super(new ProviderId("isis", "org.onosproject.provider.isis"));
    }

    @Activate
    public void activate() {
        log.debug("Activate...!!!");
    }

    @Deactivate
    public void deactivate() {
        log.debug("Deactivate...!!!");
    }

    /**
     * Internal device provider implementation.
     */
    private class InternalDeviceProvider {

    }
}
