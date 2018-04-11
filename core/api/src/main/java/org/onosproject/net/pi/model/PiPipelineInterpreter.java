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
     * Returns a PI match field ID that is equivalent to the given criterion type, if present. If not present, it means
     * that the given criterion type is not supported by this interpreter.
     *
     * @param type criterion type
     * @return optional match field ID
     */
    Optional<PiMatchFieldId> mapCriterionType(Criterion.Type type);

    /**
     * Returns the criterion type that is equivalent to the given PI match field ID, if present. If not present, it
     * means that the given match field is not supported by this interpreter.
     *
     * @param fieldId match field ID
     * @return optional criterion type
     */
    Optional<Criterion.Type> mapPiMatchFieldId(PiMatchFieldId fieldId);

    /**
     * Returns a PI table ID equivalent to the given numeric table ID (as in {@link
     * org.onosproject.net.flow.FlowRule#tableId()}). If not present, it means that the given integer table ID cannot be
     * mapped to any table of the pipeline model.
     *
     * @param flowRuleTableId a numeric table ID
     * @return PI table ID
     */
    Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId);

    /**
     * Returns an integer table ID equivalent to the given PI table ID. If not present, it means that the given PI table
     * ID cannot be mapped to any integer table ID, because such mapping would be meaningless or because such PI table
     * ID is not defined by the pipeline model.
     *
     * @param piTableId PI table ID
     * @return numeric table ID
     */
    Optional<Integer> mapPiTableId(PiTableId piTableId);

    /**
     * Returns an action of a PI pipeline that is functionally equivalent to the given traffic treatment for the given
     * table.
     *
     * @param treatment traffic treatment
     * @param piTableId PI table ID
     * @return action object
     * @throws PiInterpreterException if the treatment cannot be mapped to any PI action
     */
    PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId)
            throws PiInterpreterException;

    /**
     * Returns a collection of PI packet operations equivalent to the given outbound packet instance.
     *
     * @param packet outbound packet
     * @return collection of PI packet operations
     * @throws PiInterpreterException if the packet treatments cannot be executed by this pipeline
     */
    Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet)
            throws PiInterpreterException;

    /**
     * Returns an inbound packet equivalent to the given PI packet operation.
     *
     * @param packetOperation packet operation
     * @return inbound packet
     * @throws PiInterpreterException if the packet operation cannot be mapped to an inbound packet
     */
    InboundPacket mapInboundPacket(PiPacketOperation packetOperation)
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
