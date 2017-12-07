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
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiTableId;

import java.util.Optional;

import static org.onosproject.pipelines.basic.BasicConstants.CNT_TABLE0_ID;
import static org.onosproject.pipelines.basic.BasicConstants.TBL_TABLE0_ID;
import static org.onosproject.pipelines.basic.IntConstants.*;

/**
 * Interpreter implementation for INT pipeline.
 */
public class IntInterpreterImpl extends BasicInterpreterImpl {

    private static final ImmutableBiMap<PiTableId, PiCounterId> TABLE_COUNTER_MAP =
            new ImmutableBiMap.Builder<PiTableId, PiCounterId>()
                    .put(TBL_TABLE0_ID, CNT_TABLE0_ID)
                    .put(TBL_SET_SOURCE_SINK_ID, CNT_SET_SOURCE_SINK_ID)
                    .put(TBL_INT_SOURCE_ID, CNT_INT_SOURCE_ID)
                    .put(TBL_INT_INSERT_ID, CNT_INT_INSERT_ID)
                    .put(TBL_INT_INST_0003_ID, CNT_INT_INST_0003_ID)
                    .put(TBL_INT_INST_0407_ID, CNT_INT_INST_0407_ID)
                    .build();

    @Override
    public Optional<PiCounterId> mapTableCounter(PiTableId piTableId) {
        return Optional.ofNullable(TABLE_COUNTER_MAP.get(piTableId));
    }
}
