/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.packet;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

/**
 * Unit tests for static APIs in the packet processor class.
 */
public class PacketProcessorTest {

    /**
     * Tests a priority in the advisor range.
     */
    @Test
    public void testAdvisorPriorities() {
        int advisorPriority = PacketProcessor.advisor(3);
        assertThat(advisorPriority, lessThan(PacketProcessor.ADVISOR_MAX));
        assertThat(advisorPriority, greaterThanOrEqualTo(0));
    }

    /**
     * Tests a priority in the director range.
     */
    @Test
    public void testDirectorPriorities() {
        int directorPriority = PacketProcessor.director(3);
        assertThat(directorPriority, lessThan(PacketProcessor.DIRECTOR_MAX));
        assertThat(directorPriority, greaterThanOrEqualTo(PacketProcessor.ADVISOR_MAX));
    }

    /**
     * Tests a priority in the observer range.
     */
    @Test
    public void testObserverPriorities() {
        int observerPriority = PacketProcessor.observer(3);
        assertThat(observerPriority, lessThan(PacketProcessor.OBSERVER_MAX));
        assertThat(observerPriority, greaterThanOrEqualTo(PacketProcessor.DIRECTOR_MAX));
    }

}
