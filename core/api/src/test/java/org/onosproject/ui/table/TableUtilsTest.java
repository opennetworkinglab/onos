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

package org.onosproject.ui.table;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link TableUtils}.
 */
public class TableUtilsTest {

    private static final String FOO = "foo";
    private static final String BAR = "bar";

    private static final String ARRAY_AS_STRING =
            "[{\"foo\":\"1\",\"bar\":\"2\"},{\"foo\":\"3\",\"bar\":\"4\"}]";

    @Test
    public void basic() {
        TableModel tm = new TableModel(FOO, BAR);
        tm.addRow().cell(FOO, 1).cell(BAR, 2);
        tm.addRow().cell(FOO, 3).cell(BAR, 4);

        ArrayNode array = TableUtils.generateRowArrayNode(tm);
        Assert.assertEquals("wrong results", ARRAY_AS_STRING, array.toString());
    }

}
