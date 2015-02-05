/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.codec.impl;

import java.util.EnumMap;

import org.junit.Test;
import org.onosproject.net.flow.criteria.Criterion;

import static org.onlab.junit.TestUtils.getField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for criterion codec.
 */
public class CriterionCodecTest {

    /**
     * Checks that all criterion types are covered by the codec.
     */
    @Test
    public void checkCriterionTypes() throws Exception {
        CriterionCodec codec = new CriterionCodec();
        EnumMap<Criterion.Type, Object> formatMap = getField(codec, "formatMap");
        assertThat(formatMap, notNullValue());

        for (Criterion.Type type : Criterion.Type.values()) {
            assertThat("Entry not found for " + type.toString(),
                    formatMap.get(type), notNullValue());
        }
    }
}
