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

package org.onosproject.net.key;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test class for CommunityName.
 */
public class CommunityNameTest {

    final String cName = "CommunityName";

    /**
     * Checks that the CommunityName class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(CommunityName.class);
    }

    /**
     * Checks the construction of a community name object with a null
     * value passed into it.
     */
    @Test
    public void testCommunityNameNull() {
        CommunityName communityName = CommunityName.communityName(null);

        assertNotNull("The CommunityName should not be null.", communityName);
        assertNull("The name should be null.", communityName.name());
    }

    /**
     * Checks the construction of a community name object with a non-null
     * value passed into it.
     */
    @Test
    public void testCommunityName() {
        CommunityName communityName = CommunityName.communityName(cName);

        assertNotNull("The CommunityName should not be null.", communityName);
        assertEquals("The name should match the expected value.", cName, communityName.name());
    }
}
