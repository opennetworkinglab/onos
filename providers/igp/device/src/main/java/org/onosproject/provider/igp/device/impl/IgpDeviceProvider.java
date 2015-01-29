/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.provider.igp.device.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import static org.onosproject.net.DeviceId.deviceId;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure devices.
 */
@Component(immediate = true)
public class IgpDeviceProvider extends AbstractProvider implements DeviceProvider {

    private static final Logger LOG = getLogger(IgpDeviceProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;


    private DeviceProviderService providerService;


    /**
     * Creates an OpenFlow device provider.
     */
    public IgpDeviceProvider() {
        super(new ProviderId("igp", "org.onosproject.provider.igp"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        DeviceId diD1 = deviceId("igp:000000001");
        DeviceId diD2 = deviceId("igp:000000002");
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                providerService.deviceConnected(diD1, new DefaultDeviceDescription(diD1.uri(), Device.Type.SWITCH,
                                           null, null, null, null, null));
                providerService.deviceConnected(diD2, new DefaultDeviceDescription(diD2.uri(), Device.Type.SWITCH,
                                           null, null, null, null, null));
            }
        }).start();
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;
        LOG.info("Stopped");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return true;
    }
}