/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.common.net;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base device provider capable of engaging
 * {@link org.onosproject.net.device.DeviceDescriptionDiscovery}
 * driver behaviour to discover device and port details.
 * <p>
 * Assumes that derived classes will provide code to learn/generate
 * device identifier. Also assumes that derived classes will either obtain
 * the primordial device information sufficient to locate the correct driver,
 * or that they will know which driver should be used, e.g. from network
 * configuration.
 * </p>
 */
public abstract class AbstractDeviceProvider extends AbstractProvider
        implements DeviceProvider {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    protected DeviceProviderService providerService;

    /**
     * Creates a provider with the supplied identifier.
     *
     * @param id provider id
     */
    protected AbstractDeviceProvider(ProviderId id) {
        super(id);
    }

    @Activate
    protected void activate() {
        providerService = providerRegistry.register(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;

        log.info("Stopped");
    }

    /**
     * Discovers the device details using the device discovery behaviour of
     * the supplied driver handler context for interacting with a specific
     * device.
     *
     * @param handler driver handler context
     */
    protected void discoverDevice(DriverHandler handler) {
        DeviceId deviceId = handler.data().deviceId();
        DeviceDescriptionDiscovery discovery = handler.behaviour(DeviceDescriptionDiscovery.class);
        DeviceDescription description = discovery.discoverDeviceDetails();
        if (description != null) {
            providerService.deviceConnected(deviceId, description);
        } else {
            log.info("No other description given for device {}", deviceId);
        }
        providerService.updatePorts(deviceId, discovery.discoverPortDetails());

    }
    // TODO: inspect NETCONF, SNMP, RESTSB device providers for additional common patterns
    // TODO: provide base for port status update
    // TODO: integrate with network config for learning about management addresses to probe

}
