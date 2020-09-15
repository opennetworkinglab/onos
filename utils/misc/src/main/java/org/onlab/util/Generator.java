/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onlab.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Generator class that yields instances of T type objects as soon as they are ready.
 *
 * @param <T> type of the object.
 */
public abstract class Generator<T> implements Iterable<T> {

    private class Condition {
        private boolean isSet;

        synchronized void set() {
            isSet = true;
            notifyAll();
        }

        synchronized void await() throws InterruptedException {
            try {

                if (isSet) {
                    return;
                }

                while (!isSet) {
                    wait();
                }
            } finally {
                isSet = false;
            }
        }
    }

    private static ThreadGroup threadGroup;

    private Thread producer;
    private boolean hasFinished;
    private final Condition itemAvailableOrHasFinished = new Condition();
    private final Condition itemRequested = new Condition();
    private T nextItem;
    private boolean nextItemAvailable;
    private RuntimeException exceptionRaisedByProducer;

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return waitForNext();
            }

            @Override
            public T next() {
                if (!waitForNext()) {
                    throw new NoSuchElementException();
                }
                nextItemAvailable = false;
                return nextItem;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private boolean waitForNext() {
                if (nextItemAvailable) {
                    return true;
                }
                if (hasFinished) {
                    return false;
                }
                if (producer == null) {
                    startProducer();
                }
                itemRequested.set();
                try {
                    itemAvailableOrHasFinished.await();
                } catch (InterruptedException e) {
                    hasFinished = true;
                    producer.interrupt();
                    try {
                        producer.join();
                    } catch (InterruptedException e1) {
                        // Interrupting the broken thread
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e1);
                    }
                }
                if (exceptionRaisedByProducer != null) {
                    throw exceptionRaisedByProducer;
                }
                return !hasFinished;
            }
        };
    }

    protected abstract void run() throws InterruptedException;

    /**
     * Makes available the next item.
     *
     * @param element the next item
     * @throws InterruptedException if await fails
     */
    public void yield(T element) throws InterruptedException {
        nextItem = element;
        nextItemAvailable = true;
        itemAvailableOrHasFinished.set();
        itemRequested.await();
    }

    private void startProducer() {
        assert producer == null;
        synchronized (this) {
            if (threadGroup == null) {
                threadGroup = new ThreadGroup("onos-generator");
            }
        }
        producer = new Thread(threadGroup, () -> {
            try {
                itemRequested.await();
                Generator.this.run();
            } catch (InterruptedException e) {
                // Remaining steps in run() will shut down thread.
            } catch (RuntimeException e) {
                exceptionRaisedByProducer = e;
            }
            hasFinished = true;
            itemAvailableOrHasFinished.set();
        });
        producer.setDaemon(true);
        producer.start();
    }
}
