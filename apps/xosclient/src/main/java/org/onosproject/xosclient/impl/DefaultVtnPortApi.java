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

import com.google.common.collect.Maps;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.xosclient.api.OpenStackAccess;
import org.onosproject.xosclient.api.VtnPort;
import org.onosproject.xosclient.api.VtnPortApi;
import org.onosproject.xosclient.api.VtnPortId;
import org.onosproject.xosclient.api.VtnServiceId;
import org.onosproject.xosclient.api.XosAccess;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Port;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides CORD VTN port APIs.
 */
public final class DefaultVtnPortApi extends XosApi implements VtnPortApi {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Default constructor.
     *
     * @param baseUrl base url
     * @param access xos access
     */
    public DefaultVtnPortApi(String baseUrl, XosAccess access) {
        super(baseUrl, access);
    }

    @Override
    public Set<VtnPort> vtnPorts() {
        // TODO implement this when XOS provides this information
        return null;
    }

    @Override
    public Set<VtnPort> vtnPorts(VtnServiceId serviceId) {
        // TODO implement this when XOS provides this information
        return null;
    }

    @Override
    public VtnPort vtnPort(VtnPortId portId) {
        // TODO implement this when XOS provides this information
        return null;
    }

    @Override
    // TODO remove this when XOS provides this information
    public VtnPort vtnPort(String portName, OpenStackAccess osAccess) {
        checkNotNull(osAccess);

        OSClient osClient = getOpenStackClient(osAccess);
        Port osPort = osClient.networking().port().list()
                .stream()
                .filter(p -> p.getId().contains(portName.substring(3)))
                .findFirst().orElse(null);
        if (osPort == null) {
            log.warn("Failed to get OpenStack port for {}", portName);
            return null;
        }
        return getVtnPort(osPort);
    }

    @Override
    // TODO remove this when XOS provides this information
    public VtnPort vtnPort(VtnPortId portId, OpenStackAccess osAccess) {
        checkNotNull(osAccess);

        OSClient osClient = getOpenStackClient(osAccess);
        Port osPort = osClient.networking().port().get(portId.id());
        if (osPort == null) {
            log.warn("Failed to get OpenStack port {}", portId);
            return null;
        }
        return getVtnPort(osPort);
    }

    // TODO remove this when XOS provides this information
    private VtnPort getVtnPort(Port osPort) {
        checkNotNull(osPort);

        // assumes all vtn port has single IP address
        IP ipAddr = osPort.getFixedIps().stream().findFirst().orElse(null);
        if (ipAddr == null) {
            log.warn("Failed to get IP address for {}", osPort);
            return null;
        }

        Map<IpAddress, MacAddress> addressPairs = Maps.newHashMap();
        osPort.getAllowedAddressPairs().forEach(
                pair -> addressPairs.put(IpAddress.valueOf(pair.getIpAddress()),
                        MacAddress.valueOf(pair.getMacAddress())));

        return VtnPort.builder()
                .id(VtnPortId.of(osPort.getId()))
                .name(osPort.getName())
                .serviceId(VtnServiceId.of(osPort.getNetworkId()))
                .mac(osPort.getMacAddress())
                .ip(ipAddr.getIpAddress())
                .addressPairs(addressPairs)
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
}
