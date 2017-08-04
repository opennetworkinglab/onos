/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.driver.extensions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;
import org.onosproject.net.NshContextHeader;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for NiciraSetNshContextHeader class.
 */
public class NiciraSetNshContextHeaderTest {

    final NiciraSetNshContextHeader nshCh1 = new NiciraSetNshContextHeader(NshContextHeader.of(10),
                                                                           ExtensionTreatmentType.
                                                                           ExtensionTreatmentTypes.
                                                                           NICIRA_SET_NSH_CH1.type());
    final NiciraSetNshContextHeader sameAsNshCh1 = new NiciraSetNshContextHeader(NshContextHeader.of(10),
                                                                                 ExtensionTreatmentType.
                                                                                 ExtensionTreatmentTypes.
                                                                                 NICIRA_SET_NSH_CH1.type());
    final NiciraSetNshContextHeader nshCh2 = new NiciraSetNshContextHeader(NshContextHeader.of(20),
                                                                           ExtensionTreatmentType.
                                                                           ExtensionTreatmentTypes.
                                                                           NICIRA_SET_NSH_CH1.type());

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(nshCh1, sameAsNshCh1).addEqualityGroup(nshCh2).testEquals();
    }

    /**
     * Checks the construction of a NiciraSetNshSi object.
     */
    @Test
    public void testConstruction() {
        final NiciraSetNshContextHeader niciraSetNshCh = new NiciraSetNshContextHeader(NshContextHeader.of(10),
                                                                                       ExtensionTreatmentType.
                                                                                       ExtensionTreatmentTypes.
                                                                                       NICIRA_SET_NSH_CH1.type());
        assertThat(niciraSetNshCh, is(notNullValue()));
        assertThat(niciraSetNshCh.nshCh().nshContextHeader(), is(10));
        assertThat(niciraSetNshCh.type(), is(ExtensionTreatmentType.
                                             ExtensionTreatmentTypes.
                                             NICIRA_SET_NSH_CH1.type()));
    }
}
