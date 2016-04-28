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
package org.onosproject.ui.chart;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link ChartUtils}.
 */
public class ChartUtilsTest {

    private static final String FOO = "foo";
    private static final String BAR = "bar";

    private static final String ARRAY_AS_STRING =
            "[{\"foo\":\"1.0\",\"bar\":\"2.0\"},{\"foo\":\"3.0\",\"bar\":\"4.0\"}]";
    private static final String NODE_AS_STRING =
            "{\"dev1\":\"of:0000000000000001\",\"dev2\":\"of:0000000000000002\"}";

    @Test
    public void basic() {
        ChartModel cm = new ChartModel(FOO, BAR);
        cm.addDataPoint(1L).data(FOO, 1D).data(BAR, 2D);
        cm.addDataPoint(2L).data(FOO, 3D).data(BAR, 4D);

        ArrayNode array = ChartUtils.generateDataPointArrayNode(cm);
        Assert.assertEquals("wrong results", ARRAY_AS_STRING, array.toString());
    }

    @Test
    public void annot() {
        ChartModel cm = new ChartModel(FOO, BAR);
        cm.addAnnotation("dev1", "of:0000000000000001");
        cm.addAnnotation("dev2", "of:0000000000000002");

        ObjectNode node = ChartUtils.generateAnnotObjectNode(cm);
        Assert.assertEquals("wrong results", NODE_AS_STRING, node.toString());
    }
}
