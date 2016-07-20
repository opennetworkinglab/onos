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

import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Tests of the SharedScheduledExecutors.
 */
public class SharedScheduledExecutorsTest {

    @Test
    public void singleThread() {
        ScheduledExecutorService a = SharedScheduledExecutors.getSingleThreadExecutor();
        assertNotNull("ScheduledExecutorService must not be null", a);
        ScheduledExecutorService b = SharedScheduledExecutors.getSingleThreadExecutor();
        assertSame("factories should be same", a, b);
    }

    @Test
    public void poolThread() {
        ScheduledExecutorService a = SharedScheduledExecutors.getPoolThreadExecutor();
        assertNotNull("ScheduledExecutorService must not be null", a);
        ScheduledExecutorService b = SharedScheduledExecutors.getPoolThreadExecutor();
        assertSame("factories should be same", a, b);
    }
}
