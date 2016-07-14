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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Provides static utility methods for dealing with charts.
 */
public final class ChartUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // non-instantiable
    private ChartUtils() {
    }

    /**
     * Generates a JSON array node from the data points of the given chart model.
     *
     * @param cm the chart model
     * @return the array node representation of data points
     */
    public static ArrayNode generateDataPointArrayNode(ChartModel cm) {
        ArrayNode array = MAPPER.createArrayNode();
        for (ChartModel.DataPoint dp : cm.getDataPoints()) {
            array.add(toJsonNode(dp, cm));
        }
        return array;
    }

    /**
     * Generates a JSON object node from the annotations of the given chart model.
     *
     * @param cm the chart model
     * @return the object node representation of the annotations
     */
    public static ObjectNode generateAnnotObjectNode(ChartModel cm) {
        ObjectNode node = MAPPER.createObjectNode();
        for (ChartModel.Annot a : cm.getAnnotations()) {
            node.put(a.key(), a.valueAsString());
        }
        return node;
    }

    /**
     * Generate a JSON node from the data point and given chart model.
     *
     * @param dp the data point
     * @param cm the chart model
     * @return the node representation of a data point with series
     */
    public static JsonNode toJsonNode(ChartModel.DataPoint dp, ChartModel cm) {
        ObjectNode result = MAPPER.createObjectNode();
        String[] series = cm.getSeries();
        String[] values = dp.getAsStrings();
        int n = series.length;
        for (int i = 0; i < n; i++) {
            result.put(series[i], values[i]);
        }
        return result;
    }
}
