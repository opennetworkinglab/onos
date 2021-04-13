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

import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Handles REST API call for kubevirt load balancer.
 */
@Path("loadbalancer")
public class KubevirtLoadBalancerWebResource extends AbstractWebResource {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String LOAD_BALANCERS = "loadBalancers";

    /**
     * Returns set of all load balancers.
     *
     * @return 200 OK with set of all load balancers
     * @onos.rsModel KubevirtLoadBalancers
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLoadBalancers() {
        KubevirtLoadBalancerService service = get(KubevirtLoadBalancerService.class);
        final Iterable<KubevirtLoadBalancer> lbs = service.loadBalancers();
        return ok(encodeArray(KubevirtLoadBalancer.class, LOAD_BALANCERS, lbs)).build();
    }
}
