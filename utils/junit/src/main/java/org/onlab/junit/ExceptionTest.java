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
package org.onlab.junit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Base for exception tests.
 */
public abstract class ExceptionTest {

    protected static final Throwable CAUSE = new RuntimeException("boom");
    protected static final String MESSAGE = "Uh oh.... boom";

    protected abstract Exception getDefault();
    protected abstract Exception getWithMessage();
    protected abstract Exception getWithMessageAndCause();

    @Test
    public void noMessageNoCause() {
        Exception e = getDefault();
        assertEquals("incorrect message", null, e.getMessage());
        assertEquals("incorrect cause", null, e.getCause());
    }

    @Test
    public void withMessage() {
        Exception e = getWithMessage();
        assertEquals("incorrect message", MESSAGE, e.getMessage());
        assertEquals("incorrect cause", null, e.getCause());
    }

    @Test
    public void withCause() {
        Exception e = getWithMessageAndCause();
        assertEquals("incorrect message", MESSAGE, e.getMessage());
        assertSame("incorrect cause", CAUSE, e.getCause());
    }
}
