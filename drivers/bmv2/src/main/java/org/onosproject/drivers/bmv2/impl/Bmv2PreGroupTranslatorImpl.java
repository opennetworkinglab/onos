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
 *
 */

package org.onosproject.drivers.bmv2.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.onosproject.core.GroupId;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreGroup;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreJsonGroups;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreNode;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Implementation of BMv2 PRE group translation logic.
 */
public final class Bmv2PreGroupTranslatorImpl {

    private static final int BMV2_PORT_MAP_SIZE = 256;

    //hidden constructor
    private Bmv2PreGroupTranslatorImpl() {
    }

    /**
     * Returns a BMv2 PRE group equivalent to given group.
     *
     * @param group group
     * @return a BMv2 PRE group
     */
    public static Bmv2PreGroup translate(Group group) {
        if (!group.type().equals(GroupDescription.Type.ALL)) {
            throw new IllegalStateException("Unable to translate the group to BMv2 PRE group." +
                                               "A BMv2 PRE group is to be of ALL type. GroupId="
                                               + group.id());
        }

        Bmv2PreGroup.Bmv2PreGroupBuilder bmv2PreGroupBuilder = Bmv2PreGroup.builder();
        bmv2PreGroupBuilder.withGroupId(group.id().id());
        // RID is generated per an MG since L2 broadcast can be considered as a MG that have a single RID.
        // for simplicity RID will be assigned to zero for any node in an MG.
        int replicationId = 0;

        Set<PortNumber> outPorts = Sets.newHashSet();
        group.buckets().buckets().forEach(groupBucket -> {
            //get output instructions of the bucket
            Set<Instructions.OutputInstruction> outputInstructions = getOutputInstructions(groupBucket.treatment());
            //check validity of output instructions
            checkOutputInstructions(group.id(), outputInstructions);
            outPorts.add(outputInstructions.iterator().next().port());
        });
        validatePorts(outPorts);
        bmv2PreGroupBuilder.addNode(Bmv2PreNode.builder()
                                            .withRid(replicationId)
                                            .withPortMap(buildPortMap(outPorts))
                                            .build());
        return bmv2PreGroupBuilder.build();
    }

    /**
     * Converts a PRE group list in JSON notation to list of Bmv2PreGroups.
     *
     * @param groupListJson group list string ing JSON notation
     * @return list of Bmv2PreGroups
     * @throws IOException in case JSON string can not be parsed
     */
    public static List<Bmv2PreGroup> translate(String groupListJson) throws IOException {
        List<Bmv2PreGroup> preGroups = new ArrayList<>();
        if (groupListJson == null) {
            return preGroups;
        }
        ObjectMapper mapper = new ObjectMapper();
        Bmv2PreJsonGroups bmv2PreJsonGroups = mapper.readValue(groupListJson, Bmv2PreJsonGroups.class);

        Bmv2PreGroup.Bmv2PreGroupBuilder bmv2PreGroupBuilder;
        for (Bmv2PreJsonGroups.Mgrp mgrp : bmv2PreJsonGroups.mgrps) {

            bmv2PreGroupBuilder = Bmv2PreGroup.builder();
            bmv2PreGroupBuilder.withGroupId(mgrp.id);

            for (int l1handleId : mgrp.l1handles) {
                Bmv2PreJsonGroups.L1Handle l1handle = getL1Handle(l1handleId, bmv2PreJsonGroups.l1handles);
                if (l1handle == null) {
                    continue;
                }
                Bmv2PreJsonGroups.L2Handle l2handle = getL2Handle(l1handle.l2handle, bmv2PreJsonGroups.l2handles);
                if (l2handle == null) {
                    continue;
                }
                bmv2PreGroupBuilder.addNode(Bmv2PreNode.builder()
                                                    .withRid(l1handle.rid)
                                                    .withPortMap(buildPortMap(l2handle.ports))
                                                    .withL1Handle(l1handleId)
                                                    .build());
            }
            preGroups.add(bmv2PreGroupBuilder.build());
        }
        return preGroups;
    }

    /**
     * Retrieves L1Handle object pointed by given L1 handle pointer from L1 handles list.
     *
     * @param l1handlePointer pointer to a L1 handle
     * @param l1handles       list of L1 handles
     * @return L1 handle object if exists; null otherwise
     */
    private static Bmv2PreJsonGroups.L1Handle getL1Handle(int l1handlePointer,
                                                          Bmv2PreJsonGroups.L1Handle[] l1handles) {
        for (Bmv2PreJsonGroups.L1Handle l1Handle : l1handles) {
            if (l1handlePointer == l1Handle.handle) {
                return l1Handle;
            }
        }
        return null;
    }

