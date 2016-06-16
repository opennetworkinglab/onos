/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.onosproject.provider.bgp.topology.impl;

import static org.onosproject.bgp.controller.BgpDpid.uri;
import static org.onosproject.net.DeviceId.deviceId;

import org.onlab.packet.ChassisId;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpDpid;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider which uses an BGP controller to detect network infrastructure topology.
 */
@Component(immediate = true)
public class BgpTopologyProvider extends AbstractProvider implements DeviceProvider {

    public BgpTopologyProvider() {
        super(new ProviderId("bgp", "org.onosproject.provider.bgp"));
    }

    private static final Logger log = LoggerFactory.getLogger(BgpTopologyProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BgpController controller;

    private DeviceProviderService deviceProviderService;

    private InternalBgpProvider listener = new InternalBgpProvider();
    private static final String UNKNOWN = "unknown";

    @Activate
    public void activate() {
        deviceProviderService = deviceProviderRegistry.register(this);
        controller.addListener(listener);
    }

    @Deactivate
    public void deactivate() {
        controller.removeListener(listener);
    }

    /*
     * Implements device and link update.
     */
    private class InternalBgpProvider implements BgpNodeListener {

        @Override
        public void addNode(BgpNodeLSNlriVer4 nodeNlri) {
            log.debug("Add node {}", nodeNlri.toString());

            if (deviceProviderService == null) {
                return;
            }
            BgpDpid nodeUri = new BgpDpid(nodeNlri);
            DeviceId deviceId = deviceId(uri(nodeUri.toString()));
            ChassisId cId = new ChassisId();

            DeviceDescription description = new DefaultDeviceDescription(uri(nodeUri.toString()), Device.Type.ROUTER,
                                                                         UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, cId);
            deviceProviderService.deviceConnected(deviceId, description);

        }

        @Override
        public void deleteNode(BgpNodeLSNlriVer4 nodeNlri) {
            log.debug("Delete node {}", nodeNlri.toString());

            if (deviceProviderService == null) {
                return;
            }

            BgpDpid nodeUri = new BgpDpid(nodeNlri);
            deviceProviderService.deviceDisconnected(deviceId(uri(nodeUri.toString())));
        }
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void enablePort(DeviceId deviceId, PortNumber portNumber) {
        // TODO
    }

    @Override
    public void disablePort(DeviceId deviceId, PortNumber portNumber) {
        // TODO
    }
}
