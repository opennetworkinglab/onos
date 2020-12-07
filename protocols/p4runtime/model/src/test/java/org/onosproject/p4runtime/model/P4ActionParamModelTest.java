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

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiActionParamId;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test for P4ActionParamModel class.
 */
public class P4ActionParamModelTest {
    private final PiActionParamId piActionParamId = PiActionParamId.of("port");
    private final PiActionParamId sameAsPiActionParamId = PiActionParamId.of("port");
    private final PiActionParamId piActionParamId2 = PiActionParamId.of("dstAddr");

    private static final int BIT_WIDTH_32 = 32;
    private static final int BIT_WIDTH_64 = 64;

    private final P4ActionParamModel actionParamModel = new P4ActionParamModel(piActionParamId, BIT_WIDTH_32);

    private final P4ActionParamModel sameAsActionParamModel = new P4ActionParamModel(sameAsPiActionParamId,
                                                                                     BIT_WIDTH_32);

    private final P4ActionParamModel actionParamModel2 = new P4ActionParamModel(piActionParamId2, BIT_WIDTH_32);

    private final P4ActionParamModel actionParamModel3 = new P4ActionParamModel(piActionParamId, BIT_WIDTH_64);

    private final P4ActionParamModel actionParamModel4 = new
            P4ActionParamModel(piActionParamId, P4ActionParamModel.BIT_WIDTH_UNDEFINED);

    private final P4ActionParamModel sameAsActionParamModel4 =
            new P4ActionParamModel(sameAsPiActionParamId, P4ActionParamModel.BIT_WIDTH_UNDEFINED);

    private final P4ActionParamModel actionParamModel5 =
            new P4ActionParamModel(piActionParamId2, P4ActionParamModel.BIT_WIDTH_UNDEFINED);



    /**
     * Checks that the P4CounterModel class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(P4ActionParamModel.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(actionParamModel, sameAsActionParamModel)
                .addEqualityGroup(actionParamModel4, sameAsActionParamModel4)
                .addEqualityGroup(actionParamModel2)
                .addEqualityGroup(actionParamModel3)
                .addEqualityGroup(actionParamModel5)
                .testEquals();
    }
}