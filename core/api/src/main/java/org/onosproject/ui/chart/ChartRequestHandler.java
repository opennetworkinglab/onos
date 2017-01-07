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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.ui.RequestHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Message handler specifically for the chart views.
 */
public abstract class ChartRequestHandler extends RequestHandler {

    protected static final String LABEL = "label";
    private static final String ANNOTS = "annots";

    private final String respType;
    private final String nodeName;

    /**
     * Constructs a chart model handler for a specific graph view. When chart
     * requests come in, the handler will generate the appropriate chart data
     * points and send back the response to the client.
     *
     * @param reqType  type of the request event
     * @param respType type of the response event
     * @param nodeName name of JSON node holding data point
     */
    public ChartRequestHandler(String reqType, String respType, String nodeName) {
        super(reqType);
        this.respType = respType;
        this.nodeName = nodeName;
    }

    @Override
    public void process(ObjectNode payload) {
        ChartModel cm = createChartModel();
        populateChart(cm, payload);

        ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.set(nodeName, ChartUtils.generateDataPointArrayNode(cm));
        rootNode.set(ANNOTS, ChartUtils.generateAnnotObjectNode(cm));
        sendMessage(respType, rootNode);
    }

    /**
     * Creates the chart model using {@link #getSeries()}
     * to initialize it, ready to be populated.
     * <p>
     * This default implementation returns a chart model for all series.
     * </p>
     *
     * @return an empty chart model
     */
    protected ChartModel createChartModel() {
        List<String> series = new ArrayList<>();
        series.addAll(Arrays.asList(getSeries()));
        series.add(LABEL);
        String[] array = new String[series.size()];
        return new ChartModel(series.toArray(array));
    }

    /**
     * Subclasses should return the array of series with which to initialize
     * their chart model.
     *
     * @return the series name
     */
    protected abstract String[] getSeries();

    /**
     * Subclasses should populate the chart model by adding
     * {@link ChartModel.DataPoint datapoints}.
     * <pre>
     *     cm.addDataPoint()
     *         .data(SERIES_ONE, ...)
     *         .data(SERIES_TWO, ...)
     *         ... ;
     * </pre>
     * The request payload is provided in case there are request filtering
     * parameters.
     *
     * @param cm      the chart model
     * @param payload request payload
     */
    protected abstract void populateChart(ChartModel cm, ObjectNode payload);
}
