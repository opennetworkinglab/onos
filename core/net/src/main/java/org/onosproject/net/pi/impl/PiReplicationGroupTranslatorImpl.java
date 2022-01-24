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

package org.onosproject.net.pi.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.Instructions.TruncateInstruction;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiCloneSessionEntry;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiPreEntry;
import org.onosproject.net.pi.runtime.PiPreReplica;
import org.onosproject.net.pi.service.PiTranslationException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Implementation of replication group translation logic.
 */
final class PiReplicationGroupTranslatorImpl {

    private PiReplicationGroupTranslatorImpl() {
        // Hides constructor.
    }

    /**
     * Returns a PI PRE entry equivalent to the given group, for the given
     * pipeconf and device.
     * <p>
     * The passed group is expected to have type {@link GroupDescription.Type#ALL}
     * or {@link GroupDescription.Type#CLONE}.
     *
     * @param group    group
     * @param pipeconf pipeconf
     * @param device   device
     * @return PI PRE entry
     * @throws PiTranslationException if the group cannot be translated
     */
    static PiPreEntry translate(Group group, PiPipeconf pipeconf, Device device)
            throws PiTranslationException {

        checkNotNull(group);

        final List<OutputInstruction> outInstructions = Lists.newArrayList();
        int truncateMaxLen = PiCloneSessionEntry.DO_NOT_TRUNCATE;
        for (GroupBucket bucket : group.buckets().buckets()) {
            int numInstructionsInBucket = bucket.treatment().allInstructions().size();
            List<OutputInstruction> outputs =
                    getInstructions(bucket, Instruction.Type.OUTPUT, OutputInstruction.class);
            List<TruncateInstruction> truncates =
                    getInstructions(bucket, Instruction.Type.TRUNCATE, TruncateInstruction.class);
            if (outputs.size() != 1) {
                throw new PiTranslationException(
                        "support only groups with just one OUTPUT instruction per bucket");
            }
            outInstructions.add(outputs.get(0));
            if (truncates.size() != 0) {
                if (group.type() != GroupDescription.Type.CLONE) {
                    throw new PiTranslationException("only CLONE group support truncate instruction");
                }
                if (truncates.size() != 1) {
                    throw new PiTranslationException(
                            "support only groups with just one TRUNCATE instruction per bucket");
                }
                int truncateInstMaxLen = truncates.get(0).maxLen();
                if (truncateMaxLen != PiCloneSessionEntry.DO_NOT_TRUNCATE &&
                        truncateMaxLen != truncateInstMaxLen) {
                    throw new PiTranslationException("all TRUNCATE instruction must be the same in a CLONE group");
                }
                truncateMaxLen = truncateInstMaxLen;
            } else if (truncateMaxLen != PiCloneSessionEntry.DO_NOT_TRUNCATE) {
                // No truncate instruction found in this bucket, but previous bucket contains one.
                throw new PiTranslationException("all TRUNCATE instruction must be the same in a CLONE group");
            }
            if (numInstructionsInBucket != outputs.size() + truncates.size()) {
                throw new PiTranslationException("bucket contains unsupported instruction(s)");
            }
        }

        switch (group.type()) {
            case ALL:
                return PiMulticastGroupEntry.builder()
                        .withGroupId(group.id().id())
                        .addReplicas(getReplicas(outInstructions, device))
                        .build();
            case CLONE:
                return PiCloneSessionEntry.builder()
                        .withSessionId(group.id().id())
                        .addReplicas(getReplicas(outInstructions, device))
                        .withMaxPacketLengthBytes(truncateMaxLen)
                        .build();
            default:
                throw new PiTranslationException(format(
                        "group type %s not supported", group.type()));
        }
    }

    private static Set<PiPreReplica> getReplicas(
            Collection<OutputInstruction> instructions, Device device)
            throws PiTranslationException {
        // Account for multiple replicas for the same port.
        final Map<PortNumber, Set<PiPreReplica>> replicaMap = Maps.newHashMap();
        final List<PortNumber> ports = instructions.stream()
                .map(OutputInstruction::port)
                .collect(Collectors.toList());
        for (PortNumber port : ports) {
            if (port.isLogical()) {
                port = logicalToPipelineSpecific(port, device);
            }
            // Use incremental instance IDs for replicas of the same port.
            replicaMap.putIfAbsent(port, Sets.newHashSet());
            replicaMap.get(port).add(
                    new PiPreReplica(port, replicaMap.get(port).size()));
        }
        return replicaMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private static PortNumber logicalToPipelineSpecific(
            PortNumber logicalPort, Device device)
            throws PiTranslationException {
        if (!device.is(PiPipelineInterpreter.class)) {
            throw new PiTranslationException(
                    "missing interpreter, cannot map logical port " + logicalPort.toString());
        }
        final PiPipelineInterpreter interpreter = device.as(PiPipelineInterpreter.class);
        Optional<Long> mappedPort = interpreter.mapLogicalPort(logicalPort);
        if (!mappedPort.isPresent()) {
            throw new PiTranslationException(
                    "interpreter cannot map logical port " + logicalPort.toString());
        }
        return PortNumber.portNumber(mappedPort.get());
    }

    private static <I extends Instruction> List<I> getInstructions(GroupBucket bucket,
                                                                   Instruction.Type type,
                                                                   Class<I> instructionClass) {
        return bucket.treatment().allInstructions().stream()
                .filter(i -> i.type() == type)
                .map(instructionClass::cast)
                .collect(Collectors.toList());
    }
}
