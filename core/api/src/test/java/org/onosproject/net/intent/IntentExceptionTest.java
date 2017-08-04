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
package org.onosproject.net.intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test of the intent exception.
 */
public class IntentExceptionTest {

    @Test
    public void basics() {
        validate(new IntentException(), null, null);
        validate(new IntentException("foo"), "foo", null);

        Throwable cause = new NullPointerException("bar");
        validate(new IntentException("foo", cause), "foo", cause);
    }

    /**
     * Validates that the specified exception has the correct message and cause.
     *
     * @param e       exception to test
     * @param message expected message
     * @param cause   expected cause
     */
    protected void validate(RuntimeException e, String message, Throwable cause) {
        assertEquals("incorrect message", message, e.getMessage());
        assertEquals("incorrect cause", cause, e.getCause());
    }

}
