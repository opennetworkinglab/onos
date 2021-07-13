/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import org.onosproject.store.service.TestEventuallyConsistentMap;
import org.onosproject.store.service.WallClockTimestamp;

import static org.onosproject.pipelines.fabric.impl.behaviour.upf.DistributedFabricUpfStore.FAR_ID_MAP_NAME;
import static org.onosproject.pipelines.fabric.impl.behaviour.upf.DistributedFabricUpfStore.SERIALIZER;


public final class TestDistributedFabricUpfStore {

    private TestDistributedFabricUpfStore() {
    }

    public static DistributedFabricUpfStore build() {
        var store = new DistributedFabricUpfStore();
        TestEventuallyConsistentMap.Builder<Integer, UpfRuleIdentifier> reverseFarIdMapBuilder =
                TestEventuallyConsistentMap.builder();
        reverseFarIdMapBuilder.withName(FAR_ID_MAP_NAME)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withSerializer(SERIALIZER.build());
        store.reverseFarIdMap = reverseFarIdMapBuilder.build();

        store.activate();

        // Init with some translation state.
        store.reverseFarIdMap.put(TestUpfConstants.UPLINK_PHYSICAL_FAR_ID,
                new UpfRuleIdentifier(TestUpfConstants.SESSION_ID, TestUpfConstants.UPLINK_FAR_ID));
        store.reverseFarIdMap.put(TestUpfConstants.DOWNLINK_PHYSICAL_FAR_ID,
                new UpfRuleIdentifier(TestUpfConstants.SESSION_ID, TestUpfConstants.DOWNLINK_FAR_ID));

        return store;
    }
}
