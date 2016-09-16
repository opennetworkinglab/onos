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

package org.onosproject.newoptical.api;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;

/**
 * Test class for OpticalPathEvent class.
 */
public class OpticalPathEventTest extends AbstractEventTest {

    @Override
    @Test
    public void withoutTime() {
        OpticalConnectivityId cid = OpticalConnectivityId.of(1L);
        long before = System.currentTimeMillis();
        OpticalPathEvent event = new OpticalPathEvent(OpticalPathEvent.Type.PATH_INSTALLED, cid);
        long after = System.currentTimeMillis();
        validateEvent(event, OpticalPathEvent.Type.PATH_INSTALLED, cid, before, after);
    }

}