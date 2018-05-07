/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.d.config.sync.impl.netconf;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.d.config.sync.DeviceConfigSynchronizationProviderRegistry;
import org.onosproject.d.config.sync.DeviceConfigSynchronizationProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.yang.model.SchemaContextProvider;
import org.onosproject.yang.runtime.YangRuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;

/**
 * Main component of Dynamic config synchronizer for NETCONF.
 *
 * <ul>
 * <li> bootstrap Active and Passive synchronization modules
 * <li> start background anti-entropy mechanism for offline device configuration
 * </ul>
 */
@Component(immediate = true)
public class NetconfDeviceConfigSynchronizerComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * NETCONF dynamic config synchronizer provider ID.
     */
    public static final ProviderId PID =
            new ProviderId("netconf", "org.onosproject.d.config.sync.netconf");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceConfigSynchronizationProviderRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfController netconfController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YangRuntimeService yangRuntimeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SchemaContextProvider schemaContextProvider;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private NetconfDeviceConfigSynchronizerProvider provider;

    private DeviceConfigSynchronizationProviderService providerService;


    @Activate
    protected void activate() {
        provider = new NetconfDeviceConfigSynchronizerProvider(PID, new InnerNetconfContext());
        providerService = registry.register(provider);

        // TODO (Phase 2 or later)
        //      listen to NETCONF events (new Device appeared, etc.)
        //      for PASSIVE "state" synchronization upward

        // TODO listen to DeviceEvents (Offline pre-configuration scenario)

        // TODO background anti-entropy mechanism

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        registry.unregister(provider);
        log.info("Stopped");
    }

    /**
     * Context object to provide reference to OSGi services, etc.
     */
    @Beta
    public static interface NetconfContext {

        /**
         * Returns DeviceConfigSynchronizationProviderService interface.
         *
         * @return DeviceConfigSynchronizationProviderService
         */
        DeviceConfigSynchronizationProviderService providerService();

        SchemaContextProvider schemaContextProvider();

        YangRuntimeService yangRuntime();

        NetconfController netconfController();

    }

    class InnerNetconfContext implements NetconfContext {

        @Override
        public NetconfController netconfController() {
            return netconfController;
        }

        @Override
        public YangRuntimeService yangRuntime() {
            return yangRuntimeService;
        }

        @Override
        public SchemaContextProvider schemaContextProvider() {
            return schemaContextProvider;
        }

        @Override
        public DeviceConfigSynchronizationProviderService providerService() {
            return providerService;
        }
    }
}
