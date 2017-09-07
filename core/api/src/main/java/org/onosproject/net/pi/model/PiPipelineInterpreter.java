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
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiCounterId;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableId;

import java.util.Collection;
import java.util.Optional;

/**
 * An interpreter of a protocol-independent pipeline model.
 */
@Beta
public interface PiPipelineInterpreter extends HandlerBehaviour {

    /**
     * Returns the protocol-independent header field identifier that is equivalent to the given criterion type, if
     * present. If not present, it means that the given criterion type is not supported by this interpreter.
     *
     * @param type criterion type
     * @return optional header field identifier
     */
    Optional<PiHeaderFieldId> mapCriterionType(Criterion.Type type);

    /**
     * Returns the criterion type that is equivalent to the given protocol-independent header field identifier, if
     * present. If not present, it means that the given field identifier is not supported by this interpreter.
     *
     * @param headerFieldId header field identifier
     * @return optional criterion type
     */
    Optional<Criterion.Type> mapPiHeaderFieldId(PiHeaderFieldId headerFieldId);

    /**
     * Returns a protocol-independent table id equivalent to the given numeric table id (as in {@link
     * org.onosproject.net.flow.FlowRule#tableId()}). If not present, it means that the given numeric table id cannot be
     * mapped to any table of the pipeline model.
     *
     * @param flowRuleTableId a numeric table id
     * @return a protocol-independent table id
     */
    Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId);

    /**
     * Returns a numeric table id (as in {@link org.onosproject.net.flow.FlowRule#tableId()}) equivalent to the given
     * protocol-independent table id. If not present, it means that the given protocol-independent table id refers to a
     * table that does not exist, or that cannot be used for flow rule operations.
     *
     * @param piTableId protocol-independent table id
     * @return numeric table id
     */
    Optional<Integer> mapPiTableId(PiTableId piTableId);

    /**
     * Returns an action of a protocol-independent pipeline that is functionally equivalent to the given ONOS traffic
     * treatment for the given table.
     *
     * @param treatment a ONOS traffic treatment
     * @param piTableId PI table identifier
     * @return an action object
     * @throws PiInterpreterException if the treatment cannot be mapped to any table action
     */
    PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId)
            throws PiInterpreterException;

    /**
     * Returns a protocol-independent direct counter identifier for the given table, if present. If not present, it
     * means that the given table does not support direct counters.
     *
     * @param piTableId table identifier
     * @return optional direct counter identifier
     */
    Optional<PiCounterId> mapTableCounter(PiTableId piTableId);

    /**
     * Returns a collection of packet operations equivalent to the given OutboundPacket.
     *
     * @param packet a ONOS outbound packet
     * @return a collection of packet operations
     * @throws PiInterpreterException if the packet treatments cannot be mapped to any metadata
     */
    Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet)
            throws PiInterpreterException;

    /**
     * Returns a InboundPacket equivalent to the given packet operation.
     *
     * @param deviceId          the device that originated the packet-in
     * @param packetInOperation the packet operation
     * @return an ONOS inbound packet
     * @throws PiInterpreterException if the port can't be extracted from the packet metadata
     */
    InboundPacket mapInboundPacket(DeviceId deviceId, PiPacketOperation packetInOperation)
            throws PiInterpreterException;

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