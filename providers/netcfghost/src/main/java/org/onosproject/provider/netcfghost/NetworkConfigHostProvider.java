/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.provider.netcfghost;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

/**
 * Host provider that uses network config service to discover hosts.
 */
@Component(immediate = true)
public class NetworkConfigHostProvider extends AbstractProvider implements HostProvider {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfigRegistry;

    private ApplicationId appId;
    private static final String APP_NAME = "org.onosproject.netcfghost";
    private static final ProviderId PROVIDER_ID = new ProviderId("host", APP_NAME);
    protected HostProviderService providerService;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final InternalNetworkConfigListener networkConfigListener =
            new InternalNetworkConfigListener();

    /**
     * Creates an network config host location provider.
     */
    public NetworkConfigHostProvider() {
        super(PROVIDER_ID);
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        providerService = providerRegistry.register(this);
        networkConfigRegistry.addListener(networkConfigListener);
        readInitialConfig();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        networkConfigRegistry.removeListener(networkConfigListener);
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Host host) {
        /*
         * Note: All hosts are configured in network config host provider.
         * Therefore no probe is required.
         */
    }

    /**
     * Adds host information.
     * IP information will be appended if host exists.
     *
     * @param mac MAC address of the host
     * @param vlan VLAN ID of the host
     * @param hloc Location of the host
     * @param ips Set of IP addresses of the host
     */
    protected void addHost(MacAddress mac, VlanId vlan, HostLocation hloc, Set<IpAddress> ips) {
        HostId hid = HostId.hostId(mac, vlan);
        HostDescription desc = (ips != null) ?
                new DefaultHostDescription(mac, vlan, hloc, ips, true) :
                new DefaultHostDescription(mac, vlan, hloc, true);
        providerService.hostDetected(hid, desc, false);
    }

    /**
     * Updates host information.
     * IP information will be replaced if host exists.
     *
     * @param mac MAC address of the host
     * @param vlan VLAN ID of the host
     * @param hloc Location of the host
     * @param ips Set of IP addresses of the host
     */
    protected void updateHost(MacAddress mac, VlanId vlan, HostLocation hloc, Set<IpAddress> ips) {
        HostId hid = HostId.hostId(mac, vlan);
        HostDescription desc = new DefaultHostDescription(mac, vlan, hloc, ips, true);
        providerService.hostDetected(hid, desc, true);
    }

    /**
     * Removes host information.
     *
     * @param mac MAC address of the host
     * @param vlan VLAN ID of the host
     */
    protected void removeHost(MacAddress mac, VlanId vlan) {
        HostId hid = HostId.hostId(mac, vlan);
        providerService.hostVanished(hid);
    }

    private void readInitialConfig() {
        networkConfigRegistry.getSubjects(HostId.class).forEach(hostId -> {
            MacAddress mac = hostId.mac();
            VlanId vlan = hostId.vlanId();
            BasicHostConfig hostConfig =
                    networkConfigRegistry.getConfig(hostId, BasicHostConfig.class);
            Set<IpAddress> ipAddresses = hostConfig.ipAddresses();
            ConnectPoint location = hostConfig.location();
            HostLocation hloc = new HostLocation(location, System.currentTimeMillis());
            addHost(mac, vlan, hloc, ipAddresses);
        });
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            // Do not process non-host, register and unregister events
            if (!event.configClass().equals(BasicHostConfig.class) ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_REGISTERED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_UNREGISTERED) {
                return;
            }

            HostId hostId = (HostId) event.subject();
            MacAddress mac = hostId.mac();
            VlanId vlan = hostId.vlanId();
            BasicHostConfig hostConfig =
                    networkConfigRegistry.getConfig(hostId, BasicHostConfig.class);
            Set<IpAddress> ipAddresses = null;
            HostLocation hloc = null;

            // Note: There will be no config presented in the CONFIG_REMOVE case
            if (hostConfig != null) {
                ipAddresses = hostConfig.ipAddresses();
                ConnectPoint location = hostConfig.location();
                hloc = new HostLocation(location, System.currentTimeMillis());
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                    addHost(mac, vlan, hloc, ipAddresses);
                    break;
                case CONFIG_UPDATED:
                    updateHost(mac, vlan, hloc, ipAddresses);
                    break;
                case CONFIG_REMOVED:
                    removeHost(mac, vlan);
                    break;
                default:
                    break;
            }
        }
    }
}
