/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.demo.app.ecmp;

import com.google.common.collect.Maps;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.context.Bmv2ActionModel;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.ExtensionTreatment;

import java.util.Map;
import java.util.Set;

import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.bmv2.demo.app.ecmp.EcmpFabricApp.ECMP_CONTEXT;
import static org.onosproject.bmv2.demo.app.ecmp.EcmpInterpreter.*;

/**
 * Builder of ECMP extension treatments.
 */
public class EcmpGroupTreatmentBuilder {

    private static final Map<DeviceId, Map<Set<PortNumber>, Short>> DEVICE_GROUP_ID_MAP = Maps.newHashMap();
    private int groupId;
    private int groupSize;

    /**
     * Sets the group ID.
     *
     * @param groupId an integer value
     * @return this
     */
    public EcmpGroupTreatmentBuilder withGroupId(int groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     * Sets the group size.
     *
     * @param groupSize an integer value
     * @return this
     */
    public EcmpGroupTreatmentBuilder withGroupSize(int groupSize) {
        this.groupSize = groupSize;
        return this;
    }

    /**
     * Returns a new extension treatment.
     *
     * @return an extension treatment
     */
    public ExtensionTreatment build() {
        Bmv2ActionModel actionModel = ECMP_CONTEXT.configuration().action(ECMP_GROUP);
        int groupIdBitWidth = actionModel.runtimeData(GROUP_ID).bitWidth();
        int groupSizeBitWidth = actionModel.runtimeData(GROUP_SIZE).bitWidth();

        try {
            ImmutableByteSequence groupIdBs = fitByteSequence(ImmutableByteSequence.copyFrom(groupId), groupIdBitWidth);
            ImmutableByteSequence groupSizeBs = fitByteSequence(ImmutableByteSequence.copyFrom(groupSize),
                                                                groupSizeBitWidth);

            return new Bmv2ExtensionTreatment(Bmv2Action.builder()
                                                      .withName(ECMP_GROUP)
                                                      .addParameter(groupIdBs)
                                                      .addParameter(groupSizeBs)
                                                      .build());

        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a group ID for the given device and set of ports.
     *
     * @param deviceId a device ID
     * @param ports a set of ports
     * @return an integer value
     */
    public static int groupIdOf(DeviceId deviceId, Set<PortNumber> ports) {
        DEVICE_GROUP_ID_MAP.putIfAbsent(deviceId, Maps.newHashMap());
        // Counts the number of unique portNumber sets for each deviceId.
        // Each distinct set of portNumbers will have a unique ID.
        return DEVICE_GROUP_ID_MAP.get(deviceId).computeIfAbsent(ports, (pp) ->
                (short) (DEVICE_GROUP_ID_MAP.get(deviceId).size() + 1));
    }
}
