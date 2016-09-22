/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.DscpClass;
import static org.onosproject.net.behaviour.BandwidthProfileAction.Action;

/**
 * Test for BandwidthProfileAction class.
 */
public class BandwidthProfileActionTest {

    @Test
    public void testEquals() {
        BandwidthProfileAction passAction1 = BandwidthProfileAction.builder()
                .action(Action.PASS)
                .build();
        BandwidthProfileAction passAction2 = BandwidthProfileAction.builder()
                .action(Action.PASS)
                .build();
        BandwidthProfileAction discardAction1 = BandwidthProfileAction.builder()
                .action(Action.DISCARD)
                .build();
        BandwidthProfileAction discardAction2 = BandwidthProfileAction.builder()
                .action(Action.DISCARD)
                .build();
        BandwidthProfileAction remarkAction1 = BandwidthProfileAction.builder()
                .action(Action.REMARK)
                .dscpClass(DscpClass.AF11)
                .build();
        BandwidthProfileAction remarkAction2 = BandwidthProfileAction.builder()
                .action(Action.REMARK)
                .dscpClass(DscpClass.AF11)
                .build();
        new EqualsTester()
                .addEqualityGroup(passAction1, passAction2)
                .addEqualityGroup(discardAction1, discardAction2)
                .addEqualityGroup(remarkAction1, remarkAction2)
                .testEquals();
    }
}
