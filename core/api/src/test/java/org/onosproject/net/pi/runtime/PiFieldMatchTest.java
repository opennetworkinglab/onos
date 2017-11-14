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

package org.onosproject.net.pi.runtime;

import org.junit.Test;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchType;

import static org.junit.Assert.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DOT;
import static org.onosproject.net.pi.runtime.PiConstantsTest.ETH_HEADER_NAME;
import static org.onosproject.net.pi.runtime.PiConstantsTest.ETH_TYPE;

/**
 * Unit tests for PiFieldMatch class.
 */
public class PiFieldMatchTest {

    /**
     * Checks that the PiFieldMatch class is immutable but can be
     * inherited from.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutableBaseClass(PiFieldMatch.class);
    }

    @Test
    public void basics() {
        final PiMatchFieldId piMatchField = PiMatchFieldId.of(ETH_HEADER_NAME + DOT + ETH_TYPE);
        PiFieldMatch piFieldMatch = new PiExactFieldMatch(piMatchField, copyFrom(0x0806));

        assertEquals(piFieldMatch.fieldId(), piMatchField);
        assertEquals(piFieldMatch.type(), PiMatchType.EXACT);
    }
}
