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

package org.onosproject.net.pi.model;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiPacketOperation;

import java.util.Collection;
import java.util.Optional;

/**
 * An interpreter of a PI pipeline model.
 */
@Beta
public interface PiPipelineInterpreter extends HandlerBehaviour {

    /**
     * Returns a PI match field ID that is equivalent to the given criterion
     * type, if present. If not present, it means that the given criterion type
     * is not supported by this interpreter.
     *
     * @param type criterion type
     * @return optional match field ID
     */
    Optional<PiMatchFieldId> mapCriterionType(Criterion.Type type);

    /**
     * Returns a PI table ID equivalent to the given numeric table ID (as in
     * {@link org.onosproject.net.flow.FlowRule#tableId()}). If not present, it
     * means that the given integer table ID cannot be mapped to any table of
     * the pipeline model.
     *
     * @param flowRuleTableId a numeric table ID
     * @return PI table ID
     */
    // FIXME: remove this method. The only place where this mapping seems useful
    // is when using the default single table pipeliner which produces flow
    // rules for table 0. Instead, PI pipeliners should provide a mapping to a
    // specific PiTableId even when mapping to a single table.
    Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId);

    /**
     * Returns an action of a PI pipeline that is functionally equivalent to the
     * given traffic treatment for the given table.
     *
     * @param treatment traffic treatment
     * @param piTableId PI table ID
     * @return action object
     * @throws PiInterpreterException if the treatment cannot be mapped to any
     *                                PI action
     */
    PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId)
            throws PiInterpreterException;

    /**
     * Returns a collection of PI packet operations equivalent to the given
     * outbound packet instance.
     *
     * @param packet outbound packet
     * @return collection of PI packet operations
     * @throws PiInterpreterException if the packet treatments cannot be
     *                                executed by this pipeline
     */
    Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet)
            throws PiInterpreterException;

    /**
     * Returns an inbound packet equivalent to the given PI packet-in operation
     * for the given device.
     *
     * @param packetOperation packet operation
     * @param deviceId        ID of the device that originated the packet-in
     * @return inbound packet
     * @throws PiInterpreterException if the packet operation cannot be mapped
     *                                to an inbound packet
     */
    InboundPacket mapInboundPacket(PiPacketOperation packetOperation, DeviceId deviceId)
            throws PiInterpreterException;

    /**
     * Maps the given logical port number to the data plane port ID (integer)
     * identifying the same port for this pipeconf, if such mapping is
     * possible.
     *
     * @param port port number
     * @return optional integer
     * @deprecated in ONOS 3.0 using {@link #mapLogicalPort} instead
     */
    @Deprecated
    default Optional<Integer> mapLogicalPortNumber(PortNumber port) {
        return Optional.empty();
    }

    /**
     * Maps the given logical port number to the data plane port ID (long)
     * identifying the same port for this pipeconf, if such mapping is
     * possible.
     *
     * @param port port number
     * @return optional long
     */
    default Optional<Long> mapLogicalPort(PortNumber port) {
        return mapLogicalPortNumber(port).map(integer -> (long) integer);
    }

    /**
     * If the given table allows for mutable default actions, this method
     * returns an action instance to be used when ONOS tries to remove a
     * different default action previously set.
     *
     * @param tableId table ID
     * @return optional default action
     */
    default Optional<PiAction> getOriginalDefaultAction(PiTableId tableId) {
        return Optional.empty();
    }

    /**
     * Signals that an error was encountered while executing the interpreter.
     */
    @Beta
    class PiInterpreterException extends Exception {
        public PiInterpreterException(String message) {
            super(message);
        }
    }
}
