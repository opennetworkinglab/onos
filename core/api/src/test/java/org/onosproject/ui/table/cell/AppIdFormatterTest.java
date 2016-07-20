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

package org.onosproject.ui.table.cell;

import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.ui.table.CellFormatter;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link AppIdFormatter}.
 */
public class AppIdFormatterTest {

    private static final ApplicationId APP_ID = new ApplicationId() {
        @Override
        public short id() {
            return 25;
        }

        @Override
        public String name() {
            return "some app";
        }
    };

    private CellFormatter fmt = AppIdFormatter.INSTANCE;

    @Test
    public void basic() {
        assertEquals("wrong format", "25 : some app", fmt.format(APP_ID));
    }

}
