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

import org.onosproject.net.DeviceId;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapId;
import org.onosproject.openstackvtap.api.OpenstackVtapListener;
import org.onosproject.openstackvtap.api.OpenstackVtapService;

import java.util.Set;

/**
 * Implementation of openstack vtap.
 */
public class OpenstackVtapManager implements OpenstackVtapService {

    @Override
    public int getVtapCount(OpenstackVtap.Type type) {
        return 0;
    }

    @Override
    public Set<OpenstackVtap> getVtaps(OpenstackVtap.Type type) {
        return null;
    }

    @Override
    public OpenstackVtap getVtap(OpenstackVtapId vTapId) {
        return null;
    }

    @Override
    public Set<OpenstackVtap> getVtapsByDeviceId(OpenstackVtap.Type type, DeviceId deviceId) {
        return null;
    }

    @Override
    public void addListener(OpenstackVtapListener listener) {
    }

    @Override
    public void removeListener(OpenstackVtapListener listener) {
    }
}
