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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.openroadm;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.flow.criteria.Criterion.Type.IN_PORT;
import static org.onosproject.net.flow.criteria.Criterion.Type.OCH_SIGID;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;

/**
 * Class that models a FlowRule in an OpenROADM context.
 *
 */
public class OpenRoadmFlowRule extends DefaultFlowRule {

    /**
     * Type of conenction.
     */
    public enum Type {
        EXPRESS_LINK, // Degree to Degree
        ADD_LINK,     // SRG to Degree
        DROP_LINK,    // Degree to SRG
        LOCAL         // SRG to SRG
    }

    private Type type;

    private PortNumber inPortNumber;

    private PortNumber outPortNumber;

    private OchSignal ochSignal;

    private OchSignalType ochSignalType;

    /**
     * Constructor. Build an OpenRoadm flow rule from the passed rule.
     *
     *  @param rule ONOS flow rule that we have to process
     *  @param linePorts List of ports that are line ports (degrees) used
     *   to know the Type of this connection.
     *
     * We store and construct attributes like interface names to support
     * this OpenROADM connection.
     */
    public OpenRoadmFlowRule(FlowRule rule, List<PortNumber> linePorts) {
        super(rule);

        TrafficSelector trafficSelector = rule.selector();
        PortCriterion pc = (PortCriterion) trafficSelector.getCriterion(IN_PORT);
        checkArgument(pc != null, "Missing IN_PORT Criterion");
        inPortNumber = pc.port();

        // Generally, Sigtype and ochSignal could be null. This would mean e.g. a
        // port switching connection.
        OchSignalCriterion osc = (OchSignalCriterion) trafficSelector.getCriterion(OCH_SIGID);
        // checkArgument(osc != null, "Missing OCH_SIGID Criterion");
        if (osc != null) {
            ochSignal = osc.lambda();
        }

        TrafficTreatment trafficTreatment = rule.treatment();
        List<Instruction> instructions = trafficTreatment.immediate();

        outPortNumber = instructions.stream()
                          .filter(i -> i.type() == Instruction.Type.OUTPUT)
                          .map(i -> ((OutputInstruction) i).port())
                          .findFirst()
                          .orElse(null);
        checkArgument(outPortNumber != null, "Missing OUTPUT Instruction");

        if (linePorts.contains(inPortNumber) && linePorts.contains(outPortNumber)) {
            type = Type.EXPRESS_LINK;
        }
        if (!linePorts.contains(inPortNumber) && linePorts.contains(outPortNumber)) {
            type = Type.ADD_LINK;
        }
        if (linePorts.contains(inPortNumber) && !linePorts.contains(outPortNumber)) {
            type = Type.DROP_LINK;
        }
        if (!linePorts.contains(inPortNumber) && !linePorts.contains(outPortNumber)) {
            type = Type.LOCAL;
        }
    }

    /**
     * Get the type of the connection.
     *
     * @return type (express, add, drop, local)
     */
    public Type type() {
        return type;
    }

    /**
     * Get the input port.
     *
     * @return the input port (connection source).
     */
    public PortNumber inPort() {
        return inPortNumber;
    }


    /**
     * Get the output port.
     *
     * @return the output port (connection destination).
     */
    public PortNumber outPort() {
        return outPortNumber;
    }


    /**
     * Get the OchSignal for this connection.
     *
     * @return the OchSignal.
     */
    public OchSignal ochSignal() {
        return ochSignal;
    }


    /**
     * Get the OchSignalType for this connection.
     *
     * @return the OchSignalType.
     */
    public OchSignalType ochSignalType() {
        return ochSignalType;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OpenRoadmFlowRule)) {
            return false;
        }
        OpenRoadmFlowRule that = (OpenRoadmFlowRule) o;
        return Objects.equals(this.inPortNumber, that.inPortNumber) &&
          Objects.equals(this.outPortNumber, that.outPortNumber) &&
          Objects.equals(this.ochSignal, that.ochSignal) &&
          Objects.equals(this.ochSignalType, that.ochSignalType) &&
          Objects.equals(this.type, that.type);
    }


    @Override
    public int hashCode() {
        return Objects.hash(inPortNumber, outPortNumber, ochSignal, ochSignalType, type);
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
          .add("type", type)
          .add("inPortNumber", inPortNumber)
          .add("outPortNumber", outPortNumber)
          .add("ochSignal", ochSignal)
          .add("ochSignalType", ochSignalType)
          .toString();
    }
}
