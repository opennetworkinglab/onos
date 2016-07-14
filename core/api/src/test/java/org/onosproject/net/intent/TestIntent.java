/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onosproject.TestApplicationId;

import java.util.Collections;

/**
 * An intent used in the unit test.
 */
public class TestIntent extends Intent {

    private final int value;

    /**
     * Constructs an instance with the specified intent ID.
     *
     * @param value intent ID
     */
    public TestIntent(int value) { // FIXME
        super(new TestApplicationId("foo"), null, Collections.emptyList(),
                Intent.DEFAULT_INTENT_PRIORITY);
        this.value = value;
    }

    /**
     * Constructor for serializer.
     */
    protected TestIntent() {
        super();
        value = -1;
    }
}
