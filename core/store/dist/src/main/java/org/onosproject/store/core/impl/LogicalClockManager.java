package org.onosproject.store.core.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.store.Timestamp;
import org.onosproject.store.impl.LogicalTimestamp;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

/**
 * LogicalClockService implementation based on a AtomicCounter.
 */
@Component(immediate = true, enabled = true)
@Service
public class LogicalClockManager implements LogicalClockService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private static final String SYSTEM_LOGICAL_CLOCK_COUNTER_NAME = "sys-clock-counter";
    private AtomicCounter atomicCounter;

    @Activate
    public void activate() {
        atomicCounter = storageService.atomicCounterBuilder()
                                      .withName(SYSTEM_LOGICAL_CLOCK_COUNTER_NAME)
                                      .withPartitionsDisabled()
                                      .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Timestamp getTimestamp() {
        return new LogicalTimestamp(atomicCounter.incrementAndGet());
    }
}