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
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.OchSignal;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.onosproject.net.flow.FlowRule;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class LumentumPowerConfig<T> extends AbstractHandlerBehaviour
        implements PowerConfig<T> {

    // log
    private final Logger log = getLogger(getClass());


    @Override
    public Optional<Double> getTargetPower(PortNumber port, T component) {
        return Optional.ofNullable(acquireTargetPower(port, component));
    }

    //Used by the ROADM app to set the "attenuation" parameter
    @Override
    public void setTargetPower(PortNumber port, T component, double power) {
        if (component instanceof OchSignal) {
            setConnectionTargetPower(port, (OchSignal) component, power);
        } else {
            setPortTargetPower(port, power);
        }
    }

    @Override
    public Optional<Double> currentPower(PortNumber port, T component) {
        return Optional.ofNullable(acquireCurrentPower(port, component));
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber portNumber, T component) {

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
    public Optional<Range<Double>> getInputPowerRange(PortNumber portNumber, T component) {

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
    //The ROADM app expresses the attenuation in 0.01 dB units
    private Double acquireTargetPower(PortNumber port, T component) {
        log.debug("Lumentum get port {} target power...", port);

        if (component instanceof OchSignal) {

            Set<FlowRule> rules = getConnectionCache().get(did());
            FlowRule rule;

            if (rules == null) {
                log.error("Lumentum NETCONF fail to retrieve attenuation signal {} port {}", component, port);
                return 0.0;
            } else {
                rule = rules.stream()
                        .filter(c -> ((LumentumFlowRule) c).getOutputPort() == port)
                        .filter(c -> ((LumentumFlowRule) c).ochSignal() == component)
                        .findFirst()
                        .orElse(null);
            }

            if (rule == null) {
                log.error("Lumentum NETCONF fail to retrieve attenuation signal {} port {}", component, port);
                return 0.0;
            } else {
                log.debug("Lumentum NETCONF on port {} attenuation {}", port,
                        (((LumentumFlowRule) rule).attenuation * 100));
                return ((LumentumFlowRule) rule).attenuation * 100;
            }
        }

        return 0.0;
    }

    //TODO implement actual get configuration from the device
    //This is used by ROADM application to retrieve power parameter, with T instanceof OchSignal
    private Double acquireCurrentPower(PortNumber port, T component) {
        log.debug("Lumentum get port {} current power...", port);

        if (component instanceof OchSignal) {

            Set<FlowRule> rules = getConnectionCache().get(did());
            FlowRule rule;

            if (rules == null) {
                log.error("Lumentum NETCONF fail to retrieve power signal {} port {}", component, port);
                return 0.0;
            } else {
                rule = rules.stream()
                        .filter(c -> ((LumentumFlowRule) c).getInputPort() == port)
                        .filter(c -> ((LumentumFlowRule) c).ochSignal() == component)
                        .findFirst()
                        .orElse(null);
            }

            if (rule == null) {
                log.error("Lumentum NETCONF fail to retrieve power signal {} port {}", component, port);
                return 0.0;
            } else {
                log.debug("Lumentum NETCONF on port {} power {}", port, (((LumentumFlowRule) rule).inputPower));
                return ((double) (((LumentumFlowRule) rule).inputPower * 100));
            }
        }

        return 0.0;
    }

    //TODO implement actual get configuration from the device
    //Return PowerRange -60 dBm to 60 dBm
    private Range<Double> getTxPowerRange(PortNumber port, T component) {
        log.debug("Get port {} tx power range...", port);
        return Range.closed(-60.0, 60.0);
    }

    //TODO implement actual get configuration from the device
    //Return PowerRange -60dBm to 60 dBm
    private Range<Double> getRxPowerRange(PortNumber port, T component) {
        log.debug("Get port {} rx power range...", port);
        return Range.closed(-60.0, 60.0);
    }

    //TODO implement configuration on the device
    //Nothing to do
    private void setPortTargetPower(PortNumber port, double power) {
        log.debug("Set port {} target power {}", port, power);
    }

    //Used by the ROADM app to set the "attenuation" parameter
    private void setConnectionTargetPower(PortNumber port, OchSignal signal, double power) {
        log.debug("Set connection target power {} ochsignal {} port {}", power, signal, port);

        Set<FlowRule> rules = getConnectionCache().get(did());
        FlowRule rule = null;

        if (rules == null) {
            log.error("Lumentum NETCONF fail to retrieve power signal {} port {}", signal, port);
        } else {
            rule = rules.stream()
                    .filter(c -> ((LumentumFlowRule) c).getOutputPort() == port)
                    .filter(c -> ((LumentumFlowRule) c).ochSignal() == signal)
                    .findFirst()
                    .orElse(null);
        }

        if (rule == null) {
            log.error("Lumentum NETCONF fail to retrieve attenuation signal {} port {}", signal, port);
        } else {
            log.debug("Lumentum NETCONF setting attenuation {} on port {} signal {}", power, port, signal);

            int moduleId = ((LumentumFlowRule) rule).getConnectionModule();
            int connId = ((LumentumFlowRule) rule).getConnectionId();

            editConnection(moduleId, connId, power);
        }
    }


    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }

    //Following Lumentum documentation <edit-config> operation to edit connection parameter
    //Currently only edit the "attenuation" parameter
    private boolean editConnection(int moduleId, int connectionId, double attenuation) {

        double attenuationDouble = ((double) attenuation);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append("<edit-config>" + "\n");
        stringBuilder.append("<target>" + "\n");
        stringBuilder.append("<running/>" + "\n");
        stringBuilder.append("</target>" + "\n");
        stringBuilder.append("<config>" + "\n");
        stringBuilder.append("<connections xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append("<connection>" + "\n");
        stringBuilder.append("" +
                "<dn>ne=1;chassis=1;card=1;module=" + moduleId + ";connection=" + connectionId + "</dn>" + "\n");
        stringBuilder.append("<config>" + "\n");
        stringBuilder.append("<attenuation>" + attenuationDouble + "</attenuation>" + "\n");
        stringBuilder.append("</config>" + "\n");
        stringBuilder.append("</connection>" + "\n");
        stringBuilder.append("</connections>" + "\n");
        stringBuilder.append("</config>" + "\n");
        stringBuilder.append("</edit-config>" + "\n");
        stringBuilder.append("</rpc>" + "\n");

        log.info("Lumentum ROADM20 - edit-connection sent to device {}", did());
        log.debug("Lumentum ROADM20 - edit-connection sent to device {} {}", did(), stringBuilder);

        return editCrossConnect(stringBuilder.toString());
    }

    private boolean editCrossConnect(String xcString) {
        NetconfSession session = getNetconfSession();

        if (session == null) {
            log.error("Lumentum NETCONF - session not found for device {}", handler().data().deviceId());
            return false;
        }

        try {
            return session.editConfig(xcString);
        } catch (NetconfException e) {
            log.error("Failed to edit the CrossConnect edid-cfg for device {}",
                    handler().data().deviceId(), e);
            log.debug("Failed configuration {}", xcString);
            return false;
        }
    }

    /**
     * Helper method to get the device id.
     */
    private DeviceId did() {
        return data().deviceId();
    }

    /**
     * Helper method to get the Netconf session.
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(did()).getSession();
    }
}