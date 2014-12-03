package org.onosproject.net.intent;

import org.onosproject.core.IdGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock id generator for testing.
 */
public class MockIdGenerator implements IdGenerator {
    private AtomicLong nextId = new AtomicLong(0);

    @Override
    public long getNewId() {
        return nextId.getAndIncrement();
    }
}
