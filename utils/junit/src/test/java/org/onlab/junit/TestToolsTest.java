/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onlab.junit;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;

public class TestToolsTest {

    @Test
    public void testSuccess() {
        assertAfter(10, 100, new Runnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ < 3) {
                    assertTrue(false);
                }
            }
        });
    }

    @Test(expected = AssertionError.class)
    public void testFailure() {
        assertAfter(100, new Runnable() {
            @Override
            public void run() {
                assertTrue(false);
            }
        });
    }
}
