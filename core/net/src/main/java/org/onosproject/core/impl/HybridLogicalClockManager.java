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

import org.onosproject.core.HybridLogicalClockService;
import org.onosproject.core.HybridLogicalTime;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@link HybridLogicalClockService}.
 * <p>
 * Implementation is based on HLT <a href="http://www.cse.buffalo.edu/tech-reports/2014-04.pdf">paper</a>.
 */
@Component(immediate = true, service = HybridLogicalClockService.class)
public class HybridLogicalClockManager implements HybridLogicalClockService {

    private final Logger log = getLogger(getClass());

    protected Supplier<Long> physicalTimeSource = System::currentTimeMillis;

    private long logicalTime = 0;
    private long logicalCounter = 0;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public synchronized HybridLogicalTime timeNow() {
        final long oldLogicalTime = logicalTime;
        logicalTime = Math.max(oldLogicalTime, physicalTimeSource.get());
        if (logicalTime == oldLogicalTime) {
            logicalCounter++;
        } else {
            logicalCounter = 0;
        }
        return new HybridLogicalTime(logicalTime, logicalCounter);
    }

    @Override
    public synchronized void recordEventTime(HybridLogicalTime eTime) {
        final long oldLogicalTime = logicalTime;
        logicalTime = Math.max(oldLogicalTime, Math.max(eTime.logicalTime(), physicalTimeSource.get()));
        if (logicalTime == oldLogicalTime && oldLogicalTime == eTime.logicalTime()) {
            logicalCounter = Math.max(logicalCounter, eTime.logicalCounter()) + 1;
        } else if (logicalTime == oldLogicalTime) {
            logicalCounter++;
        } else if (logicalTime == eTime.logicalTime()) {
            logicalCounter = eTime.logicalCounter() + 1;
        } else {
            logicalCounter = 0;
        }
    }

    protected long logicalTime() {
        return logicalTime;
    }

    protected long logicalCounter() {
        return logicalCounter;
    }
}
