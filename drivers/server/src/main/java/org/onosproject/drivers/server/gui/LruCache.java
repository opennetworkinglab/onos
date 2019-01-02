/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.server.gui;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Data structure that implements Least Recently Used (LRU) policy.
 */
public class LruCache<T> extends LinkedHashMap<Integer, T> {
    private static final Logger log = getLogger(LruCache.class);

    // After this size, LRU is applied
    private final int maxEntries;
    private static final int DEFAULT_INITIAL_CAPACITY = 5;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    public LruCache(int initialCapacity,
                    float loadFactor,
                    int maxEntries) {
        super(initialCapacity, loadFactor, true);
        this.maxEntries = maxEntries;
    }

    public LruCache(int initialCapacity, int maxEntries) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, maxEntries);
    }

    public LruCache(int maxEntries) {
        this(DEFAULT_INITIAL_CAPACITY, maxEntries);
    }

    @Override
    protected synchronized boolean removeEldestEntry(
            Map.Entry<Integer, T> eldest) {
        // Remove the oldest element when size limit is reached
        return size() > maxEntries;
    }

    /**
     * Adds a new entry to the LRU.
     *
     * @param newValue the value to be added
     */
    public synchronized void add(T newValue) {
        this.put(this.getNextKey(), newValue);
    }

    /**
     * Returns the first (eldest) key of this LRU cache.
     *
     * @return first (eldest) key of this LRU cache
     */
    public synchronized Integer getFirstKey() {
        return this.keySet().iterator().next();
    }

    /**
     * Returns the last (newest) key of this LRU cache.
     *
     * @return last (newest) key of this LRU cache
     */
    public synchronized Integer getLastKey() {
        Integer out = null;
        for (Integer key : this.keySet()) {
            out = key;
        }

        return out;
    }

    /**
     * Returns the first (eldest) value of this LRU cache.
     *
     * @return first (eldest) value of this LRU cache
     */
    public synchronized T getFirstValue() {
        // Get all keys sorted
        SortedSet<Integer> keys =
            new ConcurrentSkipListSet<Integer>(this.keySet());

        // Return the value that corresponds to the first key
        return this.get(keys.first());
    }

    /**
     * Returns the last (newest) value of this LRU cache.
     *
     * @return last (newest) value of this LRU cache
     */
    public synchronized T getLastValue() {
        // Get all keys sorted
        SortedSet<Integer> keys =
            new ConcurrentSkipListSet<Integer>(this.keySet());

        // Return the value that corresponds to the last key
        return this.get(keys.last());
    }

    /**
     * Returns the first (oldest) values of this LRU cache.
     * The number is denoted by the argument.
     *
     * @param numberOfEntries the number of entries to include in the list
     * @return list of first (oldest) values of this LRU cache
     */
    public synchronized List<T> getFirstValues(int numberOfEntries) {
        List<T> outList = new ArrayList<T>();

        if (numberOfEntries <= 0) {
            return outList;
        }

        // Get all keys sorted
        SortedSet<Integer> keys =
            new ConcurrentSkipListSet<Integer>(this.keySet());

        int i = 0;

        // Iterate the sorted keys
        for (Integer k : keys) {
            // Pick up the first 'numberOfEntries' entries
            if (i >= numberOfEntries) {
                break;
            }

            outList.add(this.get(k));
            i++;
        }

        return outList;
    }

    /**
     * Returns the last (newest) values of this LRU cache.
     * The number is denoted by the argument.
     *
     * @param numberOfEntries the number of entries to include in the list
     * @return list of last (newest) values of this LRU cache
     */
    public synchronized List<T> getLastValues(int numberOfEntries) {
        List<T> outList = new ArrayList<T>();

        if (numberOfEntries <= 0) {
            return outList;
        }

        // Get all keys sorted
        NavigableSet<Integer> keys =
            new ConcurrentSkipListSet<Integer>(this.keySet());

        int i = 0;

        // Iterate the sorted keys backwards
        for (Integer k : keys.descendingSet()) {
            // Pick up the last 'numberOfEntries' entries
            if (i >= numberOfEntries) {
                break;
            }

            outList.add(this.get(k));
            i++;
        }

        return outList;
    }

    /**
     * Returns the next position to store data.
     *
     * @return next key to store data
     */
    private synchronized Integer getNextKey() {
        // The oldest will be the next..
        if (this.size() == maxEntries) {
            return this.getFirstKey();
        }

        Integer lastKey = this.getLastKey();
        // First insertion
        if (lastKey == null) {
            return new Integer(0);
        }

        // Regular next key insertion
        return new Integer(lastKey.intValue() + 1);
    }

}
