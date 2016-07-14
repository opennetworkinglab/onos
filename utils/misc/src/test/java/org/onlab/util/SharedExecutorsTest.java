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
package org.onlab.util;

import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

/**
 * Tests of the SharedExecutors  Test.
 */
public class SharedExecutorsTest {

    @Test
    public void singleThread() {
       ExecutorService a =  SharedExecutors.getSingleThreadExecutor();
       assertNotNull("ExecutorService must not be null", a);
       ExecutorService b =  SharedExecutors.getSingleThreadExecutor();
       assertSame("factories should be same", a, b);

    }

    @Test
    public void poolThread() {
        ExecutorService a =  SharedExecutors.getPoolThreadExecutor();
        assertNotNull("ExecutorService must not be null", a);
        ExecutorService b =  SharedExecutors.getPoolThreadExecutor();
        assertSame("factories should be same", a, b);

    }

    @Test
    public void timer() {
        java.util.Timer a = SharedExecutors.getTimer();
        assertNotNull("Timer must not be null", a);
        java.util.Timer b = SharedExecutors.getTimer();
        assertSame("factories should be same", a, b);
    }
}
