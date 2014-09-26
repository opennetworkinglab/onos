package org.onlab.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Set;

/**
 * A selector instrumented for unit tests.
 */
public class MockSelector extends AbstractSelector {

    int wakeUpCount = 0;

    /**
     * Creates a mock selector, specifying null as the SelectorProvider.
     */
    public MockSelector() {
        super(null);
    }

    @Override
    public String toString() {
        return "{MockSelector: wake=" + wakeUpCount + "}";
    }

    @Override
    protected void implCloseSelector() throws IOException {
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops,
                                    Object att) {
        return null;
    }

    @Override
    public Set<SelectionKey> keys() {
        return null;
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return null;
    }

    @Override
    public int selectNow() throws IOException {
        return 0;
    }

    @Override
    public int select(long timeout) throws IOException {
        return 0;
    }

    @Override
    public int select() throws IOException {
        return 0;
    }

    @Override
    public Selector wakeup() {
        wakeUpCount++;
        return null;
    }

}
