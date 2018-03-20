/*
 * Copyright 2016-present Open Networking Foundation
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ChartModel}.
 */
public class ChartModelTest {

    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String ZOO = "zoo";

    private static final Double[] VALUES1 = {0D, 1D, 2D};
    private static final Double[] VALUES2 = {3D, 4D, 5D};
    private static final Double[] VALUES3 = {6D, 7D, 8D};

    private ChartModel cm;

    @Test(expected = NullPointerException.class)
    public void guardAgainstNullSeries() {
        cm = new ChartModel((String[]) null);
    }

    @Test
    public void testSeriesCount() {
        cm = new ChartModel(FOO, BAR, ZOO);
        assertEquals("Wrong series count", 3, cm.seriesCount());
    }

    @Test
    public void emptyLabel() {
        cm = new ChartModel(FOO, BAR, ZOO);
        cm.addDataPoint(System.currentTimeMillis());

        assertEquals("bad data point count", 1, cm.dataPointCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void dataPointBandSeries() {
        cm = new ChartModel(FOO, BAR);

        cm.addDataPoint(System.currentTimeMillis())
                .data(ZOO, VALUES3[0]);
    }

    @Test
    public void testAddDataPoint() {
        cm = new ChartModel(FOO, BAR, ZOO);

        long time = System.currentTimeMillis();

        cm.addDataPoint(time)
                .data(FOO, VALUES1[0])
                .data(BAR, VALUES2[0])
                .data(ZOO, VALUES3[0]);

        cm.addDataPoint(time + 1)
                .data(FOO, VALUES1[1])
                .data(BAR, VALUES2[1])
                .data(ZOO, VALUES3[1]);

        cm.addDataPoint(time + 2)
                .data(FOO, VALUES1[2])
                .data(BAR, VALUES2[2])
                .data(ZOO, VALUES3[2]);

        assertEquals("Wrong result", 3, cm.getDataPoints()[0].size());
        assertEquals("Wrong result", 3, cm.getDataPoints()[1].size());
        assertEquals("Wrong result", 3, cm.getDataPoints()[2].size());
        assertEquals("Wrong result", 3, cm.getDataPoints().length);
    }

    @Test
    public void testGetDataPoint() {
        cm = new ChartModel(FOO, BAR);

        long time = System.currentTimeMillis();

        cm.addDataPoint(time)
                .data(FOO, VALUES1[0])
                .data(BAR, VALUES2[0]);

        cm.addDataPoint(time + 1)
                .data(FOO, VALUES1[1])
                .data(BAR, VALUES2[1]);

        assertEquals("Wrong result", (Double) 0D, cm.getDataPoints()[0].get(FOO));
        assertEquals("Wrong result", (Double) 1D, cm.getDataPoints()[1].get(FOO));
        assertEquals("Wrong result", (Double) 3D, cm.getDataPoints()[0].get(BAR));
        assertEquals("Wrong result", (Double) 4D, cm.getDataPoints()[1].get(BAR));
    }

    @Test
    public void testGetLastDataPoint() {
        cm = new ChartModel(FOO, BAR);

        long time = System.currentTimeMillis();

        cm.addDataPoint(time)
                .data(FOO, VALUES1[0])
                .data(BAR, VALUES2[0]);

        cm.addDataPoint(time + 1)
                .data(FOO, VALUES1[1])
                .data(BAR, VALUES2[1]);

        assertEquals("Wrong result", VALUES1[1], cm.getLastDataPoint().get(FOO));
        assertEquals("Wrong result", VALUES2[1], cm.getLastDataPoint().get(BAR));
    }
}
