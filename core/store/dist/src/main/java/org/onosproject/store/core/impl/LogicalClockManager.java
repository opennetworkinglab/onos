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
package org.onosproject.store.core.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.store.LogicalTimestamp;
import org.onosproject.store.Timestamp;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLOCK_WRITE;

/**
 * LogicalClockService implementation based on a {@link AtomicCounter}.
 */
@Component(immediate = true)
@Service
public class LogicalClockManager implements LogicalClockService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private static final String SYSTEM_LOGICAL_CLOCK_COUNTER_NAME = "sys-clock-counter";
    private AtomicCounter atomicCounter;

    @Activate
    public void activate() {
        atomicCounter = storageService.getAtomicCounter(SYSTEM_LOGICAL_CLOCK_COUNTER_NAME);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Timestamp getTimestamp() {
        checkPermission(CLOCK_WRITE);
        return new LogicalTimestamp(atomicCounter.incrementAndGet());
    }
}
