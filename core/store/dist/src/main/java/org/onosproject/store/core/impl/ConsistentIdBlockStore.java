package org.onosproject.store.core.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

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

import com.google.common.collect.Maps;

/**
 * Implementation of {@code IdBlockStore} using {@code AtomicCounter}.
 */
@Component(immediate = true, enabled = true)
@Service
public class ConsistentIdBlockStore implements IdBlockStore {

    private static final int MAX_TRIES = 3;

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
            }
        }
        throw new IllegalStateException("Unable to allocate ID block", exc);
    }

}
