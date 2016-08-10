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
        byte eight = 0x08;
        assertThat(ByteOperator.getBit(eight, 0), is(false));
        assertThat(ByteOperator.getBit(eight, 1), is(false));
        assertThat(ByteOperator.getBit(eight, 2), is(false));
        assertThat(ByteOperator.getBit(eight, 3), is(true));
        assertThat(ByteOperator.getBit(eight, 4), is(false));
        assertThat(ByteOperator.getBit(eight, 5), is(false));
        assertThat(ByteOperator.getBit(eight, 6), is(false));
        assertThat(ByteOperator.getBit(eight, 7), is(false));

        byte one = 0x01;
        assertThat(ByteOperator.getBit(one, 0), is(true));
        assertThat(ByteOperator.getBit(one, 1), is(false));
        assertThat(ByteOperator.getBit(one, 2), is(false));
        assertThat(ByteOperator.getBit(one, 3), is(false));
        assertThat(ByteOperator.getBit(one, 4), is(false));
        assertThat(ByteOperator.getBit(one, 5), is(false));
        assertThat(ByteOperator.getBit(one, 6), is(false));
        assertThat(ByteOperator.getBit(one, 7), is(false));
    }
}
