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

package org.onosproject.segmentrouting;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.FlowObjectiveServiceAdapter;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;

import java.util.Map;

/**
 * Mock Flow Objective Service.
 */
public class MockFlowObjectiveService extends FlowObjectiveServiceAdapter {
    private Map<MockBridgingTableKey, MockBridgingTableValue> bridgingTable;
    private Map<Integer, TrafficTreatment> nextTable;

    MockFlowObjectiveService(Map<MockBridgingTableKey, MockBridgingTableValue> bridgingTable,
                             Map<Integer, TrafficTreatment> nextTable) {
        this.bridgingTable = bridgingTable;
        this.nextTable = nextTable;
    }

    @Override
    public void forward(DeviceId deviceId, ForwardingObjective forwardingObjective) {
        TrafficSelector selector = forwardingObjective.selector();
        TrafficTreatment treatment = nextTable.get(forwardingObjective.nextId());
        MacAddress macAddress = ((EthCriterion) selector.getCriterion(Criterion.Type.ETH_DST)).mac();
        VlanId vlanId = ((VlanIdCriterion) selector.getCriterion(Criterion.Type.VLAN_VID)).vlanId();

        boolean popVlan = treatment.allInstructions().stream()
                .filter(instruction -> instruction.type().equals(Instruction.Type.L2MODIFICATION))
                .anyMatch(instruction -> ((L2ModificationInstruction) instruction).subtype()
                        .equals(L2ModificationInstruction.L2SubType.VLAN_POP));
        PortNumber portNumber = treatment.allInstructions().stream()
                .filter(instruction -> instruction.type().equals(Instruction.Type.OUTPUT))
                .map(instruction -> ((Instructions.OutputInstruction) instruction).port()).findFirst().orElse(null);
        if (portNumber == null) {
            throw new IllegalArgumentException();
        }

        Objective.Operation op = forwardingObjective.op();

        MockBridgingTableKey btKey = new MockBridgingTableKey(deviceId, macAddress, vlanId);
        MockBridgingTableValue btValue = new MockBridgingTableValue(popVlan, portNumber);

        if (op.equals(Objective.Operation.ADD)) {
            bridgingTable.put(btKey, btValue);
        } else if (op.equals(Objective.Operation.REMOVE)) {
            bridgingTable.remove(btKey, btValue);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
