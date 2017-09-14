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

package org.onosproject.net.behaviour.trafficcontrol;

import com.google.common.base.Strings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onlab.junit.ImmutableClassChecker;

import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

/**
 * Test class for PolicerId.
 */
public class PolicerIdTest {

    // Test scheme
    private static final String FOO_SCHEME = "foo";
    // OpenFlow scheme
    private static final String OF_SCHEME = "of";
    // Some test ids
    private static final Short ONE = 1;
    private static final Short TWO = 15;
    private static final String A = "A";
    private static final String LA = "a";

    /**
     * Test policer id creation from URI.
     */
    @Test
    public void testfromUriCreation() {
        // Create URI representing the id
        URI uriOne = URI.create(OF_SCHEME + ":" + Integer.toHexString(ONE));
        // Create policer id
        PolicerId one = PolicerId.policerId(uriOne);
        // Verify proper creation
        assertThat(one, notNullValue());
        assertThat(one.id(), is(uriOne.toString().toLowerCase()));
        assertThat(one.uri(), is(uriOne));
    }

    /**
     * Test policer id creation from string.
     */
    @Test
    public void testfromStringCreation() {
        // Create String representing the id
        String stringTwo = OF_SCHEME + ":" + Integer.toHexString(TWO);
        // Create policer id
        PolicerId two = PolicerId.policerId(stringTwo);
        // Verify proper creation
        assertThat(two, notNullValue());
        assertThat(two.id(), is(stringTwo));
        assertThat(two.uri(), is(URI.create(stringTwo)));
    }

    /**
     * Exception expected to raise when creating a wrong policer id.
     */
    @Rule
    public ExpectedException exceptionWrongId = ExpectedException.none();

    /**
     * Test wrong creation of a policer id.
     */
    @Test
    public void testWrongCreation() {
        // Build not allowed string
        String wrongString = Strings.repeat("x", 1025);
        // Define expected exception
        exceptionWrongId.expect(IllegalArgumentException.class);
        // Create policer id
        PolicerId.policerId(wrongString);
    }

    /**
     * Test equality between policer ids.
     */
    @Test
    public void testEquality() {
        // Create URI representing the id one
        URI uriOne = URI.create(OF_SCHEME + ":" + Integer.toHexString(ONE));
        // Create String representing the id one
        String stringOne = OF_SCHEME + ":" + Integer.toHexString(ONE);
        // Create String representing the id two
        String stringTwo = OF_SCHEME + ":" + Integer.toHexString(TWO);
        // Create String representing the id A
        String stringA = FOO_SCHEME + ":" + A;
        // Create String representing the id LA
        String stringLA = FOO_SCHEME + ":" + LA;
        // Create policer id one
        PolicerId one = PolicerId.policerId(uriOne);
        // Create policer id one
        PolicerId copyOfOne = PolicerId.policerId(stringOne);
        // Verify equality
        assertEquals(one, copyOfOne);
        // Create a different policer id
        PolicerId two = PolicerId.policerId(stringTwo);
        // Verify not equals
        assertNotEquals(two, one);
        assertNotEquals(two, copyOfOne);
        // Create policer id A
        PolicerId a = PolicerId.policerId(A);
        // Create policer id LA
        PolicerId la = PolicerId.policerId(LA);
        // Verify not equals
        assertNotEquals(a, la);
    }

    /**
     * Tests immutability of PolicerId.
     */
    @Test
    public void testImmutability() {
        ImmutableClassChecker.assertThatClassIsImmutable(PolicerId.class);
    }

}
