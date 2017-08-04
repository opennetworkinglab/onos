/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onlab.util;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for ByteOperator.
 */
public class ByteOperatorTest {

    @Test
    public void testGetBit() {
        final byte[] number = new byte[] {1, 2, 4, 8, 16, 32, 64, -128};

        for (int i = 0; i < number.length; i++) {
            for (int j = 0; j < 8; j++) {
                assertThat(ByteOperator.getBit(number[i], j), is(i == j));
            }
        }
    }
}
