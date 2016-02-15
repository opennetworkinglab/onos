/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.fnl.base;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for TsReturn class.
 */
public class TsReturnTest {

    private static final boolean OLD_BOOLEAN_EXAMPLE = false;
    private static final boolean NEW_BOOLEAN_EXAMPLE = true;
    private static final int OLD_INTEGER_EXAMPLE = 7181;
    private static final int NEW_INTEGER_EXAMPLE = 1355;


    @Before
    public void setUp() {
        // do nothing
    }

    @After
    public void tearDown() {
        // do nothing
    }

    @Test
    public void testPresent() {
        TsReturn<Boolean> bool = new TsReturn<>();
        assertFalse(bool.isPresent());

        bool.setValue(NEW_BOOLEAN_EXAMPLE);
        assertTrue(bool.isPresent());
    }

    @Test
    public void testRoleReturnBoolean() {
        TsReturn<Boolean> bool = new TsReturn<>();
        bool.setValue(OLD_BOOLEAN_EXAMPLE);

        Boolean oldValue = bool.getValue();
        changeBoolean(bool);
        Boolean newValue = bool.getValue();

        assertNotSame(oldValue, newValue);
        assertEquals(newValue, NEW_BOOLEAN_EXAMPLE);
    }

    @Test
    public void testRoleReturnInteger() {
        TsReturn<Integer> integer = new TsReturn<>();
        integer.setValue(OLD_INTEGER_EXAMPLE);

        Integer oldValue = integer.getValue();
        changeInteger(integer);
        Integer newValue = integer.getValue();

        assertNotSame(oldValue, newValue);
        assertEquals(newValue.intValue(), NEW_INTEGER_EXAMPLE);
    }

    private void changeBoolean(TsReturn bool) {
        bool.setValue(NEW_BOOLEAN_EXAMPLE);
    }

    private void changeInteger(TsReturn integer) {
        integer.setValue(NEW_INTEGER_EXAMPLE);
    }
}