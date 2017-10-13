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

package org.onosproject.driver.trafficcontrol;

import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.trafficcontrol.Policer;
import org.onosproject.net.behaviour.trafficcontrol.PolicerConfigurable;
import org.onosproject.net.behaviour.trafficcontrol.PolicerId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterService;
import org.slf4j.Logger;

import java.net.URI;
import java.util.Collection;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of policer config which allows to add, delete and get policers.
 */
public class OpenFlowPolicerConfigurable extends AbstractHandlerBehaviour implements PolicerConfigurable {

    // log
    private final Logger log = getLogger(OpenFlowPolicerConfigurable.class);
    // OpenFlow scheme
    private static final String OF_SCHEME = "of";
    // Hex
    private static final int HEX = 16;

    // Create a policer id from a meter id
    private PolicerId getPolicerIdFromMeterId(MeterId meterId) {
        // Create URI representing the meter id
        URI uri = URI.create(OF_SCHEME + ":" + Long.toHexString(meterId.id()));
        // Return the new policer id
        return PolicerId.policerId(uri);
    }

    // Create a meter id from a policer id
    private MeterId getMeterIdFromPolicerId(PolicerId policerId) {
        // Get scheme specific part
        Long id = Long.parseLong(policerId.uri().getSchemeSpecificPart(), HEX);
        // Return the meter id
        return MeterId.meterId(id);
    }

    @Override
    public PolicerId allocatePolicerId() {
        // Init step
        DriverHandler handler = handler();
        // First step is to get MeterService
        MeterService meterService = handler.get(MeterService.class);
        // There was a problem, return none
        if (meterService == null) {
            log.warn("MeterService is null");
            return PolicerId.NONE;
        }
        // Let's get the device id
        DeviceId deviceId = handler.data().deviceId();
        // Double check correspondence between schemas
        if (!deviceId.uri().getScheme().equals(OF_SCHEME)) {
            log.warn("The device {} does not seem to be managed by OpenFlow", deviceId);
            return PolicerId.NONE;
        }
        // Get a new meter id
        MeterId meterId = meterService.allocateMeterId(deviceId);
        // There was a problem
        if (meterId == null) {
            log.warn("MeterService does not provide valid ids");
            return PolicerId.NONE;
        }
        // Create a policer id from the meter id
        return getPolicerIdFromMeterId(meterId);
    }

    @Override
    public void freePolicerId(PolicerId id) {
        // Init step
        DriverHandler handler = handler();
        // First step is to get MeterService
        MeterService meterService = handler.get(MeterService.class);
        // There was a problem, return none
        if (meterService == null) {
            log.warn("MeterService is null");
            return;
        }
        // Let's get the device id
        DeviceId deviceId = handler.data().deviceId();
        // Double check correspondence with device schema
        if (!deviceId.uri().getScheme().equals(OF_SCHEME)) {
            log.warn("The device {} does not seem to be managed by OpenFlow", deviceId);
            return;
        }
        // Double check correspondence with pid schema
        if (!id.uri().getScheme().equals(OF_SCHEME)) {
            log.warn("The id {} does not seem to be OpenFlow", id);
            return;
        }
        // Get the meter id
        MeterId meterId = getMeterIdFromPolicerId(id);
        // Free the meter id
        meterService.freeMeterId(deviceId, meterId);
    }

    @Override
    public void addPolicer(Policer policer) {
        throw new UnsupportedOperationException("addPolicer not yet implemented");
    }

    @Override
    public void deletePolicer(PolicerId id) {
        throw new UnsupportedOperationException("deletePolicer not yet implemented");
    }

    @Override
    public Policer getPolicer(PolicerId policerId) {
        throw new UnsupportedOperationException("getPolicer not yet implemented");
    }

    @Override
    public Collection<Policer> getPolicers() {
        throw new UnsupportedOperationException("getPolicers not yet implemented");
    }

}
