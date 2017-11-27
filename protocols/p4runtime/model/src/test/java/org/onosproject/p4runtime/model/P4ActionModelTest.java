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
package org.onosproject.p4runtime.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiActionParamModel;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test for P4ActionModel class.
 */
public class P4ActionModelTest {

    final PiActionId actionId = PiActionId.of("dec_ttl");
    final PiActionId sameAsActionId = PiActionId.of("dec_ttl");
    final PiActionId actionId2 = PiActionId.of("mod_vlan_vid");

    private final PiActionParamId piActionParamId = PiActionParamId.of("port");
    private final PiActionParamId sameAsPiActionParamId = PiActionParamId.of("port");
    private final PiActionParamId piActionParamId2 = PiActionParamId.of("dstAddr");

    private static final int BIT_WIDTH = 32;

    private final P4ActionParamModel actionParamModel = new P4ActionParamModel(piActionParamId, BIT_WIDTH);

    private final P4ActionParamModel actionParamModel2 = new P4ActionParamModel(piActionParamId2, BIT_WIDTH);


    private final ImmutableMap<PiActionParamId, PiActionParamModel> params = new
            ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
            .put(piActionParamId, actionParamModel)
            .build();
    private final ImmutableMap<PiActionParamId, PiActionParamModel> sameAsParams = new
            ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
            .put(sameAsPiActionParamId, actionParamModel)
            .build();
    private final ImmutableMap<PiActionParamId, PiActionParamModel> params2 = new
            ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
            .put(piActionParamId2, actionParamModel)
            .build();
    private final ImmutableMap<PiActionParamId, PiActionParamModel> params3 = new
            ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
            .put(piActionParamId, actionParamModel2)
            .build();

    private final P4ActionModel actionModel = new P4ActionModel(actionId, params);

    private final P4ActionModel sameAsActionModel = new P4ActionModel(sameAsActionId, params);

    private final P4ActionModel sameAsActionModel2 = new P4ActionModel(actionId, sameAsParams);

    private final P4ActionModel actionModel2 = new P4ActionModel(actionId2, params);

    private final P4ActionModel actionModel3 = new P4ActionModel(actionId, params2);

    private final P4ActionModel actionModel4 = new P4ActionModel(actionId, params3);

    /**
     * Checks that the P4ActionModel class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(P4ActionModel.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(actionModel, sameAsActionModel, sameAsActionModel2)
                .addEqualityGroup(actionModel2)
                .addEqualityGroup(actionModel3)
                .addEqualityGroup(actionModel4)
                .testEquals();
    }
}