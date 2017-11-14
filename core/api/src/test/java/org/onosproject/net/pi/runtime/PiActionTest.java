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

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.MOD_NW_DST;

/**
 * Unit tests for PiAction class.
 */
public class PiActionTest {
    private final PiAction piAction1 = PiAction.builder().withId(PiActionId.of(MOD_NW_DST))
            .withParameter(new PiActionParam(PiActionParamId.of(DST_ADDR), copyFrom(0x0a010101)))
            .build();
    private final PiAction sameAsPiAction1 = PiAction.builder().withId(PiActionId.of(MOD_NW_DST))
            .withParameter(new PiActionParam(PiActionParamId.of(DST_ADDR), copyFrom(0x0a010101)))
            .build();
    private final PiAction piAction2 = PiAction.builder().withId(PiActionId.of("set_egress_port_0")).build();

    /**
     * Checks that the PiAction class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiAction.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piAction1, sameAsPiAction1)
                .addEqualityGroup(piAction2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiAction object with parameter.
     */
    @Test
    public void testMethodWithParameter() {
        PiActionId piActionId = PiActionId.of(MOD_NW_DST);
        PiActionParam piActionParam = new PiActionParam(PiActionParamId.of(DST_ADDR), copyFrom(0x0a010101));
        final PiAction piAction = PiAction.builder().withId(piActionId)
                .withParameter(piActionParam)
                .build();
        assertThat(piAction, is(notNullValue()));
        assertThat(piAction.id(), is(piActionId));
        assertThat(piAction.type(), is(PiTableAction.Type.ACTION));
    }

    /**
     * Checks the construction of a PiAction object with parameters.
     */
    @Test
    public void testMethodWithParameters() {
        PiActionId piActionId = PiActionId.of(MOD_NW_DST);
        Collection<PiActionParam> runtimeParams = Lists.newArrayList();
        PiActionParam piActionParam = new PiActionParam(PiActionParamId.of(DST_ADDR), copyFrom(0x0a010101));

        runtimeParams.add(piActionParam);
        final PiAction piAction = PiAction.builder().withId(piActionId)
                .withParameters(runtimeParams)
                .build();
        assertThat(piAction, is(notNullValue()));
        assertThat(piAction.id(), is(piActionId));
        assertThat(piAction.parameters(), is(runtimeParams));
        assertThat(piAction.type(), is(PiTableAction.Type.ACTION));
    }
}
