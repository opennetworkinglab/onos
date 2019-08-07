/*
 * Copyright 2018 Open Networking Foundation
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

package org.onosproject.drivers.polatis.snmp;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Direction;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;

import com.google.common.collect.Range;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.onosproject.drivers.polatis.snmp.PolatisOpticalUtility.POWER_RANGE;
import static org.onosproject.drivers.polatis.snmp.PolatisSnmpUtility.get;
import static org.onosproject.drivers.polatis.snmp.PolatisSnmpUtility.getTable;
import static org.onosproject.drivers.polatis.snmp.PolatisSnmpUtility.set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Get current or target port/channel power from a Polatis optical snmp device.
 * Set target port power or channel attenuation to an optical snmp device.
 */
public class PolatisPowerConfig<T> extends AbstractHandlerBehaviour
        implements PowerConfig<T> {

    private static final int VOA_STATE_ABSOLUTE = 2;

    private static final String OPM_TABLE_OID = ".1.3.6.1.4.1.26592.2.3.2.2.2";
    private static final String OPM_POWER_OID = "1.1";

    private static final String VOA_TABLE_OID = ".1.3.6.1.4.1.26592.2.4.2.1.2";
    private static final String VOA_LEVEL_OID = VOA_TABLE_OID + ".1.1";
    private static final String VOA_STATE_OID = VOA_TABLE_OID + ".1.4";

    private final Logger log = getLogger(getClass());

    @Override
    public Optional<Double> getTargetPower(PortNumber port, T component) {
        Long power = acquireCurrentPower(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(power.doubleValue());
    }

    @Override
    public void setTargetPower(PortNumber port, T component, double power) {
        if (component instanceof OchSignal) {
            log.warn("Channel power is not applicable.");
            return;
        }
        setPortTargetPower(port, (long) power);
    }

    @Override
    public Optional<Double> currentPower(PortNumber port, T component) {
        Long power = acquireCurrentPower(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(power.doubleValue());
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, T component) {
        Range<Long> power = getTxPowerRange(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(Range.closed((double) power.lowerEndpoint(), (double) power.upperEndpoint()));
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber port, T component) {
        Range<Long> power = getRxPowerRange(port, component);
        if (power == null) {
            return Optional.empty();
        }
        return Optional.of(Range.closed((double) power.lowerEndpoint(), (double) power.upperEndpoint()));
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
        List<TableEvent> events;
        DeviceId deviceId = handler().data().deviceId();
        List<PortNumber> ports = new ArrayList<>();

        try {
            OID[] columnOIDs = {new OID(OPM_POWER_OID)};
            events = getTable(handler(), columnOIDs);
        } catch (IOException e) {
            log.error("Error reading opm power table for device {} exception {}", deviceId, e);
            return ports;
        }

        if (events == null) {
            log.error("Error reading opm power table for device {}", deviceId);
            return ports;
        }

        for (TableEvent event : events) {
            if (event == null) {
                log.error("Error reading event for device {}", deviceId);
                continue;
            }
            VariableBinding[] columns = event.getColumns();
            if (columns == null) {
                log.error("Error reading columns for device {}", deviceId);
                continue;
            }

            VariableBinding opmColumn = columns[0];
            if (opmColumn == null) {
                continue;
            }

            int port = event.getIndex().last();
            int opm = opmColumn.getVariable().toInt();
            if (opm == 0) {
                continue;
            }

            ports.add(PortNumber.portNumber(port));
        }

        return ports;
    }

    private Long acquireTargetPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            log.warn("Channel power is not applicable.");
            return null;
        }
        log.debug("Get port{} target power...", port);

        DeviceId deviceId = handler().data().deviceId();
        Long targetPower = 0L;
        try {
            targetPower = Long.valueOf(get(handler(), VOA_LEVEL_OID + "." + port.toLong()).toInt());
        } catch (IOException e) {
            log.error("Error reading target power for device {} exception {}", deviceId, e);
        }
        return targetPower;
    }

    private Long acquireCurrentPower(PortNumber port, T component) {
        if (component instanceof OchSignal) {
            log.warn("Channel power is not applicable.");
            return null;
        }
        log.debug("Get port{} current power...", port);

        DeviceId deviceId = handler().data().deviceId();
        Long power = 0L;
        try {
            power = Long.valueOf(get(handler(), OPM_POWER_OID + "." + port.toLong()).toInt());
        } catch (IOException e) {
            log.error("Error reading current power for device {} exception {}", deviceId, e);
        }
        return power;
    }

    private boolean setPortTargetPower(PortNumber port, long power) {
        log.debug("Set port{} target power...", port);
        List<VariableBinding> vbs = new ArrayList<>();

        OID voaStateOid = new OID(VOA_STATE_OID + "." + port.toLong());
        Variable voaStateVar = new UnsignedInteger32(VOA_STATE_ABSOLUTE);
        VariableBinding voaStateVb = new VariableBinding(voaStateOid, voaStateVar);
        vbs.add(voaStateVb);

        OID voaLevelOid = new OID(VOA_LEVEL_OID + "." + port.toLong());
        Variable voaLevelVar = new UnsignedInteger32(power);
        VariableBinding voaLevelVb = new VariableBinding(voaLevelOid, voaLevelVar);
        vbs.add(voaLevelVb);

        DeviceId deviceId = handler().data().deviceId();
        try {
            set(handler(), vbs);
        } catch (IOException e) {
            log.error("Error writing ports table for device {} exception {}", deviceId, e);
            return false;
        }
        return true;
    }

    private Range<Long> getPowerRange() {
        return POWER_RANGE;
    }

    private Range<Long> getTxPowerRange(PortNumber port, T component) {
        if (component instanceof Direction) {
            log.debug("Get target port{} power range...", port);
            return getPowerRange();
        } else {
            log.warn("Channel power is not applicable.");
            return null;
        }
    }

    private Range<Long> getRxPowerRange(PortNumber port, T component) {
        log.debug("Get input port{} power range...", port);
        return getPowerRange();
    }
}
