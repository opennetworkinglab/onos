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
package org.onosproject.openstack4j;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.options.PortListOptions;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wrapper implementation of openstack4j.
 */
@Component(immediate = true)
@Service
public class OpenStack4jManager implements OpenStack4jService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public OSClient getClient(String endpoint, String tenant, String user, String password) {
        try {
            return OSFactory.builder()
                    .endpoint(endpoint)
                    .credentials(user, password)
                    .tenantName(tenant)
                    .authenticate();
        } catch (AuthenticationException e) {
            log.warn("Failed to authenticate");
            return null;
        }
    }

    @Override
    public List<Network> getNetworks(OSClient client) {
        checkNotNull(client, "OSClient is null");
        return client.networking().network().list()
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public Network getNetwork(OSClient client, String networkId) {
        checkNotNull(client, "OSClient is null");
        return client.networking().network().get(networkId);
    }

    @Override
    public List<Port> getPorts(OSClient client) {
        checkNotNull(client, "OSClient is null");
        return client.networking().port().list()
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public List<Port> getPorts(OSClient client, PortListOptions options) {
        checkNotNull(client, "OSClient is null");
        return client.networking().port().list(options)
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public Port getPort(OSClient client, String portId) {
        checkNotNull(client, "OSClient is null");
        return client.networking().port().get(portId);
    }

    @Override
    public List<Subnet> getSubnets(OSClient client) {
        checkNotNull(client, "OSClient is null");
        return client.networking().subnet().list()
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public Subnet getSubnet(OSClient client, String subnetId) {
        checkNotNull(client, "OSClient is null");
        return client.networking().subnet().get(subnetId);
    }
}
