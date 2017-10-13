/*
 * Copyright 2017 Open Networking Foundation
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

package org.onosproject.drivers.polatis.netconf;

import com.google.common.collect.Range;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.net.Direction;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.onosproject.drivers.polatis.netconf.PolatisOpticalUtility.POWER_MULTIPLIER;
import static org.onosproject.drivers.polatis.netconf.PolatisOpticalUtility.POWER_RANGE;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Get current or target port/channel power from a Polatis optical netconf device.
 * Set target port power or channel attenuation to an optical netconf device.
 */
public class PolatisPowerConfig<T> extends AbstractHandlerBehaviour
    implements PowerConfig<T> {

    private static final Logger log = getLogger(PolatisPowerConfig.class);

    @Override
    public Optional<Long> getTargetPower(PortNumber port, T component) {
        return Optional.ofNullable(acquireTargetPower(port, component));
    }

    @Override
    public void setTargetPower(PortNumber port, T component, long power) {
        if (component instanceof OchSignal) {
            log.warn("Channel power is not applicable.");
            return;
        }
        setPortTargetPower(port, power);
    }

    @Override
    public Optional<Long> currentPower(PortNumber port, T component) {
        return Optional.ofNullable(acquireCurrentPower(port, component));
    }

    @Override
    public Optional<Range<Long>> getTargetPowerRange(PortNumber port, T component) {
        return Optional.ofNullable(getTxPowerRange(port, component));
    }

    @Override
    public Optional<Range<Long>> getInputPowerRange(PortNumber port, T component) {
        return Optional.ofNullable(getRxPowerRange(port, component));
    }

    @Override
    public List<PortNumber> getPorts(T component) {
        if (component instanceof OchSignal) {
            log.warn("Channel component is not applicable.");
            return new ArrayList<PortNumber>();
        }
        log.debug("Get port config ports...");
        return acquirePorts();
    }

    private List<PortNumber> acquirePorts() {
        String filter = getPortPowerFilter(null);
        String reply = netconfGet(handler(), filter);
        List<HierarchicalConfiguration> subtrees = configsAt(reply, KEY_DATA_OPM);
        List<PortNumber> ports = new ArrayList<PortNumber>();
        for (HierarchicalConfiguration portConfig : subtrees) {
            ports.add(PortNumber.portNumber(portConfig.getLong(KEY_PORTID)));
        }
        return ports;
    }

    /**
     * Get the filter string for the OPM power NETCONF request.
     *
     * @param port the port, null to return all the opm ports
     * @return filter string
     */
    private String getPortPowerFilter(PortNumber port) {
        StringBuilder filter = new StringBuilder(xmlOpen(KEY_OPM_XMLNS))
                .append(xmlOpen(KEY_PORT))
                .append(xmlOpen(KEY_PORTID));
        if (port != null) {
            filter.append(port.toLong());
        }
        return filter.append(xmlClose(KEY_PORTID))
                .append(xmlClose(KEY_PORT))
                .append(xmlClose(KEY_OPM))
                .toString();
    }

    private Long acquireTargetPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            log.warn("Channel power is not applicable.");
            return null;
        }
        log.debug("Get port{} target power...", port);
        log.warn("This is currently unimplemented");
        return null;
    }

    private Long acquireCurrentPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            log.warn("Channel power is not applicable.");
            return null;
        }
        log.debug("Get port{} current power...", port);
        return acquirePortPower(port);
    }

    private Long acquirePortPower(PortNumber port) {
        String filter = getPortPowerFilter(port);
        String reply = netconfGet(handler(), filter);
        HierarchicalConfiguration info = configAt(reply, KEY_DATA_OPM_PORT);
        if (info == null) {
            return null;
        }
        return (long) (info.getDouble(KEY_POWER) * POWER_MULTIPLIER);
    }

    private boolean setPortTargetPower(PortNumber port, long power) {
        log.debug("Set port{} target power...", port);
        log.warn("This is currently unimplemented");
        return false;
    }

    private Range<Long> getPowerRange() {
        return POWER_RANGE;
    }

    private Range<Long> getTxPowerRange(PortNumber port, T component) {
        if (component instanceof Direction) {
            log.debug("Get target port{} power range...", port);
            return getPowerRange();
        } else {
            log.debug("Get channel attenuation range...");
            log.warn("Channel power is not applicable.");
            return null;
        }
    }

    private Range<Long> getRxPowerRange(PortNumber port, T component) {
        log.debug("Get input port{} power range...", port);
        return getPowerRange();
    }
}
