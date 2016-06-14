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

package org.onosproject.bmv2.demo.app.wcmp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.ExtensionTreatment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.bmv2.demo.app.wcmp.WcmpFabricApp.WCMP_CONTEXT;
import static org.onosproject.bmv2.demo.app.wcmp.WcmpInterpreter.*;

/**
 * Builder of WCMP extension treatment.
 */
public final class WcmpGroupTreatmentBuilder {

    private static final double MAX_ERROR = 0.0001;

    private static final Map<DeviceId, Map<Map<PortNumber, Double>, Integer>> DEVICE_GROUP_ID_MAP = Maps.newHashMap();

    private int groupId;

    /**
     * Sets the WCMP group ID.
     *
     * @param groupId an integer value
     * @return this
     */
    public WcmpGroupTreatmentBuilder withGroupId(int groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     * Returns a new extension treatment.
     *
     * @return an extension treatment
     */
    public ExtensionTreatment build() {
        checkArgument(groupId >= 0, "group id must be a non-zero positive integer");
        ImmutableByteSequence groupIdBs = ImmutableByteSequence.copyFrom(groupId);
        final int groupIdBitWidth = WCMP_CONTEXT.configuration().headerType(WCMP_META_T).field(GROUP_ID).bitWidth();
        try {
            groupIdBs = fitByteSequence(groupIdBs, groupIdBitWidth);
            return new Bmv2ExtensionTreatment(
                    Bmv2Action.builder()
                            .withName(WCMP_GROUP)
                            .addParameter(groupIdBs)
                            .build());
        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            throw new RuntimeException(e);
        }
    }

    public static int groupIdOf(DeviceId did, Map<PortNumber, Double> weightedPorts) {
        DEVICE_GROUP_ID_MAP.putIfAbsent(did, Maps.newHashMap());
        // Counts the number of unique portNumber sets for each device ID.
        // Each distinct set of portNumbers will have a unique ID.
        return DEVICE_GROUP_ID_MAP.get(did).computeIfAbsent(weightedPorts,
                                                            (pp) -> DEVICE_GROUP_ID_MAP.get(did).size() + 1);
    }

    public static List<Integer> toPrefixLengths(List<Double> weigths) throws WcmpGroupException {

        double weightSum = weigths.stream()
                .mapToDouble(Double::doubleValue)
                .map(WcmpGroupTreatmentBuilder::roundDouble)
                .sum();

        if (Math.abs(weightSum - 1) > MAX_ERROR) {
            throw new WcmpGroupException("weights sum is expected to be 1, found was " + weightSum);
        }

        final int selectorBitWidth = WCMP_CONTEXT.configuration().headerType(WCMP_META_T).field(SELECTOR).bitWidth();
        final int availableBits = selectorBitWidth - 1;

        List<Long> prefixDiffs = weigths.stream().map(w -> Math.round(w * availableBits)).collect(toList());

        final long bitSum = prefixDiffs.stream().mapToLong(Long::longValue).sum();
        final long error = availableBits - bitSum;

        if (error != 0) {
            // Lazy intuition here is that the error can be absorbed by the longest prefixDiff with the minor impact.
            Long maxDiff = Collections.max(prefixDiffs);
            int idx = prefixDiffs.indexOf(maxDiff);
            prefixDiffs.remove(idx);
            prefixDiffs.add(idx, maxDiff + error);
        }
        List<Integer> prefixLengths = Lists.newArrayList();

        int prefix = 1;
        for (Long p : prefixDiffs) {
            prefixLengths.add(prefix);
            prefix += p;
        }
        return ImmutableList.copyOf(prefixLengths);
    }

    private static double roundDouble(double n) {
        // 5 digits precision.
        return (double) Math.round(n * 100000d) / 100000d;
    }

    public static class WcmpGroupException extends Exception {
        public WcmpGroupException(String s) {
        }
    }
}
