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
package org.onosproject.xosclient.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.onosproject.xosclient.api.OpenStackAccess;
import org.onosproject.xosclient.api.VtnServiceApi;
import org.onosproject.xosclient.api.XosAccess;
import org.onosproject.xosclient.api.VtnService;
import org.onosproject.xosclient.api.VtnServiceId;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.xosclient.api.VtnServiceApi.NetworkType.*;
import static org.onosproject.xosclient.api.VtnServiceApi.ServiceType.*;

/**
 * Provides CORD VTN service and service dependency APIs.
 */
public final class DefaultVtnServiceApi extends XosApi implements VtnServiceApi {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Default constructor.
     *
     * @param baseUrl base url
     * @param access xos access
     */
    public DefaultVtnServiceApi(String baseUrl, XosAccess access) {
        super(baseUrl, access);
    }

    @Override
    public Set<VtnServiceId> services() {
        String response = restGet(EMPTY_STRING);
        log.trace("Get services {}", response);

        ObjectMapper mapper = new ObjectMapper();
        Set<VtnServiceId> services = Sets.newHashSet();

        try {
            JsonNode nodes = mapper.readTree(response);
            nodes.fieldNames().forEachRemaining(id -> services.add(VtnServiceId.of(id)));
        } catch (IOException e) {
            log.warn("Failed to get service list");
        }
        return services;
    }

    @Override
    public VtnService service(VtnServiceId serviceId) {
        // TODO implement this when XOS provides this API
        return null;
    }

    @Override
    public Set<VtnServiceId> providerServices(VtnServiceId tServiceId) {
        checkNotNull(tServiceId);

        String response = restGet(tServiceId.id());
        log.trace("Get provider services {}", response);

        ObjectMapper mapper = new ObjectMapper();
        Set<VtnServiceId> pServices = Sets.newHashSet();

        try {
            JsonNode nodes = mapper.readTree(response);
            nodes.forEach(node -> pServices.add(VtnServiceId.of(node.asText())));
        } catch (IOException e) {
            log.warn("Failed to get service dependency");
        }
        return pServices;
    }

    @Override
    public Set<VtnServiceId> tenantServices(VtnServiceId tServiceId) {
        checkNotNull(tServiceId);

        String response = restGet(EMPTY_STRING);
        log.trace("Get tenant services {}", response);

        ObjectMapper mapper = new ObjectMapper();
        Set<VtnServiceId> tServices = Sets.newHashSet();

        try {
            JsonNode nodes = mapper.readTree(response);
            nodes.fields().forEachRemaining(entry -> entry.getValue().forEach(
                    pService -> {
                        if (pService.asText().equals(tServiceId.id())) {
                            tServices.add(VtnServiceId.of(entry.getKey()));
                        }
                    }));
        } catch (IOException e) {
            log.warn("Failed to get service list");
        }
        return tServices;
    }

    @Override
    // TODO remove this when XOS provides this information
    public VtnService service(VtnServiceId serviceId, OpenStackAccess osAccess) {
        checkNotNull(osAccess);

        OSClient osClient = getOpenStackClient(osAccess);
        Network osNet = osClient.networking().network().get(serviceId.id());
        if (osNet == null) {
            log.warn("Failed to get OpenStack network {}", serviceId);
            return null;
        }

        // assumes all cord service networks has single subnet
        Subnet osSubnet = osNet.getNeutronSubnets().stream()
                .findFirst().orElse(null);
        if (osSubnet == null) {
            log.warn("Failed to get OpenStack subnet of network {}", serviceId);
            return null;
        }

        return VtnService.build()
                .id(serviceId)
                .name(osNet.getName())
                .serviceType(serviceType(osNet.getName()))
                .networkType(networkType(osNet.getName()))
                .vni(osNet.getProviderSegID())
                .subnet(osSubnet.getCidr())
                .serviceIp(osSubnet.getGateway())
                .providerServices(providerServices(serviceId))
                .tenantServices(tenantServices(serviceId))
                .build();
    }

    // TODO remove this when XOS provides this information
    private OSClient getOpenStackClient(OpenStackAccess osAccess) {
        checkNotNull(osAccess);

        // creating a client every time must be inefficient, but this method
        // will be removed once XOS provides equivalent APIs
        try {
            return OSFactory.builder()
                    .endpoint(osAccess.endpoint())
                    .credentials(osAccess.user(), osAccess.password())
                    .tenantName(osAccess.tenant())
                    .authenticate();
        } catch (AuthenticationException e) {
            log.warn("Failed to authenticate OpenStack API with {}", osAccess);
            return null;
        }
    }

    // TODO remove this when XOS provides this information
    private NetworkType networkType(String netName) {
        checkArgument(!Strings.isNullOrEmpty(netName), "VTN network name cannot be null");

        String name = netName.toUpperCase();
        if (name.contains(PUBLIC.name())) {
            return PUBLIC;
        } else if (name.contains(MANAGEMENT_HOSTS.name())) {
            return MANAGEMENT_HOSTS;
        } else if (name.contains("MANAGEMENT")) {
            return MANAGEMENT_LOCAL;
        } else {
            return PRIVATE;
        }
    }

    // TODO remove this when XOS provides this information
    private ServiceType serviceType(String netName) {
        checkArgument(!Strings.isNullOrEmpty(netName), "VTN network name cannot be null");

        String name = netName.toUpperCase();
        if (name.contains(VSG.name())) {
            return VSG;
        } else if (name.contains(ACCESS_AGENT.name())) {
            return ACCESS_AGENT;
        } else if (name.contains(ServiceType.MANAGEMENT.name())) {
            return ServiceType.MANAGEMENT;
        } else {
            return DEFAULT;
        }
    }
}
