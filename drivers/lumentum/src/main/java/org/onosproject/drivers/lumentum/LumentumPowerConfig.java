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

package org.onosproject.drivers.lumentum;

import com.google.common.collect.Range;
import org.onosproject.net.PortNumber;
import org.onosproject.net.OchSignal;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class LumentumPowerConfig<T> extends AbstractHandlerBehaviour
        implements PowerConfig<T> {

    // log
    private final Logger log = getLogger(getClass());


    @Override
    public Optional<Long> getTargetPower(PortNumber port, T component) {
        return Optional.ofNullable(acquireTargetPower(port, component));
    }

    @Override
    public void setTargetPower(PortNumber port, T component, long power) {
        if (component instanceof OchSignal) {
            setConnectionTargetPower(port, (OchSignal) component, power);
        } else {
            setPortTargetPower(port, power);
        }
    }

    @Override
    public Optional<Long> currentPower(PortNumber port, T component) {
        return Optional.ofNullable(acquireCurrentPower(port, component));
    }

    @Override
    public Optional<Range<Long>> getTargetPowerRange(PortNumber portNumber, T component) {

        log.debug("Lumentum getTargetPowerRange {}", portNumber);
        //TODO automatically read if a port is input or output

        Set<PortNumber> outputPorts = new HashSet<>();

        //Output port on the optical-line
        outputPorts.add(PortNumber.portNumber(3001));

        //Output ports of the demux module (module=2)
        IntStream.rangeClosed(5201, 5220)
                .forEach(i -> outputPorts.add(PortNumber.portNumber(i)));

        if (outputPorts.contains(portNumber)) {
            return Optional.ofNullable(getTxPowerRange(portNumber, component));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Range<Long>> getInputPowerRange(PortNumber portNumber, T component) {

        log.debug("Lumentum getInputPowerRange {}", portNumber);
        //TODO automatically read if a port is input or output

        Set<PortNumber> inputPorts = new HashSet<>();

        //Input port on the optical-line
        inputPorts.add(PortNumber.portNumber(3001));

        //Input ports of the mux module (module=1)
        IntStream.rangeClosed(4101, 4120)
                .forEach(i -> inputPorts.add(PortNumber.portNumber(i)));

        if (inputPorts.contains(portNumber)) {
            return Optional.ofNullable(getRxPowerRange(portNumber, component));
        }
        return Optional.empty();
    }

    //TODO implement actual get configuration from the device
    //This is used by ROADM application to retrieve attenuation parameter, with T instanceof OchSignal
    private Long acquireTargetPower(PortNumber port, T component) {
        log.info("Lumentum get port {} target power...", port);

        if (component instanceof OchSignal) {
            //FIXME include port in the filter
            LumentumConnection conn = LumentumNetconfRoadmFlowRuleProgrammable.CONNECTION_SET.stream()
                    .filter(c -> c.ochSignal == component)
                    .findFirst()
                    .orElse(null);

            if (conn == null) {
                log.info("Lumentum NETCONF fail to retrieve attenuation signal {} port {}", component, port);
                return 0L;
            } else {
                log.info("Lumentum NETCONF on port {} attenuation {}", port, conn.attenuation);
                return ((long) (conn.attenuation * 100));
            }
        }

        return 0L;
    }

    //TODO implement actual get configuration from the device
    //This is used by ROADM application to retrieve attenuation parameter, with T instanceof OchSignal
    private Long acquireCurrentPower(PortNumber port, T component) {
        log.info("Lumentum get port {} current power...", port);

        if (component instanceof OchSignal) {
            //FIXME include port in the filter
            LumentumConnection conn = LumentumNetconfRoadmFlowRuleProgrammable.CONNECTION_SET.stream()
                    .filter(c -> c.ochSignal == component)
                    .findFirst()
                    .orElse(null);

            if (conn == null) {
                log.info("Lumentum NETCONF fail to retrieve power signal {} port {}", component, port);
                return 0L;
            } else {
                log.info("Lumentum NETCONF on port {} power {}", port, conn.inputPower);
                return ((long) (conn.inputPower * 100));
            }
        }

        return 0L;
    }

    //TODO implement actual get configuration from the device
    //Return PowerRange -60 dBm to 60 dBm
    private Range<Long> getTxPowerRange(PortNumber port, T component) {
        log.debug("Get port {} tx power range...", port);
        return Range.closed(-60L, 60L);
    }

    //TODO implement actual get configuration from the device
    //Return PowerRange -60dBm to 60 dBm
    private Range<Long> getRxPowerRange(PortNumber port, T component) {
        log.debug("Get port {} rx power range...", port);
        return Range.closed(-60L, 60L);
    }

    //TODO implement configuration on the device
    //Nothing to do
    private void setPortTargetPower(PortNumber port, long power) {
        log.debug("Set port {} target power {}", port, power);
    }

    //TODO implement configuration on the device
    //Nothing to do
    private void setConnectionTargetPower(PortNumber port, OchSignal signal, long power) {
        log.debug("Set connection target power {} ochsignal {} port {}", power, signal, port);
    }
}