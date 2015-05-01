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
package org.onosproject.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Test for IndexedLambda.
 */
public class IndexedLambdaTest {
    /**
     * Tests equality of IndexedLambda instances.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(Lambda.indexedLambda(10), Lambda.indexedLambda(10))
                .addEqualityGroup(Lambda.indexedLambda(11), Lambda.indexedLambda(11), Lambda.indexedLambda(11))
                .testEquals();
    }
}
