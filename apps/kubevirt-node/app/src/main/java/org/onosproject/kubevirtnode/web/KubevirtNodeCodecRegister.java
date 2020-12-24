/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.web;

import org.onosproject.codec.CodecService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;
import org.onosproject.kubevirtnode.codec.KubevirtApiConfigCodec;
import org.onosproject.kubevirtnode.codec.KubevirtNodeCodec;
import org.onosproject.kubevirtnode.codec.KubevirtPhyInterfaceCodec;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for KubevirtNode.
 */
@Component(immediate = true)
public class KubevirtNodeCodecRegister {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CodecService codecService;

    @Activate
    protected void activate() {
        codecService.registerCodec(KubevirtNode.class, new KubevirtNodeCodec());
        codecService.registerCodec(KubevirtPhyInterface.class, new KubevirtPhyInterfaceCodec());
        codecService.registerCodec(KubevirtApiConfig.class, new KubevirtApiConfigCodec());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        codecService.unregisterCodec(KubevirtNode.class);
        codecService.unregisterCodec(KubevirtPhyInterface.class);
        codecService.unregisterCodec(KubevirtApiConfig.class);

        log.info("Stopped");
    }
}
