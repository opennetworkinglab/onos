/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.rest.exceptions;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Set of tests for the various exception mappers.
 */
public class ExceptionMapperTest {

    @Test
    public void emptyMessage() {
        RuntimeException exception = new NullPointerException();
        ServerErrorMapper mapper = new ServerErrorMapper();
        Object response = mapper.toResponse(exception).getEntity();
        assertTrue("incorrect response",
                   response.toString().contains("ExceptionMapperTest.emptyMessage("));
    }
}