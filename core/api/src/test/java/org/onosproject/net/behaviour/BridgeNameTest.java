/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.behaviour;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

public class BridgeNameTest {

    private static final String NAME1 = "bridge-1";
    private static final String NAME2 = "bridge-2";
    private BridgeName bridgeName1 = BridgeName.bridgeName(NAME1);
    private BridgeName sameAsBridgeName1 = BridgeName.bridgeName(NAME1);
    private BridgeName bridgeName2 = BridgeName.bridgeName(NAME2);

    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(BridgeName.class);
    }

    @Test
    public void testConstruction() {
        assertThat(bridgeName1.name(), is(NAME1));
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(bridgeName1, sameAsBridgeName1)
                .addEqualityGroup(bridgeName2)
                .testEquals();
    }

}
