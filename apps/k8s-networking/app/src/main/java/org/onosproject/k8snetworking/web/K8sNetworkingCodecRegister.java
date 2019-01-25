/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.web;

import org.onosproject.codec.CodecService;
import org.onosproject.k8snetworking.api.K8sIpam;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snetworking.codec.K8sIpamCodec;
import org.onosproject.k8snetworking.codec.K8sNetworkCodec;
import org.onosproject.k8snetworking.codec.K8sPortCodec;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for K8sNetworking.
 */
@Component(immediate = true)
public class K8sNetworkingCodecRegister {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CodecService codecService;

    @Activate
    protected void activate() {

        codecService.registerCodec(K8sIpam.class, new K8sIpamCodec());
        codecService.registerCodec(K8sNetwork.class, new K8sNetworkCodec());
        codecService.registerCodec(K8sPort.class, new K8sPortCodec());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {

        codecService.unregisterCodec(K8sIpam.class);
        codecService.unregisterCodec(K8sNetwork.class);
        codecService.unregisterCodec(K8sPort.class);

        log.info("Stopped");
    }
}
