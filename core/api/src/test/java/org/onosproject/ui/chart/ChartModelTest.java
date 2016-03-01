/*
 * Copyright 2016 Open Networking Laboratory
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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

    private static final String[] SERIES = {FOO, BAR, ZOO};

    private ChartModel cm;

    @Test(expected = NullPointerException.class)
    public void guardAgainstNullSeries() {
        cm = new ChartModel(1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void guardAgainstWrongDpNumber() {
        cm = new ChartModel(0, FOO);
    }

    @Test
    public void testSeriesCount() {
        cm = new ChartModel(1, FOO, BAR, ZOO);
        assertEquals("Wrong series count", 3, cm.seriesCount());
    }

    @Test
    public void testAddDataPoint() {
        cm = new ChartModel(2, FOO, BAR, ZOO);

        cm.addDataPoint("1", VALUES1);
        cm.addDataPoint("2", VALUES2);

        assertEquals("Wrong result", "1", cm.getDataPoints()[0].getLabel());
        assertEquals("Wrong result", "2", cm.getDataPoints()[1].getLabel());

        cm.addDataPoint("3", VALUES3);

        assertEquals("Wrong result", "2", cm.getDataPoints()[0].getLabel());
        assertEquals("Wrong result", "3", cm.getDataPoints()[1].getLabel());
    }

    @Test
    public void testGetData() {
        cm = new ChartModel(2, FOO, BAR, ZOO);

        cm.addDataPoint("1", VALUES1);
        assertThat(cm.getLastDataPoint().getValue(ZOO), is(2D));

        cm.addDataPoint("2", VALUES2);
        assertThat(cm.getLastDataPoint().getValue(BAR), is(4D));
    }

    @Test
    public void testGetSeries() {
        cm = new ChartModel(1, FOO, BAR, ZOO);

        assertArrayEquals("series", SERIES, cm.getSeries());
    }
}
