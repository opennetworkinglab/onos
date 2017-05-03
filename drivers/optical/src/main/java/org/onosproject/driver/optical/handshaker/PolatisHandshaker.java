/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.driver.optical.handshaker;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Set;
import org.onosproject.net.Device.Type;
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Handshaker for Polatis fiber switch.
 *
 */
public class PolatisHandshaker extends AbstractOpenFlowSwitch
    implements OpenFlowOpticalSwitch {

    private static final Logger log = getLogger(PolatisHandshaker.class);

    @Override
    public Boolean supportNxRole() {
        // Device is OF1.4 so response doesn't matter.
        return Boolean.FALSE;
    }

    @Override
    public void startDriverHandshake() {
        // nothing Device specific required
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        // nothing Device specific to do
        return true;
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        // nothing Device specific to do
    }

    @Override
    public Type deviceType() {
        return Type.FIBER_SWITCH;
    }

    @Override
    public List<OFPortDesc> getPortsOf(PortDescPropertyType type) {
        return this.getPorts().stream()
            .filter(pd -> isPortType(type, pd))
            .collect(ImmutableList.toImmutableList());
    }

    /**
     * Tests if OFPortDesc {@code pd} is a {@code type}.
     *
     * @param type to test if {@code pd} is of port type
     * @param pd OpenFlow port description to test.
     * @return true if {@code pd} is of port description of {@code type}
     */
    private static boolean isPortType(PortDescPropertyType type, OFPortDesc pd) {
        try {
            return pd.getProperties().stream()
                        .anyMatch(prop -> prop.getType() == type.valueOf());
        } catch (UnsupportedOperationException e) {
            log.warn("Unexpected OFPortDesc {} reveived", pd, e);
            return false;
        }
    }

    @Override
    public Set<PortDescPropertyType> getPortTypes() {
        return ImmutableSet.of(PortDescPropertyType.OPTICAL);
    }
}
