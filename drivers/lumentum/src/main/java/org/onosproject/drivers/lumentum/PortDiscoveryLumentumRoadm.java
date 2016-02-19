/*
 * Copyright 2016 Open Networking Laboratory
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

import com.google.common.collect.Lists;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.device.OmsPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TreeEvent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the ports of a Lumentum SDN ROADM device using SNMP.
 */
public class PortDiscoveryLumentumRoadm extends AbstractHandlerBehaviour
        implements PortDiscovery {

    private final Logger log = getLogger(PortDiscoveryLumentumRoadm.class);

    private static final String CTRL_PORT_STATE = ".1.3.6.1.4.1.46184.1.4.1.1.3.";

    private LumentumSnmpDevice snmp;

    @Override
    public List<PortDescription> getPorts() {
        try {
            snmp = new LumentumSnmpDevice(handler().data().deviceId());
        } catch (IOException e) {
            log.error("Failed to connect to device: ", e);

            return Collections.emptyList();
        }

        List<PortDescription> ports = Lists.newLinkedList();

        OID[] oids = {
                new OID(CTRL_PORT_STATE + "1"),
                new OID(CTRL_PORT_STATE + "2")
        };

        for (OID oid : oids) {

            for (TreeEvent event : snmp.get(oid)) {
                if (event != null) {
                    VariableBinding[] varBindings = event.getVariableBindings();
                    for (VariableBinding varBinding : varBindings) {
                        if (varBinding.getVariable().toInt() == 1) {
                            int portNumber = varBinding.getOid().removeLast();
                            int portDirection = varBinding.getOid().removeLast();
                            SparseAnnotations ann = DefaultAnnotations.builder()
                                    .set(AnnotationKeys.PORT_NAME, portDirection + "-" + portNumber)
                                    .build();
                            PortDescription p = new OmsPortDescription(
                                    PortNumber.portNumber(ports.size() + 1),
                                    true,
                                    LumentumSnmpDevice.START_CENTER_FREQ,
                                    LumentumSnmpDevice.END_CENTER_FREQ,
                                    LumentumSnmpDevice.CHANNEL_SPACING.frequency(),
                                    ann);
                            ports.add(p);
                        }
                    }
                }
            }
        }

        // Create LINE IN and LINE OUT ports as these are not reported through SNMP
        SparseAnnotations annLineIn = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, "LINE IN")
                .build();
        ports.add(new OmsPortDescription(
                PortNumber.portNumber(ports.size() + 1),
                true,
                LumentumSnmpDevice.START_CENTER_FREQ,
                LumentumSnmpDevice.END_CENTER_FREQ,
                LumentumSnmpDevice.CHANNEL_SPACING.frequency(),
                annLineIn
        ));

        SparseAnnotations annLineOut = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, "LINE OUT")
                .build();
        ports.add(new OmsPortDescription(
                PortNumber.portNumber(ports.size() + 1),
                true,
                LumentumSnmpDevice.START_CENTER_FREQ,
                LumentumSnmpDevice.END_CENTER_FREQ,
                LumentumSnmpDevice.CHANNEL_SPACING.frequency(),
                annLineOut
        ));

        return ports;
    }
}


