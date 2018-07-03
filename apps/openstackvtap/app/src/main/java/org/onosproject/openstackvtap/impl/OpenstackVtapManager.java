/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.VlanId;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtap.Type;
import org.onosproject.openstackvtap.api.OpenstackVtapAdminService;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.api.OpenstackVtapEvent;
import org.onosproject.openstackvtap.api.OpenstackVtapId;
import org.onosproject.openstackvtap.api.OpenstackVtapListener;
import org.onosproject.openstackvtap.api.OpenstackVtapService;

import java.util.Set;

/**
 * Provides basic implementation of the user APIs.
 */
@Component(immediate = true)
@Service
public class OpenstackVtapManager
        extends AbstractListenerManager<OpenstackVtapEvent, OpenstackVtapListener>
        implements OpenstackVtapService, OpenstackVtapAdminService {

    @Override
    public OpenstackVtap createVtap(Type type, OpenstackVtapCriterion vTapCriterion) {
        return null;
    }

    @Override
    public OpenstackVtap updateVtap(OpenstackVtapId vTapId, OpenstackVtap vTap) {
        return null;
    }

    @Override
    public OpenstackVtap removeVtap(OpenstackVtapId vTapId) {
        return null;
    }

    @Override
    public void setVtapOutput(DeviceId deviceId, Type type, PortNumber portNumber, VlanId vlanId) {

    }

    @Override
    public void setVtapOutput(DeviceId deviceId, Type type, PortNumber portNumber, int vni) {

    }

    @Override
    public int getVtapCount(Type type) {
        return 0;
    }

    @Override
    public Set<OpenstackVtap> getVtaps(Type type) {
        return null;
    }

    @Override
    public OpenstackVtap getVtap(OpenstackVtapId vTapId) {
        return null;
    }

    @Override
    public Set<OpenstackVtap> getVtapsByDeviceId(Type type, DeviceId deviceId) {
        return null;
    }
}
