/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.nil.host.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Null provider to advertise fake hosts.
 */
@Component(immediate = true)
public class NullHostProvider extends AbstractProvider implements HostProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry providerRegistry;

    private HostProviderService providerService;

    //make sure the device has enough ports to accomodate all of them.
    private static final int HOSTSPERDEVICE = 5;

    private final InternalHostProvider hostProvider = new InternalHostProvider();

    /**
     * Creates an OpenFlow host provider.
     */
    public NullHostProvider() {
        super(new ProviderId("null", "org.onosproject.provider.nil"));
    }

    /**
     * Creates a provider with the supplier identifier.
     *
     * @param id provider id
     */
    protected NullHostProvider(ProviderId id) {
        super(id);
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        for (Device dev : deviceService.getDevices()) {
            addHosts(dev);
        }
        deviceService.addListener(hostProvider);

        log.info("Started");
    }



    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        deviceService.removeListener(hostProvider);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Host host) {}

    private void addHosts(Device device) {
        for (int i = 0; i < HOSTSPERDEVICE; i++) {
            providerService.hostDetected(
                    HostId.hostId(MacAddress.valueOf(i + device.hashCode()),
                                  VlanId.vlanId((short) -1)),
                    buildHostDescription(device, i));
        }
    }

    private void removeHosts(Device device) {
        for (int i = 0; i < HOSTSPERDEVICE; i++) {
            providerService.hostVanished(
                    HostId.hostId(MacAddress.valueOf(i + device.hashCode()),
                                  VlanId.vlanId((short) -1)));
        }
    }

    private HostDescription buildHostDescription(Device device, int port) {
        MacAddress mac = MacAddress.valueOf(device.hashCode() + port);
        HostLocation location = new HostLocation(device.id(),
                                                 PortNumber.portNumber(port), 0L);
        return new DefaultHostDescription(mac, VlanId.vlanId((short) -1), location);
    }

    private class InternalHostProvider implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (!deviceService.getRole(event.subject().id())
                    .equals(MastershipRole.MASTER)) {
                log.info("Local node is not master for device", event.subject().id());
                return;
            }
            switch (event.type()) {

                case DEVICE_ADDED:
                    addHosts(event.subject());
                    break;
                case DEVICE_UPDATED:
                    break;
                case DEVICE_REMOVED:
                    removeHosts(event.subject());
                    break;
                case DEVICE_SUSPENDED:
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    break;
                case PORT_ADDED:
                    break;
                case PORT_UPDATED:
                    break;
                case PORT_REMOVED:
                    break;
                default:
                    break;
            }
        }


    }
}
