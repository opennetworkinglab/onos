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

package org.onosproject.pipelines.basic;

import com.google.common.collect.ImmutableBiMap;
import org.onosproject.net.pi.model.PiTableId;

import java.util.Optional;

import static org.onosproject.pipelines.basic.BasicConstants.TBL_TABLE0_ID;
import static org.onosproject.pipelines.basic.EcmpConstants.TBL_ECMP_TABLE_ID;

/**
 * Interpreter implementation for ecmp.p4.
 */
public class EcmpInterpreterImpl extends BasicInterpreterImpl {

    private static final ImmutableBiMap<Integer, PiTableId> TABLE_MAP = new ImmutableBiMap.Builder<Integer, PiTableId>()
            .put(0, TBL_TABLE0_ID)
            .put(1, TBL_ECMP_TABLE_ID)
            .build();

    @Override
    public Optional<Integer> mapPiTableId(PiTableId piTableId) {
        return Optional.ofNullable(TABLE_MAP.inverse().get(piTableId));
    }

    @Override
    public Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId) {
        return Optional.ofNullable(TABLE_MAP.get(flowRuleTableId));
    }
}
