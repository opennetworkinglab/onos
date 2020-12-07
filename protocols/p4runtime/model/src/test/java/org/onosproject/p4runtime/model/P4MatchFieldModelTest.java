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
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchType;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for P4MatchFieldModel class.
 */
public class P4MatchFieldModelTest {

    private static final PiMatchFieldId PI_MATCH_FIELD_ID_1 = PiMatchFieldId.of("MatchField1");
    private static final PiMatchFieldId PI_MATCH_FIELD_ID_2 = PiMatchFieldId.of("MatchField2");

    private static final int BIT_WIDTH_1 = 100;
    private static final int BIT_WIDTH_2 = 200;

    private static final PiMatchType PI_MATCH_TYPE_1 = PiMatchType.EXACT;
    private static final PiMatchType PI_MATCH_TYPE_2 = PiMatchType.TERNARY;

    private static final P4MatchFieldModel P4_MATCH_FIELD_MODEL_1 =
        new P4MatchFieldModel(PI_MATCH_FIELD_ID_1, BIT_WIDTH_1, PI_MATCH_TYPE_1);
    private static final P4MatchFieldModel SAME_AS_P4_MATCH_FIELD_MODEL_1 =
        new P4MatchFieldModel(PI_MATCH_FIELD_ID_1, BIT_WIDTH_1, PI_MATCH_TYPE_1);
    private static final P4MatchFieldModel P4_MATCH_FIELD_MODEL_2 =
        new P4MatchFieldModel(PI_MATCH_FIELD_ID_2, BIT_WIDTH_2, PI_MATCH_TYPE_2);

    private static final P4MatchFieldModel P4_MATCH_FIELD_MODEL_3 =
            new P4MatchFieldModel(PI_MATCH_FIELD_ID_1, P4MatchFieldModel.BIT_WIDTH_UNDEFINED, PI_MATCH_TYPE_1);
    private static final P4MatchFieldModel SAME_AS_P4_MATCH_FIELD_MODEL_3 =
            new P4MatchFieldModel(PI_MATCH_FIELD_ID_1, P4MatchFieldModel.BIT_WIDTH_UNDEFINED, PI_MATCH_TYPE_1);

    private static final P4MatchFieldModel P4_MATCH_FIELD_MODEL_4 =
            new P4MatchFieldModel(PI_MATCH_FIELD_ID_2, P4MatchFieldModel.BIT_WIDTH_UNDEFINED, PI_MATCH_TYPE_2);

    /**
     * Checks that the P4MatchFieldModel class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(P4MatchFieldModel.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
            .addEqualityGroup(P4_MATCH_FIELD_MODEL_1, SAME_AS_P4_MATCH_FIELD_MODEL_1)
            .addEqualityGroup(P4_MATCH_FIELD_MODEL_3, SAME_AS_P4_MATCH_FIELD_MODEL_3)
            .addEqualityGroup(P4_MATCH_FIELD_MODEL_2)
            .addEqualityGroup(P4_MATCH_FIELD_MODEL_4)
            .testEquals();
    }
}