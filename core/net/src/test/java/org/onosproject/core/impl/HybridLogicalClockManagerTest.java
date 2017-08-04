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
package org.onosproject.core.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.onosproject.core.HybridLogicalTime;

/**
 * Unit tests for {@link HybridLogicalClockManager}.
 */
public class HybridLogicalClockManagerTest {

    @Test
    public void testLocalEvents() {
        AtomicLong ticker = new AtomicLong();
        Supplier<Long> ptSource = ticker::get;
        HybridLogicalClockManager clockManager = new HybridLogicalClockManager();
        clockManager.physicalTimeSource = ptSource;

        HybridLogicalTime time1 = clockManager.timeNow();
        Assert.assertEquals(0, time1.logicalTime());
        Assert.assertEquals(1, time1.logicalCounter());

        HybridLogicalTime time2 = clockManager.timeNow();
        Assert.assertEquals(0, time2.logicalTime());
        Assert.assertEquals(2, time2.logicalCounter());

        ticker.incrementAndGet();

        HybridLogicalTime time3 = clockManager.timeNow();
        Assert.assertEquals(1, time3.logicalTime());
        Assert.assertEquals(0, time3.logicalCounter());

        HybridLogicalTime time4 = clockManager.timeNow();
        Assert.assertEquals(1, time4.logicalTime());
        Assert.assertEquals(1, time4.logicalCounter());
    }

    @Test
    public void testReceiveEvents() {
        AtomicLong ticker = new AtomicLong(1);
        Supplier<Long> ptSource = ticker::get;
        HybridLogicalClockManager clockManager = new HybridLogicalClockManager();
        clockManager.physicalTimeSource = ptSource;

        HybridLogicalTime time1 = clockManager.timeNow();
        Assert.assertEquals(1, time1.logicalTime());
        Assert.assertEquals(0, time1.logicalCounter());

        HybridLogicalTime eventTime1 = new HybridLogicalTime(1, 0);
        clockManager.recordEventTime(eventTime1);

        Assert.assertEquals(1, clockManager.logicalTime());
        Assert.assertEquals(1, clockManager.logicalCounter());

        HybridLogicalTime eventTime2 = new HybridLogicalTime(2, 0);
        clockManager.recordEventTime(eventTime2);

        Assert.assertEquals(2, clockManager.logicalTime());
        Assert.assertEquals(1, clockManager.logicalCounter());

        HybridLogicalTime eventTime3 = new HybridLogicalTime(2, 2);
        clockManager.recordEventTime(eventTime3);

        Assert.assertEquals(2, clockManager.logicalTime());
        Assert.assertEquals(3, clockManager.logicalCounter());

        HybridLogicalTime eventTime4 = new HybridLogicalTime(2, 1);
        clockManager.recordEventTime(eventTime4);

        Assert.assertEquals(2, clockManager.logicalTime());
        Assert.assertEquals(4, clockManager.logicalCounter());

        ticker.set(4);

        HybridLogicalTime eventTime5 = new HybridLogicalTime(3, 0);
        clockManager.recordEventTime(eventTime5);

        Assert.assertEquals(4, clockManager.logicalTime());
        Assert.assertEquals(0, clockManager.logicalCounter());
    }
}
