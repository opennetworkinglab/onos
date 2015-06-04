package org.onosproject.store.core.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.IdBlock;
import org.onosproject.core.IdBlockStore;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Map;

import static org.onlab.util.Tools.randomDelay;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@code IdBlockStore} using {@code AtomicCounter}.
 */
@Component(immediate = true, enabled = true)
@Service
public class ConsistentIdBlockStore implements IdBlockStore {

    private static final int MAX_TRIES = 5;
    private static final int RETRY_DELAY_MS = 2_000;

    private final Logger log = getLogger(getClass());
    private final Map<String, AtomicCounter> topicCounters = Maps.newConcurrentMap();

    private static final long DEFAULT_BLOCK_SIZE = 0x100000L;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public IdBlock getIdBlock(String topic) {
        AtomicCounter counter = topicCounters
                .computeIfAbsent(topic,
                                 name -> storageService.atomicCounterBuilder()
                                         .withName(name)
                                         .build());
        Throwable exc = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                Long blockBase = counter.getAndAdd(DEFAULT_BLOCK_SIZE);
                return new IdBlock(blockBase, DEFAULT_BLOCK_SIZE);
            } catch (StorageException e) {
                log.warn("Unable to allocate ID block due to {}; retrying...",
                         e.getMessage());
                exc = e;
                randomDelay(RETRY_DELAY_MS); // FIXME: This is a deliberate hack; fix in Drake
            }
        }
        throw new IllegalStateException("Unable to allocate ID block", exc);
    }

}
