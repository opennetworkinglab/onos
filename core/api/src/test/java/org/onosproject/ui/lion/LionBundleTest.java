/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

package org.onosproject.ui.lion;

import org.junit.Test;
import org.onosproject.ui.AbstractUiTest;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link LionBundle}.
 */
public class LionBundleTest extends AbstractUiTest {

    private static final String ID = "foo";
    private static final String KEY_A = "ka";
    private static final String KEY_B = "kb";
    private static final String VAL_A = "Alpha";
    private static final String VAL_B = "Beta";

    private LionBundle bundle;

    @Test
    public void basic() {
        title("basic");

        bundle = new LionBundle.Builder(ID)
                .addItem(KEY_A, VAL_A)
                .addItem(KEY_B, VAL_B)
                .build();
        print(bundle);
        assertEquals("wrong id", ID, bundle.id());
        assertEquals("wrong item count", 2, bundle.size());

        assertEquals("wrong A lookup", VAL_A, bundle.getValue(KEY_A));
        assertEquals("wrong B lookup", VAL_B, bundle.getValue(KEY_B));
    }
}
