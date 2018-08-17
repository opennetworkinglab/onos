/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknode.web;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.codec.CodecService;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.openstacknode.api.DpdkConfig;
import org.onosproject.openstacknode.api.DpdkInterface;
import org.onosproject.openstacknode.api.KeystoneConfig;
import org.onosproject.openstacknode.api.NeutronConfig;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackPhyInterface;
import org.onosproject.openstacknode.api.OpenstackSshAuth;
import org.onosproject.openstacknode.codec.DpdkConfigCodec;
import org.onosproject.openstacknode.codec.DpdkInterfaceCodec;
import org.onosproject.openstacknode.codec.KeystoneConfigCodec;
import org.onosproject.openstacknode.codec.NeutronConfigCodec;
import org.onosproject.openstacknode.codec.OpenstackAuthCodec;
import org.onosproject.openstacknode.codec.OpenstackControllerCodec;
import org.onosproject.openstacknode.codec.OpenstackNodeCodec;
import org.onosproject.openstacknode.codec.OpenstackPhyInterfaceCodec;
import org.onosproject.openstacknode.codec.OpenstackSshAuthCodec;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for OpenstackNode.
 */
@Component(immediate = true)
public class OpenstackNodeCodecRegister {

    private final org.slf4j.Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CodecService codecService;

    @Activate
    protected void activate() {
        codecService.registerCodec(OpenstackNode.class, new OpenstackNodeCodec());
        codecService.registerCodec(OpenstackAuth.class, new OpenstackAuthCodec());
        codecService.registerCodec(OpenstackPhyInterface.class, new OpenstackPhyInterfaceCodec());
        codecService.registerCodec(ControllerInfo.class, new OpenstackControllerCodec());
        codecService.registerCodec(OpenstackSshAuth.class, new OpenstackSshAuthCodec());
        codecService.registerCodec(DpdkInterface.class, new DpdkInterfaceCodec());
        codecService.registerCodec(DpdkConfig.class, new DpdkConfigCodec());
        codecService.registerCodec(KeystoneConfig.class, new KeystoneConfigCodec());
        codecService.registerCodec(NeutronConfig.class, new NeutronConfigCodec());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        codecService.unregisterCodec(OpenstackNode.class);
        codecService.unregisterCodec(OpenstackAuth.class);
        codecService.unregisterCodec(OpenstackPhyInterface.class);
        codecService.unregisterCodec(ControllerInfo.class);
        codecService.unregisterCodec(OpenstackSshAuth.class);
        codecService.unregisterCodec(DpdkConfig.class);
        codecService.unregisterCodec(DpdkInterface.class);
        codecService.unregisterCodec(KeystoneConfig.class);
        codecService.unregisterCodec(NeutronConfig.class);

        log.info("Stopped");
    }
}
