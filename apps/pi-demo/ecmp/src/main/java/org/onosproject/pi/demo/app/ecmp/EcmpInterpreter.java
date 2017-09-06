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

package org.onosproject.pi.demo.app.ecmp;

import com.google.common.collect.ImmutableBiMap;
import org.onosproject.drivers.p4runtime.DefaultP4Interpreter;
import org.onosproject.net.pi.runtime.PiTableId;

import java.util.Optional;

/**
 * Implementation of a PiPipeline interpreter for the ecmp.json configuration.
 */
public class EcmpInterpreter extends DefaultP4Interpreter {

    protected static final String ECMP_METADATA_HEADER_NAME = "ecmp_metadata";
    protected static final String ECMP_GROUP_ACTION_NAME = "ecmp_group";
    protected static final String GROUP_ID = "group_id";
    protected static final String SELECTOR = "selector";
    protected static final String ECMP_GROUP_TABLE = "ecmp_group_table";

    private static final ImmutableBiMap<Integer, PiTableId> TABLE_MAP = new ImmutableBiMap.Builder<Integer, PiTableId>()
            .put(0, PiTableId.of(TABLE0))
            .put(1, PiTableId.of(ECMP_GROUP_TABLE))
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