    /**
     * Retrieves L2Handle object pointed by given L2 handle pointer from L2 handles list.
     *
     * @param l2handlePointer pointer to a L2 handle
     * @param l2handles       list of L2 handles
     * @return L2 handle object if exists; null otherwise
     */
    private static Bmv2PreJsonGroups.L2Handle getL2Handle(int l2handlePointer,
                                                          Bmv2PreJsonGroups.L2Handle[] l2handles) {
        for (Bmv2PreJsonGroups.L2Handle l2handle : l2handles) {
            if (l2handlePointer == l2handle.handle) {
                return l2handle;
            }
        }
        return null;
    }

    /**
     * Builds a port map string composing of 1 and 0s.
     * BMv2 API reads a port map from most significant to least significant char.
     * Index of 1s indicates port numbers.
     * For example, if port numbers are 4,3 and 1, the generated port map string looks like 11010.
     *
     * @param outPorts set of output port numbers
     * @return port map string
     */
    private static String buildPortMap(Set<PortNumber> outPorts) {
        //Sorts port numbers in descending order
        SortedSet<PortNumber> outPortsSorted = new TreeSet<>((o1, o2) -> (int) (o2.toLong() - o1.toLong()));
        outPortsSorted.addAll(outPorts);
        PortNumber biggestPort = outPortsSorted.iterator().next();
        int portMapSize = (int) biggestPort.toLong() + 1;
        //init and fill port map with zero characters
        char[] portMap = new char[portMapSize];
        Arrays.fill(portMap, '0');
        //fill in the ports with 1
        outPortsSorted.forEach(portNumber -> portMap[portMapSize - (int) portNumber.toLong() - 1] = '1');
        return String.valueOf(portMap);
    }

    /**
     * Builds a port map string composing of 1 and 0s.
     * BMv2 API reads a port map from most significant to least significant char.
     * The index of 1s indicates port numbers.
     * For example, if port numbers are 4,3 and 1, the generated port map string looks like 11010.
     *
     * @param portArr array of output port numbers
     * @return port map string
     */
    private static String buildPortMap(int[] portArr) {
        Set<PortNumber> outPorts = new HashSet<>();
        for (int port : portArr) {
            outPorts.add(PortNumber.portNumber(port));
        }
        return buildPortMap(outPorts);
    }

    /**
     * Retrieves output instructions out of the instruction set of the given traffic treatment.
     *
     * @param trafficTreatment
     * @return set of output instructions
     */
    private static Set<Instructions.OutputInstruction> getOutputInstructions(TrafficTreatment trafficTreatment) {
        if (trafficTreatment == null ||
                trafficTreatment.allInstructions() == null) {
            return Sets.newHashSet();
        }
        Set<Instructions.OutputInstruction> resultList = Sets.newHashSet();
        trafficTreatment.allInstructions().forEach(instruction -> {
            if (instruction instanceof Instructions.OutputInstruction) {
                resultList.add((Instructions.OutputInstruction) instruction);
            }
        });
        return resultList;
    }

    /**
     * Checks validity of output instructions of a bucket.
     * A bucket of a an ALL group must only have one output instruction.
     * Other conditions can not pass the validation.
     *
     * @param groupId            group identifier
     * @param outputInstructions set of output instructions
     * @throws RuntimeException if the instructions can not be validated
     */
    private static void checkOutputInstructions(GroupId groupId,
                                                Set<Instructions.OutputInstruction> outputInstructions) {
        if (outputInstructions.isEmpty()) {
            throw new IllegalStateException(String.format("Group bucket contains no output instruction. GroupId=%s",
                                                     groupId));
        }
        if (outputInstructions.size() != 1) {
            throw new IllegalStateException(String.format("Group bucket contains more than one output instructions. " +
                                                             "Only one is supported. GroupId=%s", groupId));
        }
    }

    /**
     * Checks whether a port number is a valid physical BMv2 port or not.
     * If not, throws RuntimeException; does nothing otherwise.
     *
     * @param portNumber port number
     * @throws RuntimeException iff the port number can not be validated
     */
    private static void validatePort(PortNumber portNumber) {
        if (portNumber.toLong() < 0 || portNumber.toLong() >= BMV2_PORT_MAP_SIZE) {
            throw new IllegalStateException(String.format("Port number %d is not a valid BMv2 physical port number." +
                                                             "Valid port range is [0,255]", portNumber.toLong()));
        }
    }

    /**
     * Checks whether a port number is a valid physical BMv2 port or not.
     * If not, throws RuntimeException; does nothing otherwise.
     *
     * @param portNumbers port number set
     * @throws RuntimeException iff a port number can not be validated
     */
    private static void validatePorts(Set<PortNumber> portNumbers) {
        //validate one by one
        for (PortNumber portNumber : portNumbers) {
            validatePort(portNumber);
        }
    }
}