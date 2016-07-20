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
package org.onosproject.flowapi;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Test for extended traffic action value attribute.
 */
public class DefaultExtTrafficActionTest {

    private boolean terminal = true;
    private boolean sample = true;
    private boolean rpd = true;
    private boolean terminal1 = false;
    private boolean sample1 = false;
    private boolean rpd1 = false;
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.TRAFFIC_ACTION;

    @Test
    public void basics() {

        DefaultExtTrafficAction data = new DefaultExtTrafficAction(terminal, sample, rpd, type);
        DefaultExtTrafficAction sameAsData = new DefaultExtTrafficAction(terminal, sample, rpd, type);
        DefaultExtTrafficAction diffData = new DefaultExtTrafficAction(terminal1, sample1, rpd1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}