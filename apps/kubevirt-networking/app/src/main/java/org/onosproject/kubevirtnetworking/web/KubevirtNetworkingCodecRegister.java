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
package org.onosproject.kubevirtnetworking.web;

import org.onosproject.codec.CodecService;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerRule;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;
import org.onosproject.kubevirtnetworking.codec.KubevirtFloatingIpCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtHostRouteCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtIpPoolCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtLoadBalancerCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtLoadBalancerRuleCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtNetworkCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtPortCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtRouterCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtSecurityGroupCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtSecurityGroupRuleCodec;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for KubevirtNetworking.
 */
@Component(immediate = true)
public class KubevirtNetworkingCodecRegister {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CodecService codecService;

    @Activate
    protected void activate() {

        codecService.registerCodec(KubevirtHostRoute.class, new KubevirtHostRouteCodec());
        codecService.registerCodec(KubevirtIpPool.class, new KubevirtIpPoolCodec());
        codecService.registerCodec(KubevirtNetwork.class, new KubevirtNetworkCodec());
        codecService.registerCodec(KubevirtPort.class, new KubevirtPortCodec());
        codecService.registerCodec(KubevirtRouter.class, new KubevirtRouterCodec());
        codecService.registerCodec(KubevirtFloatingIp.class, new KubevirtFloatingIpCodec());
        codecService.registerCodec(KubevirtSecurityGroup.class, new KubevirtSecurityGroupCodec());
        codecService.registerCodec(KubevirtSecurityGroupRule.class, new KubevirtSecurityGroupRuleCodec());
        codecService.registerCodec(KubevirtLoadBalancer.class, new KubevirtLoadBalancerCodec());
        codecService.registerCodec(KubevirtLoadBalancerRule.class, new KubevirtLoadBalancerRuleCodec());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {

        codecService.unregisterCodec(KubevirtHostRoute.class);
        codecService.unregisterCodec(KubevirtIpPool.class);
        codecService.unregisterCodec(KubevirtNetwork.class);
        codecService.unregisterCodec(KubevirtPort.class);
        codecService.unregisterCodec(KubevirtRouter.class);
        codecService.unregisterCodec(KubevirtFloatingIp.class);
        codecService.unregisterCodec(KubevirtSecurityGroup.class);
        codecService.unregisterCodec(KubevirtSecurityGroupRule.class);
        codecService.unregisterCodec(KubevirtLoadBalancer.class);
        codecService.unregisterCodec(KubevirtLoadBalancerRule.class);

        log.info("Stopped");
    }
}
