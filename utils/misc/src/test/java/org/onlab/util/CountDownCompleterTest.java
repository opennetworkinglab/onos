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

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

/**
 * Unit tests for CountDownCompleter.
 */
public class CountDownCompleterTest {

    @Test
    public void testCountDown() {
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        CountDownCompleter<String> completer = new CountDownCompleter<>("foo", 2L, v -> callbackInvoked.set(true));
        completer.countDown();
        assertFalse(callbackInvoked.get());
        assertFalse(completer.isComplete());
        completer.countDown();
        assertTrue(callbackInvoked.get());
        assertTrue(completer.isComplete());
    }
}
