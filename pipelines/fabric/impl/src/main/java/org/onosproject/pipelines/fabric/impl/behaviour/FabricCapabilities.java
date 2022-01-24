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

package org.onosproject.pipelines.fabric.impl.behaviour;

import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.pipelines.fabric.FabricConstants;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.CPU_PORT_TXT;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_DOWNLINK_PDRS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Representation of the capabilities of a given fabric pipeconf.
 */
public class FabricCapabilities {

    private final Logger log = getLogger(getClass());

    private final PiPipeconf pipeconf;

    public FabricCapabilities(PiPipeconf pipeconf) {
        this.pipeconf = checkNotNull(pipeconf);
    }

    public boolean hasHashedTable() {
        return pipeconf.pipelineModel()
                .table(FabricConstants.FABRIC_INGRESS_NEXT_HASHED).isPresent();
    }

    public Optional<Long> cpuPort() {
        // This is probably brittle, but needed to dynamically get the CPU port
        // for different platforms.
        if (!pipeconf.extension(CPU_PORT_TXT).isPresent()) {
            log.warn("Missing {} extension in pipeconf {}", CPU_PORT_TXT, pipeconf.id());
            return Optional.empty();
        }
        try {
            final InputStream stream = pipeconf.extension(CPU_PORT_TXT).get();
            final BufferedReader buff = new BufferedReader(
                    new InputStreamReader(stream));
            final String str = buff.readLine();
            buff.close();
            if (str == null) {
                log.error("Empty CPU port file for {}", pipeconf.id());
                return Optional.empty();
            }
            try {
                return Optional.of(Long.parseLong(str));
            } catch (NumberFormatException e) {
                log.error("Invalid CPU port for {}: {}", pipeconf.id(), str);
                return Optional.empty();
            }
        } catch (IOException e) {
            log.error("Unable to read CPU port file of {}: {}",
                    pipeconf.id(), e.getMessage());
            return Optional.empty();
        }
    }

    public boolean supportDoubleVlanTerm() {
        if (pipeconf.pipelineModel()
                .table(FabricConstants.FABRIC_INGRESS_PRE_NEXT_NEXT_VLAN).isPresent()) {
            return pipeconf.pipelineModel().table(FabricConstants.FABRIC_INGRESS_PRE_NEXT_NEXT_VLAN)
                    .get().action(FabricConstants.FABRIC_INGRESS_PRE_NEXT_SET_DOUBLE_VLAN)
                    .isPresent();
        }
        return false;
    }

    /**
     * Returns true if the pipeconf supports UPF capabilities, false otherwise.
     *
     * @return boolean
     */
    public boolean supportUpf() {
        return pipeconf.pipelineModel()
                .table(FABRIC_INGRESS_SPGW_DOWNLINK_PDRS)
                .isPresent();
    }

    /**
     * Returns true if the pipeconf supports BNG user plane capabilities, false
     * otherwise.
     *
     * @return boolean
     */
    public boolean supportBng() {
        return pipeconf.pipelineModel()
                .counter(FabricConstants.FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_C_LINE_RX)
                .isPresent();
    }

    /**
     * Returns the maximum number of BNG lines supported, or 0 if this pipeconf
     * does not support BNG capabilities.
     *
     * @return maximum number of lines supported
     */
    public long bngMaxLineCount() {
        if (!supportBng()) {
            return 0;
        }
        return pipeconf.pipelineModel()
                .counter(FabricConstants.FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_C_LINE_RX)
                .orElseThrow().size();
    }
}
