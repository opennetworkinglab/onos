/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onlab.packet;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Data class.
 */
public class DataTest {

    private static final int DATA_BUFFER_SIZE = 10;
    private static final int DATA_LENGTH = 5;
    private static byte[] dataBuffer = new byte[DATA_BUFFER_SIZE];

    private Deserializer<Data> deserializer;

    @Before
    public void setUp() {
        deserializer = Data.deserializer();
    }

    @Test
    public void testDeserializePartOfBuffer() throws Exception {
        Data data = deserializer.deserialize(dataBuffer, 0, DATA_LENGTH);

        assertEquals(DATA_LENGTH, data.getData().length);
    }

}
